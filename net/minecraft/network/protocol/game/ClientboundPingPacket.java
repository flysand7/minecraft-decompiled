package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundPingPacket implements Packet<ClientGamePacketListener> {
   private final int id;

   public ClientboundPingPacket(int var1) {
      this.id = var1;
   }

   public ClientboundPingPacket(FriendlyByteBuf var1) {
      this.id = var1.readInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeInt(this.id);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePing(this);
   }

   public int getId() {
      return this.id;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
