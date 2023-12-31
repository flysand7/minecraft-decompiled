package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderWarningDistancePacket implements Packet<ClientGamePacketListener> {
   private final int warningBlocks;

   public ClientboundSetBorderWarningDistancePacket(WorldBorder var1) {
      this.warningBlocks = var1.getWarningBlocks();
   }

   public ClientboundSetBorderWarningDistancePacket(FriendlyByteBuf var1) {
      this.warningBlocks = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.warningBlocks);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSetBorderWarningDistance(this);
   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
