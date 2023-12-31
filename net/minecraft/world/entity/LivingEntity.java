package net.minecraft.world.entity;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;

public abstract class LivingEntity extends Entity implements Attackable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final UUID SPEED_MODIFIER_SPRINTING_UUID = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
   private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
   private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
   private static final AttributeModifier SPEED_MODIFIER_SPRINTING;
   public static final int HAND_SLOTS = 2;
   public static final int ARMOR_SLOTS = 4;
   public static final int EQUIPMENT_SLOT_OFFSET = 98;
   public static final int ARMOR_SLOT_OFFSET = 100;
   public static final int SWING_DURATION = 6;
   public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
   private static final int DAMAGE_SOURCE_TIMEOUT = 40;
   public static final double MIN_MOVEMENT_DISTANCE = 0.003D;
   public static final double DEFAULT_BASE_GRAVITY = 0.08D;
   public static final int DEATH_DURATION = 20;
   private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
   private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
   private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
   public static final int USE_ITEM_INTERVAL = 4;
   private static final float BASE_JUMP_POWER = 0.42F;
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0D;
   protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
   protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
   protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
   protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS;
   private static final EntityDataAccessor<Float> DATA_HEALTH_ID;
   private static final EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID;
   private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID;
   private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID;
   private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID;
   private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID;
   protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
   protected static final EntityDimensions SLEEPING_DIMENSIONS;
   public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
   private static final int MAX_HEAD_ROTATION_RELATIVE_TO_BODY = 50;
   private final AttributeMap attributes;
   private final CombatTracker combatTracker = new CombatTracker(this);
   private final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();
   private final NonNullList<ItemStack> lastHandItemStacks;
   private final NonNullList<ItemStack> lastArmorItemStacks;
   public boolean swinging;
   private boolean discardFriction;
   public InteractionHand swingingArm;
   public int swingTime;
   public int removeArrowTime;
   public int removeStingerTime;
   public int hurtTime;
   public int hurtDuration;
   public int deathTime;
   public float oAttackAnim;
   public float attackAnim;
   protected int attackStrengthTicker;
   public final WalkAnimationState walkAnimation;
   public final int invulnerableDuration;
   public final float timeOffs;
   public final float rotA;
   public float yBodyRot;
   public float yBodyRotO;
   public float yHeadRot;
   public float yHeadRotO;
   @Nullable
   protected Player lastHurtByPlayer;
   protected int lastHurtByPlayerTime;
   protected boolean dead;
   protected int noActionTime;
   protected float oRun;
   protected float run;
   protected float animStep;
   protected float animStepO;
   protected float rotOffs;
   protected int deathScore;
   protected float lastHurt;
   protected boolean jumping;
   public float xxa;
   public float yya;
   public float zza;
   protected int lerpSteps;
   protected double lerpX;
   protected double lerpY;
   protected double lerpZ;
   protected double lerpYRot;
   protected double lerpXRot;
   protected double lyHeadRot;
   protected int lerpHeadSteps;
   private boolean effectsDirty;
   @Nullable
   private LivingEntity lastHurtByMob;
   private int lastHurtByMobTimestamp;
   private LivingEntity lastHurtMob;
   private int lastHurtMobTimestamp;
   private float speed;
   private int noJumpDelay;
   private float absorptionAmount;
   protected ItemStack useItem;
   protected int useItemRemaining;
   protected int fallFlyTicks;
   private BlockPos lastPos;
   private Optional<BlockPos> lastClimbablePos;
   @Nullable
   private DamageSource lastDamageSource;
   private long lastDamageStamp;
   protected int autoSpinAttackTicks;
   private float swimAmount;
   private float swimAmountO;
   protected Brain<?> brain;
   private boolean skipDropExperience;

   protected LivingEntity(EntityType<? extends LivingEntity> var1, Level var2) {
      super(var1, var2);
      this.lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
      this.lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
      this.discardFriction = false;
      this.walkAnimation = new WalkAnimationState();
      this.invulnerableDuration = 20;
      this.effectsDirty = true;
      this.useItem = ItemStack.EMPTY;
      this.lastClimbablePos = Optional.empty();
      this.attributes = new AttributeMap(DefaultAttributes.getSupplier(var1));
      this.setHealth(this.getMaxHealth());
      this.blocksBuilding = true;
      this.rotA = (float)((Math.random() + 1.0D) * 0.009999999776482582D);
      this.reapplyPosition();
      this.timeOffs = (float)Math.random() * 12398.0F;
      this.setYRot((float)(Math.random() * 6.2831854820251465D));
      this.yHeadRot = this.getYRot();
      this.setMaxUpStep(0.6F);
      NbtOps var3 = NbtOps.INSTANCE;
      this.brain = this.makeBrain(new Dynamic(var3, (Tag)var3.createMap((Map)ImmutableMap.of(var3.createString("memories"), (Tag)var3.emptyMap()))));
   }

   public Brain<?> getBrain() {
      return this.brain;
   }

   protected Brain.Provider<?> brainProvider() {
      return Brain.provider(ImmutableList.of(), ImmutableList.of());
   }

   protected Brain<?> makeBrain(Dynamic<?> var1) {
      return this.brainProvider().makeBrain(var1);
   }

   public void kill() {
      this.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
   }

   public boolean canAttackType(EntityType<?> var1) {
      return true;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
      this.entityData.define(DATA_EFFECT_COLOR_ID, 0);
      this.entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.define(DATA_ARROW_COUNT_ID, 0);
      this.entityData.define(DATA_STINGER_COUNT_ID, 0);
      this.entityData.define(DATA_HEALTH_ID, 1.0F);
      this.entityData.define(SLEEPING_POS_ID, Optional.empty());
   }

   public static AttributeSupplier.Builder createLivingAttributes() {
      return AttributeSupplier.builder().add(Attributes.MAX_HEALTH).add(Attributes.KNOCKBACK_RESISTANCE).add(Attributes.MOVEMENT_SPEED).add(Attributes.ARMOR).add(Attributes.ARMOR_TOUGHNESS);
   }

   protected void checkFallDamage(double var1, boolean var3, BlockState var4, BlockPos var5) {
      if (!this.isInWater()) {
         this.updateInWaterStateAndDoWaterCurrentPushing();
      }

      if (!this.level().isClientSide && var3 && this.fallDistance > 0.0F) {
         this.removeSoulSpeed();
         this.tryAddSoulSpeed();
      }

      if (!this.level().isClientSide && this.fallDistance > 3.0F && var3 && !var4.isAir()) {
         double var6 = this.getX();
         double var8 = this.getY();
         double var10 = this.getZ();
         BlockPos var12 = this.blockPosition();
         if (var5.getX() != var12.getX() || var5.getZ() != var12.getZ()) {
            double var13 = var6 - (double)var5.getX() - 0.5D;
            double var15 = var10 - (double)var5.getZ() - 0.5D;
            double var17 = Math.max(Math.abs(var13), Math.abs(var15));
            var6 = (double)var5.getX() + 0.5D + var13 / var17 * 0.5D;
            var10 = (double)var5.getZ() + 0.5D + var15 / var17 * 0.5D;
         }

         float var19 = (float)Mth.ceil(this.fallDistance - 3.0F);
         double var14 = Math.min((double)(0.2F + var19 / 15.0F), 2.5D);
         int var16 = (int)(150.0D * var14);
         ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, var4), var6, var8, var10, var16, 0.0D, 0.0D, 0.0D, 0.15000000596046448D);
      }

      super.checkFallDamage(var1, var3, var4, var5);
      if (var3) {
         this.lastClimbablePos = Optional.empty();
      }

   }

   public boolean canBreatheUnderwater() {
      return this.getMobType() == MobType.UNDEAD;
   }

   public float getSwimAmount(float var1) {
      return Mth.lerp(var1, this.swimAmountO, this.swimAmount);
   }

   public void baseTick() {
      this.oAttackAnim = this.attackAnim;
      if (this.firstTick) {
         this.getSleepingPos().ifPresent(this::setPosToBed);
      }

      if (this.canSpawnSoulSpeedParticle()) {
         this.spawnSoulSpeedParticle();
      }

      super.baseTick();
      this.level().getProfiler().push("livingEntityBaseTick");
      if (this.fireImmune() || this.level().isClientSide) {
         this.clearFire();
      }

      if (this.isAlive()) {
         boolean var1 = this instanceof Player;
         if (!this.level().isClientSide) {
            if (this.isInWall()) {
               this.hurt(this.damageSources().inWall(), 1.0F);
            } else if (var1 && !this.level().getWorldBorder().isWithinBounds(this.getBoundingBox())) {
               double var2 = this.level().getWorldBorder().getDistanceToBorder(this) + this.level().getWorldBorder().getDamageSafeZone();
               if (var2 < 0.0D) {
                  double var4 = this.level().getWorldBorder().getDamagePerBlock();
                  if (var4 > 0.0D) {
                     this.hurt(this.damageSources().outOfBorder(), (float)Math.max(1, Mth.floor(-var2 * var4)));
                  }
               }
            }
         }

         if (this.isEyeInFluid(FluidTags.WATER) && !this.level().getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            boolean var11 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!var1 || !((Player)this).getAbilities().invulnerable);
            if (var11) {
               this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
               if (this.getAirSupply() == -20) {
                  this.setAirSupply(0);
                  Vec3 var3 = this.getDeltaMovement();

                  for(int var13 = 0; var13 < 8; ++var13) {
                     double var5 = this.random.nextDouble() - this.random.nextDouble();
                     double var7 = this.random.nextDouble() - this.random.nextDouble();
                     double var9 = this.random.nextDouble() - this.random.nextDouble();
                     this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + var5, this.getY() + var7, this.getZ() + var9, var3.x, var3.y, var3.z);
                  }

                  this.hurt(this.damageSources().drown(), 2.0F);
               }
            }

            if (!this.level().isClientSide && this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
               this.stopRiding();
            }
         } else if (this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
         }

         if (!this.level().isClientSide) {
            BlockPos var12 = this.blockPosition();
            if (!Objects.equal(this.lastPos, var12)) {
               this.lastPos = var12;
               this.onChangedBlock(var12);
            }
         }
      }

      if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
         this.extinguishFire();
      }

      if (this.hurtTime > 0) {
         --this.hurtTime;
      }

      if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
         --this.invulnerableTime;
      }

      if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
         this.tickDeath();
      }

      if (this.lastHurtByPlayerTime > 0) {
         --this.lastHurtByPlayerTime;
      } else {
         this.lastHurtByPlayer = null;
      }

      if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
         this.lastHurtMob = null;
      }

      if (this.lastHurtByMob != null) {
         if (!this.lastHurtByMob.isAlive()) {
            this.setLastHurtByMob((LivingEntity)null);
         } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
            this.setLastHurtByMob((LivingEntity)null);
         }
      }

      this.tickEffects();
      this.animStepO = this.animStep;
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.level().getProfiler().pop();
   }

   public boolean canSpawnSoulSpeedParticle() {
      return this.tickCount % 5 == 0 && this.getDeltaMovement().x != 0.0D && this.getDeltaMovement().z != 0.0D && !this.isSpectator() && EnchantmentHelper.hasSoulSpeed(this) && this.onSoulSpeedBlock();
   }

   protected void spawnSoulSpeedParticle() {
      Vec3 var1 = this.getDeltaMovement();
      this.level().addParticle(ParticleTypes.SOUL, this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), var1.x * -0.2D, 0.1D, var1.z * -0.2D);
      float var2 = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;
      this.playSound(SoundEvents.SOUL_ESCAPE, var2, 0.6F + this.random.nextFloat() * 0.4F);
   }

   protected boolean onSoulSpeedBlock() {
      return this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.SOUL_SPEED_BLOCKS);
   }

   protected float getBlockSpeedFactor() {
      return this.onSoulSpeedBlock() && EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
   }

   protected boolean shouldRemoveSoulSpeed(BlockState var1) {
      return !var1.isAir() || this.isFallFlying();
   }

   protected void removeSoulSpeed() {
      AttributeInstance var1 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var1 != null) {
         if (var1.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
            var1.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
         }

      }
   }

   protected void tryAddSoulSpeed() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int var1 = EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);
         if (var1 > 0 && this.onSoulSpeedBlock()) {
            AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (var2 == null) {
               return;
            }

            var2.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", (double)(0.03F * (1.0F + (float)var1 * 0.35F)), AttributeModifier.Operation.ADDITION));
            if (this.getRandom().nextFloat() < 0.04F) {
               ItemStack var3 = this.getItemBySlot(EquipmentSlot.FEET);
               var3.hurtAndBreak(1, this, (var0) -> {
                  var0.broadcastBreakEvent(EquipmentSlot.FEET);
               });
            }
         }
      }

   }

   protected void removeFrost() {
      AttributeInstance var1 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var1 != null) {
         if (var1.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
            var1.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
         }

      }
   }

   protected void tryAddFrost() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int var1 = this.getTicksFrozen();
         if (var1 > 0) {
            AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (var2 == null) {
               return;
            }

            float var3 = -0.05F * this.getPercentFrozen();
            var2.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", (double)var3, AttributeModifier.Operation.ADDITION));
         }
      }

   }

   protected void onChangedBlock(BlockPos var1) {
      int var2 = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, this);
      if (var2 > 0) {
         FrostWalkerEnchantment.onEntityMoved(this, this.level(), var1, var2);
      }

      if (this.shouldRemoveSoulSpeed(this.getBlockStateOnLegacy())) {
         this.removeSoulSpeed();
      }

      this.tryAddSoulSpeed();
   }

   public boolean isBaby() {
      return false;
   }

   public float getScale() {
      return this.isBaby() ? 0.5F : 1.0F;
   }

   protected boolean isAffectedByFluids() {
      return true;
   }

   protected void tickDeath() {
      ++this.deathTime;
      if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }

   }

   public boolean shouldDropExperience() {
      return !this.isBaby();
   }

   protected boolean shouldDropLoot() {
      return !this.isBaby();
   }

   protected int decreaseAirSupply(int var1) {
      int var2 = EnchantmentHelper.getRespiration(this);
      return var2 > 0 && this.random.nextInt(var2 + 1) > 0 ? var1 : var1 - 1;
   }

   protected int increaseAirSupply(int var1) {
      return Math.min(var1 + 4, this.getMaxAirSupply());
   }

   public int getExperienceReward() {
      return 0;
   }

   protected boolean isAlwaysExperienceDropper() {
      return false;
   }

   public RandomSource getRandom() {
      return this.random;
   }

   @Nullable
   public LivingEntity getLastHurtByMob() {
      return this.lastHurtByMob;
   }

   public LivingEntity getLastAttacker() {
      return this.getLastHurtByMob();
   }

   public int getLastHurtByMobTimestamp() {
      return this.lastHurtByMobTimestamp;
   }

   public void setLastHurtByPlayer(@Nullable Player var1) {
      this.lastHurtByPlayer = var1;
      this.lastHurtByPlayerTime = this.tickCount;
   }

   public void setLastHurtByMob(@Nullable LivingEntity var1) {
      this.lastHurtByMob = var1;
      this.lastHurtByMobTimestamp = this.tickCount;
   }

   @Nullable
   public LivingEntity getLastHurtMob() {
      return this.lastHurtMob;
   }

   public int getLastHurtMobTimestamp() {
      return this.lastHurtMobTimestamp;
   }

   public void setLastHurtMob(Entity var1) {
      if (var1 instanceof LivingEntity) {
         this.lastHurtMob = (LivingEntity)var1;
      } else {
         this.lastHurtMob = null;
      }

      this.lastHurtMobTimestamp = this.tickCount;
   }

   public int getNoActionTime() {
      return this.noActionTime;
   }

   public void setNoActionTime(int var1) {
      this.noActionTime = var1;
   }

   public boolean shouldDiscardFriction() {
      return this.discardFriction;
   }

   public void setDiscardFriction(boolean var1) {
      this.discardFriction = var1;
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot var1) {
      return true;
   }

   public void onEquipItem(EquipmentSlot var1, ItemStack var2, ItemStack var3) {
      boolean var4 = var3.isEmpty() && var2.isEmpty();
      if (!var4 && !ItemStack.isSameItemSameTags(var2, var3) && !this.firstTick) {
         Equipable var5 = Equipable.get(var3);
         if (var5 != null && !this.isSpectator() && var5.getEquipmentSlot() == var1) {
            if (!this.level().isClientSide() && !this.isSilent()) {
               this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), var5.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F);
            }

            if (this.doesEmitEquipEvent(var1)) {
               this.gameEvent(GameEvent.EQUIP);
            }
         }

      }
   }

   public void remove(Entity.RemovalReason var1) {
      super.remove(var1);
      this.brain.clearMemories();
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      var1.putFloat("Health", this.getHealth());
      var1.putShort("HurtTime", (short)this.hurtTime);
      var1.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
      var1.putShort("DeathTime", (short)this.deathTime);
      var1.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
      var1.put("Attributes", this.getAttributes().save());
      if (!this.activeEffects.isEmpty()) {
         ListTag var2 = new ListTag();
         Iterator var3 = this.activeEffects.values().iterator();

         while(var3.hasNext()) {
            MobEffectInstance var4 = (MobEffectInstance)var3.next();
            var2.add(var4.save(new CompoundTag()));
         }

         var1.put("ActiveEffects", var2);
      }

      var1.putBoolean("FallFlying", this.isFallFlying());
      this.getSleepingPos().ifPresent((var1x) -> {
         var1.putInt("SleepingX", var1x.getX());
         var1.putInt("SleepingY", var1x.getY());
         var1.putInt("SleepingZ", var1x.getZ());
      });
      DataResult var5 = this.brain.serializeStart(NbtOps.INSTANCE);
      Logger var10001 = LOGGER;
      java.util.Objects.requireNonNull(var10001);
      var5.resultOrPartial(var10001::error).ifPresent((var1x) -> {
         var1.put("Brain", var1x);
      });
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      this.setAbsorptionAmount(var1.getFloat("AbsorptionAmount"));
      if (var1.contains("Attributes", 9) && this.level() != null && !this.level().isClientSide) {
         this.getAttributes().load(var1.getList("Attributes", 10));
      }

      if (var1.contains("ActiveEffects", 9)) {
         ListTag var2 = var1.getList("ActiveEffects", 10);

         for(int var3 = 0; var3 < var2.size(); ++var3) {
            CompoundTag var4 = var2.getCompound(var3);
            MobEffectInstance var5 = MobEffectInstance.load(var4);
            if (var5 != null) {
               this.activeEffects.put(var5.getEffect(), var5);
            }
         }
      }

      if (var1.contains("Health", 99)) {
         this.setHealth(var1.getFloat("Health"));
      }

      this.hurtTime = var1.getShort("HurtTime");
      this.deathTime = var1.getShort("DeathTime");
      this.lastHurtByMobTimestamp = var1.getInt("HurtByTimestamp");
      if (var1.contains("Team", 8)) {
         String var6 = var1.getString("Team");
         PlayerTeam var8 = this.level().getScoreboard().getPlayerTeam(var6);
         boolean var9 = var8 != null && this.level().getScoreboard().addPlayerToTeam(this.getStringUUID(), var8);
         if (!var9) {
            LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", var6);
         }
      }

      if (var1.getBoolean("FallFlying")) {
         this.setSharedFlag(7, true);
      }

      if (var1.contains("SleepingX", 99) && var1.contains("SleepingY", 99) && var1.contains("SleepingZ", 99)) {
         BlockPos var7 = new BlockPos(var1.getInt("SleepingX"), var1.getInt("SleepingY"), var1.getInt("SleepingZ"));
         this.setSleepingPos(var7);
         this.entityData.set(DATA_POSE, Pose.SLEEPING);
         if (!this.firstTick) {
            this.setPosToBed(var7);
         }
      }

      if (var1.contains("Brain", 10)) {
         this.brain = this.makeBrain(new Dynamic(NbtOps.INSTANCE, var1.get("Brain")));
      }

   }

   protected void tickEffects() {
      Iterator var1 = this.activeEffects.keySet().iterator();

      try {
         while(var1.hasNext()) {
            MobEffect var2 = (MobEffect)var1.next();
            MobEffectInstance var3 = (MobEffectInstance)this.activeEffects.get(var2);
            if (!var3.tick(this, () -> {
               this.onEffectUpdated(var3, true, (Entity)null);
            })) {
               if (!this.level().isClientSide) {
                  var1.remove();
                  this.onEffectRemoved(var3);
               }
            } else if (var3.getDuration() % 600 == 0) {
               this.onEffectUpdated(var3, false, (Entity)null);
            }
         }
      } catch (ConcurrentModificationException var11) {
      }

      if (this.effectsDirty) {
         if (!this.level().isClientSide) {
            this.updateInvisibilityStatus();
            this.updateGlowingStatus();
         }

         this.effectsDirty = false;
      }

      int var12 = (Integer)this.entityData.get(DATA_EFFECT_COLOR_ID);
      boolean var13 = (Boolean)this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
      if (var12 > 0) {
         boolean var4;
         if (this.isInvisible()) {
            var4 = this.random.nextInt(15) == 0;
         } else {
            var4 = this.random.nextBoolean();
         }

         if (var13) {
            var4 &= this.random.nextInt(5) == 0;
         }

         if (var4 && var12 > 0) {
            double var5 = (double)(var12 >> 16 & 255) / 255.0D;
            double var7 = (double)(var12 >> 8 & 255) / 255.0D;
            double var9 = (double)(var12 >> 0 & 255) / 255.0D;
            this.level().addParticle(var13 ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), var5, var7, var9);
         }
      }

   }

   protected void updateInvisibilityStatus() {
      if (this.activeEffects.isEmpty()) {
         this.removeEffectParticles();
         this.setInvisible(false);
      } else {
         Collection var1 = this.activeEffects.values();
         this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(var1));
         this.entityData.set(DATA_EFFECT_COLOR_ID, PotionUtils.getColor(var1));
         this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
      }

   }

   private void updateGlowingStatus() {
      boolean var1 = this.isCurrentlyGlowing();
      if (this.getSharedFlag(6) != var1) {
         this.setSharedFlag(6, var1);
      }

   }

   public double getVisibilityPercent(@Nullable Entity var1) {
      double var2 = 1.0D;
      if (this.isDiscrete()) {
         var2 *= 0.8D;
      }

      if (this.isInvisible()) {
         float var4 = this.getArmorCoverPercentage();
         if (var4 < 0.1F) {
            var4 = 0.1F;
         }

         var2 *= 0.7D * (double)var4;
      }

      if (var1 != null) {
         ItemStack var6 = this.getItemBySlot(EquipmentSlot.HEAD);
         EntityType var5 = var1.getType();
         if (var5 == EntityType.SKELETON && var6.is(Items.SKELETON_SKULL) || var5 == EntityType.ZOMBIE && var6.is(Items.ZOMBIE_HEAD) || var5 == EntityType.PIGLIN && var6.is(Items.PIGLIN_HEAD) || var5 == EntityType.PIGLIN_BRUTE && var6.is(Items.PIGLIN_HEAD) || var5 == EntityType.CREEPER && var6.is(Items.CREEPER_HEAD)) {
            var2 *= 0.5D;
         }
      }

      return var2;
   }

   public boolean canAttack(LivingEntity var1) {
      return var1 instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : var1.canBeSeenAsEnemy();
   }

   public boolean canAttack(LivingEntity var1, TargetingConditions var2) {
      return var2.test(this, var1);
   }

   public boolean canBeSeenAsEnemy() {
      return !this.isInvulnerable() && this.canBeSeenByAnyone();
   }

   public boolean canBeSeenByAnyone() {
      return !this.isSpectator() && this.isAlive();
   }

   public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> var0) {
      Iterator var1 = var0.iterator();

      MobEffectInstance var2;
      do {
         if (!var1.hasNext()) {
            return true;
         }

         var2 = (MobEffectInstance)var1.next();
      } while(!var2.isVisible() || var2.isAmbient());

      return false;
   }

   protected void removeEffectParticles() {
      this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
   }

   public boolean removeAllEffects() {
      if (this.level().isClientSide) {
         return false;
      } else {
         Iterator var1 = this.activeEffects.values().iterator();

         boolean var2;
         for(var2 = false; var1.hasNext(); var2 = true) {
            this.onEffectRemoved((MobEffectInstance)var1.next());
            var1.remove();
         }

         return var2;
      }
   }

   public Collection<MobEffectInstance> getActiveEffects() {
      return this.activeEffects.values();
   }

   public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
      return this.activeEffects;
   }

   public boolean hasEffect(MobEffect var1) {
      return this.activeEffects.containsKey(var1);
   }

   @Nullable
   public MobEffectInstance getEffect(MobEffect var1) {
      return (MobEffectInstance)this.activeEffects.get(var1);
   }

   public final boolean addEffect(MobEffectInstance var1) {
      return this.addEffect(var1, (Entity)null);
   }

   public boolean addEffect(MobEffectInstance var1, @Nullable Entity var2) {
      if (!this.canBeAffected(var1)) {
         return false;
      } else {
         MobEffectInstance var3 = (MobEffectInstance)this.activeEffects.get(var1.getEffect());
         if (var3 == null) {
            this.activeEffects.put(var1.getEffect(), var1);
            this.onEffectAdded(var1, var2);
            return true;
         } else if (var3.update(var1)) {
            this.onEffectUpdated(var3, true, var2);
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean canBeAffected(MobEffectInstance var1) {
      if (this.getMobType() == MobType.UNDEAD) {
         MobEffect var2 = var1.getEffect();
         if (var2 == MobEffects.REGENERATION || var2 == MobEffects.POISON) {
            return false;
         }
      }

      return true;
   }

   public void forceAddEffect(MobEffectInstance var1, @Nullable Entity var2) {
      if (this.canBeAffected(var1)) {
         MobEffectInstance var3 = (MobEffectInstance)this.activeEffects.put(var1.getEffect(), var1);
         if (var3 == null) {
            this.onEffectAdded(var1, var2);
         } else {
            this.onEffectUpdated(var1, true, var2);
         }

      }
   }

   public boolean isInvertedHealAndHarm() {
      return this.getMobType() == MobType.UNDEAD;
   }

   @Nullable
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect var1) {
      return (MobEffectInstance)this.activeEffects.remove(var1);
   }

   public boolean removeEffect(MobEffect var1) {
      MobEffectInstance var2 = this.removeEffectNoUpdate(var1);
      if (var2 != null) {
         this.onEffectRemoved(var2);
         return true;
      } else {
         return false;
      }
   }

   protected void onEffectAdded(MobEffectInstance var1, @Nullable Entity var2) {
      this.effectsDirty = true;
      if (!this.level().isClientSide) {
         var1.getEffect().addAttributeModifiers(this, this.getAttributes(), var1.getAmplifier());
         this.sendEffectToPassengers(var1);
      }

   }

   public void sendEffectToPassengers(MobEffectInstance var1) {
      Iterator var2 = this.getPassengers().iterator();

      while(var2.hasNext()) {
         Entity var3 = (Entity)var2.next();
         if (var3 instanceof ServerPlayer) {
            ServerPlayer var4 = (ServerPlayer)var3;
            var4.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), var1));
         }
      }

   }

   protected void onEffectUpdated(MobEffectInstance var1, boolean var2, @Nullable Entity var3) {
      this.effectsDirty = true;
      if (var2 && !this.level().isClientSide) {
         MobEffect var4 = var1.getEffect();
         var4.removeAttributeModifiers(this, this.getAttributes(), var1.getAmplifier());
         var4.addAttributeModifiers(this, this.getAttributes(), var1.getAmplifier());
      }

      if (!this.level().isClientSide) {
         this.sendEffectToPassengers(var1);
      }

   }

   protected void onEffectRemoved(MobEffectInstance var1) {
      this.effectsDirty = true;
      if (!this.level().isClientSide) {
         var1.getEffect().removeAttributeModifiers(this, this.getAttributes(), var1.getAmplifier());
         Iterator var2 = this.getPassengers().iterator();

         while(var2.hasNext()) {
            Entity var3 = (Entity)var2.next();
            if (var3 instanceof ServerPlayer) {
               ServerPlayer var4 = (ServerPlayer)var3;
               var4.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), var1.getEffect()));
            }
         }
      }

   }

   public void heal(float var1) {
      float var2 = this.getHealth();
      if (var2 > 0.0F) {
         this.setHealth(var2 + var1);
      }

   }

   public float getHealth() {
      return (Float)this.entityData.get(DATA_HEALTH_ID);
   }

   public void setHealth(float var1) {
      this.entityData.set(DATA_HEALTH_ID, Mth.clamp(var1, 0.0F, this.getMaxHealth()));
   }

   public boolean isDeadOrDying() {
      return this.getHealth() <= 0.0F;
   }

   public boolean hurt(DamageSource var1, float var2) {
      if (this.isInvulnerableTo(var1)) {
         return false;
      } else if (this.level().isClientSide) {
         return false;
      } else if (this.isDeadOrDying()) {
         return false;
      } else if (var1.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         return false;
      } else {
         if (this.isSleeping() && !this.level().isClientSide) {
            this.stopSleeping();
         }

         this.noActionTime = 0;
         float var3 = var2;
         boolean var4 = false;
         float var5 = 0.0F;
         if (var2 > 0.0F && this.isDamageSourceBlocked(var1)) {
            this.hurtCurrentlyUsedShield(var2);
            var5 = var2;
            var2 = 0.0F;
            if (!var1.is(DamageTypeTags.IS_PROJECTILE)) {
               Entity var6 = var1.getDirectEntity();
               if (var6 instanceof LivingEntity) {
                  LivingEntity var7 = (LivingEntity)var6;
                  this.blockUsingShield(var7);
               }
            }

            var4 = true;
         }

         if (var1.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            var2 *= 5.0F;
         }

         this.walkAnimation.setSpeed(1.5F);
         boolean var12 = true;
         if ((float)this.invulnerableTime > 10.0F && !var1.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (var2 <= this.lastHurt) {
               return false;
            }

            this.actuallyHurt(var1, var2 - this.lastHurt);
            this.lastHurt = var2;
            var12 = false;
         } else {
            this.lastHurt = var2;
            this.invulnerableTime = 20;
            this.actuallyHurt(var1, var2);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }

         if (var1.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.hurtHelmet(var1, var2);
            var2 *= 0.75F;
         }

         Entity var13 = var1.getEntity();
         if (var13 != null) {
            if (var13 instanceof LivingEntity) {
               LivingEntity var8 = (LivingEntity)var13;
               if (!var1.is(DamageTypeTags.NO_ANGER)) {
                  this.setLastHurtByMob(var8);
               }
            }

            if (var13 instanceof Player) {
               Player var14 = (Player)var13;
               this.lastHurtByPlayerTime = 100;
               this.lastHurtByPlayer = var14;
            } else if (var13 instanceof Wolf) {
               Wolf var9 = (Wolf)var13;
               if (var9.isTame()) {
                  this.lastHurtByPlayerTime = 100;
                  LivingEntity var11 = var9.getOwner();
                  if (var11 instanceof Player) {
                     Player var10 = (Player)var11;
                     this.lastHurtByPlayer = var10;
                  } else {
                     this.lastHurtByPlayer = null;
                  }
               }
            }
         }

         if (var12) {
            if (var4) {
               this.level().broadcastEntityEvent(this, (byte)29);
            } else {
               this.level().broadcastDamageEvent(this, var1);
            }

            if (!var1.is(DamageTypeTags.NO_IMPACT) && (!var4 || var2 > 0.0F)) {
               this.markHurt();
            }

            if (var13 != null && !var1.is(DamageTypeTags.IS_EXPLOSION)) {
               double var15 = var13.getX() - this.getX();

               double var18;
               for(var18 = var13.getZ() - this.getZ(); var15 * var15 + var18 * var18 < 1.0E-4D; var18 = (Math.random() - Math.random()) * 0.01D) {
                  var15 = (Math.random() - Math.random()) * 0.01D;
               }

               this.knockback(0.4000000059604645D, var15, var18);
               if (!var4) {
                  this.indicateDamage(var15, var18);
               }
            }
         }

         if (this.isDeadOrDying()) {
            if (!this.checkTotemDeathProtection(var1)) {
               SoundEvent var16 = this.getDeathSound();
               if (var12 && var16 != null) {
                  this.playSound(var16, this.getSoundVolume(), this.getVoicePitch());
               }

               this.die(var1);
            }
         } else if (var12) {
            this.playHurtSound(var1);
         }

         boolean var17 = !var4 || var2 > 0.0F;
         if (var17) {
            this.lastDamageSource = var1;
            this.lastDamageStamp = this.level().getGameTime();
         }

         if (this instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer)this, var1, var3, var2, var4);
            if (var5 > 0.0F && var5 < 3.4028235E37F) {
               ((ServerPlayer)this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(var5 * 10.0F));
            }
         }

         if (var13 instanceof ServerPlayer) {
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)var13, this, var1, var3, var2, var4);
         }

         return var17;
      }
   }

   protected void blockUsingShield(LivingEntity var1) {
      var1.blockedByShield(this);
   }

   protected void blockedByShield(LivingEntity var1) {
      var1.knockback(0.5D, var1.getX() - this.getX(), var1.getZ() - this.getZ());
   }

   private boolean checkTotemDeathProtection(DamageSource var1) {
      if (var1.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      } else {
         ItemStack var2 = null;
         InteractionHand[] var4 = InteractionHand.values();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            InteractionHand var7 = var4[var6];
            ItemStack var3 = this.getItemInHand(var7);
            if (var3.is(Items.TOTEM_OF_UNDYING)) {
               var2 = var3.copy();
               var3.shrink(1);
               break;
            }
         }

         if (var2 != null) {
            if (this instanceof ServerPlayer) {
               ServerPlayer var8 = (ServerPlayer)this;
               var8.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
               CriteriaTriggers.USED_TOTEM.trigger(var8, var2);
            }

            this.setHealth(1.0F);
            this.removeAllEffects();
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            this.level().broadcastEntityEvent(this, (byte)35);
         }

         return var2 != null;
      }
   }

   @Nullable
   public DamageSource getLastDamageSource() {
      if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
         this.lastDamageSource = null;
      }

      return this.lastDamageSource;
   }

   protected void playHurtSound(DamageSource var1) {
      SoundEvent var2 = this.getHurtSound(var1);
      if (var2 != null) {
         this.playSound(var2, this.getSoundVolume(), this.getVoicePitch());
      }

   }

   public boolean isDamageSourceBlocked(DamageSource var1) {
      Entity var2 = var1.getDirectEntity();
      boolean var3 = false;
      if (var2 instanceof AbstractArrow) {
         AbstractArrow var4 = (AbstractArrow)var2;
         if (var4.getPierceLevel() > 0) {
            var3 = true;
         }
      }

      if (!var1.is(DamageTypeTags.BYPASSES_SHIELD) && this.isBlocking() && !var3) {
         Vec3 var7 = var1.getSourcePosition();
         if (var7 != null) {
            Vec3 var5 = this.getViewVector(1.0F);
            Vec3 var6 = var7.vectorTo(this.position()).normalize();
            var6 = new Vec3(var6.x, 0.0D, var6.z);
            if (var6.dot(var5) < 0.0D) {
               return true;
            }
         }
      }

      return false;
   }

   private void breakItem(ItemStack var1) {
      if (!var1.isEmpty()) {
         if (!this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK, this.getSoundSource(), 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F, false);
         }

         this.spawnItemParticles(var1, 5);
      }

   }

   public void die(DamageSource var1) {
      if (!this.isRemoved() && !this.dead) {
         Entity var2 = var1.getEntity();
         LivingEntity var3 = this.getKillCredit();
         if (this.deathScore >= 0 && var3 != null) {
            var3.awardKillScore(this, this.deathScore, var1);
         }

         if (this.isSleeping()) {
            this.stopSleeping();
         }

         if (!this.level().isClientSide && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
         }

         this.dead = true;
         this.getCombatTracker().recheckStatus();
         Level var5 = this.level();
         if (var5 instanceof ServerLevel) {
            ServerLevel var4 = (ServerLevel)var5;
            if (var2 == null || var2.killedEntity(var4, this)) {
               this.gameEvent(GameEvent.ENTITY_DIE);
               this.dropAllDeathLoot(var1);
               this.createWitherRose(var3);
            }

            this.level().broadcastEntityEvent(this, (byte)3);
         }

         this.setPose(Pose.DYING);
      }
   }

   protected void createWitherRose(@Nullable LivingEntity var1) {
      if (!this.level().isClientSide) {
         boolean var2 = false;
         if (var1 instanceof WitherBoss) {
            if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
               BlockPos var3 = this.blockPosition();
               BlockState var4 = Blocks.WITHER_ROSE.defaultBlockState();
               if (this.level().getBlockState(var3).isAir() && var4.canSurvive(this.level(), var3)) {
                  this.level().setBlock(var3, var4, 3);
                  var2 = true;
               }
            }

            if (!var2) {
               ItemEntity var5 = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
               this.level().addFreshEntity(var5);
            }
         }

      }
   }

   protected void dropAllDeathLoot(DamageSource var1) {
      Entity var2 = var1.getEntity();
      int var3;
      if (var2 instanceof Player) {
         var3 = EnchantmentHelper.getMobLooting((LivingEntity)var2);
      } else {
         var3 = 0;
      }

      boolean var4 = this.lastHurtByPlayerTime > 0;
      if (this.shouldDropLoot() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         this.dropFromLootTable(var1, var4);
         this.dropCustomDeathLoot(var1, var3, var4);
      }

      this.dropEquipment();
      this.dropExperience();
   }

   protected void dropEquipment() {
   }

   protected void dropExperience() {
      if (this.level() instanceof ServerLevel && !this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
         ExperienceOrb.award((ServerLevel)this.level(), this.position(), this.getExperienceReward());
      }

   }

   protected void dropCustomDeathLoot(DamageSource var1, int var2, boolean var3) {
   }

   public ResourceLocation getLootTable() {
      return this.getType().getDefaultLootTable();
   }

   public long getLootTableSeed() {
      return 0L;
   }

   protected void dropFromLootTable(DamageSource var1, boolean var2) {
      ResourceLocation var3 = this.getLootTable();
      LootTable var4 = this.level().getServer().getLootData().getLootTable(var3);
      LootParams.Builder var5 = (new LootParams.Builder((ServerLevel)this.level())).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.DAMAGE_SOURCE, var1).withOptionalParameter(LootContextParams.KILLER_ENTITY, var1.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, var1.getDirectEntity());
      if (var2 && this.lastHurtByPlayer != null) {
         var5 = var5.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
      }

      LootParams var6 = var5.create(LootContextParamSets.ENTITY);
      var4.getRandomItems(var6, this.getLootTableSeed(), this::spawnAtLocation);
   }

   public void knockback(double var1, double var3, double var5) {
      var1 *= 1.0D - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      if (!(var1 <= 0.0D)) {
         this.hasImpulse = true;
         Vec3 var7 = this.getDeltaMovement();
         Vec3 var8 = (new Vec3(var3, 0.0D, var5)).normalize().scale(var1);
         this.setDeltaMovement(var7.x / 2.0D - var8.x, this.onGround() ? Math.min(0.4D, var7.y / 2.0D + var1) : var7.y, var7.z / 2.0D - var8.z);
      }
   }

   public void indicateDamage(double var1, double var3) {
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.GENERIC_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.GENERIC_DEATH;
   }

   private SoundEvent getFallDamageSound(int var1) {
      return var1 > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
   }

   public void skipDropExperience() {
      this.skipDropExperience = true;
   }

   public boolean wasExperienceConsumed() {
      return this.skipDropExperience;
   }

   protected Vec3 getMeleeAttackReferencePosition() {
      Entity var2 = this.getVehicle();
      if (var2 instanceof RiderShieldingMount) {
         RiderShieldingMount var1 = (RiderShieldingMount)var2;
         return this.position().add(0.0D, var1.getRiderShieldingHeight(), 0.0D);
      } else {
         return this.position();
      }
   }

   public float getHurtDir() {
      return 0.0F;
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
   }

   protected SoundEvent getDrinkingSound(ItemStack var1) {
      return var1.getDrinkingSound();
   }

   public SoundEvent getEatingSound(ItemStack var1) {
      return var1.getEatingSound();
   }

   public Optional<BlockPos> getLastClimbablePos() {
      return this.lastClimbablePos;
   }

   public boolean onClimbable() {
      if (this.isSpectator()) {
         return false;
      } else {
         BlockPos var1 = this.blockPosition();
         BlockState var2 = this.getFeetBlockState();
         if (var2.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(var1);
            return true;
         } else if (var2.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(var1, var2)) {
            this.lastClimbablePos = Optional.of(var1);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean trapdoorUsableAsLadder(BlockPos var1, BlockState var2) {
      if ((Boolean)var2.getValue(TrapDoorBlock.OPEN)) {
         BlockState var3 = this.level().getBlockState(var1.below());
         if (var3.is(Blocks.LADDER) && var3.getValue(LadderBlock.FACING) == var2.getValue(TrapDoorBlock.FACING)) {
            return true;
         }
      }

      return false;
   }

   public boolean isAlive() {
      return !this.isRemoved() && this.getHealth() > 0.0F;
   }

   public boolean causeFallDamage(float var1, float var2, DamageSource var3) {
      boolean var4 = super.causeFallDamage(var1, var2, var3);
      int var5 = this.calculateFallDamage(var1, var2);
      if (var5 > 0) {
         this.playSound(this.getFallDamageSound(var5), 1.0F, 1.0F);
         this.playBlockFallSound();
         this.hurt(var3, (float)var5);
         return true;
      } else {
         return var4;
      }
   }

   protected int calculateFallDamage(float var1, float var2) {
      if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      } else {
         MobEffectInstance var3 = this.getEffect(MobEffects.JUMP);
         float var4 = var3 == null ? 0.0F : (float)(var3.getAmplifier() + 1);
         return Mth.ceil((var1 - 3.0F - var4) * var2);
      }
   }

   protected void playBlockFallSound() {
      if (!this.isSilent()) {
         int var1 = Mth.floor(this.getX());
         int var2 = Mth.floor(this.getY() - 0.20000000298023224D);
         int var3 = Mth.floor(this.getZ());
         BlockState var4 = this.level().getBlockState(new BlockPos(var1, var2, var3));
         if (!var4.isAir()) {
            SoundType var5 = var4.getSoundType();
            this.playSound(var5.getFallSound(), var5.getVolume() * 0.5F, var5.getPitch() * 0.75F);
         }

      }
   }

   public void animateHurt(float var1) {
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
   }

   public int getArmorValue() {
      return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
   }

   protected void hurtArmor(DamageSource var1, float var2) {
   }

   protected void hurtHelmet(DamageSource var1, float var2) {
   }

   protected void hurtCurrentlyUsedShield(float var1) {
   }

   protected float getDamageAfterArmorAbsorb(DamageSource var1, float var2) {
      if (!var1.is(DamageTypeTags.BYPASSES_ARMOR)) {
         this.hurtArmor(var1, var2);
         var2 = CombatRules.getDamageAfterAbsorb(var2, (float)this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      }

      return var2;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource var1, float var2) {
      if (var1.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return var2;
      } else {
         int var3;
         if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !var1.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            var3 = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
            int var4 = 25 - var3;
            float var5 = var2 * (float)var4;
            float var6 = var2;
            var2 = Math.max(var5 / 25.0F, 0.0F);
            float var7 = var6 - var2;
            if (var7 > 0.0F && var7 < 3.4028235E37F) {
               if (this instanceof ServerPlayer) {
                  ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(var7 * 10.0F));
               } else if (var1.getEntity() instanceof ServerPlayer) {
                  ((ServerPlayer)var1.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(var7 * 10.0F));
               }
            }
         }

         if (var2 <= 0.0F) {
            return 0.0F;
         } else if (var1.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return var2;
         } else {
            var3 = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), var1);
            if (var3 > 0) {
               var2 = CombatRules.getDamageAfterMagicAbsorb(var2, (float)var3);
            }

            return var2;
         }
      }
   }

   protected void actuallyHurt(DamageSource var1, float var2) {
      if (!this.isInvulnerableTo(var1)) {
         var2 = this.getDamageAfterArmorAbsorb(var1, var2);
         var2 = this.getDamageAfterMagicAbsorb(var1, var2);
         float var3 = var2;
         var2 = Math.max(var2 - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (var3 - var2));
         float var4 = var3 - var2;
         if (var4 > 0.0F && var4 < 3.4028235E37F) {
            Entity var6 = var1.getEntity();
            if (var6 instanceof ServerPlayer) {
               ServerPlayer var5 = (ServerPlayer)var6;
               var5.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(var4 * 10.0F));
            }
         }

         if (var2 != 0.0F) {
            this.getCombatTracker().recordDamage(var1, var2);
            this.setHealth(this.getHealth() - var2);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - var2);
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public CombatTracker getCombatTracker() {
      return this.combatTracker;
   }

   @Nullable
   public LivingEntity getKillCredit() {
      if (this.lastHurtByPlayer != null) {
         return this.lastHurtByPlayer;
      } else {
         return this.lastHurtByMob != null ? this.lastHurtByMob : null;
      }
   }

   public final float getMaxHealth() {
      return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
   }

   public final int getArrowCount() {
      return (Integer)this.entityData.get(DATA_ARROW_COUNT_ID);
   }

   public final void setArrowCount(int var1) {
      this.entityData.set(DATA_ARROW_COUNT_ID, var1);
   }

   public final int getStingerCount() {
      return (Integer)this.entityData.get(DATA_STINGER_COUNT_ID);
   }

   public final void setStingerCount(int var1) {
      this.entityData.set(DATA_STINGER_COUNT_ID, var1);
   }

   private int getCurrentSwingDuration() {
      if (MobEffectUtil.hasDigSpeed(this)) {
         return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
      } else {
         return this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
      }
   }

   public void swing(InteractionHand var1) {
      this.swing(var1, false);
   }

   public void swing(InteractionHand var1, boolean var2) {
      if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
         this.swingTime = -1;
         this.swinging = true;
         this.swingingArm = var1;
         if (this.level() instanceof ServerLevel) {
            ClientboundAnimatePacket var3 = new ClientboundAnimatePacket(this, var1 == InteractionHand.MAIN_HAND ? 0 : 3);
            ServerChunkCache var4 = ((ServerLevel)this.level()).getChunkSource();
            if (var2) {
               var4.broadcastAndSend(this, var3);
            } else {
               var4.broadcast(this, var3);
            }
         }
      }

   }

   public void handleDamageEvent(DamageSource var1) {
      this.walkAnimation.setSpeed(1.5F);
      this.invulnerableTime = 20;
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
      SoundEvent var2 = this.getHurtSound(var1);
      if (var2 != null) {
         this.playSound(var2, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }

      this.hurt(this.damageSources().generic(), 0.0F);
      this.lastDamageSource = var1;
      this.lastDamageStamp = this.level().getGameTime();
   }

   public void handleEntityEvent(byte var1) {
      switch(var1) {
      case 3:
         SoundEvent var15 = this.getDeathSound();
         if (var15 != null) {
            this.playSound(var15, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
         }

         if (!(this instanceof Player)) {
            this.setHealth(0.0F);
            this.die(this.damageSources().generic());
         }
         break;
      case 29:
         this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.level().random.nextFloat() * 0.4F);
         break;
      case 30:
         this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
         break;
      case 46:
         boolean var2 = true;

         for(int var3 = 0; var3 < 128; ++var3) {
            double var4 = (double)var3 / 127.0D;
            float var6 = (this.random.nextFloat() - 0.5F) * 0.2F;
            float var7 = (this.random.nextFloat() - 0.5F) * 0.2F;
            float var8 = (this.random.nextFloat() - 0.5F) * 0.2F;
            double var9 = Mth.lerp(var4, this.xo, this.getX()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
            double var11 = Mth.lerp(var4, this.yo, this.getY()) + this.random.nextDouble() * (double)this.getBbHeight();
            double var13 = Mth.lerp(var4, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
            this.level().addParticle(ParticleTypes.PORTAL, var9, var11, var13, (double)var6, (double)var7, (double)var8);
         }

         return;
      case 47:
         this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
         break;
      case 48:
         this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
         break;
      case 49:
         this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
         break;
      case 50:
         this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
         break;
      case 51:
         this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
         break;
      case 52:
         this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
         break;
      case 54:
         HoneyBlock.showJumpParticles(this);
         break;
      case 55:
         this.swapHandItems();
         break;
      case 60:
         this.makePoofParticles();
         break;
      default:
         super.handleEntityEvent(var1);
      }

   }

   private void makePoofParticles() {
      for(int var1 = 0; var1 < 20; ++var1) {
         double var2 = this.random.nextGaussian() * 0.02D;
         double var4 = this.random.nextGaussian() * 0.02D;
         double var6 = this.random.nextGaussian() * 0.02D;
         this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), var2, var4, var6);
      }

   }

   private void swapHandItems() {
      ItemStack var1 = this.getItemBySlot(EquipmentSlot.OFFHAND);
      this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
      this.setItemSlot(EquipmentSlot.MAINHAND, var1);
   }

   protected void onBelowWorld() {
      this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
   }

   protected void updateSwingTime() {
      int var1 = this.getCurrentSwingDuration();
      if (this.swinging) {
         ++this.swingTime;
         if (this.swingTime >= var1) {
            this.swingTime = 0;
            this.swinging = false;
         }
      } else {
         this.swingTime = 0;
      }

      this.attackAnim = (float)this.swingTime / (float)var1;
   }

   @Nullable
   public AttributeInstance getAttribute(Attribute var1) {
      return this.getAttributes().getInstance(var1);
   }

   public double getAttributeValue(Holder<Attribute> var1) {
      return this.getAttributeValue((Attribute)var1.value());
   }

   public double getAttributeValue(Attribute var1) {
      return this.getAttributes().getValue(var1);
   }

   public double getAttributeBaseValue(Holder<Attribute> var1) {
      return this.getAttributeBaseValue((Attribute)var1.value());
   }

   public double getAttributeBaseValue(Attribute var1) {
      return this.getAttributes().getBaseValue(var1);
   }

   public AttributeMap getAttributes() {
      return this.attributes;
   }

   public MobType getMobType() {
      return MobType.UNDEFINED;
   }

   public ItemStack getMainHandItem() {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
   }

   public ItemStack getOffhandItem() {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
   }

   public boolean isHolding(Item var1) {
      return this.isHolding((var1x) -> {
         return var1x.is(var1);
      });
   }

   public boolean isHolding(Predicate<ItemStack> var1) {
      return var1.test(this.getMainHandItem()) || var1.test(this.getOffhandItem());
   }

   public ItemStack getItemInHand(InteractionHand var1) {
      if (var1 == InteractionHand.MAIN_HAND) {
         return this.getItemBySlot(EquipmentSlot.MAINHAND);
      } else if (var1 == InteractionHand.OFF_HAND) {
         return this.getItemBySlot(EquipmentSlot.OFFHAND);
      } else {
         throw new IllegalArgumentException("Invalid hand " + var1);
      }
   }

   public void setItemInHand(InteractionHand var1, ItemStack var2) {
      if (var1 == InteractionHand.MAIN_HAND) {
         this.setItemSlot(EquipmentSlot.MAINHAND, var2);
      } else {
         if (var1 != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("Invalid hand " + var1);
         }

         this.setItemSlot(EquipmentSlot.OFFHAND, var2);
      }

   }

   public boolean hasItemInSlot(EquipmentSlot var1) {
      return !this.getItemBySlot(var1).isEmpty();
   }

   public abstract Iterable<ItemStack> getArmorSlots();

   public abstract ItemStack getItemBySlot(EquipmentSlot var1);

   public abstract void setItemSlot(EquipmentSlot var1, ItemStack var2);

   protected void verifyEquippedItem(ItemStack var1) {
      CompoundTag var2 = var1.getTag();
      if (var2 != null) {
         var1.getItem().verifyTagAfterLoad(var2);
      }

   }

   public float getArmorCoverPercentage() {
      Iterable var1 = this.getArmorSlots();
      int var2 = 0;
      int var3 = 0;

      for(Iterator var4 = var1.iterator(); var4.hasNext(); ++var2) {
         ItemStack var5 = (ItemStack)var4.next();
         if (!var5.isEmpty()) {
            ++var3;
         }
      }

      return var2 > 0 ? (float)var3 / (float)var2 : 0.0F;
   }

   public void setSprinting(boolean var1) {
      super.setSprinting(var1);
      AttributeInstance var2 = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (var2.getModifier(SPEED_MODIFIER_SPRINTING_UUID) != null) {
         var2.removeModifier(SPEED_MODIFIER_SPRINTING);
      }

      if (var1) {
         var2.addTransientModifier(SPEED_MODIFIER_SPRINTING);
      }

   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   protected boolean isImmobile() {
      return this.isDeadOrDying();
   }

   public void push(Entity var1) {
      if (!this.isSleeping()) {
         super.push(var1);
      }

   }

   private void dismountVehicle(Entity var1) {
      Vec3 var2;
      if (this.isRemoved()) {
         var2 = this.position();
      } else if (!var1.isRemoved() && !this.level().getBlockState(var1.blockPosition()).is(BlockTags.PORTALS)) {
         var2 = var1.getDismountLocationForPassenger(this);
      } else {
         double var3 = Math.max(this.getY(), var1.getY());
         var2 = new Vec3(this.getX(), var3, this.getZ());
      }

      this.dismountTo(var2.x, var2.y, var2.z);
   }

   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   protected float getJumpPower() {
      return 0.42F * this.getBlockJumpFactor() + this.getJumpBoostPower();
   }

   public float getJumpBoostPower() {
      return this.hasEffect(MobEffects.JUMP) ? 0.1F * ((float)this.getEffect(MobEffects.JUMP).getAmplifier() + 1.0F) : 0.0F;
   }

   protected void jumpFromGround() {
      Vec3 var1 = this.getDeltaMovement();
      this.setDeltaMovement(var1.x, (double)this.getJumpPower(), var1.z);
      if (this.isSprinting()) {
         float var2 = this.getYRot() * 0.017453292F;
         this.setDeltaMovement(this.getDeltaMovement().add((double)(-Mth.sin(var2) * 0.2F), 0.0D, (double)(Mth.cos(var2) * 0.2F)));
      }

      this.hasImpulse = true;
   }

   protected void goDownInWater() {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03999999910593033D, 0.0D));
   }

   protected void jumpInLiquid(TagKey<Fluid> var1) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.03999999910593033D, 0.0D));
   }

   protected float getWaterSlowDown() {
      return 0.8F;
   }

   public boolean canStandOnFluid(FluidState var1) {
      return false;
   }

   public void travel(Vec3 var1) {
      if (this.isControlledByLocalInstance()) {
         double var2 = 0.08D;
         boolean var4 = this.getDeltaMovement().y <= 0.0D;
         if (var4 && this.hasEffect(MobEffects.SLOW_FALLING)) {
            var2 = 0.01D;
         }

         FluidState var5 = this.level().getFluidState(this.blockPosition());
         float var8;
         double var25;
         if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(var5)) {
            var25 = this.getY();
            var8 = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
            float var27 = 0.02F;
            float var29 = (float)EnchantmentHelper.getDepthStrider(this);
            if (var29 > 3.0F) {
               var29 = 3.0F;
            }

            if (!this.onGround()) {
               var29 *= 0.5F;
            }

            if (var29 > 0.0F) {
               var8 += (0.54600006F - var8) * var29 / 3.0F;
               var27 += (this.getSpeed() - var27) * var29 / 3.0F;
            }

            if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
               var8 = 0.96F;
            }

            this.moveRelative(var27, var1);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 var28 = this.getDeltaMovement();
            if (this.horizontalCollision && this.onClimbable()) {
               var28 = new Vec3(var28.x, 0.2D, var28.z);
            }

            this.setDeltaMovement(var28.multiply((double)var8, 0.800000011920929D, (double)var8));
            Vec3 var12 = this.getFluidFallingAdjustedMovement(var2, var4, this.getDeltaMovement());
            this.setDeltaMovement(var12);
            if (this.horizontalCollision && this.isFree(var12.x, var12.y + 0.6000000238418579D - this.getY() + var25, var12.z)) {
               this.setDeltaMovement(var12.x, 0.30000001192092896D, var12.z);
            }
         } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(var5)) {
            var25 = this.getY();
            this.moveRelative(0.02F, var1);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 var23;
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, 0.800000011920929D, 0.5D));
               var23 = this.getFluidFallingAdjustedMovement(var2, var4, this.getDeltaMovement());
               this.setDeltaMovement(var23);
            } else {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            }

            if (!this.isNoGravity()) {
               this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -var2 / 4.0D, 0.0D));
            }

            var23 = this.getDeltaMovement();
            if (this.horizontalCollision && this.isFree(var23.x, var23.y + 0.6000000238418579D - this.getY() + var25, var23.z)) {
               this.setDeltaMovement(var23.x, 0.30000001192092896D, var23.z);
            }
         } else if (this.isFallFlying()) {
            this.checkSlowFallDistance();
            Vec3 var6 = this.getDeltaMovement();
            Vec3 var7 = this.getLookAngle();
            var8 = this.getXRot() * 0.017453292F;
            double var9 = Math.sqrt(var7.x * var7.x + var7.z * var7.z);
            double var11 = var6.horizontalDistance();
            double var13 = var7.length();
            double var15 = Math.cos((double)var8);
            var15 = var15 * var15 * Math.min(1.0D, var13 / 0.4D);
            var6 = this.getDeltaMovement().add(0.0D, var2 * (-1.0D + var15 * 0.75D), 0.0D);
            double var17;
            if (var6.y < 0.0D && var9 > 0.0D) {
               var17 = var6.y * -0.1D * var15;
               var6 = var6.add(var7.x * var17 / var9, var17, var7.z * var17 / var9);
            }

            if (var8 < 0.0F && var9 > 0.0D) {
               var17 = var11 * (double)(-Mth.sin(var8)) * 0.04D;
               var6 = var6.add(-var7.x * var17 / var9, var17 * 3.2D, -var7.z * var17 / var9);
            }

            if (var9 > 0.0D) {
               var6 = var6.add((var7.x / var9 * var11 - var6.x) * 0.1D, 0.0D, (var7.z / var9 * var11 - var6.z) * 0.1D);
            }

            this.setDeltaMovement(var6.multiply(0.9900000095367432D, 0.9800000190734863D, 0.9900000095367432D));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.horizontalCollision && !this.level().isClientSide) {
               var17 = this.getDeltaMovement().horizontalDistance();
               double var19 = var11 - var17;
               float var21 = (float)(var19 * 10.0D - 3.0D);
               if (var21 > 0.0F) {
                  this.playSound(this.getFallDamageSound((int)var21), 1.0F, 1.0F);
                  this.hurt(this.damageSources().flyIntoWall(), var21);
               }
            }

            if (this.onGround() && !this.level().isClientSide) {
               this.setSharedFlag(7, false);
            }
         } else {
            BlockPos var24 = this.getBlockPosBelowThatAffectsMyMovement();
            float var22 = this.level().getBlockState(var24).getBlock().getFriction();
            var8 = this.onGround() ? var22 * 0.91F : 0.91F;
            Vec3 var26 = this.handleRelativeFrictionAndCalculateMovement(var1, var22);
            double var10 = var26.y;
            if (this.hasEffect(MobEffects.LEVITATION)) {
               var10 += (0.05D * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - var26.y) * 0.2D;
            } else if (this.level().isClientSide && !this.level().hasChunkAt(var24)) {
               if (this.getY() > (double)this.level().getMinBuildHeight()) {
                  var10 = -0.1D;
               } else {
                  var10 = 0.0D;
               }
            } else if (!this.isNoGravity()) {
               var10 -= var2;
            }

            if (this.shouldDiscardFriction()) {
               this.setDeltaMovement(var26.x, var10, var26.z);
            } else {
               this.setDeltaMovement(var26.x * (double)var8, var10 * 0.9800000190734863D, var26.z * (double)var8);
            }
         }
      }

      this.calculateEntityAnimation(this instanceof FlyingAnimal);
   }

   private void travelRidden(Player var1, Vec3 var2) {
      Vec3 var3 = this.getRiddenInput(var1, var2);
      this.tickRidden(var1, var3);
      if (this.isControlledByLocalInstance()) {
         this.setSpeed(this.getRiddenSpeed(var1));
         this.travel(var3);
      } else {
         this.calculateEntityAnimation(false);
         this.setDeltaMovement(Vec3.ZERO);
         this.tryCheckInsideBlocks();
      }

   }

   protected void tickRidden(Player var1, Vec3 var2) {
   }

   protected Vec3 getRiddenInput(Player var1, Vec3 var2) {
      return var2;
   }

   protected float getRiddenSpeed(Player var1) {
      return this.getSpeed();
   }

   public void calculateEntityAnimation(boolean var1) {
      float var2 = (float)Mth.length(this.getX() - this.xo, var1 ? this.getY() - this.yo : 0.0D, this.getZ() - this.zo);
      this.updateWalkAnimation(var2);
   }

   protected void updateWalkAnimation(float var1) {
      float var2 = Math.min(var1 * 4.0F, 1.0F);
      this.walkAnimation.update(var2, 0.4F);
   }

   public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 var1, float var2) {
      this.moveRelative(this.getFrictionInfluencedSpeed(var2), var1);
      this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 var3 = this.getDeltaMovement();
      if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
         var3 = new Vec3(var3.x, 0.2D, var3.z);
      }

      return var3;
   }

   public Vec3 getFluidFallingAdjustedMovement(double var1, boolean var3, Vec3 var4) {
      if (!this.isNoGravity() && !this.isSprinting()) {
         double var5;
         if (var3 && Math.abs(var4.y - 0.005D) >= 0.003D && Math.abs(var4.y - var1 / 16.0D) < 0.003D) {
            var5 = -0.003D;
         } else {
            var5 = var4.y - var1 / 16.0D;
         }

         return new Vec3(var4.x, var5, var4.z);
      } else {
         return var4;
      }
   }

   private Vec3 handleOnClimbable(Vec3 var1) {
      if (this.onClimbable()) {
         this.resetFallDistance();
         float var2 = 0.15F;
         double var3 = Mth.clamp(var1.x, -0.15000000596046448D, 0.15000000596046448D);
         double var5 = Mth.clamp(var1.z, -0.15000000596046448D, 0.15000000596046448D);
         double var7 = Math.max(var1.y, -0.15000000596046448D);
         if (var7 < 0.0D && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
            var7 = 0.0D;
         }

         var1 = new Vec3(var3, var7, var5);
      }

      return var1;
   }

   private float getFrictionInfluencedSpeed(float var1) {
      return this.onGround() ? this.getSpeed() * (0.21600002F / (var1 * var1 * var1)) : this.getFlyingSpeed();
   }

   protected float getFlyingSpeed() {
      return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float var1) {
      this.speed = var1;
   }

   public boolean doHurtTarget(Entity var1) {
      this.setLastHurtMob(var1);
      return false;
   }

   public void tick() {
      super.tick();
      this.updatingUsingItem();
      this.updateSwimAmount();
      if (!this.level().isClientSide) {
         int var1 = this.getArrowCount();
         if (var1 > 0) {
            if (this.removeArrowTime <= 0) {
               this.removeArrowTime = 20 * (30 - var1);
            }

            --this.removeArrowTime;
            if (this.removeArrowTime <= 0) {
               this.setArrowCount(var1 - 1);
            }
         }

         int var2 = this.getStingerCount();
         if (var2 > 0) {
            if (this.removeStingerTime <= 0) {
               this.removeStingerTime = 20 * (30 - var2);
            }

            --this.removeStingerTime;
            if (this.removeStingerTime <= 0) {
               this.setStingerCount(var2 - 1);
            }
         }

         this.detectEquipmentUpdates();
         if (this.tickCount % 20 == 0) {
            this.getCombatTracker().recheckStatus();
         }

         if (this.isSleeping() && !this.checkBedExists()) {
            this.stopSleeping();
         }
      }

      if (!this.isRemoved()) {
         this.aiStep();
      }

      double var11 = this.getX() - this.xo;
      double var3 = this.getZ() - this.zo;
      float var5 = (float)(var11 * var11 + var3 * var3);
      float var6 = this.yBodyRot;
      float var7 = 0.0F;
      this.oRun = this.run;
      float var8 = 0.0F;
      if (var5 > 0.0025000002F) {
         var8 = 1.0F;
         var7 = (float)Math.sqrt((double)var5) * 3.0F;
         float var9 = (float)Mth.atan2(var3, var11) * 57.295776F - 90.0F;
         float var10 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - var9);
         if (95.0F < var10 && var10 < 265.0F) {
            var6 = var9 - 180.0F;
         } else {
            var6 = var9;
         }
      }

      if (this.attackAnim > 0.0F) {
         var6 = this.getYRot();
      }

      if (!this.onGround()) {
         var8 = 0.0F;
      }

      this.run += (var8 - this.run) * 0.3F;
      this.level().getProfiler().push("headTurn");
      var7 = this.tickHeadTurn(var6, var7);
      this.level().getProfiler().pop();
      this.level().getProfiler().push("rangeChecks");

      while(this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while(this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      while(this.yBodyRot - this.yBodyRotO < -180.0F) {
         this.yBodyRotO -= 360.0F;
      }

      while(this.yBodyRot - this.yBodyRotO >= 180.0F) {
         this.yBodyRotO += 360.0F;
      }

      while(this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while(this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while(this.yHeadRot - this.yHeadRotO < -180.0F) {
         this.yHeadRotO -= 360.0F;
      }

      while(this.yHeadRot - this.yHeadRotO >= 180.0F) {
         this.yHeadRotO += 360.0F;
      }

      this.level().getProfiler().pop();
      this.animStep += var7;
      if (this.isFallFlying()) {
         ++this.fallFlyTicks;
      } else {
         this.fallFlyTicks = 0;
      }

      if (this.isSleeping()) {
         this.setXRot(0.0F);
      }

   }

   private void detectEquipmentUpdates() {
      Map var1 = this.collectEquipmentChanges();
      if (var1 != null) {
         this.handleHandSwap(var1);
         if (!var1.isEmpty()) {
            this.handleEquipmentChanges(var1);
         }
      }

   }

   @Nullable
   private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
      EnumMap var1 = null;
      EquipmentSlot[] var2 = EquipmentSlot.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         EquipmentSlot var5 = var2[var4];
         ItemStack var6;
         switch(var5.getType()) {
         case HAND:
            var6 = this.getLastHandItem(var5);
            break;
         case ARMOR:
            var6 = this.getLastArmorItem(var5);
            break;
         default:
            continue;
         }

         ItemStack var7 = this.getItemBySlot(var5);
         if (this.equipmentHasChanged(var6, var7)) {
            if (var1 == null) {
               var1 = Maps.newEnumMap(EquipmentSlot.class);
            }

            var1.put(var5, var7);
            if (!var6.isEmpty()) {
               this.getAttributes().removeAttributeModifiers(var6.getAttributeModifiers(var5));
            }

            if (!var7.isEmpty()) {
               this.getAttributes().addTransientAttributeModifiers(var7.getAttributeModifiers(var5));
            }
         }
      }

      return var1;
   }

   public boolean equipmentHasChanged(ItemStack var1, ItemStack var2) {
      return !ItemStack.matches(var2, var1);
   }

   private void handleHandSwap(Map<EquipmentSlot, ItemStack> var1) {
      ItemStack var2 = (ItemStack)var1.get(EquipmentSlot.MAINHAND);
      ItemStack var3 = (ItemStack)var1.get(EquipmentSlot.OFFHAND);
      if (var2 != null && var3 != null && ItemStack.matches(var2, this.getLastHandItem(EquipmentSlot.OFFHAND)) && ItemStack.matches(var3, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
         ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
         var1.remove(EquipmentSlot.MAINHAND);
         var1.remove(EquipmentSlot.OFFHAND);
         this.setLastHandItem(EquipmentSlot.MAINHAND, var2.copy());
         this.setLastHandItem(EquipmentSlot.OFFHAND, var3.copy());
      }

   }

   private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> var1) {
      ArrayList var2 = Lists.newArrayListWithCapacity(var1.size());
      var1.forEach((var2x, var3) -> {
         ItemStack var4 = var3.copy();
         var2.add(Pair.of(var2x, var4));
         switch(var2x.getType()) {
         case HAND:
            this.setLastHandItem(var2x, var4);
            break;
         case ARMOR:
            this.setLastArmorItem(var2x, var4);
         }

      });
      ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), var2));
   }

   private ItemStack getLastArmorItem(EquipmentSlot var1) {
      return (ItemStack)this.lastArmorItemStacks.get(var1.getIndex());
   }

   private void setLastArmorItem(EquipmentSlot var1, ItemStack var2) {
      this.lastArmorItemStacks.set(var1.getIndex(), var2);
   }

   private ItemStack getLastHandItem(EquipmentSlot var1) {
      return (ItemStack)this.lastHandItemStacks.get(var1.getIndex());
   }

   private void setLastHandItem(EquipmentSlot var1, ItemStack var2) {
      this.lastHandItemStacks.set(var1.getIndex(), var2);
   }

   protected float tickHeadTurn(float var1, float var2) {
      float var3 = Mth.wrapDegrees(var1 - this.yBodyRot);
      this.yBodyRot += var3 * 0.3F;
      float var4 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
      if (Math.abs(var4) > 50.0F) {
         this.yBodyRot += var4 - (float)(Mth.sign((double)var4) * 50);
      }

      boolean var5 = var4 < -90.0F || var4 >= 90.0F;
      if (var5) {
         var2 *= -1.0F;
      }

      return var2;
   }

   public void aiStep() {
      if (this.noJumpDelay > 0) {
         --this.noJumpDelay;
      }

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
      } else if (!this.isEffectiveAi()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
      }

      if (this.lerpHeadSteps > 0) {
         this.yHeadRot += (float)Mth.wrapDegrees(this.lyHeadRot - (double)this.yHeadRot) / (float)this.lerpHeadSteps;
         --this.lerpHeadSteps;
      }

      Vec3 var13 = this.getDeltaMovement();
      double var2 = var13.x;
      double var4 = var13.y;
      double var6 = var13.z;
      if (Math.abs(var13.x) < 0.003D) {
         var2 = 0.0D;
      }

      if (Math.abs(var13.y) < 0.003D) {
         var4 = 0.0D;
      }

      if (Math.abs(var13.z) < 0.003D) {
         var6 = 0.0D;
      }

      this.setDeltaMovement(var2, var4, var6);
      this.level().getProfiler().push("ai");
      if (this.isImmobile()) {
         this.jumping = false;
         this.xxa = 0.0F;
         this.zza = 0.0F;
      } else if (this.isEffectiveAi()) {
         this.level().getProfiler().push("newAi");
         this.serverAiStep();
         this.level().getProfiler().pop();
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("jump");
      if (this.jumping && this.isAffectedByFluids()) {
         double var8;
         if (this.isInLava()) {
            var8 = this.getFluidHeight(FluidTags.LAVA);
         } else {
            var8 = this.getFluidHeight(FluidTags.WATER);
         }

         boolean var10 = this.isInWater() && var8 > 0.0D;
         double var11 = this.getFluidJumpThreshold();
         if (!var10 || this.onGround() && !(var8 > var11)) {
            if (this.isInLava() && (!this.onGround() || var8 > var11)) {
               this.jumpInLiquid(FluidTags.LAVA);
            } else if ((this.onGround() || var10 && var8 <= var11) && this.noJumpDelay == 0) {
               this.jumpFromGround();
               this.noJumpDelay = 10;
            }
         } else {
            this.jumpInLiquid(FluidTags.WATER);
         }
      } else {
         this.noJumpDelay = 0;
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("travel");
      this.xxa *= 0.98F;
      this.zza *= 0.98F;
      this.updateFallFlying();
      AABB var14 = this.getBoundingBox();
      Vec3 var9 = new Vec3((double)this.xxa, (double)this.yya, (double)this.zza);
      if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
         this.resetFallDistance();
      }

      label104: {
         LivingEntity var17 = this.getControllingPassenger();
         if (var17 instanceof Player) {
            Player var15 = (Player)var17;
            if (this.isAlive()) {
               this.travelRidden(var15, var9);
               break label104;
            }
         }

         this.travel(var9);
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("freezing");
      if (!this.level().isClientSide && !this.isDeadOrDying()) {
         int var16 = this.getTicksFrozen();
         if (this.isInPowderSnow && this.canFreeze()) {
            this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), var16 + 1));
         } else {
            this.setTicksFrozen(Math.max(0, var16 - 2));
         }
      }

      this.removeFrost();
      this.tryAddFrost();
      if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
         this.hurt(this.damageSources().freeze(), 1.0F);
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("push");
      if (this.autoSpinAttackTicks > 0) {
         --this.autoSpinAttackTicks;
         this.checkAutoSpinAttack(var14, this.getBoundingBox());
      }

      this.pushEntities();
      this.level().getProfiler().pop();
      if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
         this.hurt(this.damageSources().drown(), 1.0F);
      }

   }

   public boolean isSensitiveToWater() {
      return false;
   }

   private void updateFallFlying() {
      boolean var1 = this.getSharedFlag(7);
      if (var1 && !this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
         ItemStack var2 = this.getItemBySlot(EquipmentSlot.CHEST);
         if (var2.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(var2)) {
            var1 = true;
            int var3 = this.fallFlyTicks + 1;
            if (!this.level().isClientSide && var3 % 10 == 0) {
               int var4 = var3 / 10;
               if (var4 % 2 == 0) {
                  var2.hurtAndBreak(1, this, (var0) -> {
                     var0.broadcastBreakEvent(EquipmentSlot.CHEST);
                  });
               }

               this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
         } else {
            var1 = false;
         }
      } else {
         var1 = false;
      }

      if (!this.level().isClientSide) {
         this.setSharedFlag(7, var1);
      }

   }

   protected void serverAiStep() {
   }

   protected void pushEntities() {
      if (this.level().isClientSide()) {
         this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
      } else {
         List var1 = this.level().getEntities((Entity)this, this.getBoundingBox(), EntitySelector.pushableBy(this));
         if (!var1.isEmpty()) {
            int var2 = this.level().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            int var3;
            if (var2 > 0 && var1.size() > var2 - 1 && this.random.nextInt(4) == 0) {
               var3 = 0;

               for(int var4 = 0; var4 < var1.size(); ++var4) {
                  if (!((Entity)var1.get(var4)).isPassenger()) {
                     ++var3;
                  }
               }

               if (var3 > var2 - 1) {
                  this.hurt(this.damageSources().cramming(), 6.0F);
               }
            }

            for(var3 = 0; var3 < var1.size(); ++var3) {
               Entity var5 = (Entity)var1.get(var3);
               this.doPush(var5);
            }
         }

      }
   }

   protected void checkAutoSpinAttack(AABB var1, AABB var2) {
      AABB var3 = var1.minmax(var2);
      List var4 = this.level().getEntities(this, var3);
      if (!var4.isEmpty()) {
         for(int var5 = 0; var5 < var4.size(); ++var5) {
            Entity var6 = (Entity)var4.get(var5);
            if (var6 instanceof LivingEntity) {
               this.doAutoAttackOnTouch((LivingEntity)var6);
               this.autoSpinAttackTicks = 0;
               this.setDeltaMovement(this.getDeltaMovement().scale(-0.2D));
               break;
            }
         }
      } else if (this.horizontalCollision) {
         this.autoSpinAttackTicks = 0;
      }

      if (!this.level().isClientSide && this.autoSpinAttackTicks <= 0) {
         this.setLivingEntityFlag(4, false);
      }

   }

   protected void doPush(Entity var1) {
      var1.push(this);
   }

   protected void doAutoAttackOnTouch(LivingEntity var1) {
   }

   public boolean isAutoSpinAttack() {
      return ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
   }

   public void stopRiding() {
      Entity var1 = this.getVehicle();
      super.stopRiding();
      if (var1 != null && var1 != this.getVehicle() && !this.level().isClientSide) {
         this.dismountVehicle(var1);
      }

   }

   public void rideTick() {
      super.rideTick();
      this.oRun = this.run;
      this.run = 0.0F;
      this.resetFallDistance();
   }

   public void lerpTo(double var1, double var3, double var5, float var7, float var8, int var9, boolean var10) {
      this.lerpX = var1;
      this.lerpY = var3;
      this.lerpZ = var5;
      this.lerpYRot = (double)var7;
      this.lerpXRot = (double)var8;
      this.lerpSteps = var9;
   }

   public void lerpHeadTo(float var1, int var2) {
      this.lyHeadRot = (double)var1;
      this.lerpHeadSteps = var2;
   }

   public void setJumping(boolean var1) {
      this.jumping = var1;
   }

   public void onItemPickup(ItemEntity var1) {
      Entity var2 = var1.getOwner();
      if (var2 instanceof ServerPlayer) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)var2, var1.getItem(), this);
      }

   }

   public void take(Entity var1, int var2) {
      if (!var1.isRemoved() && !this.level().isClientSide && (var1 instanceof ItemEntity || var1 instanceof AbstractArrow || var1 instanceof ExperienceOrb)) {
         ((ServerLevel)this.level()).getChunkSource().broadcast(var1, new ClientboundTakeItemEntityPacket(var1.getId(), this.getId(), var2));
      }

   }

   public boolean hasLineOfSight(Entity var1) {
      if (var1.level() != this.level()) {
         return false;
      } else {
         Vec3 var2 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
         Vec3 var3 = new Vec3(var1.getX(), var1.getEyeY(), var1.getZ());
         if (var3.distanceTo(var2) > 128.0D) {
            return false;
         } else {
            return this.level().clip(new ClipContext(var2, var3, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
         }
      }
   }

   public float getViewYRot(float var1) {
      return var1 == 1.0F ? this.yHeadRot : Mth.lerp(var1, this.yHeadRotO, this.yHeadRot);
   }

   public float getAttackAnim(float var1) {
      float var2 = this.attackAnim - this.oAttackAnim;
      if (var2 < 0.0F) {
         ++var2;
      }

      return this.oAttackAnim + var2 * var1;
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.onClimbable();
   }

   public float getYHeadRot() {
      return this.yHeadRot;
   }

   public void setYHeadRot(float var1) {
      this.yHeadRot = var1;
   }

   public void setYBodyRot(float var1) {
      this.yBodyRot = var1;
   }

   protected Vec3 getRelativePortalPosition(Direction.Axis var1, BlockUtil.FoundRectangle var2) {
      return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(var1, var2));
   }

   public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 var0) {
      return new Vec3(var0.x, var0.y, 0.0D);
   }

   public float getAbsorptionAmount() {
      return this.absorptionAmount;
   }

   public void setAbsorptionAmount(float var1) {
      if (var1 < 0.0F) {
         var1 = 0.0F;
      }

      this.absorptionAmount = var1;
   }

   public void onEnterCombat() {
   }

   public void onLeaveCombat() {
   }

   protected void updateEffectVisibility() {
      this.effectsDirty = true;
   }

   public abstract HumanoidArm getMainArm();

   public boolean isUsingItem() {
      return ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
   }

   public InteractionHand getUsedItemHand() {
      return ((Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private void updatingUsingItem() {
      if (this.isUsingItem()) {
         if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            this.updateUsingItem(this.useItem);
         } else {
            this.stopUsingItem();
         }
      }

   }

   protected void updateUsingItem(ItemStack var1) {
      var1.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
      if (this.shouldTriggerItemUseEffects()) {
         this.triggerItemUseEffects(var1, 5);
      }

      if (--this.useItemRemaining == 0 && !this.level().isClientSide && !var1.useOnRelease()) {
         this.completeUsingItem();
      }

   }

   private boolean shouldTriggerItemUseEffects() {
      int var1 = this.getUseItemRemainingTicks();
      FoodProperties var2 = this.useItem.getItem().getFoodProperties();
      boolean var3 = var2 != null && var2.isFastFood();
      var3 |= var1 <= this.useItem.getUseDuration() - 7;
      return var3 && var1 % 4 == 0;
   }

   private void updateSwimAmount() {
      this.swimAmountO = this.swimAmount;
      if (this.isVisuallySwimming()) {
         this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
      } else {
         this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
      }

   }

   protected void setLivingEntityFlag(int var1, boolean var2) {
      byte var3 = (Byte)this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
      int var4;
      if (var2) {
         var4 = var3 | var1;
      } else {
         var4 = var3 & ~var1;
      }

      this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)var4);
   }

   public void startUsingItem(InteractionHand var1) {
      ItemStack var2 = this.getItemInHand(var1);
      if (!var2.isEmpty() && !this.isUsingItem()) {
         this.useItem = var2;
         this.useItemRemaining = var2.getUseDuration();
         if (!this.level().isClientSide) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, var1 == InteractionHand.OFF_HAND);
            this.gameEvent(GameEvent.ITEM_INTERACT_START);
         }

      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      super.onSyncedDataUpdated(var1);
      if (SLEEPING_POS_ID.equals(var1)) {
         if (this.level().isClientSide) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
         }
      } else if (DATA_LIVING_ENTITY_FLAGS.equals(var1) && this.level().isClientSide) {
         if (this.isUsingItem() && this.useItem.isEmpty()) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            if (!this.useItem.isEmpty()) {
               this.useItemRemaining = this.useItem.getUseDuration();
            }
         } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
            this.useItem = ItemStack.EMPTY;
            this.useItemRemaining = 0;
         }
      }

   }

   public void lookAt(EntityAnchorArgument.Anchor var1, Vec3 var2) {
      super.lookAt(var1, var2);
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
   }

   protected void triggerItemUseEffects(ItemStack var1, int var2) {
      if (!var1.isEmpty() && this.isUsingItem()) {
         if (var1.getUseAnimation() == UseAnim.DRINK) {
            this.playSound(this.getDrinkingSound(var1), 0.5F, this.level().random.nextFloat() * 0.1F + 0.9F);
         }

         if (var1.getUseAnimation() == UseAnim.EAT) {
            this.spawnItemParticles(var1, var2);
            this.playSound(this.getEatingSound(var1), 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
         }

      }
   }

   private void spawnItemParticles(ItemStack var1, int var2) {
      for(int var3 = 0; var3 < var2; ++var3) {
         Vec3 var4 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
         var4 = var4.xRot(-this.getXRot() * 0.017453292F);
         var4 = var4.yRot(-this.getYRot() * 0.017453292F);
         double var5 = (double)(-this.random.nextFloat()) * 0.6D - 0.3D;
         Vec3 var7 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.3D, var5, 0.6D);
         var7 = var7.xRot(-this.getXRot() * 0.017453292F);
         var7 = var7.yRot(-this.getYRot() * 0.017453292F);
         var7 = var7.add(this.getX(), this.getEyeY(), this.getZ());
         this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, var1), var7.x, var7.y, var7.z, var4.x, var4.y + 0.05D, var4.z);
      }

   }

   protected void completeUsingItem() {
      if (!this.level().isClientSide || this.isUsingItem()) {
         InteractionHand var1 = this.getUsedItemHand();
         if (!this.useItem.equals(this.getItemInHand(var1))) {
            this.releaseUsingItem();
         } else {
            if (!this.useItem.isEmpty() && this.isUsingItem()) {
               this.triggerItemUseEffects(this.useItem, 16);
               ItemStack var2 = this.useItem.finishUsingItem(this.level(), this);
               if (var2 != this.useItem) {
                  this.setItemInHand(var1, var2);
               }

               this.stopUsingItem();
            }

         }
      }
   }

   public ItemStack getUseItem() {
      return this.useItem;
   }

   public int getUseItemRemainingTicks() {
      return this.useItemRemaining;
   }

   public int getTicksUsingItem() {
      return this.isUsingItem() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
   }

   public void releaseUsingItem() {
      if (!this.useItem.isEmpty()) {
         this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
         if (this.useItem.useOnRelease()) {
            this.updatingUsingItem();
         }
      }

      this.stopUsingItem();
   }

   public void stopUsingItem() {
      if (!this.level().isClientSide) {
         boolean var1 = this.isUsingItem();
         this.setLivingEntityFlag(1, false);
         if (var1) {
            this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
         }
      }

      this.useItem = ItemStack.EMPTY;
      this.useItemRemaining = 0;
   }

   public boolean isBlocking() {
      if (this.isUsingItem() && !this.useItem.isEmpty()) {
         Item var1 = this.useItem.getItem();
         if (var1.getUseAnimation(this.useItem) != UseAnim.BLOCK) {
            return false;
         } else {
            return var1.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
         }
      } else {
         return false;
      }
   }

   public boolean isSuppressingSlidingDownLadder() {
      return this.isShiftKeyDown();
   }

   public boolean isFallFlying() {
      return this.getSharedFlag(7);
   }

   public boolean isVisuallySwimming() {
      return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
   }

   public int getFallFlyingTicks() {
      return this.fallFlyTicks;
   }

   public boolean randomTeleport(double var1, double var3, double var5, boolean var7) {
      double var8 = this.getX();
      double var10 = this.getY();
      double var12 = this.getZ();
      double var14 = var3;
      boolean var16 = false;
      BlockPos var17 = BlockPos.containing(var1, var3, var5);
      Level var18 = this.level();
      if (var18.hasChunkAt(var17)) {
         boolean var19 = false;

         while(!var19 && var17.getY() > var18.getMinBuildHeight()) {
            BlockPos var20 = var17.below();
            BlockState var21 = var18.getBlockState(var20);
            if (var21.blocksMotion()) {
               var19 = true;
            } else {
               --var14;
               var17 = var20;
            }
         }

         if (var19) {
            this.teleportTo(var1, var14, var5);
            if (var18.noCollision(this) && !var18.containsAnyLiquid(this.getBoundingBox())) {
               var16 = true;
            }
         }
      }

      if (!var16) {
         this.teleportTo(var8, var10, var12);
         return false;
      } else {
         if (var7) {
            var18.broadcastEntityEvent(this, (byte)46);
         }

         if (this instanceof PathfinderMob) {
            ((PathfinderMob)this).getNavigation().stop();
         }

         return true;
      }
   }

   public boolean isAffectedByPotions() {
      return true;
   }

   public boolean attackable() {
      return true;
   }

   public void setRecordPlayingNearby(BlockPos var1, boolean var2) {
   }

   public boolean canTakeItem(ItemStack var1) {
      return false;
   }

   public EntityDimensions getDimensions(Pose var1) {
      return var1 == Pose.SLEEPING ? SLEEPING_DIMENSIONS : super.getDimensions(var1).scale(this.getScale());
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING);
   }

   public AABB getLocalBoundsForPose(Pose var1) {
      EntityDimensions var2 = this.getDimensions(var1);
      return new AABB((double)(-var2.width / 2.0F), 0.0D, (double)(-var2.width / 2.0F), (double)(var2.width / 2.0F), (double)var2.height, (double)(var2.width / 2.0F));
   }

   public boolean canChangeDimensions() {
      return super.canChangeDimensions() && !this.isSleeping();
   }

   public Optional<BlockPos> getSleepingPos() {
      return (Optional)this.entityData.get(SLEEPING_POS_ID);
   }

   public void setSleepingPos(BlockPos var1) {
      this.entityData.set(SLEEPING_POS_ID, Optional.of(var1));
   }

   public void clearSleepingPos() {
      this.entityData.set(SLEEPING_POS_ID, Optional.empty());
   }

   public boolean isSleeping() {
      return this.getSleepingPos().isPresent();
   }

   public void startSleeping(BlockPos var1) {
      if (this.isPassenger()) {
         this.stopRiding();
      }

      BlockState var2 = this.level().getBlockState(var1);
      if (var2.getBlock() instanceof BedBlock) {
         this.level().setBlock(var1, (BlockState)var2.setValue(BedBlock.OCCUPIED, true), 3);
      }

      this.setPose(Pose.SLEEPING);
      this.setPosToBed(var1);
      this.setSleepingPos(var1);
      this.setDeltaMovement(Vec3.ZERO);
      this.hasImpulse = true;
   }

   private void setPosToBed(BlockPos var1) {
      this.setPos((double)var1.getX() + 0.5D, (double)var1.getY() + 0.6875D, (double)var1.getZ() + 0.5D);
   }

   private boolean checkBedExists() {
      return (Boolean)this.getSleepingPos().map((var1) -> {
         return this.level().getBlockState(var1).getBlock() instanceof BedBlock;
      }).orElse(false);
   }

   public void stopSleeping() {
      Optional var10000 = this.getSleepingPos();
      Level var10001 = this.level();
      java.util.Objects.requireNonNull(var10001);
      var10000.filter(var10001::hasChunkAt).ifPresent((var1x) -> {
         BlockState var2 = this.level().getBlockState(var1x);
         if (var2.getBlock() instanceof BedBlock) {
            Direction var3 = (Direction)var2.getValue(BedBlock.FACING);
            this.level().setBlock(var1x, (BlockState)var2.setValue(BedBlock.OCCUPIED, false), 3);
            Vec3 var4 = (Vec3)BedBlock.findStandUpPosition(this.getType(), this.level(), var1x, var3, this.getYRot()).orElseGet(() -> {
               BlockPos var1 = var1x.above();
               return new Vec3((double)var1.getX() + 0.5D, (double)var1.getY() + 0.1D, (double)var1.getZ() + 0.5D);
            });
            Vec3 var5 = Vec3.atBottomCenterOf(var1x).subtract(var4).normalize();
            float var6 = (float)Mth.wrapDegrees(Mth.atan2(var5.z, var5.x) * 57.2957763671875D - 90.0D);
            this.setPos(var4.x, var4.y, var4.z);
            this.setYRot(var6);
            this.setXRot(0.0F);
         }

      });
      Vec3 var1 = this.position();
      this.setPose(Pose.STANDING);
      this.setPos(var1.x, var1.y, var1.z);
      this.clearSleepingPos();
   }

   @Nullable
   public Direction getBedOrientation() {
      BlockPos var1 = (BlockPos)this.getSleepingPos().orElse((Object)null);
      return var1 != null ? BedBlock.getBedOrientation(this.level(), var1) : null;
   }

   public boolean isInWall() {
      return !this.isSleeping() && super.isInWall();
   }

   protected final float getEyeHeight(Pose var1, EntityDimensions var2) {
      return var1 == Pose.SLEEPING ? 0.2F : this.getStandingEyeHeight(var1, var2);
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return super.getEyeHeight(var1, var2);
   }

   public ItemStack getProjectile(ItemStack var1) {
      return ItemStack.EMPTY;
   }

   public ItemStack eat(Level var1, ItemStack var2) {
      if (var2.isEdible()) {
         var1.playSound((Player)null, this.getX(), this.getY(), this.getZ(), this.getEatingSound(var2), SoundSource.NEUTRAL, 1.0F, 1.0F + (var1.random.nextFloat() - var1.random.nextFloat()) * 0.4F);
         this.addEatEffect(var2, var1, this);
         if (!(this instanceof Player) || !((Player)this).getAbilities().instabuild) {
            var2.shrink(1);
         }

         this.gameEvent(GameEvent.EAT);
      }

      return var2;
   }

   private void addEatEffect(ItemStack var1, Level var2, LivingEntity var3) {
      Item var4 = var1.getItem();
      if (var4.isEdible()) {
         List var5 = var4.getFoodProperties().getEffects();
         Iterator var6 = var5.iterator();

         while(var6.hasNext()) {
            Pair var7 = (Pair)var6.next();
            if (!var2.isClientSide && var7.getFirst() != null && var2.random.nextFloat() < (Float)var7.getSecond()) {
               var3.addEffect(new MobEffectInstance((MobEffectInstance)var7.getFirst()));
            }
         }
      }

   }

   private static byte entityEventForEquipmentBreak(EquipmentSlot var0) {
      switch(var0) {
      case MAINHAND:
         return 47;
      case OFFHAND:
         return 48;
      case HEAD:
         return 49;
      case CHEST:
         return 50;
      case FEET:
         return 52;
      case LEGS:
         return 51;
      default:
         return 47;
      }
   }

   public void broadcastBreakEvent(EquipmentSlot var1) {
      this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(var1));
   }

   public void broadcastBreakEvent(InteractionHand var1) {
      this.broadcastBreakEvent(var1 == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
   }

   public AABB getBoundingBoxForCulling() {
      if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
         float var1 = 0.5F;
         return this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D);
      } else {
         return super.getBoundingBoxForCulling();
      }
   }

   public static EquipmentSlot getEquipmentSlotForItem(ItemStack var0) {
      Equipable var1 = Equipable.get(var0);
      return var1 != null ? var1.getEquipmentSlot() : EquipmentSlot.MAINHAND;
   }

   private static SlotAccess createEquipmentSlotAccess(LivingEntity var0, EquipmentSlot var1) {
      return var1 != EquipmentSlot.HEAD && var1 != EquipmentSlot.MAINHAND && var1 != EquipmentSlot.OFFHAND ? SlotAccess.forEquipmentSlot(var0, var1, (var1x) -> {
         return var1x.isEmpty() || Mob.getEquipmentSlotForItem(var1x) == var1;
      }) : SlotAccess.forEquipmentSlot(var0, var1);
   }

   @Nullable
   private static EquipmentSlot getEquipmentSlot(int var0) {
      if (var0 == 100 + EquipmentSlot.HEAD.getIndex()) {
         return EquipmentSlot.HEAD;
      } else if (var0 == 100 + EquipmentSlot.CHEST.getIndex()) {
         return EquipmentSlot.CHEST;
      } else if (var0 == 100 + EquipmentSlot.LEGS.getIndex()) {
         return EquipmentSlot.LEGS;
      } else if (var0 == 100 + EquipmentSlot.FEET.getIndex()) {
         return EquipmentSlot.FEET;
      } else if (var0 == 98) {
         return EquipmentSlot.MAINHAND;
      } else {
         return var0 == 99 ? EquipmentSlot.OFFHAND : null;
      }
   }

   public SlotAccess getSlot(int var1) {
      EquipmentSlot var2 = getEquipmentSlot(var1);
      return var2 != null ? createEquipmentSlotAccess(this, var2) : super.getSlot(var1);
   }

   public boolean canFreeze() {
      if (this.isSpectator()) {
         return false;
      } else {
         boolean var1 = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
         return var1 && super.canFreeze();
      }
   }

   public boolean isCurrentlyGlowing() {
      return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
   }

   public float getVisualRotationYInDegrees() {
      return this.yBodyRot;
   }

   public void recreateFromPacket(ClientboundAddEntityPacket var1) {
      double var2 = var1.getX();
      double var4 = var1.getY();
      double var6 = var1.getZ();
      float var8 = var1.getYRot();
      float var9 = var1.getXRot();
      this.syncPacketPositionCodec(var2, var4, var6);
      this.yBodyRot = var1.getYHeadRot();
      this.yHeadRot = var1.getYHeadRot();
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.setId(var1.getId());
      this.setUUID(var1.getUUID());
      this.absMoveTo(var2, var4, var6, var8, var9);
      this.setDeltaMovement(var1.getXa(), var1.getYa(), var1.getZa());
   }

   public boolean canDisableShield() {
      return this.getMainHandItem().getItem() instanceof AxeItem;
   }

   public float maxUpStep() {
      float var1 = super.maxUpStep();
      return this.getControllingPassenger() instanceof Player ? Math.max(var1, 1.0F) : var1;
   }

   static {
      SPEED_MODIFIER_SPRINTING = new AttributeModifier(SPEED_MODIFIER_SPRINTING_UUID, "Sprinting speed boost", 0.30000001192092896D, AttributeModifier.Operation.MULTIPLY_TOTAL);
      DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
      DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
      DATA_EFFECT_COLOR_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
      DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
      DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
      DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
      SLEEPING_POS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
      SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F);
   }

   public static record Fallsounds(SoundEvent a, SoundEvent b) {
      private final SoundEvent small;
      private final SoundEvent big;

      public Fallsounds(SoundEvent var1, SoundEvent var2) {
         this.small = var1;
         this.big = var2;
      }

      public SoundEvent small() {
         return this.small;
      }

      public SoundEvent big() {
         return this.big;
      }
   }
}
