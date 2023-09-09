package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.LivingEntity;

public record ClientboundHurtAnimationPacket(int a, float b) implements Packet<ClientGamePacketListener> {
   private final int id;
   private final float yaw;

   public ClientboundHurtAnimationPacket(LivingEntity var1) {
      this(var1.getId(), var1.getHurtDir());
   }

   public ClientboundHurtAnimationPacket(FriendlyByteBuf var1) {
      this(var1.readVarInt(), var1.readFloat());
   }

   public ClientboundHurtAnimationPacket(int var1, float var2) {
      this.id = var1;
      this.yaw = var2;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.id);
      var1.writeFloat(this.yaw);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleHurtAnimation(this);
   }

   public int id() {
      return this.id;
   }

   public float yaw() {
      return this.yaw;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
