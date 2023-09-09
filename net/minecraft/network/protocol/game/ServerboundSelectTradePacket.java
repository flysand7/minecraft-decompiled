package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundSelectTradePacket implements Packet<ServerGamePacketListener> {
   private final int item;

   public ServerboundSelectTradePacket(int var1) {
      this.item = var1;
   }

   public ServerboundSelectTradePacket(FriendlyByteBuf var1) {
      this.item = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.item);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleSelectTrade(this);
   }

   public int getItem() {
      return this.item;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
