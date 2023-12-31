package net.minecraft.world.entity.ai.behavior.warden;

import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

public class SonicBoom extends Behavior<Warden> {
   private static final int DISTANCE_XZ = 15;
   private static final int DISTANCE_Y = 20;
   private static final double KNOCKBACK_VERTICAL = 0.5D;
   private static final double KNOCKBACK_HORIZONTAL = 2.5D;
   public static final int COOLDOWN = 40;
   private static final int TICKS_BEFORE_PLAYING_SOUND = Mth.ceil(34.0D);
   private static final int DURATION = Mth.ceil(60.0F);

   public SonicBoom() {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.SONIC_BOOM_COOLDOWN, MemoryStatus.VALUE_ABSENT, MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, MemoryStatus.REGISTERED, MemoryModuleType.SONIC_BOOM_SOUND_DELAY, MemoryStatus.REGISTERED), DURATION);
   }

   protected boolean checkExtraStartConditions(ServerLevel var1, Warden var2) {
      return var2.closerThan((Entity)var2.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).get(), 15.0D, 20.0D);
   }

   protected boolean canStillUse(ServerLevel var1, Warden var2, long var3) {
      return true;
   }

   protected void start(ServerLevel var1, Warden var2, long var3) {
      var2.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_COOLING_DOWN, true, (long)DURATION);
      var2.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_DELAY, Unit.INSTANCE, (long)TICKS_BEFORE_PLAYING_SOUND);
      var1.broadcastEntityEvent(var2, (byte)62);
      var2.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
   }

   protected void tick(ServerLevel var1, Warden var2, long var3) {
      var2.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((var1x) -> {
         var2.getLookControl().setLookAt(var1x.position());
      });
      if (!var2.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_DELAY) && !var2.getBrain().hasMemoryValue(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN)) {
         var2.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_SOUND_COOLDOWN, Unit.INSTANCE, (long)(DURATION - TICKS_BEFORE_PLAYING_SOUND));
         Optional var10000 = var2.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
         Objects.requireNonNull(var2);
         var10000.filter(var2::canTargetEntity).filter((var1x) -> {
            return var2.closerThan(var1x, 15.0D, 20.0D);
         }).ifPresent((var2x) -> {
            Vec3 var3 = var2.position().add(0.0D, 1.600000023841858D, 0.0D);
            Vec3 var4 = var2x.getEyePosition().subtract(var3);
            Vec3 var5 = var4.normalize();

            for(int var6 = 1; var6 < Mth.floor(var4.length()) + 7; ++var6) {
               Vec3 var7 = var3.add(var5.scale((double)var6));
               var1.sendParticles(ParticleTypes.SONIC_BOOM, var7.x, var7.y, var7.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }

            var2.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
            var2x.hurt(var1.damageSources().sonicBoom(var2), 10.0F);
            double var10 = 0.5D * (1.0D - var2x.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            double var8 = 2.5D * (1.0D - var2x.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            var2x.push(var5.x() * var8, var5.y() * var10, var5.z() * var8);
         });
      }
   }

   protected void stop(ServerLevel var1, Warden var2, long var3) {
      setCooldown(var2, 40);
   }

   public static void setCooldown(LivingEntity var0, int var1) {
      var0.getBrain().setMemoryWithExpiry(MemoryModuleType.SONIC_BOOM_COOLDOWN, Unit.INSTANCE, (long)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected boolean checkExtraStartConditions(ServerLevel var1, LivingEntity var2) {
      return this.checkExtraStartConditions(var1, (Warden)var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected boolean canStillUse(ServerLevel var1, LivingEntity var2, long var3) {
      return this.canStillUse(var1, (Warden)var2, var3);
   }

   // $FF: synthetic method
   protected void stop(ServerLevel var1, LivingEntity var2, long var3) {
      this.stop(var1, (Warden)var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void tick(ServerLevel var1, LivingEntity var2, long var3) {
      this.tick(var1, (Warden)var2, var3);
   }

   // $FF: synthetic method
   protected void start(ServerLevel var1, LivingEntity var2, long var3) {
      this.start(var1, (Warden)var2, var3);
   }
}
