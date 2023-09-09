package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public class PacketBundleUnpacker extends MessageToMessageEncoder<Packet<?>> {
   private final PacketFlow flow;

   public PacketBundleUnpacker(PacketFlow var1) {
      this.flow = var1;
   }

   protected void encode(ChannelHandlerContext var1, Packet<?> var2, List<Object> var3) throws Exception {
      BundlerInfo.Provider var4 = (BundlerInfo.Provider)var1.channel().attr(BundlerInfo.BUNDLER_PROVIDER).get();
      if (var4 == null) {
         throw new EncoderException("Bundler not configured: " + var2);
      } else {
         BundlerInfo var10000 = var4.getBundlerInfo(this.flow);
         Objects.requireNonNull(var3);
         var10000.unbundlePacket(var2, var3::add);
      }
   }

   // $FF: synthetic method
   protected void encode(ChannelHandlerContext var1, Object var2, List var3) throws Exception {
      this.encode(var1, (Packet)var2, var3);
   }
}
