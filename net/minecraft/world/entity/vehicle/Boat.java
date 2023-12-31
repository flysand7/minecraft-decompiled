package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class Boat extends Entity implements VariantHolder<Boat.Type> {
   private static final EntityDataAccessor<Integer> DATA_ID_HURT;
   private static final EntityDataAccessor<Integer> DATA_ID_HURTDIR;
   private static final EntityDataAccessor<Float> DATA_ID_DAMAGE;
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE;
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_LEFT;
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_RIGHT;
   private static final EntityDataAccessor<Integer> DATA_ID_BUBBLE_TIME;
   public static final int PADDLE_LEFT = 0;
   public static final int PADDLE_RIGHT = 1;
   private static final int TIME_TO_EJECT = 60;
   private static final float PADDLE_SPEED = 0.3926991F;
   public static final double PADDLE_SOUND_TIME = 0.7853981852531433D;
   public static final int BUBBLE_TIME = 60;
   private final float[] paddlePositions;
   private float invFriction;
   private float outOfControlTicks;
   private float deltaRotation;
   private int lerpSteps;
   private double lerpX;
   private double lerpY;
   private double lerpZ;
   private double lerpYRot;
   private double lerpXRot;
   private boolean inputLeft;
   private boolean inputRight;
   private boolean inputUp;
   private boolean inputDown;
   private double waterLevel;
   private float landFriction;
   private Boat.Status status;
   private Boat.Status oldStatus;
   private double lastYd;
   private boolean isAboveBubbleColumn;
   private boolean bubbleColumnDirectionIsDown;
   private float bubbleMultiplier;
   private float bubbleAngle;
   private float bubbleAngleO;

   public Boat(EntityType<? extends Boat> var1, Level var2) {
      super(var1, var2);
      this.paddlePositions = new float[2];
      this.blocksBuilding = true;
   }

   public Boat(Level var1, double var2, double var4, double var6) {
      this(EntityType.BOAT, var1);
      this.setPos(var2, var4, var6);
      this.xo = var2;
      this.yo = var4;
      this.zo = var6;
   }

   protected float getEyeHeight(Pose var1, EntityDimensions var2) {
      return var2.height;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_ID_HURT, 0);
      this.entityData.define(DATA_ID_HURTDIR, 1);
      this.entityData.define(DATA_ID_DAMAGE, 0.0F);
      this.entityData.define(DATA_ID_TYPE, Boat.Type.OAK.ordinal());
      this.entityData.define(DATA_ID_PADDLE_LEFT, false);
      this.entityData.define(DATA_ID_PADDLE_RIGHT, false);
      this.entityData.define(DATA_ID_BUBBLE_TIME, 0);
   }

   public boolean canCollideWith(Entity var1) {
      return canVehicleCollide(this, var1);
   }

   public static boolean canVehicleCollide(Entity var0, Entity var1) {
      return (var1.canBeCollidedWith() || var1.isPushable()) && !var0.isPassengerOfSameVehicle(var1);
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public boolean isPushable() {
      return true;
   }

   protected Vec3 getRelativePortalPosition(Direction.Axis var1, BlockUtil.FoundRectangle var2) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(var1, var2));
   }

   public double getPassengersRidingOffset() {
      return this.getVariant() == Boat.Type.BAMBOO ? 0.25D : -0.1D;
   }

   public boolean hurt(DamageSource var1, float var2) {
      if (this.isInvulnerableTo(var1)) {
         return false;
      } else if (!this.level().isClientSide && !this.isRemoved()) {
         this.setHurtDir(-this.getHurtDir());
         this.setHurtTime(10);
         this.setDamage(this.getDamage() + var2 * 10.0F);
         this.markHurt();
         this.gameEvent(GameEvent.ENTITY_DAMAGE, var1.getEntity());
         boolean var3 = var1.getEntity() instanceof Player && ((Player)var1.getEntity()).getAbilities().instabuild;
         if (var3 || this.getDamage() > 40.0F) {
            if (!var3 && this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
               this.destroy(var1);
            }

            this.discard();
         }

         return true;
      } else {
         return true;
      }
   }

   protected void destroy(DamageSource var1) {
      this.spawnAtLocation(this.getDropItem());
   }

   public void onAboveBubbleCol(boolean var1) {
      if (!this.level().isClientSide) {
         this.isAboveBubbleColumn = true;
         this.bubbleColumnDirectionIsDown = var1;
         if (this.getBubbleTime() == 0) {
            this.setBubbleTime(60);
         }
      }

      this.level().addParticle(ParticleTypes.SPLASH, this.getX() + (double)this.random.nextFloat(), this.getY() + 0.7D, this.getZ() + (double)this.random.nextFloat(), 0.0D, 0.0D, 0.0D);
      if (this.random.nextInt(20) == 0) {
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false);
         this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
      }

   }

   public void push(Entity var1) {
      if (var1 instanceof Boat) {
         if (var1.getBoundingBox().minY < this.getBoundingBox().maxY) {
            super.push(var1);
         }
      } else if (var1.getBoundingBox().minY <= this.getBoundingBox().minY) {
         super.push(var1);
      }

   }

   public Item getDropItem() {
      Item var10000;
      switch(this.getVariant()) {
      case SPRUCE:
         var10000 = Items.SPRUCE_BOAT;
         break;
      case BIRCH:
         var10000 = Items.BIRCH_BOAT;
         break;
      case JUNGLE:
         var10000 = Items.JUNGLE_BOAT;
         break;
      case ACACIA:
         var10000 = Items.ACACIA_BOAT;
         break;
      case CHERRY:
         var10000 = Items.CHERRY_BOAT;
         break;
      case DARK_OAK:
         var10000 = Items.DARK_OAK_BOAT;
         break;
      case MANGROVE:
         var10000 = Items.MANGROVE_BOAT;
         break;
      case BAMBOO:
         var10000 = Items.BAMBOO_RAFT;
         break;
      default:
         var10000 = Items.OAK_BOAT;
      }

      return var10000;
   }

   public void animateHurt(float var1) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() * 11.0F);
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public void lerpTo(double var1, double var3, double var5, float var7, float var8, int var9, boolean var10) {
      this.lerpX = var1;
      this.lerpY = var3;
      this.lerpZ = var5;
      this.lerpYRot = (double)var7;
      this.lerpXRot = (double)var8;
      this.lerpSteps = 10;
   }

   public Direction getMotionDirection() {
      return this.getDirection().getClockWise();
   }

   public void tick() {
      this.oldStatus = this.status;
      this.status = this.getStatus();
      if (this.status != Boat.Status.UNDER_WATER && this.status != Boat.Status.UNDER_FLOWING_WATER) {
         this.outOfControlTicks = 0.0F;
      } else {
         ++this.outOfControlTicks;
      }

      if (!this.level().isClientSide && this.outOfControlTicks >= 60.0F) {
         this.ejectPassengers();
      }

      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      super.tick();
      this.tickLerp();
      if (this.isControlledByLocalInstance()) {
         if (!(this.getFirstPassenger() instanceof Player)) {
            this.setPaddleState(false, false);
         }

         this.floatBoat();
         if (this.level().isClientSide) {
            this.controlBoat();
            this.level().sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      this.tickBubbleColumn();

      for(int var1 = 0; var1 <= 1; ++var1) {
         if (this.getPaddleState(var1)) {
            if (!this.isSilent() && (double)(this.paddlePositions[var1] % 6.2831855F) <= 0.7853981852531433D && (double)((this.paddlePositions[var1] + 0.3926991F) % 6.2831855F) >= 0.7853981852531433D) {
               SoundEvent var2 = this.getPaddleSound();
               if (var2 != null) {
                  Vec3 var3 = this.getViewVector(1.0F);
                  double var4 = var1 == 1 ? -var3.z : var3.z;
                  double var6 = var1 == 1 ? var3.x : -var3.x;
                  this.level().playSound((Player)null, this.getX() + var4, this.getY(), this.getZ() + var6, var2, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());
               }
            }

            float[] var10000 = this.paddlePositions;
            var10000[var1] += 0.3926991F;
         } else {
            this.paddlePositions[var1] = 0.0F;
         }
      }

      this.checkInsideBlocks();
      List var8 = this.level().getEntities((Entity)this, this.getBoundingBox().inflate(0.20000000298023224D, -0.009999999776482582D, 0.20000000298023224D), EntitySelector.pushableBy(this));
      if (!var8.isEmpty()) {
         boolean var9 = !this.level().isClientSide && !(this.getControllingPassenger() instanceof Player);

         for(int var10 = 0; var10 < var8.size(); ++var10) {
            Entity var11 = (Entity)var8.get(var10);
            if (!var11.hasPassenger((Entity)this)) {
               if (var9 && this.getPassengers().size() < this.getMaxPassengers() && !var11.isPassenger() && this.hasEnoughSpaceFor(var11) && var11 instanceof LivingEntity && !(var11 instanceof WaterAnimal) && !(var11 instanceof Player)) {
                  var11.startRiding(this);
               } else {
                  this.push(var11);
               }
            }
         }
      }

   }

   private void tickBubbleColumn() {
      int var1;
      if (this.level().isClientSide) {
         var1 = this.getBubbleTime();
         if (var1 > 0) {
            this.bubbleMultiplier += 0.05F;
         } else {
            this.bubbleMultiplier -= 0.1F;
         }

         this.bubbleMultiplier = Mth.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
         this.bubbleAngleO = this.bubbleAngle;
         this.bubbleAngle = 10.0F * (float)Math.sin((double)(0.5F * (float)this.level().getGameTime())) * this.bubbleMultiplier;
      } else {
         if (!this.isAboveBubbleColumn) {
            this.setBubbleTime(0);
         }

         var1 = this.getBubbleTime();
         if (var1 > 0) {
            --var1;
            this.setBubbleTime(var1);
            int var2 = 60 - var1 - 1;
            if (var2 > 0 && var1 == 0) {
               this.setBubbleTime(0);
               Vec3 var3 = this.getDeltaMovement();
               if (this.bubbleColumnDirectionIsDown) {
                  this.setDeltaMovement(var3.add(0.0D, -0.7D, 0.0D));
                  this.ejectPassengers();
               } else {
                  this.setDeltaMovement(var3.x, this.hasPassenger((var0) -> {
                     return var0 instanceof Player;
                  }) ? 2.7D : 0.6D, var3.z);
               }
            }

            this.isAboveBubbleColumn = false;
         }
      }

   }

   @Nullable
   protected SoundEvent getPaddleSound() {
      switch(this.getStatus()) {
      case IN_WATER:
      case UNDER_WATER:
      case UNDER_FLOWING_WATER:
         return SoundEvents.BOAT_PADDLE_WATER;
      case ON_LAND:
         return SoundEvents.BOAT_PADDLE_LAND;
      case IN_AIR:
      default:
         return null;
      }
   }

   private void tickLerp() {
      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         double var1 = this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps;
         double var3 = this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps;
         double var5 = this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps;
         double var7 = Mth.wrapDegrees(this.lerpYRot - (double)this.getYRot());
         this.setYRot(this.getYRot() + (float)var7 / (float)this.lerpSteps);
         this.setXRot(this.getXRot() + (float)(this.lerpXRot - (double)this.getXRot()) / (float)this.lerpSteps);
         --this.lerpSteps;
         this.setPos(var1, var3, var5);
         this.setRot(this.getYRot(), this.getXRot());
      }
   }

   public void setPaddleState(boolean var1, boolean var2) {
      this.entityData.set(DATA_ID_PADDLE_LEFT, var1);
      this.entityData.set(DATA_ID_PADDLE_RIGHT, var2);
   }

   public float getRowingTime(int var1, float var2) {
      return this.getPaddleState(var1) ? Mth.clampedLerp(this.paddlePositions[var1] - 0.3926991F, this.paddlePositions[var1], var2) : 0.0F;
   }

   private Boat.Status getStatus() {
      Boat.Status var1 = this.isUnderwater();
      if (var1 != null) {
         this.waterLevel = this.getBoundingBox().maxY;
         return var1;
      } else if (this.checkInWater()) {
         return Boat.Status.IN_WATER;
      } else {
         float var2 = this.getGroundFriction();
         if (var2 > 0.0F) {
            this.landFriction = var2;
            return Boat.Status.ON_LAND;
         } else {
            return Boat.Status.IN_AIR;
         }
      }
   }

   public float getWaterLevelAbove() {
      AABB var1 = this.getBoundingBox();
      int var2 = Mth.floor(var1.minX);
      int var3 = Mth.ceil(var1.maxX);
      int var4 = Mth.floor(var1.maxY);
      int var5 = Mth.ceil(var1.maxY - this.lastYd);
      int var6 = Mth.floor(var1.minZ);
      int var7 = Mth.ceil(var1.maxZ);
      BlockPos.MutableBlockPos var8 = new BlockPos.MutableBlockPos();

      label39:
      for(int var9 = var4; var9 < var5; ++var9) {
         float var10 = 0.0F;

         for(int var11 = var2; var11 < var3; ++var11) {
            for(int var12 = var6; var12 < var7; ++var12) {
               var8.set(var11, var9, var12);
               FluidState var13 = this.level().getFluidState(var8);
               if (var13.is(FluidTags.WATER)) {
                  var10 = Math.max(var10, var13.getHeight(this.level(), var8));
               }

               if (var10 >= 1.0F) {
                  continue label39;
               }
            }
         }

         if (var10 < 1.0F) {
            return (float)var8.getY() + var10;
         }
      }

      return (float)(var5 + 1);
   }

   public float getGroundFriction() {
      AABB var1 = this.getBoundingBox();
      AABB var2 = new AABB(var1.minX, var1.minY - 0.001D, var1.minZ, var1.maxX, var1.minY, var1.maxZ);
      int var3 = Mth.floor(var2.minX) - 1;
      int var4 = Mth.ceil(var2.maxX) + 1;
      int var5 = Mth.floor(var2.minY) - 1;
      int var6 = Mth.ceil(var2.maxY) + 1;
      int var7 = Mth.floor(var2.minZ) - 1;
      int var8 = Mth.ceil(var2.maxZ) + 1;
      VoxelShape var9 = Shapes.create(var2);
      float var10 = 0.0F;
      int var11 = 0;
      BlockPos.MutableBlockPos var12 = new BlockPos.MutableBlockPos();

      for(int var13 = var3; var13 < var4; ++var13) {
         for(int var14 = var7; var14 < var8; ++var14) {
            int var15 = (var13 != var3 && var13 != var4 - 1 ? 0 : 1) + (var14 != var7 && var14 != var8 - 1 ? 0 : 1);
            if (var15 != 2) {
               for(int var16 = var5; var16 < var6; ++var16) {
                  if (var15 <= 0 || var16 != var5 && var16 != var6 - 1) {
                     var12.set(var13, var16, var14);
                     BlockState var17 = this.level().getBlockState(var12);
                     if (!(var17.getBlock() instanceof WaterlilyBlock) && Shapes.joinIsNotEmpty(var17.getCollisionShape(this.level(), var12).move((double)var13, (double)var16, (double)var14), var9, BooleanOp.AND)) {
                        var10 += var17.getBlock().getFriction();
                        ++var11;
                     }
                  }
               }
            }
         }
      }

      return var10 / (float)var11;
   }

   private boolean checkInWater() {
      AABB var1 = this.getBoundingBox();
      int var2 = Mth.floor(var1.minX);
      int var3 = Mth.ceil(var1.maxX);
      int var4 = Mth.floor(var1.minY);
      int var5 = Mth.ceil(var1.minY + 0.001D);
      int var6 = Mth.floor(var1.minZ);
      int var7 = Mth.ceil(var1.maxZ);
      boolean var8 = false;
      this.waterLevel = -1.7976931348623157E308D;
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();

      for(int var10 = var2; var10 < var3; ++var10) {
         for(int var11 = var4; var11 < var5; ++var11) {
            for(int var12 = var6; var12 < var7; ++var12) {
               var9.set(var10, var11, var12);
               FluidState var13 = this.level().getFluidState(var9);
               if (var13.is(FluidTags.WATER)) {
                  float var14 = (float)var11 + var13.getHeight(this.level(), var9);
                  this.waterLevel = Math.max((double)var14, this.waterLevel);
                  var8 |= var1.minY < (double)var14;
               }
            }
         }
      }

      return var8;
   }

   @Nullable
   private Boat.Status isUnderwater() {
      AABB var1 = this.getBoundingBox();
      double var2 = var1.maxY + 0.001D;
      int var4 = Mth.floor(var1.minX);
      int var5 = Mth.ceil(var1.maxX);
      int var6 = Mth.floor(var1.maxY);
      int var7 = Mth.ceil(var2);
      int var8 = Mth.floor(var1.minZ);
      int var9 = Mth.ceil(var1.maxZ);
      boolean var10 = false;
      BlockPos.MutableBlockPos var11 = new BlockPos.MutableBlockPos();

      for(int var12 = var4; var12 < var5; ++var12) {
         for(int var13 = var6; var13 < var7; ++var13) {
            for(int var14 = var8; var14 < var9; ++var14) {
               var11.set(var12, var13, var14);
               FluidState var15 = this.level().getFluidState(var11);
               if (var15.is(FluidTags.WATER) && var2 < (double)((float)var11.getY() + var15.getHeight(this.level(), var11))) {
                  if (!var15.isSource()) {
                     return Boat.Status.UNDER_FLOWING_WATER;
                  }

                  var10 = true;
               }
            }
         }
      }

      return var10 ? Boat.Status.UNDER_WATER : null;
   }

   private void floatBoat() {
      double var1 = -0.03999999910593033D;
      double var3 = this.isNoGravity() ? 0.0D : -0.03999999910593033D;
      double var5 = 0.0D;
      this.invFriction = 0.05F;
      if (this.oldStatus == Boat.Status.IN_AIR && this.status != Boat.Status.IN_AIR && this.status != Boat.Status.ON_LAND) {
         this.waterLevel = this.getY(1.0D);
         this.setPos(this.getX(), (double)(this.getWaterLevelAbove() - this.getBbHeight()) + 0.101D, this.getZ());
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
         this.lastYd = 0.0D;
         this.status = Boat.Status.IN_WATER;
      } else {
         if (this.status == Boat.Status.IN_WATER) {
            var5 = (this.waterLevel - this.getY()) / (double)this.getBbHeight();
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_FLOWING_WATER) {
            var3 = -7.0E-4D;
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_WATER) {
            var5 = 0.009999999776482582D;
            this.invFriction = 0.45F;
         } else if (this.status == Boat.Status.IN_AIR) {
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.ON_LAND) {
            this.invFriction = this.landFriction;
            if (this.getControllingPassenger() instanceof Player) {
               this.landFriction /= 2.0F;
            }
         }

         Vec3 var7 = this.getDeltaMovement();
         this.setDeltaMovement(var7.x * (double)this.invFriction, var7.y + var3, var7.z * (double)this.invFriction);
         this.deltaRotation *= this.invFriction;
         if (var5 > 0.0D) {
            Vec3 var8 = this.getDeltaMovement();
            this.setDeltaMovement(var8.x, (var8.y + var5 * 0.06153846016296973D) * 0.75D, var8.z);
         }
      }

   }

   private void controlBoat() {
      if (this.isVehicle()) {
         float var1 = 0.0F;
         if (this.inputLeft) {
            --this.deltaRotation;
         }

         if (this.inputRight) {
            ++this.deltaRotation;
         }

         if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
            var1 += 0.005F;
         }

         this.setYRot(this.getYRot() + this.deltaRotation);
         if (this.inputUp) {
            var1 += 0.04F;
         }

         if (this.inputDown) {
            var1 -= 0.005F;
         }

         this.setDeltaMovement(this.getDeltaMovement().add((double)(Mth.sin(-this.getYRot() * 0.017453292F) * var1), 0.0D, (double)(Mth.cos(this.getYRot() * 0.017453292F) * var1)));
         this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
      }
   }

   protected float getSinglePassengerXOffset() {
      return 0.0F;
   }

   public boolean hasEnoughSpaceFor(Entity var1) {
      return var1.getBbWidth() < this.getBbWidth();
   }

   protected void positionRider(Entity var1, Entity.MoveFunction var2) {
      if (this.hasPassenger(var1)) {
         float var3 = this.getSinglePassengerXOffset();
         float var4 = (float)((this.isRemoved() ? 0.009999999776482582D : this.getPassengersRidingOffset()) + var1.getMyRidingOffset());
         if (this.getPassengers().size() > 1) {
            int var5 = this.getPassengers().indexOf(var1);
            if (var5 == 0) {
               var3 = 0.2F;
            } else {
               var3 = -0.6F;
            }

            if (var1 instanceof Animal) {
               var3 += 0.2F;
            }
         }

         Vec3 var7 = (new Vec3((double)var3, 0.0D, 0.0D)).yRot(-this.getYRot() * 0.017453292F - 1.5707964F);
         var2.accept(var1, this.getX() + var7.x, this.getY() + (double)var4, this.getZ() + var7.z);
         var1.setYRot(var1.getYRot() + this.deltaRotation);
         var1.setYHeadRot(var1.getYHeadRot() + this.deltaRotation);
         this.clampRotation(var1);
         if (var1 instanceof Animal && this.getPassengers().size() == this.getMaxPassengers()) {
            int var6 = var1.getId() % 2 == 0 ? 90 : 270;
            var1.setYBodyRot(((Animal)var1).yBodyRot + (float)var6);
            var1.setYHeadRot(var1.getYHeadRot() + (float)var6);
         }

      }
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity var1) {
      Vec3 var2 = getCollisionHorizontalEscapeVector((double)(this.getBbWidth() * Mth.SQRT_OF_TWO), (double)var1.getBbWidth(), var1.getYRot());
      double var3 = this.getX() + var2.x;
      double var5 = this.getZ() + var2.z;
      BlockPos var7 = BlockPos.containing(var3, this.getBoundingBox().maxY, var5);
      BlockPos var8 = var7.below();
      if (!this.level().isWaterAt(var8)) {
         ArrayList var9 = Lists.newArrayList();
         double var10 = this.level().getBlockFloorHeight(var7);
         if (DismountHelper.isBlockFloorValid(var10)) {
            var9.add(new Vec3(var3, (double)var7.getY() + var10, var5));
         }

         double var12 = this.level().getBlockFloorHeight(var8);
         if (DismountHelper.isBlockFloorValid(var12)) {
            var9.add(new Vec3(var3, (double)var8.getY() + var12, var5));
         }

         UnmodifiableIterator var14 = var1.getDismountPoses().iterator();

         while(var14.hasNext()) {
            Pose var15 = (Pose)var14.next();
            Iterator var16 = var9.iterator();

            while(var16.hasNext()) {
               Vec3 var17 = (Vec3)var16.next();
               if (DismountHelper.canDismountTo(this.level(), var17, var1, var15)) {
                  var1.setPose(var15);
                  return var17;
               }
            }
         }
      }

      return super.getDismountLocationForPassenger(var1);
   }

   protected void clampRotation(Entity var1) {
      var1.setYBodyRot(this.getYRot());
      float var2 = Mth.wrapDegrees(var1.getYRot() - this.getYRot());
      float var3 = Mth.clamp(var2, -105.0F, 105.0F);
      var1.yRotO += var3 - var2;
      var1.setYRot(var1.getYRot() + var3 - var2);
      var1.setYHeadRot(var1.getYRot());
   }

   public void onPassengerTurned(Entity var1) {
      this.clampRotation(var1);
   }

   protected void addAdditionalSaveData(CompoundTag var1) {
      var1.putString("Type", this.getVariant().getSerializedName());
   }

   protected void readAdditionalSaveData(CompoundTag var1) {
      if (var1.contains("Type", 8)) {
         this.setVariant(Boat.Type.byName(var1.getString("Type")));
      }

   }

   public InteractionResult interact(Player var1, InteractionHand var2) {
      if (var1.isSecondaryUseActive()) {
         return InteractionResult.PASS;
      } else if (this.outOfControlTicks < 60.0F) {
         if (!this.level().isClientSide) {
            return var1.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void checkFallDamage(double var1, boolean var3, BlockState var4, BlockPos var5) {
      this.lastYd = this.getDeltaMovement().y;
      if (!this.isPassenger()) {
         if (var3) {
            if (this.fallDistance > 3.0F) {
               if (this.status != Boat.Status.ON_LAND) {
                  this.resetFallDistance();
                  return;
               }

               this.causeFallDamage(this.fallDistance, 1.0F, this.damageSources().fall());
               if (!this.level().isClientSide && !this.isRemoved()) {
                  this.kill();
                  if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                     int var6;
                     for(var6 = 0; var6 < 3; ++var6) {
                        this.spawnAtLocation(this.getVariant().getPlanks());
                     }

                     for(var6 = 0; var6 < 2; ++var6) {
                        this.spawnAtLocation(Items.STICK);
                     }
                  }
               }
            }

            this.resetFallDistance();
         } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && var1 < 0.0D) {
            this.fallDistance -= (float)var1;
         }

      }
   }

   public boolean getPaddleState(int var1) {
      return (Boolean)this.entityData.get(var1 == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) && this.getControllingPassenger() != null;
   }

   public void setDamage(float var1) {
      this.entityData.set(DATA_ID_DAMAGE, var1);
   }

   public float getDamage() {
      return (Float)this.entityData.get(DATA_ID_DAMAGE);
   }

   public void setHurtTime(int var1) {
      this.entityData.set(DATA_ID_HURT, var1);
   }

   public int getHurtTime() {
      return (Integer)this.entityData.get(DATA_ID_HURT);
   }

   private void setBubbleTime(int var1) {
      this.entityData.set(DATA_ID_BUBBLE_TIME, var1);
   }

   private int getBubbleTime() {
      return (Integer)this.entityData.get(DATA_ID_BUBBLE_TIME);
   }

   public float getBubbleAngle(float var1) {
      return Mth.lerp(var1, this.bubbleAngleO, this.bubbleAngle);
   }

   public void setHurtDir(int var1) {
      this.entityData.set(DATA_ID_HURTDIR, var1);
   }

   public int getHurtDir() {
      return (Integer)this.entityData.get(DATA_ID_HURTDIR);
   }

   public void setVariant(Boat.Type var1) {
      this.entityData.set(DATA_ID_TYPE, var1.ordinal());
   }

   public Boat.Type getVariant() {
      return Boat.Type.byId((Integer)this.entityData.get(DATA_ID_TYPE));
   }

   protected boolean canAddPassenger(Entity var1) {
      return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(FluidTags.WATER);
   }

   protected int getMaxPassengers() {
      return 2;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      Entity var2 = this.getFirstPassenger();
      LivingEntity var10000;
      if (var2 instanceof LivingEntity) {
         LivingEntity var1 = (LivingEntity)var2;
         var10000 = var1;
      } else {
         var10000 = null;
      }

      return var10000;
   }

   public void setInput(boolean var1, boolean var2, boolean var3, boolean var4) {
      this.inputLeft = var1;
      this.inputRight = var2;
      this.inputUp = var3;
      this.inputDown = var4;
   }

   public boolean isUnderWater() {
      return this.status == Boat.Status.UNDER_WATER || this.status == Boat.Status.UNDER_FLOWING_WATER;
   }

   public ItemStack getPickResult() {
      return new ItemStack(this.getDropItem());
   }

   // $FF: synthetic method
   public Object getVariant() {
      return this.getVariant();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void setVariant(Object var1) {
      this.setVariant((Boat.Type)var1);
   }

   static {
      DATA_ID_HURT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
      DATA_ID_HURTDIR = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
      DATA_ID_DAMAGE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.FLOAT);
      DATA_ID_TYPE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
      DATA_ID_PADDLE_LEFT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
      DATA_ID_PADDLE_RIGHT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
      DATA_ID_BUBBLE_TIME = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   }

   public static enum Type implements StringRepresentable {
      OAK(Blocks.OAK_PLANKS, "oak"),
      SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
      BIRCH(Blocks.BIRCH_PLANKS, "birch"),
      JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
      ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
      CHERRY(Blocks.CHERRY_PLANKS, "cherry"),
      DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak"),
      MANGROVE(Blocks.MANGROVE_PLANKS, "mangrove"),
      BAMBOO(Blocks.BAMBOO_PLANKS, "bamboo");

      private final String name;
      private final Block planks;
      public static final StringRepresentable.EnumCodec<Boat.Type> CODEC = StringRepresentable.fromEnum(Boat.Type::values);
      private static final IntFunction<Boat.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

      private Type(Block var3, String var4) {
         this.name = var4;
         this.planks = var3;
      }

      public String getSerializedName() {
         return this.name;
      }

      public String getName() {
         return this.name;
      }

      public Block getPlanks() {
         return this.planks;
      }

      public String toString() {
         return this.name;
      }

      public static Boat.Type byId(int var0) {
         return (Boat.Type)BY_ID.apply(var0);
      }

      public static Boat.Type byName(String var0) {
         return (Boat.Type)CODEC.byName(var0, OAK);
      }

      // $FF: synthetic method
      private static Boat.Type[] $values() {
         return new Boat.Type[]{OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, CHERRY, DARK_OAK, MANGROVE, BAMBOO};
      }
   }

   public static enum Status {
      IN_WATER,
      UNDER_WATER,
      UNDER_FLOWING_WATER,
      ON_LAND,
      IN_AIR;

      private Status() {
      }

      // $FF: synthetic method
      private static Boat.Status[] $values() {
         return new Boat.Status[]{IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR};
      }
   }
}
