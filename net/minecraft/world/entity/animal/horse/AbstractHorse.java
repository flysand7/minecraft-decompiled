package net.minecraft.world.entity.animal.horse;

import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStandGoal;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractHorse extends Animal implements ContainerListener, HasCustomInventoryScreen, OwnableEntity, PlayerRideableJumping, Saddleable {
   public static final int EQUIPMENT_SLOT_OFFSET = 400;
   public static final int CHEST_SLOT_OFFSET = 499;
   public static final int INVENTORY_SLOT_OFFSET = 500;
   public static final double BREEDING_CROSS_FACTOR = 0.15D;
   private static final float MIN_MOVEMENT_SPEED = (float)generateSpeed(() -> {
      return 0.0D;
   });
   private static final float MAX_MOVEMENT_SPEED = (float)generateSpeed(() -> {
      return 1.0D;
   });
   private static final float MIN_JUMP_STRENGTH = (float)generateJumpStrength(() -> {
      return 0.0D;
   });
   private static final float MAX_JUMP_STRENGTH = (float)generateJumpStrength(() -> {
      return 1.0D;
   });
   private static final float MIN_HEALTH = generateMaxHealth((var0) -> {
      return 0;
   });
   private static final float MAX_HEALTH = generateMaxHealth((var0) -> {
      return var0 - 1;
   });
   private static final float BACKWARDS_MOVE_SPEED_FACTOR = 0.25F;
   private static final float SIDEWAYS_MOVE_SPEED_FACTOR = 0.5F;
   private static final Predicate<LivingEntity> PARENT_HORSE_SELECTOR = (var0) -> {
      return var0 instanceof AbstractHorse && ((AbstractHorse)var0).isBred();
   };
   private static final TargetingConditions MOMMY_TARGETING;
   private static final Ingredient FOOD_ITEMS;
   private static final EntityDataAccessor<Byte> DATA_ID_FLAGS;
   private static final int FLAG_TAME = 2;
   private static final int FLAG_SADDLE = 4;
   private static final int FLAG_BRED = 8;
   private static final int FLAG_EATING = 16;
   private static final int FLAG_STANDING = 32;
   private static final int FLAG_OPEN_MOUTH = 64;
   public static final int INV_SLOT_SADDLE = 0;
   public static final int INV_SLOT_ARMOR = 1;
   public static final int INV_BASE_COUNT = 2;
   private int eatingCounter;
   private int mouthCounter;
   private int standCounter;
   public int tailCounter;
   public int sprintCounter;
   protected boolean isJumping;
   protected SimpleContainer inventory;
   protected int temper;
   protected float playerJumpPendingScale;
   protected boolean allowStandSliding;
   private float eatAnim;
   private float eatAnimO;
   private float standAnim;
   private float standAnimO;
   private float mouthAnim;
   private float mouthAnimO;
   protected boolean canGallop = true;
   protected int gallopSoundCounter;
   @Nullable
   private UUID owner;

   protected AbstractHorse(EntityType<? extends AbstractHorse> var1, Level var2) {
      super(var1, var2);
      this.setMaxUpStep(1.0F);
      this.createInventory();
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new PanicGoal(this, 1.2D));
      this.goalSelector.addGoal(1, new RunAroundLikeCrazyGoal(this, 1.2D));
      this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D, AbstractHorse.class));
      this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0D));
      this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
      this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
      this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      if (this.canPerformRearing()) {
         this.goalSelector.addGoal(9, new RandomStandGoal(this));
      }

      this.addBehaviourGoals();
   }

   protected void addBehaviourGoals() {
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(3, new TemptGoal(this, 1.25D, Ingredient.of(Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE), false));
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_FLAGS, (byte)0);
   }

   protected boolean getFlag(int var1) {
      return ((Byte)this.entityData.get(DATA_ID_FLAGS) & var1) != 0;
   }

   protected void setFlag(int var1, boolean var2) {
      byte var3 = (Byte)this.entityData.get(DATA_ID_FLAGS);
      if (var2) {
         this.entityData.set(DATA_ID_FLAGS, (byte)(var3 | var1));
      } else {
         this.entityData.set(DATA_ID_FLAGS, (byte)(var3 & ~var1));
      }

   }

   public boolean isTamed() {
      return this.getFlag(2);
   }

   @Nullable
   public UUID getOwnerUUID() {
      return this.owner;
   }

   public void setOwnerUUID(@Nullable UUID var1) {
      this.owner = var1;
   }

   public boolean isJumping() {
      return this.isJumping;
   }

   public void setTamed(boolean var1) {
      this.setFlag(2, var1);
   }

   public void setIsJumping(boolean var1) {
      this.isJumping = var1;
   }

   protected void onLeashDistance(float var1) {
      if (var1 > 6.0F && this.isEating()) {
         this.setEating(false);
      }

   }

   public boolean isEating() {
      return this.getFlag(16);
   }

   public boolean isStanding() {
      return this.getFlag(32);
   }

   public boolean isBred() {
      return this.getFlag(8);
   }

   public void setBred(boolean var1) {
      this.setFlag(8, var1);
   }

   public boolean isSaddleable() {
      return this.isAlive() && !this.isBaby() && this.isTamed();
   }

   public void equipSaddle(@Nullable SoundSource var1) {
      this.inventory.setItem(0, new ItemStack(Items.SADDLE));
   }

   public void equipArmor(Player var1, ItemStack var2) {
      if (this.isArmor(var2)) {
         this.inventory.setItem(1, var2.copyWithCount(1));
         if (!var1.getAbilities().instabuild) {
            var2.shrink(1);
         }
      }

   }

   public boolean isSaddled() {
      return this.getFlag(4);
   }

   public int getTemper() {
      return this.temper;
   }

   public void setTemper(int var1) {
      this.temper = var1;
   }

   public int modifyTemper(int var1) {
      int var2 = Mth.clamp(this.getTemper() + var1, 0, this.getMaxTemper());
      this.setTemper(var2);
      return var2;
   }

   public boolean isPushable() {
      return !this.isVehicle();
   }

   private void eating() {
      this.openMouth();
      if (!this.isSilent()) {
         SoundEvent var1 = this.getEatingSound();
         if (var1 != null) {
            this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), var1, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
         }
      }

   }

   public boolean causeFallDamage(float var1, float var2, DamageSource var3) {
      if (var1 > 1.0F) {
         this.playSound(SoundEvents.HORSE_LAND, 0.4F, 1.0F);
      }

      int var4 = this.calculateFallDamage(var1, var2);
      if (var4 <= 0) {
         return false;
      } else {
         this.hurt(var3, (float)var4);
         if (this.isVehicle()) {
            Iterator var5 = this.getIndirectPassengers().iterator();

            while(var5.hasNext()) {
               Entity var6 = (Entity)var5.next();
               var6.hurt(var3, (float)var4);
            }
         }

         this.playBlockFallSound();
         return true;
      }
   }

   protected int calculateFallDamage(float var1, float var2) {
      return Mth.ceil((var1 * 0.5F - 3.0F) * var2);
   }

   protected int getInventorySize() {
      return 2;
   }

   protected void createInventory() {
      SimpleContainer var1 = this.inventory;
      this.inventory = new SimpleContainer(this.getInventorySize());
      if (var1 != null) {
         var1.removeListener(this);
         int var2 = Math.min(var1.getContainerSize(), this.inventory.getContainerSize());

         for(int var3 = 0; var3 < var2; ++var3) {
            ItemStack var4 = var1.getItem(var3);
            if (!var4.isEmpty()) {
               this.inventory.setItem(var3, var4.copy());
            }
         }
      }

      this.inventory.addListener(this);
      this.updateContainerEquipment();
   }

   protected void updateContainerEquipment() {
      if (!this.level().isClientSide) {
         this.setFlag(4, !this.inventory.getItem(0).isEmpty());
      }
   }

   public void containerChanged(Container var1) {
      boolean var2 = this.isSaddled();
      this.updateContainerEquipment();
      if (this.tickCount > 20 && !var2 && this.isSaddled()) {
         this.playSound(this.getSaddleSoundEvent(), 0.5F, 1.0F);
      }

   }

   public double getCustomJump() {
      return this.getAttributeValue(Attributes.JUMP_STRENGTH);
   }

   public boolean hurt(DamageSource var1, float var2) {
      boolean var3 = super.hurt(var1, var2);
      if (var3 && this.random.nextInt(3) == 0) {
         this.standIfPossible();
      }

      return var3;
   }

   protected boolean canPerformRearing() {
      return true;
   }

   @Nullable
   protected SoundEvent getEatingSound() {
      return null;
   }

   @Nullable
   protected SoundEvent getAngrySound() {
      return null;
   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
      if (!var2.liquid()) {
         BlockState var3 = this.level().getBlockState(var1.above());
         SoundType var4 = var2.getSoundType();
         if (var3.is(Blocks.SNOW)) {
            var4 = var3.getSoundType();
         }

         if (this.isVehicle() && this.canGallop) {
            ++this.gallopSoundCounter;
            if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
               this.playGallopSound(var4);
            } else if (this.gallopSoundCounter <= 5) {
               this.playSound(SoundEvents.HORSE_STEP_WOOD, var4.getVolume() * 0.15F, var4.getPitch());
            }
         } else if (this.isWoodSoundType(var4)) {
            this.playSound(SoundEvents.HORSE_STEP_WOOD, var4.getVolume() * 0.15F, var4.getPitch());
         } else {
            this.playSound(SoundEvents.HORSE_STEP, var4.getVolume() * 0.15F, var4.getPitch());
         }

      }
   }

   private boolean isWoodSoundType(SoundType var1) {
      return var1 == SoundType.WOOD || var1 == SoundType.NETHER_WOOD || var1 == SoundType.STEM || var1 == SoundType.CHERRY_WOOD || var1 == SoundType.BAMBOO_WOOD;
   }

   protected void playGallopSound(SoundType var1) {
      this.playSound(SoundEvents.HORSE_GALLOP, var1.getVolume() * 0.15F, var1.getPitch());
   }

   public static AttributeSupplier.Builder createBaseHorseAttributes() {
      return Mob.createMobAttributes().add(Attributes.JUMP_STRENGTH).add(Attributes.MAX_HEALTH, 53.0D).add(Attributes.MOVEMENT_SPEED, 0.22499999403953552D);
   }

   public int getMaxSpawnClusterSize() {
      return 6;
   }

   public int getMaxTemper() {
      return 100;
   }

   protected float getSoundVolume() {
      return 0.8F;
   }

   public int getAmbientSoundInterval() {
      return 400;
   }

   public void openCustomInventoryScreen(Player var1) {
      if (!this.level().isClientSide && (!this.isVehicle() || this.hasPassenger(var1)) && this.isTamed()) {
         var1.openHorseInventory(this, this.inventory);
      }

   }

   public InteractionResult fedFood(Player var1, ItemStack var2) {
      boolean var3 = this.handleEating(var1, var2);
      if (!var1.getAbilities().instabuild) {
         var2.shrink(1);
      }

      if (this.level().isClientSide) {
         return InteractionResult.CONSUME;
      } else {
         return var3 ? InteractionResult.SUCCESS : InteractionResult.PASS;
      }
   }

   protected boolean handleEating(Player var1, ItemStack var2) {
      boolean var3 = false;
      float var4 = 0.0F;
      short var5 = 0;
      byte var6 = 0;
      if (var2.is(Items.WHEAT)) {
         var4 = 2.0F;
         var5 = 20;
         var6 = 3;
      } else if (var2.is(Items.SUGAR)) {
         var4 = 1.0F;
         var5 = 30;
         var6 = 3;
      } else if (var2.is(Blocks.HAY_BLOCK.asItem())) {
         var4 = 20.0F;
         var5 = 180;
      } else if (var2.is(Items.APPLE)) {
         var4 = 3.0F;
         var5 = 60;
         var6 = 3;
      } else if (var2.is(Items.GOLDEN_CARROT)) {
         var4 = 4.0F;
         var5 = 60;
         var6 = 5;
         if (!this.level().isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            var3 = true;
            this.setInLove(var1);
         }
      } else if (var2.is(Items.GOLDEN_APPLE) || var2.is(Items.ENCHANTED_GOLDEN_APPLE)) {
         var4 = 10.0F;
         var5 = 240;
         var6 = 10;
         if (!this.level().isClientSide && this.isTamed() && this.getAge() == 0 && !this.isInLove()) {
            var3 = true;
            this.setInLove(var1);
         }
      }

      if (this.getHealth() < this.getMaxHealth() && var4 > 0.0F) {
         this.heal(var4);
         var3 = true;
      }

      if (this.isBaby() && var5 > 0) {
         this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), 0.0D, 0.0D, 0.0D);
         if (!this.level().isClientSide) {
            this.ageUp(var5);
         }

         var3 = true;
      }

      if (var6 > 0 && (var3 || !this.isTamed()) && this.getTemper() < this.getMaxTemper()) {
         var3 = true;
         if (!this.level().isClientSide) {
            this.modifyTemper(var6);
         }
      }

      if (var3) {
         this.eating();
         this.gameEvent(GameEvent.EAT);
      }

      return var3;
   }

   protected void doPlayerRide(Player var1) {
      this.setEating(false);
      this.setStanding(false);
      if (!this.level().isClientSide) {
         var1.setYRot(this.getYRot());
         var1.setXRot(this.getXRot());
         var1.startRiding(this);
      }

   }

   public boolean isImmobile() {
      return super.isImmobile() && this.isVehicle() && this.isSaddled() || this.isEating() || this.isStanding();
   }

   public boolean isFood(ItemStack var1) {
      return FOOD_ITEMS.test(var1);
   }

   private void moveTail() {
      this.tailCounter = 1;
   }

   protected void dropEquipment() {
      super.dropEquipment();
      if (this.inventory != null) {
         for(int var1 = 0; var1 < this.inventory.getContainerSize(); ++var1) {
            ItemStack var2 = this.inventory.getItem(var1);
            if (!var2.isEmpty() && !EnchantmentHelper.hasVanishingCurse(var2)) {
               this.spawnAtLocation(var2);
            }
         }

      }
   }

   public void aiStep() {
      if (this.random.nextInt(200) == 0) {
         this.moveTail();
      }

      super.aiStep();
      if (!this.level().isClientSide && this.isAlive()) {
         if (this.random.nextInt(900) == 0 && this.deathTime == 0) {
            this.heal(1.0F);
         }

         if (this.canEatGrass()) {
            if (!this.isEating() && !this.isVehicle() && this.random.nextInt(300) == 0 && this.level().getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
               this.setEating(true);
            }

            if (this.isEating() && ++this.eatingCounter > 50) {
               this.eatingCounter = 0;
               this.setEating(false);
            }
         }

         this.followMommy();
      }
   }

   protected void followMommy() {
      if (this.isBred() && this.isBaby() && !this.isEating()) {
         LivingEntity var1 = this.level().getNearestEntity(AbstractHorse.class, MOMMY_TARGETING, this, this.getX(), this.getY(), this.getZ(), this.getBoundingBox().inflate(16.0D));
         if (var1 != null && this.distanceToSqr(var1) > 4.0D) {
            this.navigation.createPath((Entity)var1, 0);
         }
      }

   }

   public boolean canEatGrass() {
      return true;
   }

   public void tick() {
      super.tick();
      if (this.mouthCounter > 0 && ++this.mouthCounter > 30) {
         this.mouthCounter = 0;
         this.setFlag(64, false);
      }

      if (this.isEffectiveAi() && this.standCounter > 0 && ++this.standCounter > 20) {
         this.standCounter = 0;
         this.setStanding(false);
      }

      if (this.tailCounter > 0 && ++this.tailCounter > 8) {
         this.tailCounter = 0;
      }

      if (this.sprintCounter > 0) {
         ++this.sprintCounter;
         if (this.sprintCounter > 300) {
            this.sprintCounter = 0;
         }
      }

      this.eatAnimO = this.eatAnim;
      if (this.isEating()) {
         this.eatAnim += (1.0F - this.eatAnim) * 0.4F + 0.05F;
         if (this.eatAnim > 1.0F) {
            this.eatAnim = 1.0F;
         }
      } else {
         this.eatAnim += (0.0F - this.eatAnim) * 0.4F - 0.05F;
         if (this.eatAnim < 0.0F) {
            this.eatAnim = 0.0F;
         }
      }

      this.standAnimO = this.standAnim;
      if (this.isStanding()) {
         this.eatAnim = 0.0F;
         this.eatAnimO = this.eatAnim;
         this.standAnim += (1.0F - this.standAnim) * 0.4F + 0.05F;
         if (this.standAnim > 1.0F) {
            this.standAnim = 1.0F;
         }
      } else {
         this.allowStandSliding = false;
         this.standAnim += (0.8F * this.standAnim * this.standAnim * this.standAnim - this.standAnim) * 0.6F - 0.05F;
         if (this.standAnim < 0.0F) {
            this.standAnim = 0.0F;
         }
      }

      this.mouthAnimO = this.mouthAnim;
      if (this.getFlag(64)) {
         this.mouthAnim += (1.0F - this.mouthAnim) * 0.7F + 0.05F;
         if (this.mouthAnim > 1.0F) {
            this.mouthAnim = 1.0F;
         }
      } else {
         this.mouthAnim += (0.0F - this.mouthAnim) * 0.7F - 0.05F;
         if (this.mouthAnim < 0.0F) {
            this.mouthAnim = 0.0F;
         }
      }

   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      if (!this.isVehicle() && !this.isBaby()) {
         if (this.isTamed() && var1.isSecondaryUseActive()) {
            this.openCustomInventoryScreen(var1);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
         } else {
            ItemStack var3 = var1.getItemInHand(var2);
            if (!var3.isEmpty()) {
               InteractionResult var4 = var3.interactLivingEntity(var1, this, var2);
               if (var4.consumesAction()) {
                  return var4;
               }

               if (this.canWearArmor() && this.isArmor(var3) && !this.isWearingArmor()) {
                  this.equipArmor(var1, var3);
                  return InteractionResult.sidedSuccess(this.level().isClientSide);
               }
            }

            this.doPlayerRide(var1);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
         }
      } else {
         return super.mobInteract(var1, var2);
      }
   }

   private void openMouth() {
      if (!this.level().isClientSide) {
         this.mouthCounter = 1;
         this.setFlag(64, true);
      }

   }

   public void setEating(boolean var1) {
      this.setFlag(16, var1);
   }

   public void setStanding(boolean var1) {
      if (var1) {
         this.setEating(false);
      }

      this.setFlag(32, var1);
   }

   @Nullable
   public SoundEvent getAmbientStandSound() {
      return this.getAmbientSound();
   }

   public void standIfPossible() {
      if (this.canPerformRearing() && this.isEffectiveAi()) {
         this.standCounter = 1;
         this.setStanding(true);
      }

   }

   public void makeMad() {
      if (!this.isStanding()) {
         this.standIfPossible();
         SoundEvent var1 = this.getAngrySound();
         if (var1 != null) {
            this.playSound(var1, this.getSoundVolume(), this.getVoicePitch());
         }
      }

   }

   public boolean tameWithName(Player var1) {
      this.setOwnerUUID(var1.getUUID());
      this.setTamed(true);
      if (var1 instanceof ServerPlayer) {
         CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayer)var1, this);
      }

      this.level().broadcastEntityEvent(this, (byte)7);
      return true;
   }

   protected void tickRidden(Player var1, Vec3 var2) {
      super.tickRidden(var1, var2);
      Vec2 var3 = this.getRiddenRotation(var1);
      this.setRot(var3.y, var3.x);
      this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
      if (this.isControlledByLocalInstance()) {
         if (var2.z <= 0.0D) {
            this.gallopSoundCounter = 0;
         }

         if (this.onGround()) {
            this.setIsJumping(false);
            if (this.playerJumpPendingScale > 0.0F && !this.isJumping()) {
               this.executeRidersJump(this.playerJumpPendingScale, var2);
            }

            this.playerJumpPendingScale = 0.0F;
         }
      }

   }

   protected Vec2 getRiddenRotation(LivingEntity var1) {
      return new Vec2(var1.getXRot() * 0.5F, var1.getYRot());
   }

   protected Vec3 getRiddenInput(Player var1, Vec3 var2) {
      if (this.onGround() && this.playerJumpPendingScale == 0.0F && this.isStanding() && !this.allowStandSliding) {
         return Vec3.ZERO;
      } else {
         float var3 = var1.xxa * 0.5F;
         float var4 = var1.zza;
         if (var4 <= 0.0F) {
            var4 *= 0.25F;
         }

         return new Vec3((double)var3, 0.0D, (double)var4);
      }
   }

   protected float getRiddenSpeed(Player var1) {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   protected void executeRidersJump(float var1, Vec3 var2) {
      double var3 = this.getCustomJump() * (double)var1 * (double)this.getBlockJumpFactor();
      double var5 = var3 + (double)this.getJumpBoostPower();
      Vec3 var7 = this.getDeltaMovement();
      this.setDeltaMovement(var7.x, var5, var7.z);
      this.setIsJumping(true);
      this.hasImpulse = true;
      if (var2.z > 0.0D) {
         float var8 = Mth.sin(this.getYRot() * 0.017453292F);
         float var9 = Mth.cos(this.getYRot() * 0.017453292F);
         this.setDeltaMovement(this.getDeltaMovement().add((double)(-0.4F * var8 * var1), 0.0D, (double)(0.4F * var9 * var1)));
      }

   }

   protected void playJumpSound() {
      this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putBoolean("EatingHaystack", this.isEating());
      var1.putBoolean("Bred", this.isBred());
      var1.putInt("Temper", this.getTemper());
      var1.putBoolean("Tame", this.isTamed());
      if (this.getOwnerUUID() != null) {
         var1.putUUID("Owner", this.getOwnerUUID());
      }

      if (!this.inventory.getItem(0).isEmpty()) {
         var1.put("SaddleItem", this.inventory.getItem(0).save(new CompoundTag()));
      }

   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setEating(var1.getBoolean("EatingHaystack"));
      this.setBred(var1.getBoolean("Bred"));
      this.setTemper(var1.getInt("Temper"));
      this.setTamed(var1.getBoolean("Tame"));
      UUID var2;
      if (var1.hasUUID("Owner")) {
         var2 = var1.getUUID("Owner");
      } else {
         String var3 = var1.getString("Owner");
         var2 = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), var3);
      }

      if (var2 != null) {
         this.setOwnerUUID(var2);
      }

      if (var1.contains("SaddleItem", 10)) {
         ItemStack var4 = ItemStack.of(var1.getCompound("SaddleItem"));
         if (var4.is(Items.SADDLE)) {
            this.inventory.setItem(0, var4);
         }
      }

      this.updateContainerEquipment();
   }

   public boolean canMate(Animal var1) {
      return false;
   }

   protected boolean canParent() {
      return !this.isVehicle() && !this.isPassenger() && this.isTamed() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return null;
   }

   protected void setOffspringAttributes(AgeableMob var1, AbstractHorse var2) {
      this.setOffspringAttribute(var1, var2, Attributes.MAX_HEALTH, (double)MIN_HEALTH, (double)MAX_HEALTH);
      this.setOffspringAttribute(var1, var2, Attributes.JUMP_STRENGTH, (double)MIN_JUMP_STRENGTH, (double)MAX_JUMP_STRENGTH);
      this.setOffspringAttribute(var1, var2, Attributes.MOVEMENT_SPEED, (double)MIN_MOVEMENT_SPEED, (double)MAX_MOVEMENT_SPEED);
   }

   private void setOffspringAttribute(AgeableMob var1, AbstractHorse var2, Attribute var3, double var4, double var6) {
      double var8 = createOffspringAttribute(this.getAttributeBaseValue(var3), var1.getAttributeBaseValue(var3), var4, var6, this.random);
      var2.getAttribute(var3).setBaseValue(var8);
   }

   static double createOffspringAttribute(double var0, double var2, double var4, double var6, RandomSource var8) {
      if (var6 <= var4) {
         throw new IllegalArgumentException("Incorrect range for an attribute");
      } else {
         var0 = Mth.clamp(var0, var4, var6);
         var2 = Mth.clamp(var2, var4, var6);
         double var9 = 0.15D * (var6 - var4);
         double var11 = Math.abs(var0 - var2) + var9 * 2.0D;
         double var13 = (var0 + var2) / 2.0D;
         double var15 = (var8.nextDouble() + var8.nextDouble() + var8.nextDouble()) / 3.0D - 0.5D;
         double var17 = var13 + var11 * var15;
         double var19;
         if (var17 > var6) {
            var19 = var17 - var6;
            return var6 - var19;
         } else if (var17 < var4) {
            var19 = var4 - var17;
            return var4 + var19;
         } else {
            return var17;
         }
      }
   }

   public float getEatAnim(float var1) {
      return Mth.lerp(var1, this.eatAnimO, this.eatAnim);
   }

   public float getStandAnim(float var1) {
      return Mth.lerp(var1, this.standAnimO, this.standAnim);
   }

   public float getMouthAnim(float var1) {
      return Mth.lerp(var1, this.mouthAnimO, this.mouthAnim);
   }

   public void onPlayerJump(int var1) {
      if (this.isSaddled()) {
         if (var1 < 0) {
            var1 = 0;
         } else {
            this.allowStandSliding = true;
            this.standIfPossible();
         }

         if (var1 >= 90) {
            this.playerJumpPendingScale = 1.0F;
         } else {
            this.playerJumpPendingScale = 0.4F + 0.4F * (float)var1 / 90.0F;
         }

      }
   }

   public boolean canJump() {
      return this.isSaddled();
   }

   public void handleStartJump(int var1) {
      this.allowStandSliding = true;
      this.standIfPossible();
      this.playJumpSound();
   }

   public void handleStopJump() {
   }

   protected void spawnTamingParticles(boolean var1) {
      SimpleParticleType var2 = var1 ? ParticleTypes.HEART : ParticleTypes.SMOKE;

      for(int var3 = 0; var3 < 7; ++var3) {
         double var4 = this.random.nextGaussian() * 0.02D;
         double var6 = this.random.nextGaussian() * 0.02D;
         double var8 = this.random.nextGaussian() * 0.02D;
         this.level().addParticle(var2, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), var4, var6, var8);
      }

   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 7) {
         this.spawnTamingParticles(true);
      } else if (var1 == 6) {
         this.spawnTamingParticles(false);
      } else {
         super.handleEntityEvent(var1);
      }

   }

   protected void positionRider(Entity var1, Entity.MoveFunction var2) {
      super.positionRider(var1, var2);
      if (this.standAnimO > 0.0F) {
         float var3 = Mth.sin(this.yBodyRot * 0.017453292F);
         float var4 = Mth.cos(this.yBodyRot * 0.017453292F);
         float var5 = 0.7F * this.standAnimO;
         float var6 = 0.15F * this.standAnimO;
         var2.accept(var1, this.getX() + (double)(var5 * var3), this.getY() + this.getPassengersRidingOffset() + var1.getMyRidingOffset() + (double)var6, this.getZ() - (double)(var5 * var4));
         if (var1 instanceof LivingEntity) {
            ((LivingEntity)var1).yBodyRot = this.yBodyRot;
         }
      }

   }

   protected static float generateMaxHealth(IntUnaryOperator var0) {
      return 15.0F + (float)var0.applyAsInt(8) + (float)var0.applyAsInt(9);
   }

   protected static double generateJumpStrength(DoubleSupplier var0) {
      return 0.4000000059604645D + var0.getAsDouble() * 0.2D + var0.getAsDouble() * 0.2D + var0.getAsDouble() * 0.2D;
   }

   protected static double generateSpeed(DoubleSupplier var0) {
      return (0.44999998807907104D + var0.getAsDouble() * 0.3D + var0.getAsDouble() * 0.3D + var0.getAsDouble() * 0.3D) * 0.25D;
   }

   public boolean onClimbable() {
      return false;
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return var2.height * 0.95F;
   }

   public boolean canWearArmor() {
      return false;
   }

   public boolean isWearingArmor() {
      return !this.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
   }

   public boolean isArmor(ItemStack var1) {
      return false;
   }

   private SlotAccess createEquipmentSlotAccess(final int var1, final Predicate<ItemStack> var2) {
      return new SlotAccess() {
         public ItemStack get() {
            return AbstractHorse.this.inventory.getItem(var1);
         }

         public boolean set(ItemStack var1x) {
            if (!var2.test(var1x)) {
               return false;
            } else {
               AbstractHorse.this.inventory.setItem(var1, var1x);
               AbstractHorse.this.updateContainerEquipment();
               return true;
            }
         }
      };
   }

   public SlotAccess getSlot(int var1) {
      int var2 = var1 - 400;
      if (var2 >= 0 && var2 < 2 && var2 < this.inventory.getContainerSize()) {
         if (var2 == 0) {
            return this.createEquipmentSlotAccess(var2, (var0) -> {
               return var0.isEmpty() || var0.is(Items.SADDLE);
            });
         }

         if (var2 == 1) {
            if (!this.canWearArmor()) {
               return SlotAccess.NULL;
            }

            return this.createEquipmentSlotAccess(var2, (var1x) -> {
               return var1x.isEmpty() || this.isArmor(var1x);
            });
         }
      }

      int var3 = var1 - 500 + 2;
      return var3 >= 2 && var3 < this.inventory.getContainerSize() ? SlotAccess.forContainer(this.inventory, var3) : super.getSlot(var1);
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      Entity var3 = this.getFirstPassenger();
      if (var3 instanceof Mob) {
         Mob var1 = (Mob)var3;
         return var1;
      } else {
         if (this.isSaddled()) {
            var3 = this.getFirstPassenger();
            if (var3 instanceof Player) {
               Player var2 = (Player)var3;
               return var2;
            }
         }

         return null;
      }
   }

   @Nullable
   private Vec3 getDismountLocationInDirection(Vec3 var1, LivingEntity var2) {
      double var3 = this.getX() + var1.x;
      double var5 = this.getBoundingBox().minY;
      double var7 = this.getZ() + var1.z;
      BlockPos.MutableBlockPos var9 = new BlockPos.MutableBlockPos();
      UnmodifiableIterator var10 = var2.getDismountPoses().iterator();

      while(var10.hasNext()) {
         Pose var11 = (Pose)var10.next();
         var9.set(var3, var5, var7);
         double var12 = this.getBoundingBox().maxY + 0.75D;

         while(true) {
            double var14 = this.level().getBlockFloorHeight(var9);
            if ((double)var9.getY() + var14 > var12) {
               break;
            }

            if (DismountHelper.isBlockFloorValid(var14)) {
               AABB var16 = var2.getLocalBoundsForPose(var11);
               Vec3 var17 = new Vec3(var3, (double)var9.getY() + var14, var7);
               if (DismountHelper.canDismountTo(this.level(), var2, var16.move(var17))) {
                  var2.setPose(var11);
                  return var17;
               }
            }

            var9.move(Direction.UP);
            if (!((double)var9.getY() < var12)) {
               break;
            }
         }
      }

      return null;
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity var1) {
      Vec3 var2 = getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)var1.getBbWidth(), this.getYRot() + (var1.getMainArm() == HumanoidArm.RIGHT ? 90.0F : -90.0F));
      Vec3 var3 = this.getDismountLocationInDirection(var2, var1);
      if (var3 != null) {
         return var3;
      } else {
         Vec3 var4 = getCollisionHorizontalEscapeVector((double)this.getBbWidth(), (double)var1.getBbWidth(), this.getYRot() + (var1.getMainArm() == HumanoidArm.LEFT ? 90.0F : -90.0F));
         Vec3 var5 = this.getDismountLocationInDirection(var4, var1);
         return var5 != null ? var5 : this.position();
      }
   }

   protected void randomizeAttributes(RandomSource var1) {
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      if (var4 == null) {
         var4 = new AgeableMob.AgeableMobGroupData(0.2F);
      }

      this.randomizeAttributes(var1.getRandom());
      return super.finalizeSpawn(var1, var2, var3, (SpawnGroupData)var4, var5);
   }

   public boolean hasInventoryChanged(Container var1) {
      return this.inventory != var1;
   }

   public int getAmbientStandInterval() {
      return this.getAmbientSoundInterval();
   }

   // $FF: synthetic method
   public EntityGetter level() {
      return super.level();
   }

   static {
      MOMMY_TARGETING = TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight().selector(PARENT_HORSE_SELECTOR);
      FOOD_ITEMS = Ingredient.of(Items.WHEAT, Items.SUGAR, Blocks.HAY_BLOCK.asItem(), Items.APPLE, Items.GOLDEN_CARROT, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE);
      DATA_ID_FLAGS = SynchedEntityData.defineId(AbstractHorse.class, EntityDataSerializers.BYTE);
   }
}
