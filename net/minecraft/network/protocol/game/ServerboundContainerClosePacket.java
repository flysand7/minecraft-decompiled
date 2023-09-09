package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundContainerClosePacket implements Packet<ServerGamePacketListener> {
   private final int containerId;

   public ServerboundContainerClosePacket(int var1) {
      this.containerId = var1;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleContainerClose(this);
   }

   public ServerboundContainerClosePacket(FriendlyByteBuf var1) {
      this.containerId = var1.readByte();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByte(this.containerId);
   }

   public int getContainerId() {
      return this.containerId;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
