package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundGameProfilePacket implements Packet<ClientLoginPacketListener> {
   private final GameProfile gameProfile;

   public ClientboundGameProfilePacket(GameProfile var1) {
      this.gameProfile = var1;
   }

   public ClientboundGameProfilePacket(FriendlyByteBuf var1) {
      this.gameProfile = var1.readGameProfile();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeGameProfile(this.gameProfile);
   }

   public void handle(ClientLoginPacketListener var1) {
      var1.handleGameProfile(this);
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientLoginPacketListener)var1);
   }
}
