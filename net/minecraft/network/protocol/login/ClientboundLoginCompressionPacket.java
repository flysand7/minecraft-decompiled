package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundLoginCompressionPacket implements Packet<ClientLoginPacketListener> {
   private final int compressionThreshold;

   public ClientboundLoginCompressionPacket(int var1) {
      this.compressionThreshold = var1;
   }

   public ClientboundLoginCompressionPacket(FriendlyByteBuf var1) {
      this.compressionThreshold = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.compressionThreshold);
   }

   public void handle(ClientLoginPacketListener var1) {
      var1.handleCompression(this);
   }

   public int getCompressionThreshold() {
      return this.compressionThreshold;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientLoginPacketListener)var1);
   }
}
