package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

public class Guardian extends Monster {
   protected static final int ATTACK_TIME = 80;
   private static final EntityDataAccessor<Boolean> DATA_ID_MOVING;
   private static final EntityDataAccessor<Integer> DATA_ID_ATTACK_TARGET;
   private float clientSideTailAnimation;
   private float clientSideTailAnimationO;
   private float clientSideTailAnimationSpeed;
   private float clientSideSpikesAnimation;
   private float clientSideSpikesAnimationO;
   @Nullable
   private LivingEntity clientSideCachedAttackTarget;
   private int clientSideAttackTime;
   private boolean clientSideTouchedGround;
   @Nullable
   protected RandomStrollGoal randomStrollGoal;

   public Guardian(EntityType<? extends Guardian> var1, Level var2) {
      super(var1, var2);
      this.xpReward = 10;
      this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
      this.moveControl = new Guardian.GuardianMoveControl(this);
      this.clientSideTailAnimation = this.random.nextFloat();
      this.clientSideTailAnimationO = this.clientSideTailAnimation;
   }

   protected void registerGoals() {
      MoveTowardsRestrictionGoal var1 = new MoveTowardsRestrictionGoal(this, 1.0D);
      this.randomStrollGoal = new RandomStrollGoal(this, 1.0D, 80);
      this.goalSelector.addGoal(4, new Guardian.GuardianAttackGoal(this));
      this.goalSelector.addGoal(5, var1);
      this.goalSelector.addGoal(7, this.randomStrollGoal);
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Guardian.class, 12.0F, 0.01F));
      this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
      this.randomStrollGoal.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      var1.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, LivingEntity.class, 10, true, false, new Guardian.GuardianAttackSelector(this)));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MAX_HEALTH, 30.0D);
   }

   protected PathNavigation createNavigation(Level var1) {
      return new WaterBoundPathNavigation(this, var1);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_MOVING, false);
      this.entityData.define(DATA_ID_ATTACK_TARGET, 0);
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   public MobType getMobType() {
      return MobType.WATER;
   }

   public boolean isMoving() {
      return (Boolean)this.entityData.get(DATA_ID_MOVING);
   }

   void setMoving(boolean var1) {
      this.entityData.set(DATA_ID_MOVING, var1);
   }

   public int getAttackDuration() {
      return 80;
   }

   void setActiveAttackTarget(int var1) {
      this.entityData.set(DATA_ID_ATTACK_TARGET, var1);
   }

   public boolean hasActiveAttackTarget() {
      return (Integer)this.entityData.get(DATA_ID_ATTACK_TARGET) != 0;
   }

   @Nullable
   public LivingEntity getActiveAttackTarget() {
      if (!this.hasActiveAttackTarget()) {
         return null;
      } else if (this.level().isClientSide) {
         if (this.clientSideCachedAttackTarget != null) {
            return this.clientSideCachedAttackTarget;
         } else {
            Entity var1 = this.level().getEntity((Integer)this.entityData.get(DATA_ID_ATTACK_TARGET));
            if (var1 instanceof LivingEntity) {
               this.clientSideCachedAttackTarget = (LivingEntity)var1;
               return this.clientSideCachedAttackTarget;
            } else {
               return null;
            }
         }
      } else {
         return this.getTarget();
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      super.onSyncedDataUpdated(var1);
      if (DATA_ID_ATTACK_TARGET.equals(var1)) {
         this.clientSideAttackTime = 0;
         this.clientSideCachedAttackTarget = null;
      }

   }

   public int getAmbientSoundInterval() {
      return 160;
   }

   protected SoundEvent getAmbientSound() {
      return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.GUARDIAN_AMBIENT_LAND;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_HURT : SoundEvents.GUARDIAN_HURT_LAND;
   }

   protected SoundEvent getDeathSound() {
      return this.isInWaterOrBubble() ? SoundEvents.GUARDIAN_DEATH : SoundEvents.GUARDIAN_DEATH_LAND;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return var2.height * 0.5F;
   }

   public float getWalkTargetValue(BlockPos var1, LevelReader var2) {
      return var2.getFluidState(var1).is(FluidTags.WATER) ? 10.0F + var2.getPathfindingCostFromLightLevels(var1) : super.getWalkTargetValue(var1, var2);
   }

   public void aiStep() {
      if (this.isAlive()) {
         if (this.level().isClientSide) {
            this.clientSideTailAnimationO = this.clientSideTailAnimation;
            Vec3 var1;
            if (!this.isInWater()) {
               this.clientSideTailAnimationSpeed = 2.0F;
               var1 = this.getDeltaMovement();
               if (var1.y > 0.0D && this.clientSideTouchedGround && !this.isSilent()) {
                  this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getFlopSound(), this.getSoundSource(), 1.0F, 1.0F, false);
               }

               this.clientSideTouchedGround = var1.y < 0.0D && this.level().loadedAndEntityCanStandOn(this.blockPosition().below(), this);
            } else if (this.isMoving()) {
               if (this.clientSideTailAnimationSpeed < 0.5F) {
                  this.clientSideTailAnimationSpeed = 4.0F;
               } else {
                  this.clientSideTailAnimationSpeed += (0.5F - this.clientSideTailAnimationSpeed) * 0.1F;
               }
            } else {
               this.clientSideTailAnimationSpeed += (0.125F - this.clientSideTailAnimationSpeed) * 0.2F;
            }

            this.clientSideTailAnimation += this.clientSideTailAnimationSpeed;
            this.clientSideSpikesAnimationO = this.clientSideSpikesAnimation;
            if (!this.isInWaterOrBubble()) {
               this.clientSideSpikesAnimation = this.random.nextFloat();
            } else if (this.isMoving()) {
               this.clientSideSpikesAnimation += (0.0F - this.clientSideSpikesAnimation) * 0.25F;
            } else {
               this.clientSideSpikesAnimation += (1.0F - this.clientSideSpikesAnimation) * 0.06F;
            }

            if (this.isMoving() && this.isInWater()) {
               var1 = this.getViewVector(0.0F);

               for(int var2 = 0; var2 < 2; ++var2) {
                  this.level().addParticle(ParticleTypes.BUBBLE, this.getRandomX(0.5D) - var1.x * 1.5D, this.getRandomY() - var1.y * 1.5D, this.getRandomZ(0.5D) - var1.z * 1.5D, 0.0D, 0.0D, 0.0D);
               }
            }

            if (this.hasActiveAttackTarget()) {
               if (this.clientSideAttackTime < this.getAttackDuration()) {
                  ++this.clientSideAttackTime;
               }

               LivingEntity var14 = this.getActiveAttackTarget();
               if (var14 != null) {
                  this.getLookControl().setLookAt(var14, 90.0F, 90.0F);
                  this.getLookControl().tick();
                  double var15 = (double)this.getAttackAnimationScale(0.0F);
                  double var4 = var14.getX() - this.getX();
                  double var6 = var14.getY(0.5D) - this.getEyeY();
                  double var8 = var14.getZ() - this.getZ();
                  double var10 = Math.sqrt(var4 * var4 + var6 * var6 + var8 * var8);
                  var4 /= var10;
                  var6 /= var10;
                  var8 /= var10;
                  double var12 = this.random.nextDouble();

                  while(var12 < var10) {
                     var12 += 1.8D - var15 + this.random.nextDouble() * (1.7D - var15);
                     this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + var4 * var12, this.getEyeY() + var6 * var12, this.getZ() + var8 * var12, 0.0D, 0.0D, 0.0D);
                  }
               }
            }
         }

         if (this.isInWaterOrBubble()) {
            this.setAirSupply(300);
         } else if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().add((double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F), 0.5D, (double)((this.random.nextFloat() * 2.0F - 1.0F) * 0.4F)));
            this.setYRot(this.random.nextFloat() * 360.0F);
            this.setOnGround(false);
            this.hasImpulse = true;
         }

         if (this.hasActiveAttackTarget()) {
            this.setYRot(this.yHeadRot);
         }
      }

      super.aiStep();
   }

   protected SoundEvent getFlopSound() {
      return SoundEvents.GUARDIAN_FLOP;
   }

   public float getTailAnimation(float var1) {
      return Mth.lerp(var1, this.clientSideTailAnimationO, this.clientSideTailAnimation);
   }

   public float getSpikesAnimation(float var1) {
      return Mth.lerp(var1, this.clientSideSpikesAnimationO, this.clientSideSpikesAnimation);
   }

   public float getAttackAnimationScale(float var1) {
      return ((float)this.clientSideAttackTime + var1) / (float)this.getAttackDuration();
   }

   public float getClientSideAttackTime() {
      return (float)this.clientSideAttackTime;
   }

   public boolean checkSpawnObstruction(LevelReader var1) {
      return var1.isUnobstructed(this);
   }

   public static boolean checkGuardianSpawnRules(EntityType<? extends Guardian> var0, LevelAccessor var1, MobSpawnType var2, BlockPos var3, RandomSource var4) {
      return (var4.nextInt(20) == 0 || !var1.canSeeSkyFromBelowWater(var3)) && var1.getDifficulty() != Difficulty.PEACEFUL && (var2 == MobSpawnType.SPAWNER || var1.getFluidState(var3).is(FluidTags.WATER)) && var1.getFluidState(var3.below()).is(FluidTags.WATER);
   }

   public boolean hurt(DamageSource var1, float var2) {
      if (this.level().isClientSide) {
         return false;
      } else {
         if (!this.isMoving() && !var1.is(DamageTypeTags.AVOIDS_GUARDIAN_THORNS) && !var1.is(DamageTypes.THORNS)) {
            Entity var4 = var1.getDirectEntity();
            if (var4 instanceof LivingEntity) {
               LivingEntity var3 = (LivingEntity)var4;
               var3.hurt(this.damageSources().thorns(this), 2.0F);
            }
         }

         if (this.randomStrollGoal != null) {
            this.randomStrollGoal.trigger();
         }

         return super.hurt(var1, var2);
      }
   }

   public int getMaxHeadXRot() {
      return 180;
   }

   public void travel(Vec3 var1) {
      if (this.isControlledByLocalInstance() && this.isInWater()) {
         this.moveRelative(0.1F, var1);
         this.move(MoverType.SELF, this.getDeltaMovement());
         this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
         if (!this.isMoving() && this.getTarget() == null) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
         }
      } else {
         super.travel(var1);
      }

   }

   static {
      DATA_ID_MOVING = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.BOOLEAN);
      DATA_ID_ATTACK_TARGET = SynchedEntityData.defineId(Guardian.class, EntityDataSerializers.INT);
   }

   static class GuardianMoveControl extends MoveControl {
      private final Guardian guardian;

      public GuardianMoveControl(Guardian var1) {
         super(var1);
         this.guardian = var1;
      }

      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO && !this.guardian.getNavigation().isDone()) {
            Vec3 var1 = new Vec3(this.wantedX - this.guardian.getX(), this.wantedY - this.guardian.getY(), this.wantedZ - this.guardian.getZ());
            double var2 = var1.length();
            double var4 = var1.x / var2;
            double var6 = var1.y / var2;
            double var8 = var1.z / var2;
            float var10 = (float)(Mth.atan2(var1.z, var1.x) * 57.2957763671875D) - 90.0F;
            this.guardian.setYRot(this.rotlerp(this.guardian.getYRot(), var10, 90.0F));
            this.guardian.yBodyRot = this.guardian.getYRot();
            float var11 = (float)(this.speedModifier * this.guardian.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float var12 = Mth.lerp(0.125F, this.guardian.getSpeed(), var11);
            this.guardian.setSpeed(var12);
            double var13 = Math.sin((double)(this.guardian.tickCount + this.guardian.getId()) * 0.5D) * 0.05D;
            double var15 = Math.cos((double)(this.guardian.getYRot() * 0.017453292F));
            double var17 = Math.sin((double)(this.guardian.getYRot() * 0.017453292F));
            double var19 = Math.sin((double)(this.guardian.tickCount + this.guardian.getId()) * 0.75D) * 0.05D;
            this.guardian.setDeltaMovement(this.guardian.getDeltaMovement().add(var13 * var15, var19 * (var17 + var15) * 0.25D + (double)var12 * var6 * 0.1D, var13 * var17));
            LookControl var21 = this.guardian.getLookControl();
            double var22 = this.guardian.getX() + var4 * 2.0D;
            double var24 = this.guardian.getEyeY() + var6 / var2;
            double var26 = this.guardian.getZ() + var8 * 2.0D;
            double var28 = var21.getWantedX();
            double var30 = var21.getWantedY();
            double var32 = var21.getWantedZ();
            if (!var21.isLookingAtTarget()) {
               var28 = var22;
               var30 = var24;
               var32 = var26;
            }

            this.guardian.getLookControl().setLookAt(Mth.lerp(0.125D, var28, var22), Mth.lerp(0.125D, var30, var24), Mth.lerp(0.125D, var32, var26), 10.0F, 40.0F);
            this.guardian.setMoving(true);
         } else {
            this.guardian.setSpeed(0.0F);
            this.guardian.setMoving(false);
         }
      }
   }

   static class GuardianAttackGoal extends Goal {
      private final Guardian guardian;
      private int attackTime;
      private final boolean elder;

      public GuardianAttackGoal(Guardian var1) {
         this.guardian = var1;
         this.elder = var1 instanceof ElderGuardian;
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      public boolean canUse() {
         LivingEntity var1 = this.guardian.getTarget();
         return var1 != null && var1.isAlive();
      }

      public boolean canContinueToUse() {
         return super.canContinueToUse() && (this.elder || this.guardian.getTarget() != null && this.guardian.distanceToSqr(this.guardian.getTarget()) > 9.0D);
      }

      public void start() {
         this.attackTime = -10;
         this.guardian.getNavigation().stop();
         LivingEntity var1 = this.guardian.getTarget();
         if (var1 != null) {
            this.guardian.getLookControl().setLookAt(var1, 90.0F, 90.0F);
         }

         this.guardian.hasImpulse = true;
      }

      public void stop() {
         this.guardian.setActiveAttackTarget(0);
         this.guardian.setTarget((LivingEntity)null);
         this.guardian.randomStrollGoal.trigger();
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         LivingEntity var1 = this.guardian.getTarget();
         if (var1 != null) {
            this.guardian.getNavigation().stop();
            this.guardian.getLookControl().setLookAt(var1, 90.0F, 90.0F);
            if (!this.guardian.hasLineOfSight(var1)) {
               this.guardian.setTarget((LivingEntity)null);
            } else {
               ++this.attackTime;
               if (this.attackTime == 0) {
                  this.guardian.setActiveAttackTarget(var1.getId());
                  if (!this.guardian.isSilent()) {
                     this.guardian.level().broadcastEntityEvent(this.guardian, (byte)21);
                  }
               } else if (this.attackTime >= this.guardian.getAttackDuration()) {
                  float var2 = 1.0F;
                  if (this.guardian.level().getDifficulty() == Difficulty.HARD) {
                     var2 += 2.0F;
                  }

                  if (this.elder) {
                     var2 += 2.0F;
                  }

                  var1.hurt(this.guardian.damageSources().indirectMagic(this.guardian, this.guardian), var2);
                  var1.hurt(this.guardian.damageSources().mobAttack(this.guardian), (float)this.guardian.getAttributeValue(Attributes.ATTACK_DAMAGE));
                  this.guardian.setTarget((LivingEntity)null);
               }

               super.tick();
            }
         }
      }
   }

   static class GuardianAttackSelector implements Predicate<LivingEntity> {
      private final Guardian guardian;

      public GuardianAttackSelector(Guardian var1) {
         this.guardian = var1;
      }

      public boolean test(@Nullable LivingEntity var1) {
         return (var1 instanceof Player || var1 instanceof Squid || var1 instanceof Axolotl) && var1.distanceToSqr(this.guardian) > 9.0D;
      }

      // $FF: synthetic method
      public boolean test(@Nullable Object var1) {
         return this.test((LivingEntity)var1);
      }
   }
}
