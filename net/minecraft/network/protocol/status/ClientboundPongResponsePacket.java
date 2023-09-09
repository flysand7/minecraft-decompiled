package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundPongResponsePacket implements Packet<ClientStatusPacketListener> {
   private final long time;

   public ClientboundPongResponsePacket(long var1) {
      this.time = var1;
   }

   public ClientboundPongResponsePacket(FriendlyByteBuf var1) {
      this.time = var1.readLong();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeLong(this.time);
   }

   public void handle(ClientStatusPacketListener var1) {
      var1.handlePongResponse(this);
   }

   public long getTime() {
      return this.time;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientStatusPacketListener)var1);
   }
}
