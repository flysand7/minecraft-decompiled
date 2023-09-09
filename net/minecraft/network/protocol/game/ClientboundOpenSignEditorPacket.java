package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundOpenSignEditorPacket implements Packet<ClientGamePacketListener> {
   private final BlockPos pos;
   private final boolean isFrontText;

   public ClientboundOpenSignEditorPacket(BlockPos var1, boolean var2) {
      this.pos = var1;
      this.isFrontText = var2;
   }

   public ClientboundOpenSignEditorPacket(FriendlyByteBuf var1) {
      this.pos = var1.readBlockPos();
      this.isFrontText = var1.readBoolean();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeBlockPos(this.pos);
      var1.writeBoolean(this.isFrontText);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleOpenSignEditor(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public boolean isFrontText() {
      return this.isFrontText;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
