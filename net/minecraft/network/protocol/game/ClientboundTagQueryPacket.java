package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundTagQueryPacket implements Packet<ClientGamePacketListener> {
   private final int transactionId;
   @Nullable
   private final CompoundTag tag;

   public ClientboundTagQueryPacket(int var1, @Nullable CompoundTag var2) {
      this.transactionId = var1;
      this.tag = var2;
   }

   public ClientboundTagQueryPacket(FriendlyByteBuf var1) {
      this.transactionId = var1.readVarInt();
      this.tag = var1.readNbt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.transactionId);
      var1.writeNbt(this.tag);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleTagQueryPacket(this);
   }

   public int getTransactionId() {
      return this.transactionId;
   }

   @Nullable
   public CompoundTag getTag() {
      return this.tag;
   }

   public boolean isSkippable() {
      return true;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
