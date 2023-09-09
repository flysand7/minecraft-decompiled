package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundClearTitlesPacket implements Packet<ClientGamePacketListener> {
   private final boolean resetTimes;

   public ClientboundClearTitlesPacket(boolean var1) {
      this.resetTimes = var1;
   }

   public ClientboundClearTitlesPacket(FriendlyByteBuf var1) {
      this.resetTimes = var1.readBoolean();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeBoolean(this.resetTimes);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleTitlesClear(this);
   }

   public boolean shouldResetTimes() {
      return this.resetTimes;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
