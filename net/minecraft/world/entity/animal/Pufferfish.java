package net.minecraft.world.entity.animal;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class Pufferfish extends AbstractFish {
   private static final EntityDataAccessor<Integer> PUFF_STATE;
   int inflateCounter;
   int deflateTimer;
   private static final Predicate<LivingEntity> SCARY_MOB;
   static final TargetingConditions targetingConditions;
   public static final int STATE_SMALL = 0;
   public static final int STATE_MID = 1;
   public static final int STATE_FULL = 2;

   public Pufferfish(EntityType<? extends Pufferfish> var1, Level var2) {
      super(var1, var2);
      this.refreshDimensions();
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(PUFF_STATE, 0);
   }

   public int getPuffState() {
      return (Integer)this.entityData.get(PUFF_STATE);
   }

   public void setPuffState(int var1) {
      this.entityData.set(PUFF_STATE, var1);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      if (PUFF_STATE.equals(var1)) {
         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(var1);
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("PuffState", this.getPuffState());
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setPuffState(Math.min(var1.getInt("PuffState"), 2));
   }

   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.PUFFERFISH_BUCKET);
   }

   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(1, new Pufferfish.PufferfishPuffGoal(this));
   }

   public void tick() {
      if (!this.level().isClientSide && this.isAlive() && this.isEffectiveAi()) {
         if (this.inflateCounter > 0) {
            if (this.getPuffState() == 0) {
               this.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getVoicePitch());
               this.setPuffState(1);
            } else if (this.inflateCounter > 40 && this.getPuffState() == 1) {
               this.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, this.getSoundVolume(), this.getVoicePitch());
               this.setPuffState(2);
            }

            ++this.inflateCounter;
         } else if (this.getPuffState() != 0) {
            if (this.deflateTimer > 60 && this.getPuffState() == 2) {
               this.playSound(SoundEvents.PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getVoicePitch());
               this.setPuffState(1);
            } else if (this.deflateTimer > 100 && this.getPuffState() == 1) {
               this.playSound(SoundEvents.PUFFER_FISH_BLOW_OUT, this.getSoundVolume(), this.getVoicePitch());
               this.setPuffState(0);
            }

            ++this.deflateTimer;
         }
      }

      super.tick();
   }

   public void aiStep() {
      super.aiStep();
      if (this.isAlive() && this.getPuffState() > 0) {
         List var1 = this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(0.3D), (var1x) -> {
            return targetingConditions.test(this, var1x);
         });
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            Mob var3 = (Mob)var2.next();
            if (var3.isAlive()) {
               this.touch(var3);
            }
         }
      }

   }

   private void touch(Mob var1) {
      int var2 = this.getPuffState();
      if (var1.hurt(this.damageSources().mobAttack(this), (float)(1 + var2))) {
         var1.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * var2, 0), this);
         this.playSound(SoundEvents.PUFFER_FISH_STING, 1.0F, 1.0F);
      }

   }

   public void playerTouch(Player var1) {
      int var2 = this.getPuffState();
      if (var1 instanceof ServerPlayer && var2 > 0 && var1.hurt(this.damageSources().mobAttack(this), (float)(1 + var2))) {
         if (!this.isSilent()) {
            ((ServerPlayer)var1).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.PUFFER_FISH_STING, 0.0F));
         }

         var1.addEffect(new MobEffectInstance(MobEffects.POISON, 60 * var2, 0), this);
      }

   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.PUFFER_FISH_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PUFFER_FISH_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.PUFFER_FISH_HURT;
   }

   protected SoundEvent getFlopSound() {
      return SoundEvents.PUFFER_FISH_FLOP;
   }

   public EntityDimensions getDimensions(Pose var1) {
      return super.getDimensions(var1).scale(getScale(this.getPuffState()));
   }

   private static float getScale(int var0) {
      switch(var0) {
      case 0:
         return 0.5F;
      case 1:
         return 0.7F;
      default:
         return 1.0F;
      }
   }

   static {
      PUFF_STATE = SynchedEntityData.defineId(Pufferfish.class, EntityDataSerializers.INT);
      SCARY_MOB = (var0) -> {
         if (var0 instanceof Player && ((Player)var0).isCreative()) {
            return false;
         } else {
            return var0.getType() == EntityType.AXOLOTL || var0.getMobType() != MobType.WATER;
         }
      };
      targetingConditions = TargetingConditions.forNonCombat().ignoreInvisibilityTesting().ignoreLineOfSight().selector(SCARY_MOB);
   }

   private static class PufferfishPuffGoal extends Goal {
      private final Pufferfish fish;

      public PufferfishPuffGoal(Pufferfish var1) {
         this.fish = var1;
      }

      public boolean canUse() {
         List var1 = this.fish.level().getEntitiesOfClass(LivingEntity.class, this.fish.getBoundingBox().inflate(2.0D), (var1x) -> {
            return Pufferfish.targetingConditions.test(this.fish, var1x);
         });
         return !var1.isEmpty();
      }

      public void start() {
         this.fish.inflateCounter = 1;
         this.fish.deflateTimer = 0;
      }

      public void stop() {
         this.fish.inflateCounter = 0;
      }
   }
}
