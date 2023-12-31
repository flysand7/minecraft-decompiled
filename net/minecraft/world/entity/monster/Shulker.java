package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Shulker extends AbstractGolem implements VariantHolder<Optional<DyeColor>>, Enemy {
   private static final UUID COVERED_ARMOR_MODIFIER_UUID = UUID.fromString("7E0292F2-9434-48D5-A29F-9583AF7DF27F");
   private static final AttributeModifier COVERED_ARMOR_MODIFIER;
   protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID;
   protected static final EntityDataAccessor<Byte> DATA_PEEK_ID;
   protected static final EntityDataAccessor<Byte> DATA_COLOR_ID;
   private static final int TELEPORT_STEPS = 6;
   private static final byte NO_COLOR = 16;
   private static final byte DEFAULT_COLOR = 16;
   private static final int MAX_TELEPORT_DISTANCE = 8;
   private static final int OTHER_SHULKER_SCAN_RADIUS = 8;
   private static final int OTHER_SHULKER_LIMIT = 5;
   private static final float PEEK_PER_TICK = 0.05F;
   static final Vector3f FORWARD;
   private float currentPeekAmountO;
   private float currentPeekAmount;
   @Nullable
   private BlockPos clientOldAttachPosition;
   private int clientSideTeleportInterpolation;
   private static final float MAX_LID_OPEN = 1.0F;

   public Shulker(EntityType<? extends Shulker> var1, Level var2) {
      super(var1, var2);
      this.xpReward = 5;
      this.lookControl = new Shulker.ShulkerLookControl(this);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.02F, true));
      this.goalSelector.addGoal(4, new Shulker.ShulkerAttackGoal());
      this.goalSelector.addGoal(7, new Shulker.ShulkerPeekGoal());
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, new Class[]{this.getClass()})).setAlertOthers());
      this.targetSelector.addGoal(2, new Shulker.ShulkerNearestAttackGoal(this));
      this.targetSelector.addGoal(3, new Shulker.ShulkerDefenseAttackGoal(this));
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.SHULKER_AMBIENT;
   }

   public void playAmbientSound() {
      if (!this.isClosed()) {
         super.playAmbientSound();
      }

   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.SHULKER_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return this.isClosed() ? SoundEvents.SHULKER_HURT_CLOSED : SoundEvents.SHULKER_HURT;
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ATTACH_FACE_ID, Direction.DOWN);
      this.entityData.define(DATA_PEEK_ID, (byte)0);
      this.entityData.define(DATA_COLOR_ID, (byte)16);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0D);
   }

   protected BodyRotationControl createBodyControl() {
      return new Shulker.ShulkerBodyRotationControl(this);
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setAttachFace(Direction.from3DDataValue(var1.getByte("AttachFace")));
      this.entityData.set(DATA_PEEK_ID, var1.getByte("Peek"));
      if (var1.contains("Color", 99)) {
         this.entityData.set(DATA_COLOR_ID, var1.getByte("Color"));
      }

   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putByte("AttachFace", (byte)this.getAttachFace().get3DDataValue());
      var1.putByte("Peek", (Byte)this.entityData.get(DATA_PEEK_ID));
      var1.putByte("Color", (Byte)this.entityData.get(DATA_COLOR_ID));
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && !this.isPassenger() && !this.canStayAt(this.blockPosition(), this.getAttachFace())) {
         this.findNewAttachment();
      }

      if (this.updatePeekAmount()) {
         this.onPeekAmountChange();
      }

      if (this.level().isClientSide) {
         if (this.clientSideTeleportInterpolation > 0) {
            --this.clientSideTeleportInterpolation;
         } else {
            this.clientOldAttachPosition = null;
         }
      }

   }

   private void findNewAttachment() {
      Direction var1 = this.findAttachableSurface(this.blockPosition());
      if (var1 != null) {
         this.setAttachFace(var1);
      } else {
         this.teleportSomewhere();
      }

   }

   protected AABB makeBoundingBox() {
      float var1 = getPhysicalPeek(this.currentPeekAmount);
      Direction var2 = this.getAttachFace().getOpposite();
      float var3 = this.getType().getWidth() / 2.0F;
      return getProgressAabb(var2, var1).move(this.getX() - (double)var3, this.getY(), this.getZ() - (double)var3);
   }

   private static float getPhysicalPeek(float var0) {
      return 0.5F - Mth.sin((0.5F + var0) * 3.1415927F) * 0.5F;
   }

   private boolean updatePeekAmount() {
      this.currentPeekAmountO = this.currentPeekAmount;
      float var1 = (float)this.getRawPeekAmount() * 0.01F;
      if (this.currentPeekAmount == var1) {
         return false;
      } else {
         if (this.currentPeekAmount > var1) {
            this.currentPeekAmount = Mth.clamp(this.currentPeekAmount - 0.05F, var1, 1.0F);
         } else {
            this.currentPeekAmount = Mth.clamp(this.currentPeekAmount + 0.05F, 0.0F, var1);
         }

         return true;
      }
   }

   private void onPeekAmountChange() {
      this.reapplyPosition();
      float var1 = getPhysicalPeek(this.currentPeekAmount);
      float var2 = getPhysicalPeek(this.currentPeekAmountO);
      Direction var3 = this.getAttachFace().getOpposite();
      float var4 = var1 - var2;
      if (!(var4 <= 0.0F)) {
         List var5 = this.level().getEntities((Entity)this, getProgressDeltaAabb(var3, var2, var1).move(this.getX() - 0.5D, this.getY(), this.getZ() - 0.5D), EntitySelector.NO_SPECTATORS.and((var1x) -> {
            return !var1x.isPassengerOfSameVehicle(this);
         }));
         Iterator var6 = var5.iterator();

         while(var6.hasNext()) {
            Entity var7 = (Entity)var6.next();
            if (!(var7 instanceof Shulker) && !var7.noPhysics) {
               var7.move(MoverType.SHULKER, new Vec3((double)(var4 * (float)var3.getStepX()), (double)(var4 * (float)var3.getStepY()), (double)(var4 * (float)var3.getStepZ())));
            }
         }

      }
   }

   public static AABB getProgressAabb(Direction var0, float var1) {
      return getProgressDeltaAabb(var0, -1.0F, var1);
   }

   public static AABB getProgressDeltaAabb(Direction var0, float var1, float var2) {
      double var3 = (double)Math.max(var1, var2);
      double var5 = (double)Math.min(var1, var2);
      return (new AABB(BlockPos.ZERO)).expandTowards((double)var0.getStepX() * var3, (double)var0.getStepY() * var3, (double)var0.getStepZ() * var3).contract((double)(-var0.getStepX()) * (1.0D + var5), (double)(-var0.getStepY()) * (1.0D + var5), (double)(-var0.getStepZ()) * (1.0D + var5));
   }

   public double getMyRidingOffset() {
      EntityType var1 = this.getVehicle().getType();
      return !(this.getVehicle() instanceof Boat) && var1 != EntityType.MINECART ? super.getMyRidingOffset() : 0.1875D - this.getVehicle().getPassengersRidingOffset();
   }

   public boolean startRiding(Entity var1, boolean var2) {
      if (this.level().isClientSide()) {
         this.clientOldAttachPosition = null;
         this.clientSideTeleportInterpolation = 0;
      }

      this.setAttachFace(Direction.DOWN);
      return super.startRiding(var1, var2);
   }

   public void stopRiding() {
      super.stopRiding();
      if (this.level().isClientSide) {
         this.clientOldAttachPosition = this.blockPosition();
      }

      this.yBodyRotO = 0.0F;
      this.yBodyRot = 0.0F;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      this.setYRot(0.0F);
      this.yHeadRot = this.getYRot();
      this.setOldPosAndRot();
      return super.finalizeSpawn(var1, var2, var3, var4, var5);
   }

   public void move(MoverType var1, Vec3 var2) {
      if (var1 == MoverType.SHULKER_BOX) {
         this.teleportSomewhere();
      } else {
         super.move(var1, var2);
      }

   }

   public Vec3 getDeltaMovement() {
      return Vec3.ZERO;
   }

   public void setDeltaMovement(Vec3 var1) {
   }

   public void setPos(double var1, double var3, double var5) {
      BlockPos var7 = this.blockPosition();
      if (this.isPassenger()) {
         super.setPos(var1, var3, var5);
      } else {
         super.setPos((double)Mth.floor(var1) + 0.5D, (double)Mth.floor(var3 + 0.5D), (double)Mth.floor(var5) + 0.5D);
      }

      if (this.tickCount != 0) {
         BlockPos var8 = this.blockPosition();
         if (!var8.equals(var7)) {
            this.entityData.set(DATA_PEEK_ID, (byte)0);
            this.hasImpulse = true;
            if (this.level().isClientSide && !this.isPassenger() && !var8.equals(this.clientOldAttachPosition)) {
               this.clientOldAttachPosition = var7;
               this.clientSideTeleportInterpolation = 6;
               this.xOld = this.getX();
               this.yOld = this.getY();
               this.zOld = this.getZ();
            }
         }

      }
   }

   @Nullable
   protected Direction findAttachableSurface(BlockPos var1) {
      Direction[] var2 = Direction.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Direction var5 = var2[var4];
         if (this.canStayAt(var1, var5)) {
            return var5;
         }
      }

      return null;
   }

   boolean canStayAt(BlockPos var1, Direction var2) {
      if (this.isPositionBlocked(var1)) {
         return false;
      } else {
         Direction var3 = var2.getOpposite();
         if (!this.level().loadedAndEntityCanStandOnFace(var1.relative(var2), this, var3)) {
            return false;
         } else {
            AABB var4 = getProgressAabb(var3, 1.0F).move(var1).deflate(1.0E-6D);
            return this.level().noCollision(this, var4);
         }
      }
   }

   private boolean isPositionBlocked(BlockPos var1) {
      BlockState var2 = this.level().getBlockState(var1);
      if (var2.isAir()) {
         return false;
      } else {
         boolean var3 = var2.is(Blocks.MOVING_PISTON) && var1.equals(this.blockPosition());
         return !var3;
      }
   }

   protected boolean teleportSomewhere() {
      if (!this.isNoAi() && this.isAlive()) {
         BlockPos var1 = this.blockPosition();

         for(int var2 = 0; var2 < 5; ++var2) {
            BlockPos var3 = var1.offset(Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8));
            if (var3.getY() > this.level().getMinBuildHeight() && this.level().isEmptyBlock(var3) && this.level().getWorldBorder().isWithinBounds(var3) && this.level().noCollision(this, (new AABB(var3)).deflate(1.0E-6D))) {
               Direction var4 = this.findAttachableSurface(var3);
               if (var4 != null) {
                  this.unRide();
                  this.setAttachFace(var4);
                  this.playSound(SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
                  this.setPos((double)var3.getX() + 0.5D, (double)var3.getY(), (double)var3.getZ() + 0.5D);
                  this.level().gameEvent(GameEvent.TELEPORT, var1, GameEvent.Context.of((Entity)this));
                  this.entityData.set(DATA_PEEK_ID, (byte)0);
                  this.setTarget((LivingEntity)null);
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void lerpTo(double var1, double var3, double var5, float var7, float var8, int var9, boolean var10) {
      this.lerpSteps = 0;
      this.setPos(var1, var3, var5);
      this.setRot(var7, var8);
   }

   public boolean hurt(DamageSource var1, float var2) {
      Entity var3;
      if (this.isClosed()) {
         var3 = var1.getDirectEntity();
         if (var3 instanceof AbstractArrow) {
            return false;
         }
      }

      if (!super.hurt(var1, var2)) {
         return false;
      } else {
         if ((double)this.getHealth() < (double)this.getMaxHealth() * 0.5D && this.random.nextInt(4) == 0) {
            this.teleportSomewhere();
         } else if (var1.is(DamageTypeTags.IS_PROJECTILE)) {
            var3 = var1.getDirectEntity();
            if (var3 != null && var3.getType() == EntityType.SHULKER_BULLET) {
               this.hitByShulkerBullet();
            }
         }

         return true;
      }
   }

   private boolean isClosed() {
      return this.getRawPeekAmount() == 0;
   }

   private void hitByShulkerBullet() {
      Vec3 var1 = this.position();
      AABB var2 = this.getBoundingBox();
      if (!this.isClosed() && this.teleportSomewhere()) {
         int var3 = this.level().getEntities((EntityTypeTest)EntityType.SHULKER, var2.inflate(8.0D), Entity::isAlive).size();
         float var4 = (float)(var3 - 1) / 5.0F;
         if (!(this.level().random.nextFloat() < var4)) {
            Shulker var5 = (Shulker)EntityType.SHULKER.create(this.level());
            if (var5 != null) {
               var5.setVariant(this.getVariant());
               var5.moveTo(var1);
               this.level().addFreshEntity(var5);
            }

         }
      }
   }

   public boolean canBeCollidedWith() {
      return this.isAlive();
   }

   public Direction getAttachFace() {
      return (Direction)this.entityData.get(DATA_ATTACH_FACE_ID);
   }

   private void setAttachFace(Direction var1) {
      this.entityData.set(DATA_ATTACH_FACE_ID, var1);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      if (DATA_ATTACH_FACE_ID.equals(var1)) {
         this.setBoundingBox(this.makeBoundingBox());
      }

      super.onSyncedDataUpdated(var1);
   }

   private int getRawPeekAmount() {
      return (Byte)this.entityData.get(DATA_PEEK_ID);
   }

   void setRawPeekAmount(int var1) {
      if (!this.level().isClientSide) {
         this.getAttribute(Attributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER);
         if (var1 == 0) {
            this.getAttribute(Attributes.ARMOR).addPermanentModifier(COVERED_ARMOR_MODIFIER);
            this.playSound(SoundEvents.SHULKER_CLOSE, 1.0F, 1.0F);
            this.gameEvent(GameEvent.CONTAINER_CLOSE);
         } else {
            this.playSound(SoundEvents.SHULKER_OPEN, 1.0F, 1.0F);
            this.gameEvent(GameEvent.CONTAINER_OPEN);
         }
      }

      this.entityData.set(DATA_PEEK_ID, (byte)var1);
   }

   public float getClientPeekAmount(float var1) {
      return Mth.lerp(var1, this.currentPeekAmountO, this.currentPeekAmount);
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return 0.5F;
   }

   public void recreateFromPacket(ClientboundAddEntityPacket var1) {
      super.recreateFromPacket(var1);
      this.yBodyRot = 0.0F;
      this.yBodyRotO = 0.0F;
   }

   public int getMaxHeadXRot() {
      return 180;
   }

   public int getMaxHeadYRot() {
      return 180;
   }

   public void push(Entity var1) {
   }

   public float getPickRadius() {
      return 0.0F;
   }

   public Optional<Vec3> getRenderPosition(float var1) {
      if (this.clientOldAttachPosition != null && this.clientSideTeleportInterpolation > 0) {
         double var2 = (double)((float)this.clientSideTeleportInterpolation - var1) / 6.0D;
         var2 *= var2;
         BlockPos var4 = this.blockPosition();
         double var5 = (double)(var4.getX() - this.clientOldAttachPosition.getX()) * var2;
         double var7 = (double)(var4.getY() - this.clientOldAttachPosition.getY()) * var2;
         double var9 = (double)(var4.getZ() - this.clientOldAttachPosition.getZ()) * var2;
         return Optional.of(new Vec3(-var5, -var7, -var9));
      } else {
         return Optional.empty();
      }
   }

   public void setVariant(Optional<DyeColor> var1) {
      this.entityData.set(DATA_COLOR_ID, (Byte)var1.map((var0) -> {
         return (byte)var0.getId();
      }).orElse((byte)16));
   }

   public Optional<DyeColor> getVariant() {
      return Optional.ofNullable(this.getColor());
   }

   @Nullable
   public DyeColor getColor() {
      byte var1 = (Byte)this.entityData.get(DATA_COLOR_ID);
      return var1 != 16 && var1 <= 15 ? DyeColor.byId(var1) : null;
   }

   // $FF: synthetic method
   public Object getVariant() {
      return this.getVariant();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void setVariant(Object var1) {
      this.setVariant((Optional)var1);
   }

   static {
      COVERED_ARMOR_MODIFIER = new AttributeModifier(COVERED_ARMOR_MODIFIER_UUID, "Covered armor bonus", 20.0D, AttributeModifier.Operation.ADDITION);
      DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.DIRECTION);
      DATA_PEEK_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
      DATA_COLOR_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
      FORWARD = (Vector3f)Util.make(() -> {
         Vec3i var0 = Direction.SOUTH.getNormal();
         return new Vector3f((float)var0.getX(), (float)var0.getY(), (float)var0.getZ());
      });
   }

   class ShulkerLookControl extends LookControl {
      public ShulkerLookControl(Mob var2) {
         super(var2);
      }

      protected void clampHeadRotationToBody() {
      }

      protected Optional<Float> getYRotD() {
         Direction var1 = Shulker.this.getAttachFace().getOpposite();
         Vector3f var2 = var1.getRotation().transform(new Vector3f(Shulker.FORWARD));
         Vec3i var3 = var1.getNormal();
         Vector3f var4 = new Vector3f((float)var3.getX(), (float)var3.getY(), (float)var3.getZ());
         var4.cross(var2);
         double var5 = this.wantedX - this.mob.getX();
         double var7 = this.wantedY - this.mob.getEyeY();
         double var9 = this.wantedZ - this.mob.getZ();
         Vector3f var11 = new Vector3f((float)var5, (float)var7, (float)var9);
         float var12 = var4.dot(var11);
         float var13 = var2.dot(var11);
         return !(Math.abs(var12) > 1.0E-5F) && !(Math.abs(var13) > 1.0E-5F) ? Optional.empty() : Optional.of((float)(Mth.atan2((double)(-var12), (double)var13) * 57.2957763671875D));
      }

      protected Optional<Float> getXRotD() {
         return Optional.of(0.0F);
      }
   }

   class ShulkerAttackGoal extends Goal {
      private int attackTime;

      public ShulkerAttackGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
      }

      public boolean canUse() {
         LivingEntity var1 = Shulker.this.getTarget();
         if (var1 != null && var1.isAlive()) {
            return Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL;
         } else {
            return false;
         }
      }

      public void start() {
         this.attackTime = 20;
         Shulker.this.setRawPeekAmount(100);
      }

      public void stop() {
         Shulker.this.setRawPeekAmount(0);
      }

      public boolean requiresUpdateEveryTick() {
         return true;
      }

      public void tick() {
         if (Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL) {
            --this.attackTime;
            LivingEntity var1 = Shulker.this.getTarget();
            if (var1 != null) {
               Shulker.this.getLookControl().setLookAt(var1, 180.0F, 180.0F);
               double var2 = Shulker.this.distanceToSqr(var1);
               if (var2 < 400.0D) {
                  if (this.attackTime <= 0) {
                     this.attackTime = 20 + Shulker.this.random.nextInt(10) * 20 / 2;
                     Shulker.this.level().addFreshEntity(new ShulkerBullet(Shulker.this.level(), Shulker.this, var1, Shulker.this.getAttachFace().getAxis()));
                     Shulker.this.playSound(SoundEvents.SHULKER_SHOOT, 2.0F, (Shulker.this.random.nextFloat() - Shulker.this.random.nextFloat()) * 0.2F + 1.0F);
                  }
               } else {
                  Shulker.this.setTarget((LivingEntity)null);
               }

               super.tick();
            }
         }
      }
   }

   class ShulkerPeekGoal extends Goal {
      private int peekTime;

      ShulkerPeekGoal() {
      }

      public boolean canUse() {
         return Shulker.this.getTarget() == null && Shulker.this.random.nextInt(reducedTickDelay(40)) == 0 && Shulker.this.canStayAt(Shulker.this.blockPosition(), Shulker.this.getAttachFace());
      }

      public boolean canContinueToUse() {
         return Shulker.this.getTarget() == null && this.peekTime > 0;
      }

      public void start() {
         this.peekTime = this.adjustedTickDelay(20 * (1 + Shulker.this.random.nextInt(3)));
         Shulker.this.setRawPeekAmount(30);
      }

      public void stop() {
         if (Shulker.this.getTarget() == null) {
            Shulker.this.setRawPeekAmount(0);
         }

      }

      public void tick() {
         --this.peekTime;
      }
   }

   class ShulkerNearestAttackGoal extends NearestAttackableTargetGoal<Player> {
      public ShulkerNearestAttackGoal(Shulker var2) {
         super(var2, Player.class, true);
      }

      public boolean canUse() {
         return Shulker.this.level().getDifficulty() == Difficulty.PEACEFUL ? false : super.canUse();
      }

      protected AABB getTargetSearchArea(double var1) {
         Direction var3 = ((Shulker)this.mob).getAttachFace();
         if (var3.getAxis() == Direction.Axis.X) {
            return this.mob.getBoundingBox().inflate(4.0D, var1, var1);
         } else {
            return var3.getAxis() == Direction.Axis.Z ? this.mob.getBoundingBox().inflate(var1, var1, 4.0D) : this.mob.getBoundingBox().inflate(var1, 4.0D, var1);
         }
      }
   }

   static class ShulkerDefenseAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
      public ShulkerDefenseAttackGoal(Shulker var1) {
         super(var1, LivingEntity.class, 10, true, false, (var0) -> {
            return var0 instanceof Enemy;
         });
      }

      public boolean canUse() {
         return this.mob.getTeam() == null ? false : super.canUse();
      }

      protected AABB getTargetSearchArea(double var1) {
         Direction var3 = ((Shulker)this.mob).getAttachFace();
         if (var3.getAxis() == Direction.Axis.X) {
            return this.mob.getBoundingBox().inflate(4.0D, var1, var1);
         } else {
            return var3.getAxis() == Direction.Axis.Z ? this.mob.getBoundingBox().inflate(var1, var1, 4.0D) : this.mob.getBoundingBox().inflate(var1, 4.0D, var1);
         }
      }
   }

   private static class ShulkerBodyRotationControl extends BodyRotationControl {
      public ShulkerBodyRotationControl(Mob var1) {
         super(var1);
      }

      public void clientTick() {
      }
   }
}
