package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class Blaze extends Monster {
   private float allowedHeightOffset = 0.5F;
   private int nextHeightOffsetChangeTick;
   private static final EntityDataAccessor<Byte> DATA_FLAGS_ID;

   public Blaze(EntityType<? extends Blaze> var1, Level var2) {
      super(var1, var2);
      this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
      this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
      this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
      this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
      this.xpReward = 10;
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(4, new Blaze.BlazeAttackGoal(this));
      this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
      this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D, 0.0F));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[0])).setAlertOthers());
      this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.23000000417232513D).add(Attributes.FOLLOW_RANGE, 48.0D);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_FLAGS_ID, (byte)0);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.BLAZE_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.BLAZE_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.BLAZE_DEATH;
   }

   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   public void aiStep() {
      if (!this.onGround() && this.getDeltaMovement().y < 0.0D) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D));
      }

      if (this.level().isClientSide) {
         if (this.random.nextInt(24) == 0 && !this.isSilent()) {
            this.level().playLocalSound(this.getX() + 0.5D, this.getY() + 0.5D, this.getZ() + 0.5D, SoundEvents.BLAZE_BURN, this.getSoundSource(), 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F, false);
         }

         for(int var1 = 0; var1 < 2; ++var1) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);
         }
      }

      super.aiStep();
   }

   public boolean isSensitiveToWater() {
      return true;
   }

   protected void customServerAiStep() {
      --this.nextHeightOffsetChangeTick;
      if (this.nextHeightOffsetChangeTick <= 0) {
         this.nextHeightOffsetChangeTick = 100;
         this.allowedHeightOffset = (float)this.random.triangle(0.5D, 6.891D);
      }

      LivingEntity var1 = this.getTarget();
      if (var1 != null && var1.getEyeY() > this.getEyeY() + (double)this.allowedHeightOffset && this.canAttack(var1)) {
         Vec3 var2 = this.getDeltaMovement();
         this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (0.30000001192092896D - var2.y) * 0.30000001192092896D, 0.0D));
         this.hasImpulse = true;
      }

      super.customServerAiStep();
   }

   public boolean isOnFire() {
      return this.isCharged();
   }

   private boolean isCharged() {
      return ((Byte)this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
   }

   void setCharged(boolean var1) {
      byte var2 = (Byte)this.entityData.get(DATA_FLAGS_ID);
      if (var1) {
         var2 = (byte)(var2 | 1);
      } else {
         var2 &= -2;
      }

      this.entityData.set(DATA_FLAGS_ID, var2);
   }

   static {
      DATA_FLAGS_ID = SynchedEntityData.defineId(Blaze.class, EntityDataSerializers.BYTE);
   }

   static class BlazeAttackGoal extends Goal {
      private final Blaze blaze;
      private int attackStep;
      private int attackTime;
      private int lastSeen;

      public BlazeAttackGoal(Blaze var1) {
         this.blaze = var1;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      public boolean canUse() {
         LivingEntity var1 = this.blaze.getTarget();
         return var1 != null && var1.isAlive() && this.blaze.canAttack(var1);
      }

      public void start() {
         this.attackStep = 0;
      }

      public void stop() {
         this.blaze.setCharged(false);
         this.lastSeen = 0;
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         --this.attackTime;
         LivingEntity var1 = this.blaze.getTarget();
         if (var1 != null) {
            boolean var2 = this.blaze.getSensing().hasLineOfSight(var1);
            if (var2) {
               this.lastSeen = 0;
            } else {
               ++this.lastSeen;
            }

            double var3 = this.blaze.distanceToSqr(var1);
            if (var3 < 4.0D) {
               if (!var2) {
                  return;
               }

               if (this.attackTime <= 0) {
                  this.attackTime = 20;
                  this.blaze.doHurtTarget(var1);
               }

               this.blaze.getMoveControl().setWantedPosition(var1.getX(), var1.getY(), var1.getZ(), 1.0D);
            } else if (var3 < this.getFollowDistance() * this.getFollowDistance() && var2) {
               double var5 = var1.getX() - this.blaze.getX();
               double var7 = var1.getY(0.5D) - this.blaze.getY(0.5D);
               double var9 = var1.getZ() - this.blaze.getZ();
               if (this.attackTime <= 0) {
                  ++this.attackStep;
                  if (this.attackStep == 1) {
                     this.attackTime = 60;
                     this.blaze.setCharged(true);
                  } else if (this.attackStep <= 4) {
                     this.attackTime = 6;
                  } else {
                     this.attackTime = 100;
                     this.attackStep = 0;
                     this.blaze.setCharged(false);
                  }

                  if (this.attackStep > 1) {
                     double var11 = Math.sqrt(Math.sqrt(var3)) * 0.5D;
                     if (!this.blaze.isSilent()) {
                        this.blaze.level().levelEvent((Player)null, 1018, this.blaze.blockPosition(), 0);
                     }

                     for(int var13 = 0; var13 < 1; ++var13) {
                        SmallFireball var14 = new SmallFireball(this.blaze.level(), this.blaze, this.blaze.getRandom().triangle(var5, 2.297D * var11), var7, this.blaze.getRandom().triangle(var9, 2.297D * var11));
                        var14.setPos(var14.getX(), this.blaze.getY(0.5D) + 0.5D, var14.getZ());
                        this.blaze.level().addFreshEntity(var14);
                     }
                  }
               }

               this.blaze.getLookControl().setLookAt(var1, 10.0F, 10.0F);
            } else if (this.lastSeen < 5) {
               this.blaze.getMoveControl().setWantedPosition(var1.getX(), var1.getY(), var1.getZ(), 1.0D);
            }

            super.tick();
         }
      }

      private double getFollowDistance() {
         return this.blaze.getAttributeValue(Attributes.FOLLOW_RANGE);
      }
   }
}
