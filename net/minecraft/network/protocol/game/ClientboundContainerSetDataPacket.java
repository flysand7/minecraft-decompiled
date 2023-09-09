package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundContainerSetDataPacket implements Packet<ClientGamePacketListener> {
   private final int containerId;
   private final int id;
   private final int value;

   public ClientboundContainerSetDataPacket(int var1, int var2, int var3) {
      this.containerId = var1;
      this.id = var2;
      this.value = var3;
   }

   public ClientboundContainerSetDataPacket(FriendlyByteBuf var1) {
      this.containerId = var1.readUnsignedByte();
      this.id = var1.readShort();
      this.value = var1.readShort();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByte(this.containerId);
      var1.writeShort(this.id);
      var1.writeShort(this.value);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleContainerSetData(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public int getId() {
      return this.id;
   }

   public int getValue() {
      return this.value;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
