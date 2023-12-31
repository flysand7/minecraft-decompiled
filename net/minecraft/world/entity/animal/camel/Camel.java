package net.minecraft.world.entity.animal.camel;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RiderShieldingMount;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Camel extends AbstractHorse implements PlayerRideableJumping, RiderShieldingMount, Saddleable {
   public static final Ingredient TEMPTATION_ITEM;
   public static final int DASH_COOLDOWN_TICKS = 55;
   public static final int MAX_HEAD_Y_ROT = 30;
   private static final float RUNNING_SPEED_BONUS = 0.1F;
   private static final float DASH_VERTICAL_MOMENTUM = 1.4285F;
   private static final float DASH_HORIZONTAL_MOMENTUM = 22.2222F;
   private static final int DASH_MINIMUM_DURATION_TICKS = 5;
   private static final int SITDOWN_DURATION_TICKS = 40;
   private static final int STANDUP_DURATION_TICKS = 52;
   private static final int IDLE_MINIMAL_DURATION_TICKS = 80;
   private static final float SITTING_HEIGHT_DIFFERENCE = 1.43F;
   public static final EntityDataAccessor<Boolean> DASH;
   public static final EntityDataAccessor<Long> LAST_POSE_CHANGE_TICK;
   public final AnimationState sitAnimationState = new AnimationState();
   public final AnimationState sitPoseAnimationState = new AnimationState();
   public final AnimationState sitUpAnimationState = new AnimationState();
   public final AnimationState idleAnimationState = new AnimationState();
   public final AnimationState dashAnimationState = new AnimationState();
   private static final EntityDimensions SITTING_DIMENSIONS;
   private int dashCooldown = 0;
   private int idleAnimationTimeout = 0;

   public Camel(EntityType<? extends Camel> var1, Level var2) {
      super(var1, var2);
      this.setMaxUpStep(1.5F);
      this.moveControl = new Camel.CamelMoveControl();
      GroundPathNavigation var3 = (GroundPathNavigation)this.getNavigation();
      var3.setCanFloat(true);
      var3.setCanWalkOverFences(true);
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putLong("LastPoseTick", (Long)this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      long var2 = var1.getLong("LastPoseTick");
      if (var2 < 0L) {
         this.setPose(Pose.SITTING);
      }

      this.resetLastPoseChangeTick(var2);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 32.0D).add(Attributes.MOVEMENT_SPEED, 0.09000000357627869D).add(Attributes.JUMP_STRENGTH, 0.41999998688697815D);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DASH, false);
      this.entityData.define(LAST_POSE_CHANGE_TICK, 0L);
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      CamelAi.initMemories(this, var1.getRandom());
      this.resetLastPoseChangeTickToFullStand(var1.getLevel().getGameTime());
      return super.finalizeSpawn(var1, var2, var3, var4, var5);
   }

   protected Brain.Provider<Camel> brainProvider() {
      return CamelAi.brainProvider();
   }

   protected void registerGoals() {
   }

   protected Brain<?> makeBrain(Dynamic<?> var1) {
      return CamelAi.makeBrain(this.brainProvider().makeBrain(var1));
   }

   public EntityDimensions getDimensions(Pose var1) {
      return var1 == Pose.SITTING ? SITTING_DIMENSIONS.scale(this.getScale()) : super.getDimensions(var1);
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return var2.height - 0.1F;
   }

   public double getRiderShieldingHeight() {
      return 0.5D;
   }

   protected void customServerAiStep() {
      this.level().getProfiler().push("camelBrain");
      Brain var1 = this.getBrain();
      var1.tick((ServerLevel)this.level(), this);
      this.level().getProfiler().pop();
      this.level().getProfiler().push("camelActivityUpdate");
      CamelAi.updateActivity(this);
      this.level().getProfiler().pop();
      super.customServerAiStep();
   }

   public void tick() {
      super.tick();
      if (this.isDashing() && this.dashCooldown < 50 && (this.onGround() || this.isInWater() || this.isPassenger())) {
         this.setDashing(false);
      }

      if (this.dashCooldown > 0) {
         --this.dashCooldown;
         if (this.dashCooldown == 0) {
            this.level().playSound((Player)null, (BlockPos)this.blockPosition(), SoundEvents.CAMEL_DASH_READY, SoundSource.NEUTRAL, 1.0F, 1.0F);
         }
      }

      if (this.level().isClientSide()) {
         this.setupAnimationStates();
      }

      if (this.refuseToMove()) {
         this.clampHeadRotationToBody(this, 30.0F);
      }

      if (this.isCamelSitting() && this.isInWater()) {
         this.standUpInstantly();
      }

   }

   private void setupAnimationStates() {
      if (this.idleAnimationTimeout <= 0) {
         this.idleAnimationTimeout = this.random.nextInt(40) + 80;
         this.idleAnimationState.start(this.tickCount);
      } else {
         --this.idleAnimationTimeout;
      }

      if (this.isCamelVisuallySitting()) {
         this.sitUpAnimationState.stop();
         this.dashAnimationState.stop();
         if (this.isVisuallySittingDown()) {
            this.sitAnimationState.startIfStopped(this.tickCount);
            this.sitPoseAnimationState.stop();
         } else {
            this.sitAnimationState.stop();
            this.sitPoseAnimationState.startIfStopped(this.tickCount);
         }
      } else {
         this.sitAnimationState.stop();
         this.sitPoseAnimationState.stop();
         this.dashAnimationState.animateWhen(this.isDashing(), this.tickCount);
         this.sitUpAnimationState.animateWhen(this.isInPoseTransition() && this.getPoseTime() >= 0L, this.tickCount);
      }

   }

   protected void updateWalkAnimation(float var1) {
      float var2;
      if (this.getPose() == Pose.STANDING && !this.dashAnimationState.isStarted()) {
         var2 = Math.min(var1 * 6.0F, 1.0F);
      } else {
         var2 = 0.0F;
      }

      this.walkAnimation.update(var2, 0.2F);
   }

   public void travel(Vec3 var1) {
      if (this.refuseToMove() && this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
         var1 = var1.multiply(0.0D, 1.0D, 0.0D);
      }

      super.travel(var1);
   }

   protected void tickRidden(Player var1, Vec3 var2) {
      super.tickRidden(var1, var2);
      if (var1.zza > 0.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }

   }

   public boolean refuseToMove() {
      return this.isCamelSitting() || this.isInPoseTransition();
   }

   protected float getRiddenSpeed(Player var1) {
      float var2 = var1.isSprinting() && this.getJumpCooldown() == 0 ? 0.1F : 0.0F;
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) + var2;
   }

   protected Vec2 getRiddenRotation(LivingEntity var1) {
      return this.refuseToMove() ? new Vec2(this.getXRot(), this.getYRot()) : super.getRiddenRotation(var1);
   }

   protected Vec3 getRiddenInput(Player var1, Vec3 var2) {
      return this.refuseToMove() ? Vec3.ZERO : super.getRiddenInput(var1, var2);
   }

   public boolean canJump() {
      return !this.refuseToMove() && super.canJump();
   }

   public void onPlayerJump(int var1) {
      if (this.isSaddled() && this.dashCooldown <= 0 && this.onGround()) {
         super.onPlayerJump(var1);
      }
   }

   public boolean canSprint() {
      return true;
   }

   protected void executeRidersJump(float var1, Vec3 var2) {
      double var3 = this.getAttributeValue(Attributes.JUMP_STRENGTH) * (double)this.getBlockJumpFactor() + (double)this.getJumpBoostPower();
      this.addDeltaMovement(this.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)(22.2222F * var1) * this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (double)this.getBlockSpeedFactor()).add(0.0D, (double)(1.4285F * var1) * var3, 0.0D));
      this.dashCooldown = 55;
      this.setDashing(true);
      this.hasImpulse = true;
   }

   public boolean isDashing() {
      return (Boolean)this.entityData.get(DASH);
   }

   public void setDashing(boolean var1) {
      this.entityData.set(DASH, var1);
   }

   public boolean isPanicking() {
      return this.getBrain().checkMemory(MemoryModuleType.IS_PANICKING, MemoryStatus.VALUE_PRESENT);
   }

   public void handleStartJump(int var1) {
      this.playSound(SoundEvents.CAMEL_DASH, 1.0F, 1.0F);
      this.setDashing(true);
   }

   public void handleStopJump() {
   }

   public int getJumpCooldown() {
      return this.dashCooldown;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.CAMEL_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.CAMEL_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.CAMEL_HURT;
   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
      if (var2.getSoundType() == SoundType.SAND) {
         this.playSound(SoundEvents.CAMEL_STEP_SAND, 1.0F, 1.0F);
      } else {
         this.playSound(SoundEvents.CAMEL_STEP, 1.0F, 1.0F);
      }

   }

   public boolean isFood(ItemStack var1) {
      return TEMPTATION_ITEM.test(var1);
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      if (var1.isSecondaryUseActive() && !this.isBaby()) {
         this.openCustomInventoryScreen(var1);
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         InteractionResult var4 = var3.interactLivingEntity(var1, this, var2);
         if (var4.consumesAction()) {
            return var4;
         } else if (this.isFood(var3)) {
            return this.fedFood(var1, var3);
         } else {
            if (this.getPassengers().size() < 2 && !this.isBaby()) {
               this.doPlayerRide(var1);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
         }
      }
   }

   protected void onLeashDistance(float var1) {
      if (var1 > 6.0F && this.isCamelSitting() && !this.isInPoseTransition()) {
         this.standUp();
      }

   }

   protected boolean handleEating(Player var1, ItemStack var2) {
      if (!this.isFood(var2)) {
         return false;
      } else {
         boolean var3 = this.getHealth() < this.getMaxHealth();
         if (var3) {
            this.heal(2.0F);
         }

         boolean var4 = this.isTamed() && this.getAge() == 0 && this.canFallInLove();
         if (var4) {
            this.setInLove(var1);
         }

         boolean var5 = this.isBaby();
         if (var5) {
            this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
            if (!this.level().isClientSide) {
               this.ageUp(10);
            }
         }

         if (!var3 && !var4 && !var5) {
            return false;
         } else {
            if (!this.isSilent()) {
               SoundEvent var6 = this.getEatingSound();
               if (var6 != null) {
                  this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), var6, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
               }
            }

            return true;
         }
      }
   }

   protected boolean canPerformRearing() {
      return false;
   }

   public boolean canMate(Animal var1) {
      boolean var10000;
      if (var1 != this && var1 instanceof Camel) {
         Camel var2 = (Camel)var1;
         if (this.canParent() && var2.canParent()) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   @Nullable
   public Camel getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return (Camel)EntityType.CAMEL.create(var1);
   }

   @Nullable
   protected SoundEvent getEatingSound() {
      return SoundEvents.CAMEL_EAT;
   }

   protected void actuallyHurt(DamageSource var1, float var2) {
      this.standUpInstantly();
      super.actuallyHurt(var1, var2);
   }

   protected void positionRider(Entity var1, Entity.MoveFunction var2) {
      int var3 = this.getPassengers().indexOf(var1);
      if (var3 >= 0) {
         boolean var4 = var3 == 0;
         float var5 = 0.5F;
         float var6 = (float)(this.isRemoved() ? 0.009999999776482582D : this.getBodyAnchorAnimationYOffset(var4, 0.0F) + var1.getMyRidingOffset());
         if (this.getPassengers().size() > 1) {
            if (!var4) {
               var5 = -0.7F;
            }

            if (var1 instanceof Animal) {
               var5 += 0.2F;
            }
         }

         Vec3 var7 = (new Vec3(0.0D, 0.0D, (double)var5)).yRot(-this.yBodyRot * 0.017453292F);
         var2.accept(var1, this.getX() + var7.x, this.getY() + (double)var6, this.getZ() + var7.z);
         this.clampRotation(var1);
      }
   }

   private double getBodyAnchorAnimationYOffset(boolean var1, float var2) {
      double var3 = this.getPassengersRidingOffset();
      float var5 = this.getScale() * 1.43F;
      float var6 = var5 - this.getScale() * 0.2F;
      float var7 = var5 - var6;
      boolean var8 = this.isInPoseTransition();
      boolean var9 = this.isCamelSitting();
      if (var8) {
         int var10 = var9 ? 40 : 52;
         int var11;
         float var12;
         if (var9) {
            var11 = 28;
            var12 = var1 ? 0.5F : 0.1F;
         } else {
            var11 = var1 ? 24 : 32;
            var12 = var1 ? 0.6F : 0.35F;
         }

         float var13 = Mth.clamp((float)this.getPoseTime() + var2, 0.0F, (float)var10);
         boolean var14 = var13 < (float)var11;
         float var15 = var14 ? var13 / (float)var11 : (var13 - (float)var11) / (float)(var10 - var11);
         float var16 = var5 - var12 * var6;
         var3 += var9 ? (double)Mth.lerp(var15, var14 ? var5 : var16, var14 ? var16 : var7) : (double)Mth.lerp(var15, var14 ? var7 - var5 : var7 - var16, var14 ? var7 - var16 : 0.0F);
      }

      if (var9 && !var8) {
         var3 += (double)var7;
      }

      return var3;
   }

   public Vec3 getLeashOffset(float var1) {
      return new Vec3(0.0D, this.getBodyAnchorAnimationYOffset(true, var1) - (double)(0.2F * this.getScale()), (double)(this.getBbWidth() * 0.56F));
   }

   public double getPassengersRidingOffset() {
      return (double)(this.getDimensions(this.isCamelSitting() ? Pose.SITTING : Pose.STANDING).height - (this.isBaby() ? 0.35F : 0.6F));
   }

   public void onPassengerTurned(Entity var1) {
      if (this.getControllingPassenger() != var1) {
         this.clampRotation(var1);
      }

   }

   private void clampRotation(Entity var1) {
      var1.setYBodyRot(this.getYRot());
      float var2 = var1.getYRot();
      float var3 = Mth.wrapDegrees(var2 - this.getYRot());
      float var4 = Mth.clamp(var3, -160.0F, 160.0F);
      var1.yRotO += var4 - var3;
      float var5 = var2 + var4 - var3;
      var1.setYRot(var5);
      var1.setYHeadRot(var5);
   }

   private void clampHeadRotationToBody(Entity var1, float var2) {
      float var3 = var1.getYHeadRot();
      float var4 = Mth.wrapDegrees(this.yBodyRot - var3);
      float var5 = Mth.clamp(Mth.wrapDegrees(this.yBodyRot - var3), -var2, var2);
      float var6 = var3 + var4 - var5;
      var1.setYHeadRot(var6);
   }

   public int getMaxHeadYRot() {
      return 30;
   }

   protected boolean canAddPassenger(Entity var1) {
      return this.getPassengers().size() <= 2;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      if (!this.getPassengers().isEmpty() && this.isSaddled()) {
         Entity var1 = (Entity)this.getPassengers().get(0);
         if (var1 instanceof LivingEntity) {
            LivingEntity var2 = (LivingEntity)var1;
            return var2;
         }
      }

      return null;
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   public boolean isCamelSitting() {
      return (Long)this.entityData.get(LAST_POSE_CHANGE_TICK) < 0L;
   }

   public boolean isCamelVisuallySitting() {
      return this.getPoseTime() < 0L != this.isCamelSitting();
   }

   public boolean isInPoseTransition() {
      long var1 = this.getPoseTime();
      return var1 < (long)(this.isCamelSitting() ? 40 : 52);
   }

   private boolean isVisuallySittingDown() {
      return this.isCamelSitting() && this.getPoseTime() < 40L && this.getPoseTime() >= 0L;
   }

   public void sitDown() {
      if (!this.isCamelSitting()) {
         this.playSound(SoundEvents.CAMEL_SIT, 1.0F, 1.0F);
         this.setPose(Pose.SITTING);
         this.resetLastPoseChangeTick(-this.level().getGameTime());
      }
   }

   public void standUp() {
      if (this.isCamelSitting()) {
         this.playSound(SoundEvents.CAMEL_STAND, 1.0F, 1.0F);
         this.setPose(Pose.STANDING);
         this.resetLastPoseChangeTick(this.level().getGameTime());
      }
   }

   public void standUpInstantly() {
      this.setPose(Pose.STANDING);
      this.resetLastPoseChangeTickToFullStand(this.level().getGameTime());
   }

   @VisibleForTesting
   public void resetLastPoseChangeTick(long var1) {
      this.entityData.set(LAST_POSE_CHANGE_TICK, var1);
   }

   private void resetLastPoseChangeTickToFullStand(long var1) {
      this.resetLastPoseChangeTick(Math.max(0L, var1 - 52L - 1L));
   }

   public long getPoseTime() {
      return this.level().getGameTime() - Math.abs((Long)this.entityData.get(LAST_POSE_CHANGE_TICK));
   }

   public SoundEvent getSaddleSoundEvent() {
      return SoundEvents.CAMEL_SADDLE;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      if (!this.firstTick && DASH.equals(var1)) {
         this.dashCooldown = this.dashCooldown == 0 ? 55 : this.dashCooldown;
      }

      super.onSyncedDataUpdated(var1);
   }

   protected BodyRotationControl createBodyControl() {
      return new Camel.CamelBodyRotationControl(this);
   }

   public boolean isTamed() {
      return true;
   }

   public void openCustomInventoryScreen(Player var1) {
      if (!this.level().isClientSide) {
         var1.openHorseInventory(this, this.inventory);
      }

   }

   // $FF: synthetic method
   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return this.getBreedOffspring(var1, var2);
   }

   static {
      TEMPTATION_ITEM = Ingredient.of(Items.CACTUS);
      DASH = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.BOOLEAN);
      LAST_POSE_CHANGE_TICK = SynchedEntityData.defineId(Camel.class, EntityDataSerializers.LONG);
      SITTING_DIMENSIONS = EntityDimensions.scalable(EntityType.CAMEL.getWidth(), EntityType.CAMEL.getHeight() - 1.43F);
   }

   private class CamelMoveControl extends MoveControl {
      public CamelMoveControl() {
         super(Camel.this);
      }

      public void tick() {
         if (this.operation == MoveControl.Operation.MOVE_TO && !Camel.this.isLeashed() && Camel.this.isCamelSitting() && !Camel.this.isInPoseTransition()) {
            Camel.this.standUp();
         }

         super.tick();
      }
   }

   class CamelBodyRotationControl extends BodyRotationControl {
      public CamelBodyRotationControl(Camel var2) {
         super(var2);
      }

      public void clientTick() {
         if (!Camel.this.refuseToMove()) {
            super.clientTick();
         }

      }
   }
}
