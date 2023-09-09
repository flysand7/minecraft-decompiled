package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ClientboundDamageEventPacket(int a, int b, int c, int d, Optional<Vec3> e) implements Packet<ClientGamePacketListener> {
   private final int entityId;
   private final int sourceTypeId;
   private final int sourceCauseId;
   private final int sourceDirectId;
   private final Optional<Vec3> sourcePosition;

   public ClientboundDamageEventPacket(Entity var1, DamageSource var2) {
      this(var1.getId(), var1.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getId(var2.type()), var2.getEntity() != null ? var2.getEntity().getId() : -1, var2.getDirectEntity() != null ? var2.getDirectEntity().getId() : -1, Optional.ofNullable(var2.sourcePositionRaw()));
   }

   public ClientboundDamageEventPacket(FriendlyByteBuf var1) {
      this(var1.readVarInt(), var1.readVarInt(), readOptionalEntityId(var1), readOptionalEntityId(var1), var1.readOptional((var0) -> {
         return new Vec3(var0.readDouble(), var0.readDouble(), var0.readDouble());
      }));
   }

   public ClientboundDamageEventPacket(int var1, int var2, int var3, int var4, Optional<Vec3> var5) {
      this.entityId = var1;
      this.sourceTypeId = var2;
      this.sourceCauseId = var3;
      this.sourceDirectId = var4;
      this.sourcePosition = var5;
   }

   private static void writeOptionalEntityId(FriendlyByteBuf var0, int var1) {
      var0.writeVarInt(var1 + 1);
   }

   private static int readOptionalEntityId(FriendlyByteBuf var0) {
      return var0.readVarInt() - 1;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.entityId);
      var1.writeVarInt(this.sourceTypeId);
      writeOptionalEntityId(var1, this.sourceCauseId);
      writeOptionalEntityId(var1, this.sourceDirectId);
      var1.writeOptional(this.sourcePosition, (var0, var1x) -> {
         var0.writeDouble(var1x.x());
         var0.writeDouble(var1x.y());
         var0.writeDouble(var1x.z());
      });
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleDamageEvent(this);
   }

   public DamageSource getSource(Level var1) {
      Holder var2 = (Holder)var1.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(this.sourceTypeId).get();
      if (this.sourcePosition.isPresent()) {
         return new DamageSource(var2, (Vec3)this.sourcePosition.get());
      } else {
         Entity var3 = var1.getEntity(this.sourceCauseId);
         Entity var4 = var1.getEntity(this.sourceDirectId);
         return new DamageSource(var2, var4, var3);
      }
   }

   public int entityId() {
      return this.entityId;
   }

   public int sourceTypeId() {
      return this.sourceTypeId;
   }

   public int sourceCauseId() {
      return this.sourceCauseId;
   }

   public int sourceDirectId() {
      return this.sourceDirectId;
   }

   public Optional<Vec3> sourcePosition() {
      return this.sourcePosition;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
