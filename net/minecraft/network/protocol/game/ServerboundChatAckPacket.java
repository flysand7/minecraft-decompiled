package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatAckPacket(int a) implements Packet<ServerGamePacketListener> {
   private final int offset;

   public ServerboundChatAckPacket(FriendlyByteBuf var1) {
      this(var1.readVarInt());
   }

   public ServerboundChatAckPacket(int var1) {
      this.offset = var1;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.offset);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleChatAck(this);
   }

   public int offset() {
      return this.offset;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
