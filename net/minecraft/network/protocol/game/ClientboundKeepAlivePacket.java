package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundKeepAlivePacket implements Packet<ClientGamePacketListener> {
   private final long id;

   public ClientboundKeepAlivePacket(long var1) {
      this.id = var1;
   }

   public ClientboundKeepAlivePacket(FriendlyByteBuf var1) {
      this.id = var1.readLong();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeLong(this.id);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleKeepAlive(this);
   }

   public long getId() {
      return this.id;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
