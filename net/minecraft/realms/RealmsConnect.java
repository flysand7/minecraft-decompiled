package net.minecraft.realms;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.dto.RealmsServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.chat.report.ReportEnvironment;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.quickplay.QuickPlayLog;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import org.slf4j.Logger;

public class RealmsConnect {
   static final Logger LOGGER = LogUtils.getLogger();
   final Screen onlineScreen;
   volatile boolean aborted;
   @Nullable
   Connection connection;

   public RealmsConnect(Screen var1) {
      this.onlineScreen = var1;
   }

   public void connect(final RealmsServer var1, ServerAddress var2) {
      final Minecraft var3 = Minecraft.getInstance();
      var3.setConnectedToRealms(true);
      var3.prepareForMultiplayer();
      var3.getNarrator().sayNow((Component)Component.translatable("mco.connect.success"));
      final String var4 = var2.getHost();
      final int var5 = var2.getPort();
      (new Thread("Realms-connect-task") {
         public void run() {
            InetSocketAddress var1x = null;

            String var3x;
            try {
               var1x = new InetSocketAddress(var4, var5);
               if (RealmsConnect.this.aborted) {
                  return;
               }

               RealmsConnect.this.connection = Connection.connectToServer(var1x, var3.options.useNativeTransport());
               if (RealmsConnect.this.aborted) {
                  return;
               }

               ClientHandshakePacketListenerImpl var2 = new ClientHandshakePacketListenerImpl(RealmsConnect.this.connection, var3, var1.toServerData(var4), RealmsConnect.this.onlineScreen, false, (Duration)null, (var0) -> {
               });
               if (var1.worldType == RealmsServer.WorldType.MINIGAME) {
                  var2.setMinigameName(var1.minigameName);
               }

               RealmsConnect.this.connection.setListener(var2);
               if (RealmsConnect.this.aborted) {
                  return;
               }

               RealmsConnect.this.connection.send(new ClientIntentionPacket(var4, var5, ConnectionProtocol.LOGIN));
               if (RealmsConnect.this.aborted) {
                  return;
               }

               var3x = var3.getUser().getName();
               UUID var7 = var3.getUser().getProfileId();
               RealmsConnect.this.connection.send(new ServerboundHelloPacket(var3x, Optional.ofNullable(var7)));
               var3.updateReportEnvironment(ReportEnvironment.realm(var1));
               var3.quickPlayLog().setWorldData(QuickPlayLog.Type.REALMS, String.valueOf(var1.id), var1.name);
            } catch (Exception var5x) {
               var3.getDownloadedPackSource().clearServerPack();
               if (RealmsConnect.this.aborted) {
                  return;
               }

               RealmsConnect.LOGGER.error("Couldn't connect to world", var5x);
               var3x = var5x.toString();
               if (var1x != null) {
                  String var4x = var1x + ":" + var5;
                  var3x = var3x.replaceAll(var4x, "");
               }

               DisconnectedRealmsScreen var6 = new DisconnectedRealmsScreen(RealmsConnect.this.onlineScreen, CommonComponents.CONNECT_FAILED, Component.translatable("disconnect.genericReason", var3x));
               var3.execute(() -> {
                  var3.setScreen(var6);
               });
            }

         }
      }).start();
   }

   public void abort() {
      this.aborted = true;
      if (this.connection != null && this.connection.isConnected()) {
         this.connection.disconnect(Component.translatable("disconnect.genericReason"));
         this.connection.handleDisconnection();
      }

   }

   public void tick() {
      if (this.connection != null) {
         if (this.connection.isConnected()) {
            this.connection.tick();
         } else {
            this.connection.handleDisconnection();
         }
      }

   }
}
