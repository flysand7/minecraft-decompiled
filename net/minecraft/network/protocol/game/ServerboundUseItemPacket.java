package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
   private final InteractionHand hand;
   private final int sequence;

   public ServerboundUseItemPacket(InteractionHand var1, int var2) {
      this.hand = var1;
      this.sequence = var2;
   }

   public ServerboundUseItemPacket(FriendlyByteBuf var1) {
      this.hand = (InteractionHand)var1.readEnum(InteractionHand.class);
      this.sequence = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeEnum(this.hand);
      var1.writeVarInt(this.sequence);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleUseItem(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public int getSequence() {
      return this.sequence;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
