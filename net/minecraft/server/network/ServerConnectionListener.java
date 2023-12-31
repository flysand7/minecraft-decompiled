package net.minecraft.server.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RateKickingConnection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.LazyLoadedValue;
import org.slf4j.Logger;

public class ServerConnectionListener {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final LazyLoadedValue<NioEventLoopGroup> SERVER_EVENT_GROUP = new LazyLoadedValue(() -> {
      return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
   });
   public static final LazyLoadedValue<EpollEventLoopGroup> SERVER_EPOLL_EVENT_GROUP = new LazyLoadedValue(() -> {
      return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
   });
   final MinecraftServer server;
   public volatile boolean running;
   private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
   final List<Connection> connections = Collections.synchronizedList(Lists.newArrayList());

   public ServerConnectionListener(MinecraftServer var1) {
      this.server = var1;
      this.running = true;
   }

   public void startTcpServerListener(@Nullable InetAddress var1, int var2) throws IOException {
      synchronized(this.channels) {
         Class var4;
         LazyLoadedValue var5;
         if (Epoll.isAvailable() && this.server.isEpollEnabled()) {
            var4 = EpollServerSocketChannel.class;
            var5 = SERVER_EPOLL_EVENT_GROUP;
            LOGGER.info("Using epoll channel type");
         } else {
            var4 = NioServerSocketChannel.class;
            var5 = SERVER_EVENT_GROUP;
            LOGGER.info("Using default channel type");
         }

         this.channels.add(((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(var4)).childHandler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel var1) {
               try {
                  var1.config().setOption(ChannelOption.TCP_NODELAY, true);
               } catch (ChannelException var5) {
               }

               ChannelPipeline var2 = var1.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("legacy_query", new LegacyQueryHandler(ServerConnectionListener.this));
               Connection.configureSerialization(var2, PacketFlow.SERVERBOUND);
               int var3 = ServerConnectionListener.this.server.getRateLimitPacketsPerSecond();
               Object var4 = var3 > 0 ? new RateKickingConnection(var3) : new Connection(PacketFlow.SERVERBOUND);
               ServerConnectionListener.this.connections.add(var4);
               var2.addLast("packet_handler", (ChannelHandler)var4);
               ((Connection)var4).setListener(new ServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, (Connection)var4));
            }
         }).group((EventLoopGroup)var5.get()).localAddress(var1, var2)).bind().syncUninterruptibly());
      }
   }

   public SocketAddress startMemoryChannel() {
      ChannelFuture var1;
      synchronized(this.channels) {
         var1 = ((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(LocalServerChannel.class)).childHandler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel var1) {
               Connection var2 = new Connection(PacketFlow.SERVERBOUND);
               var2.setListener(new MemoryServerHandshakePacketListenerImpl(ServerConnectionListener.this.server, var2));
               ServerConnectionListener.this.connections.add(var2);
               ChannelPipeline var3 = var1.pipeline();
               var3.addLast("packet_handler", var2);
            }
         }).group((EventLoopGroup)SERVER_EVENT_GROUP.get()).localAddress(LocalAddress.ANY)).bind().syncUninterruptibly();
         this.channels.add(var1);
      }

      return var1.channel().localAddress();
   }

   public void stop() {
      this.running = false;
      Iterator var1 = this.channels.iterator();

      while(var1.hasNext()) {
         ChannelFuture var2 = (ChannelFuture)var1.next();

         try {
            var2.channel().close().sync();
         } catch (InterruptedException var4) {
            LOGGER.error("Interrupted whilst closing channel");
         }
      }

   }

   public void tick() {
      synchronized(this.connections) {
         Iterator var2 = this.connections.iterator();

         while(true) {
            while(true) {
               Connection var3;
               do {
                  if (!var2.hasNext()) {
                     return;
                  }

                  var3 = (Connection)var2.next();
               } while(var3.isConnecting());

               if (var3.isConnected()) {
                  try {
                     var3.tick();
                  } catch (Exception var7) {
                     if (var3.isMemoryConnection()) {
                        throw new ReportedException(CrashReport.forThrowable(var7, "Ticking memory connection"));
                     }

                     LOGGER.warn("Failed to handle packet for {}", var3.getRemoteAddress(), var7);
                     MutableComponent var5 = Component.literal("Internal server error");
                     var3.send(new ClientboundDisconnectPacket(var5), PacketSendListener.thenRun(() -> {
                        var3.disconnect(var5);
                     }));
                     var3.setReadOnly();
                  }
               } else {
                  var2.remove();
                  var3.handleDisconnection();
               }
            }
         }
      }
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public List<Connection> getConnections() {
      return this.connections;
   }

   private static class LatencySimulator extends ChannelInboundHandlerAdapter {
      private static final Timer TIMER = new HashedWheelTimer();
      private final int delay;
      private final int jitter;
      private final List<ServerConnectionListener.LatencySimulator.DelayedMessage> queuedMessages = Lists.newArrayList();

      public LatencySimulator(int var1, int var2) {
         this.delay = var1;
         this.jitter = var2;
      }

      public void channelRead(ChannelHandlerContext var1, Object var2) {
         this.delayDownstream(var1, var2);
      }

      private void delayDownstream(ChannelHandlerContext var1, Object var2) {
         int var3 = this.delay + (int)(Math.random() * (double)this.jitter);
         this.queuedMessages.add(new ServerConnectionListener.LatencySimulator.DelayedMessage(var1, var2));
         TIMER.newTimeout(this::onTimeout, (long)var3, TimeUnit.MILLISECONDS);
      }

      private void onTimeout(Timeout var1) {
         ServerConnectionListener.LatencySimulator.DelayedMessage var2 = (ServerConnectionListener.LatencySimulator.DelayedMessage)this.queuedMessages.remove(0);
         var2.ctx.fireChannelRead(var2.msg);
      }

      private static class DelayedMessage {
         public final ChannelHandlerContext ctx;
         public final Object msg;

         public DelayedMessage(ChannelHandlerContext var1, Object var2) {
            this.ctx = var1;
            this.msg = var2;
         }
      }
   }
}
