package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class Allay extends PathfinderMob implements InventoryCarrier, VibrationSystem {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Vec3i ITEM_PICKUP_REACH = new Vec3i(1, 1, 1);
   private static final int LIFTING_ITEM_ANIMATION_DURATION = 5;
   private static final float DANCING_LOOP_DURATION = 55.0F;
   private static final float SPINNING_ANIMATION_DURATION = 15.0F;
   private static final Ingredient DUPLICATION_ITEM;
   private static final int DUPLICATION_COOLDOWN_TICKS = 6000;
   private static final int NUM_OF_DUPLICATION_HEARTS = 3;
   private static final double RIDING_OFFSET = 0.4D;
   private static final EntityDataAccessor<Boolean> DATA_DANCING;
   private static final EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE;
   protected static final ImmutableList<SensorType<? extends Sensor<? super Allay>>> SENSOR_TYPES;
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES;
   public static final ImmutableList<Float> THROW_SOUND_PITCHES;
   private final DynamicGameEventListener<VibrationSystem.Listener> dynamicVibrationListener;
   private VibrationSystem.Data vibrationData;
   private final VibrationSystem.User vibrationUser;
   private final DynamicGameEventListener<Allay.JukeboxListener> dynamicJukeboxListener;
   private final SimpleContainer inventory = new SimpleContainer(1);
   @Nullable
   private BlockPos jukeboxPos;
   private long duplicationCooldown;
   private float holdingItemAnimationTicks;
   private float holdingItemAnimationTicks0;
   private float dancingAnimationTicks;
   private float spinningAnimationTicks;
   private float spinningAnimationTicks0;

   public Allay(EntityType<? extends Allay> var1, Level var2) {
      super(var1, var2);
      this.moveControl = new FlyingMoveControl(this, 20, true);
      this.setCanPickUpLoot(this.canPickUpLoot());
      this.vibrationUser = new Allay.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      this.dynamicVibrationListener = new DynamicGameEventListener(new VibrationSystem.Listener(this));
      this.dynamicJukeboxListener = new DynamicGameEventListener(new Allay.JukeboxListener(this.vibrationUser.getPositionSource(), GameEvent.JUKEBOX_PLAY.getNotificationRadius()));
   }

   protected Brain.Provider<Allay> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected Brain<?> makeBrain(Dynamic<?> var1) {
      return AllayAi.makeBrain(this.brainProvider().makeBrain(var1));
   }

   public Brain<Allay> getBrain() {
      return super.getBrain();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FLYING_SPEED, 0.10000000149011612D).add(Attributes.MOVEMENT_SPEED, 0.10000000149011612D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.FOLLOW_RANGE, 48.0D);
   }

   protected PathNavigation createNavigation(Level var1) {
      FlyingPathNavigation var2 = new FlyingPathNavigation(this, var1);
      var2.setCanOpenDoors(false);
      var2.setCanFloat(true);
      var2.setCanPassDoors(true);
      return var2;
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_DANCING, false);
      this.entityData.define(DATA_CAN_DUPLICATE, true);
   }

   public void travel(Vec3 var1) {
      if (this.isControlledByLocalInstance()) {
         if (this.isInWater()) {
            this.moveRelative(0.02F, var1);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.800000011920929D));
         } else if (this.isInLava()) {
            this.moveRelative(0.02F, var1);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
         } else {
            this.moveRelative(this.getSpeed(), var1);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9100000262260437D));
         }
      }

      this.calculateEntityAnimation(false);
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return var2.height * 0.6F;
   }

   public boolean hurt(DamageSource var1, float var2) {
      Entity var4 = var1.getEntity();
      if (var4 instanceof Player) {
         Player var3 = (Player)var4;
         Optional var5 = this.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
         if (var5.isPresent() && var3.getUUID().equals(var5.get())) {
            return false;
         }
      }

      return super.hurt(var1, var2);
   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
   }

   protected void checkFallDamage(double var1, boolean var3, BlockState var4, BlockPos var5) {
   }

   protected SoundEvent getAmbientSound() {
      return this.hasItemInSlot(EquipmentSlot.MAINHAND) ? SoundEvents.ALLAY_AMBIENT_WITH_ITEM : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.ALLAY_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ALLAY_DEATH;
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   protected void customServerAiStep() {
      this.level().getProfiler().push("allayBrain");
      this.getBrain().tick((ServerLevel)this.level(), this);
      this.level().getProfiler().pop();
      this.level().getProfiler().push("allayActivityUpdate");
      AllayAi.updateActivity(this);
      this.level().getProfiler().pop();
      super.customServerAiStep();
   }

   public void aiStep() {
      super.aiStep();
      if (!this.level().isClientSide && this.isAlive() && this.tickCount % 10 == 0) {
         this.heal(1.0F);
      }

      if (this.isDancing() && this.shouldStopDancing() && this.tickCount % 20 == 0) {
         this.setDancing(false);
         this.jukeboxPos = null;
      }

      this.updateDuplicationCooldown();
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide) {
         this.holdingItemAnimationTicks0 = this.holdingItemAnimationTicks;
         if (this.hasItemInHand()) {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks + 1.0F, 0.0F, 5.0F);
         } else {
            this.holdingItemAnimationTicks = Mth.clamp(this.holdingItemAnimationTicks - 1.0F, 0.0F, 5.0F);
         }

         if (this.isDancing()) {
            ++this.dancingAnimationTicks;
            this.spinningAnimationTicks0 = this.spinningAnimationTicks;
            if (this.isSpinning()) {
               ++this.spinningAnimationTicks;
            } else {
               --this.spinningAnimationTicks;
            }

            this.spinningAnimationTicks = Mth.clamp(this.spinningAnimationTicks, 0.0F, 15.0F);
         } else {
            this.dancingAnimationTicks = 0.0F;
            this.spinningAnimationTicks = 0.0F;
            this.spinningAnimationTicks0 = 0.0F;
         }
      } else {
         VibrationSystem.Ticker.tick(this.level(), this.vibrationData, this.vibrationUser);
         if (this.isPanicking()) {
            this.setDancing(false);
         }
      }

   }

   public boolean canPickUpLoot() {
      return !this.isOnPickupCooldown() && this.hasItemInHand();
   }

   public boolean hasItemInHand() {
      return !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty();
   }

   public boolean canTakeItem(ItemStack var1) {
      return false;
   }

   private boolean isOnPickupCooldown() {
      return this.getBrain().checkMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryStatus.VALUE_PRESENT);
   }

   protected InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      ItemStack var4 = this.getItemInHand(InteractionHand.MAIN_HAND);
      if (this.isDancing() && this.isDuplicationItem(var3) && this.canDuplicate()) {
         this.duplicateAllay();
         this.level().broadcastEntityEvent(this, (byte)18);
         this.level().playSound((Player)var1, (Entity)this, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.removeInteractionItem(var1, var3);
         return InteractionResult.SUCCESS;
      } else if (var4.isEmpty() && !var3.isEmpty()) {
         ItemStack var7 = var3.copyWithCount(1);
         this.setItemInHand(InteractionHand.MAIN_HAND, var7);
         this.removeInteractionItem(var1, var3);
         this.level().playSound((Player)var1, (Entity)this, SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.getBrain().setMemory(MemoryModuleType.LIKED_PLAYER, (Object)var1.getUUID());
         return InteractionResult.SUCCESS;
      } else if (!var4.isEmpty() && var2 == InteractionHand.MAIN_HAND && var3.isEmpty()) {
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
         this.level().playSound((Player)var1, (Entity)this, SoundEvents.ALLAY_ITEM_TAKEN, SoundSource.NEUTRAL, 2.0F, 1.0F);
         this.swing(InteractionHand.MAIN_HAND);
         Iterator var5 = this.getInventory().removeAllItems().iterator();

         while(var5.hasNext()) {
            ItemStack var6 = (ItemStack)var5.next();
            BehaviorUtils.throwItem(this, var6, this.position());
         }

         this.getBrain().eraseMemory(MemoryModuleType.LIKED_PLAYER);
         var1.addItem(var4);
         return InteractionResult.SUCCESS;
      } else {
         return super.mobInteract(var1, var2);
      }
   }

   public void setJukeboxPlaying(BlockPos var1, boolean var2) {
      if (var2) {
         if (!this.isDancing()) {
            this.jukeboxPos = var1;
            this.setDancing(true);
         }
      } else if (var1.equals(this.jukeboxPos) || this.jukeboxPos == null) {
         this.jukeboxPos = null;
         this.setDancing(false);
      }

   }

   public SimpleContainer getInventory() {
      return this.inventory;
   }

   protected Vec3i getPickupReach() {
      return ITEM_PICKUP_REACH;
   }

   public boolean wantsToPickUp(ItemStack var1) {
      ItemStack var2 = this.getItemInHand(InteractionHand.MAIN_HAND);
      return !var2.isEmpty() && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && this.inventory.canAddItem(var1) && this.allayConsidersItemEqual(var2, var1);
   }

   private boolean allayConsidersItemEqual(ItemStack var1, ItemStack var2) {
      return ItemStack.isSameItem(var1, var2) && !this.hasNonMatchingPotion(var1, var2);
   }

   private boolean hasNonMatchingPotion(ItemStack var1, ItemStack var2) {
      CompoundTag var3 = var1.getTag();
      boolean var4 = var3 != null && var3.contains("Potion");
      if (!var4) {
         return false;
      } else {
         CompoundTag var5 = var2.getTag();
         boolean var6 = var5 != null && var5.contains("Potion");
         if (!var6) {
            return true;
         } else {
            Tag var7 = var3.get("Potion");
            Tag var8 = var5.get("Potion");
            return var7 != null && var8 != null && !var7.equals(var8);
         }
      }
   }

   protected void pickUpItem(ItemEntity var1) {
      InventoryCarrier.pickUpItem(this, this, var1);
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   public boolean isFlapping() {
      return !this.onGround();
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> var1) {
      Level var3 = this.level();
      if (var3 instanceof ServerLevel) {
         ServerLevel var2 = (ServerLevel)var3;
         var1.accept(this.dynamicVibrationListener, var2);
         var1.accept(this.dynamicJukeboxListener, var2);
      }

   }

   public boolean isDancing() {
      return (Boolean)this.entityData.get(DATA_DANCING);
   }

   public boolean isPanicking() {
      return this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent();
   }

   public void setDancing(boolean var1) {
      if (!this.level().isClientSide && this.isEffectiveAi() && (!var1 || !this.isPanicking())) {
         this.entityData.set(DATA_DANCING, var1);
      }
   }

   private boolean shouldStopDancing() {
      return this.jukeboxPos == null || !this.jukeboxPos.closerToCenterThan(this.position(), (double)GameEvent.JUKEBOX_PLAY.getNotificationRadius()) || !this.level().getBlockState(this.jukeboxPos).is(Blocks.JUKEBOX);
   }

   public float getHoldingItemAnimationProgress(float var1) {
      return Mth.lerp(var1, this.holdingItemAnimationTicks0, this.holdingItemAnimationTicks) / 5.0F;
   }

   public boolean isSpinning() {
      float var1 = this.dancingAnimationTicks % 55.0F;
      return var1 < 15.0F;
   }

   public float getSpinningProgress(float var1) {
      return Mth.lerp(var1, this.spinningAnimationTicks0, this.spinningAnimationTicks) / 15.0F;
   }

   public boolean equipmentHasChanged(ItemStack var1, ItemStack var2) {
      return !this.allayConsidersItemEqual(var1, var2);
   }

   protected void dropEquipment() {
      super.dropEquipment();
      this.inventory.removeAllItems().forEach(this::spawnAtLocation);
      ItemStack var1 = this.getItemBySlot(EquipmentSlot.MAINHAND);
      if (!var1.isEmpty() && !EnchantmentHelper.hasVanishingCurse(var1)) {
         this.spawnAtLocation(var1);
         this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      }

   }

   public boolean removeWhenFarAway(double var1) {
      return false;
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      this.writeInventoryToTag(var1);
      DataResult var10000 = VibrationSystem.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.vibrationData);
      Logger var10001 = LOGGER;
      Objects.requireNonNull(var10001);
      var10000.resultOrPartial(var10001::error).ifPresent((var1x) -> {
         var1.put("listener", var1x);
      });
      var1.putLong("DuplicationCooldown", this.duplicationCooldown);
      var1.putBoolean("CanDuplicate", this.canDuplicate());
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.readInventoryFromTag(var1);
      if (var1.contains("listener", 10)) {
         DataResult var10000 = VibrationSystem.Data.CODEC.parse(new Dynamic(NbtOps.INSTANCE, var1.getCompound("listener")));
         Logger var10001 = LOGGER;
         Objects.requireNonNull(var10001);
         var10000.resultOrPartial(var10001::error).ifPresent((var1x) -> {
            this.vibrationData = var1x;
         });
      }

      this.duplicationCooldown = (long)var1.getInt("DuplicationCooldown");
      this.entityData.set(DATA_CAN_DUPLICATE, var1.getBoolean("CanDuplicate"));
   }

   protected boolean shouldStayCloseToLeashHolder() {
      return false;
   }

   private void updateDuplicationCooldown() {
      if (this.duplicationCooldown > 0L) {
         --this.duplicationCooldown;
      }

      if (!this.level().isClientSide() && this.duplicationCooldown == 0L && !this.canDuplicate()) {
         this.entityData.set(DATA_CAN_DUPLICATE, true);
      }

   }

   private boolean isDuplicationItem(ItemStack var1) {
      return DUPLICATION_ITEM.test(var1);
   }

   private void duplicateAllay() {
      Allay var1 = (Allay)EntityType.ALLAY.create(this.level());
      if (var1 != null) {
         var1.moveTo(this.position());
         var1.setPersistenceRequired();
         var1.resetDuplicationCooldown();
         this.resetDuplicationCooldown();
         this.level().addFreshEntity(var1);
      }

   }

   private void resetDuplicationCooldown() {
      this.duplicationCooldown = 6000L;
      this.entityData.set(DATA_CAN_DUPLICATE, false);
   }

   private boolean canDuplicate() {
      return (Boolean)this.entityData.get(DATA_CAN_DUPLICATE);
   }

   private void removeInteractionItem(Player var1, ItemStack var2) {
      if (!var1.getAbilities().instabuild) {
         var2.shrink(1);
      }

   }

   public Vec3 getLeashOffset() {
      return new Vec3(0.0D, (double)this.getEyeHeight() * 0.6D, (double)this.getBbWidth() * 0.1D);
   }

   public double getMyRidingOffset() {
      return 0.4D;
   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 18) {
         for(int var2 = 0; var2 < 3; ++var2) {
            this.spawnHeartParticle();
         }
      } else {
         super.handleEntityEvent(var1);
      }

   }

   private void spawnHeartParticle() {
      double var1 = this.random.nextGaussian() * 0.02D;
      double var3 = this.random.nextGaussian() * 0.02D;
      double var5 = this.random.nextGaussian() * 0.02D;
      this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), var1, var3, var5);
   }

   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   static {
      DUPLICATION_ITEM = Ingredient.of(Items.AMETHYST_SHARD);
      DATA_DANCING = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
      DATA_CAN_DUPLICATE = SynchedEntityData.defineId(Allay.class, EntityDataSerializers.BOOLEAN);
      SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.HURT_BY, SensorType.NEAREST_ITEMS);
      MEMORY_TYPES = ImmutableList.of(MemoryModuleType.PATH, MemoryModuleType.LOOK_TARGET, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.HURT_BY, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryModuleType.LIKED_PLAYER, MemoryModuleType.LIKED_NOTEBLOCK_POSITION, MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, MemoryModuleType.IS_PANICKING, new MemoryModuleType[0]);
      THROW_SOUND_PITCHES = ImmutableList.of(0.5625F, 0.625F, 0.75F, 0.9375F, 1.0F, 1.0F, 1.125F, 1.25F, 1.5F, 1.875F, 2.0F, 2.25F, new Float[]{2.5F, 3.0F, 3.75F, 4.0F});
   }

   private class VibrationUser implements VibrationSystem.User {
      private static final int VIBRATION_EVENT_LISTENER_RANGE = 16;
      private final PositionSource positionSource = new EntityPositionSource(Allay.this, Allay.this.getEyeHeight());

      VibrationUser() {
      }

      public int getListenerRadius() {
         return 16;
      }

      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      public boolean canReceiveVibration(ServerLevel var1, BlockPos var2, GameEvent var3, GameEvent.Context var4) {
         if (Allay.this.isNoAi()) {
            return false;
         } else {
            Optional var5 = Allay.this.getBrain().getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
            if (var5.isEmpty()) {
               return true;
            } else {
               GlobalPos var6 = (GlobalPos)var5.get();
               return var6.dimension().equals(var1.dimension()) && var6.pos().equals(var2);
            }
         }
      }

      public void onReceiveVibration(ServerLevel var1, BlockPos var2, GameEvent var3, @Nullable Entity var4, @Nullable Entity var5, float var6) {
         if (var3 == GameEvent.NOTE_BLOCK_PLAY) {
            AllayAi.hearNoteblock(Allay.this, new BlockPos(var2));
         }

      }

      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.ALLAY_CAN_LISTEN;
      }
   }

   class JukeboxListener implements GameEventListener {
      private final PositionSource listenerSource;
      private final int listenerRadius;

      public JukeboxListener(PositionSource var2, int var3) {
         this.listenerSource = var2;
         this.listenerRadius = var3;
      }

      public PositionSource getListenerSource() {
         return this.listenerSource;
      }

      public int getListenerRadius() {
         return this.listenerRadius;
      }

      public boolean handleGameEvent(ServerLevel var1, GameEvent var2, GameEvent.Context var3, Vec3 var4) {
         if (var2 == GameEvent.JUKEBOX_PLAY) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(var4), true);
            return true;
         } else if (var2 == GameEvent.JUKEBOX_STOP_PLAY) {
            Allay.this.setJukeboxPlaying(BlockPos.containing(var4), false);
            return true;
         } else {
            return false;
         }
      }
   }
}
