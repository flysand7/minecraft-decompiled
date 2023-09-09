package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundLockDifficultyPacket implements Packet<ServerGamePacketListener> {
   private final boolean locked;

   public ServerboundLockDifficultyPacket(boolean var1) {
      this.locked = var1;
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleLockDifficulty(this);
   }

   public ServerboundLockDifficultyPacket(FriendlyByteBuf var1) {
      this.locked = var1.readBoolean();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeBoolean(this.locked);
   }

   public boolean isLocked() {
      return this.locked;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
