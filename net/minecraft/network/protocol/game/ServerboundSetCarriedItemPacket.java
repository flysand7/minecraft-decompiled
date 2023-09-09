package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundSetCarriedItemPacket implements Packet<ServerGamePacketListener> {
   private final int slot;

   public ServerboundSetCarriedItemPacket(int var1) {
      this.slot = var1;
   }

   public ServerboundSetCarriedItemPacket(FriendlyByteBuf var1) {
      this.slot = var1.readShort();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeShort(this.slot);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleSetCarriedItem(this);
   }

   public int getSlot() {
      return this.slot;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
