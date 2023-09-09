package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetDefaultSpawnPositionPacket implements Packet<ClientGamePacketListener> {
   private final BlockPos pos;
   private final float angle;

   public ClientboundSetDefaultSpawnPositionPacket(BlockPos var1, float var2) {
      this.pos = var1;
      this.angle = var2;
   }

   public ClientboundSetDefaultSpawnPositionPacket(FriendlyByteBuf var1) {
      this.pos = var1.readBlockPos();
      this.angle = var1.readFloat();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeBlockPos(this.pos);
      var1.writeFloat(this.angle);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSetSpawn(this);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public float getAngle() {
      return this.angle;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
