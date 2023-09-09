package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetChunkCacheRadiusPacket implements Packet<ClientGamePacketListener> {
   private final int radius;

   public ClientboundSetChunkCacheRadiusPacket(int var1) {
      this.radius = var1;
   }

   public ClientboundSetChunkCacheRadiusPacket(FriendlyByteBuf var1) {
      this.radius = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.radius);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSetChunkCacheRadius(this);
   }

   public int getRadius() {
      return this.radius;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
