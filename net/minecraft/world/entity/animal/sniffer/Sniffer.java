package net.minecraft.world.entity.animal.sniffer;

import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Sniffer extends Animal {
   private static final int DIGGING_PARTICLES_DELAY_TICKS = 1700;
   private static final int DIGGING_PARTICLES_DURATION_TICKS = 6000;
   private static final int DIGGING_PARTICLES_AMOUNT = 30;
   private static final int DIGGING_DROP_SEED_OFFSET_TICKS = 120;
   private static final int SNIFFER_BABY_AGE_TICKS = 48000;
   private static final float DIGGING_BB_HEIGHT_OFFSET = 0.4F;
   private static final EntityDimensions DIGGING_DIMENSIONS;
   private static final EntityDataAccessor<Sniffer.State> DATA_STATE;
   private static final EntityDataAccessor<Integer> DATA_DROP_SEED_AT_TICK;
   public final AnimationState feelingHappyAnimationState = new AnimationState();
   public final AnimationState scentingAnimationState = new AnimationState();
   public final AnimationState sniffingAnimationState = new AnimationState();
   public final AnimationState diggingAnimationState = new AnimationState();
   public final AnimationState risingAnimationState = new AnimationState();

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.10000000149011612D).add(Attributes.MAX_HEALTH, 14.0D);
   }

   public Sniffer(EntityType<? extends Animal> var1, Level var2) {
      super(var1, var2);
      this.entityData.define(DATA_STATE, Sniffer.State.IDLING);
      this.entityData.define(DATA_DROP_SEED_AT_TICK, 0);
      this.getNavigation().setCanFloat(true);
      this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
      this.setPathfindingMalus(BlockPathTypes.DANGER_POWDER_SNOW, -1.0F);
      this.setPathfindingMalus(BlockPathTypes.DAMAGE_CAUTIOUS, -1.0F);
   }

   protected float getStandingEyeHeight(Pose var1, EntityDimensions var2) {
      return this.getDimensions(var1).height * 0.6F;
   }

   public void onPathfindingStart() {
      super.onPathfindingStart();
      if (this.isOnFire() || this.isInWater()) {
         this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
      }

   }

   public void onPathfindingDone() {
      this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
   }

   public EntityDimensions getDimensions(Pose var1) {
      return this.entityData.hasItem(DATA_STATE) && this.getState() == Sniffer.State.DIGGING ? DIGGING_DIMENSIONS.scale(this.getScale()) : super.getDimensions(var1);
   }

   public boolean isPanicking() {
      return this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent();
   }

   public boolean isSearching() {
      return this.getState() == Sniffer.State.SEARCHING;
   }

   public boolean isTempted() {
      return (Boolean)this.brain.getMemory(MemoryModuleType.IS_TEMPTED).orElse(false);
   }

   public boolean canSniff() {
      return !this.isTempted() && !this.isPanicking() && !this.isInWater() && !this.isInLove() && this.onGround() && !this.isPassenger();
   }

   public boolean canPlayDiggingSound() {
      return this.getState() == Sniffer.State.DIGGING || this.getState() == Sniffer.State.SEARCHING;
   }

   private BlockPos getHeadBlock() {
      Vec3 var1 = this.getHeadPosition();
      return BlockPos.containing(var1.x(), this.getY() + 0.20000000298023224D, var1.z());
   }

   private Vec3 getHeadPosition() {
      return this.position().add(this.getForward().scale(2.25D));
   }

   private Sniffer.State getState() {
      return (Sniffer.State)this.entityData.get(DATA_STATE);
   }

   private Sniffer setState(Sniffer.State var1) {
      this.entityData.set(DATA_STATE, var1);
      return this;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> var1) {
      if (DATA_STATE.equals(var1)) {
         Sniffer.State var2 = this.getState();
         this.resetAnimations();
         switch(var2) {
         case SCENTING:
            this.scentingAnimationState.startIfStopped(this.tickCount);
            break;
         case SNIFFING:
            this.sniffingAnimationState.startIfStopped(this.tickCount);
            break;
         case DIGGING:
            this.diggingAnimationState.startIfStopped(this.tickCount);
            break;
         case RISING:
            this.risingAnimationState.startIfStopped(this.tickCount);
            break;
         case FEELING_HAPPY:
            this.feelingHappyAnimationState.startIfStopped(this.tickCount);
         }

         this.refreshDimensions();
      }

      super.onSyncedDataUpdated(var1);
   }

   private void resetAnimations() {
      this.diggingAnimationState.stop();
      this.sniffingAnimationState.stop();
      this.risingAnimationState.stop();
      this.feelingHappyAnimationState.stop();
      this.scentingAnimationState.stop();
   }

   public Sniffer transitionTo(Sniffer.State var1) {
      switch(var1) {
      case SCENTING:
         this.setState(Sniffer.State.SCENTING).onScentingStart();
         break;
      case SNIFFING:
         this.playSound(SoundEvents.SNIFFER_SNIFFING, 1.0F, 1.0F);
         this.setState(Sniffer.State.SNIFFING);
         break;
      case DIGGING:
         this.setState(Sniffer.State.DIGGING).onDiggingStart();
         break;
      case RISING:
         this.playSound(SoundEvents.SNIFFER_DIGGING_STOP, 1.0F, 1.0F);
         this.setState(Sniffer.State.RISING);
         break;
      case FEELING_HAPPY:
         this.playSound(SoundEvents.SNIFFER_HAPPY, 1.0F, 1.0F);
         this.setState(Sniffer.State.FEELING_HAPPY);
         break;
      case IDLING:
         this.setState(Sniffer.State.IDLING);
         break;
      case SEARCHING:
         this.setState(Sniffer.State.SEARCHING);
      }

      return this;
   }

   private Sniffer onScentingStart() {
      this.playSound(SoundEvents.SNIFFER_SCENTING, 1.0F, this.isBaby() ? 1.3F : 1.0F);
      return this;
   }

   private Sniffer onDiggingStart() {
      this.entityData.set(DATA_DROP_SEED_AT_TICK, this.tickCount + 120);
      this.level().broadcastEntityEvent(this, (byte)63);
      return this;
   }

   public Sniffer onDiggingComplete(boolean var1) {
      if (var1) {
         this.storeExploredPosition(this.getOnPos());
      }

      return this;
   }

   Optional<BlockPos> calculateDigPosition() {
      return IntStream.range(0, 5).mapToObj((var1) -> {
         return LandRandomPos.getPos(this, 10 + 2 * var1, 3);
      }).filter(Objects::nonNull).map(BlockPos::containing).filter((var1) -> {
         return this.level().getWorldBorder().isWithinBounds(var1);
      }).map(BlockPos::below).filter(this::canDig).findFirst();
   }

   boolean canDig() {
      return !this.isPanicking() && !this.isTempted() && !this.isBaby() && !this.isInWater() && this.onGround() && !this.isPassenger() && this.canDig(this.getHeadBlock().below());
   }

   private boolean canDig(BlockPos var1) {
      return this.level().getBlockState(var1).is(BlockTags.SNIFFER_DIGGABLE_BLOCK) && this.getExploredPositions().noneMatch((var2) -> {
         return GlobalPos.of(this.level().dimension(), var1).equals(var2);
      }) && (Boolean)Optional.ofNullable(this.getNavigation().createPath((BlockPos)var1, 1)).map(Path::canReach).orElse(false);
   }

   private void dropSeed() {
      if (!this.level().isClientSide() && (Integer)this.entityData.get(DATA_DROP_SEED_AT_TICK) == this.tickCount) {
         ServerLevel var1 = (ServerLevel)this.level();
         LootTable var2 = var1.getServer().getLootData().getLootTable(BuiltInLootTables.SNIFFER_DIGGING);
         LootParams var3 = (new LootParams.Builder(var1)).withParameter(LootContextParams.ORIGIN, this.getHeadPosition()).withParameter(LootContextParams.THIS_ENTITY, this).create(LootContextParamSets.GIFT);
         ObjectArrayList var4 = var2.getRandomItems(var3);
         BlockPos var5 = this.getHeadBlock();
         Iterator var6 = var4.iterator();

         while(var6.hasNext()) {
            ItemStack var7 = (ItemStack)var6.next();
            ItemEntity var8 = new ItemEntity(var1, (double)var5.getX(), (double)var5.getY(), (double)var5.getZ(), var7);
            var8.setDefaultPickUpDelay();
            var1.addFreshEntity(var8);
         }

         this.playSound(SoundEvents.SNIFFER_DROP_SEED, 1.0F, 1.0F);
      }
   }

   private Sniffer emitDiggingParticles(AnimationState var1) {
      boolean var2 = var1.getAccumulatedTime() > 1700L && var1.getAccumulatedTime() < 6000L;
      if (var2) {
         BlockPos var3 = this.getHeadBlock();
         BlockState var4 = this.level().getBlockState(var3.below());
         if (var4.getRenderShape() != RenderShape.INVISIBLE) {
            for(int var5 = 0; var5 < 30; ++var5) {
               Vec3 var6 = Vec3.atCenterOf(var3).add(0.0D, -0.6499999761581421D, 0.0D);
               this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, var4), var6.x, var6.y, var6.z, 0.0D, 0.0D, 0.0D);
            }

            if (this.tickCount % 10 == 0) {
               this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), var4.getSoundType().getHitSound(), this.getSoundSource(), 0.5F, 0.5F, false);
            }
         }
      }

      if (this.tickCount % 10 == 0) {
         this.level().gameEvent(GameEvent.ENTITY_SHAKE, this.getHeadBlock(), GameEvent.Context.of((Entity)this));
      }

      return this;
   }

   private Sniffer storeExploredPosition(BlockPos var1) {
      List var2 = (List)this.getExploredPositions().limit(20L).collect(Collectors.toList());
      var2.add(0, GlobalPos.of(this.level().dimension(), var1));
      this.getBrain().setMemory(MemoryModuleType.SNIFFER_EXPLORED_POSITIONS, (Object)var2);
      return this;
   }

   private Stream<GlobalPos> getExploredPositions() {
      return this.getBrain().getMemory(MemoryModuleType.SNIFFER_EXPLORED_POSITIONS).stream().flatMap(Collection::stream);
   }

   protected void jumpFromGround() {
      super.jumpFromGround();
      double var1 = this.moveControl.getSpeedModifier();
      if (var1 > 0.0D) {
         double var3 = this.getDeltaMovement().horizontalDistanceSqr();
         if (var3 < 0.01D) {
            this.moveRelative(0.1F, new Vec3(0.0D, 0.0D, 1.0D));
         }
      }

   }

   public void spawnChildFromBreeding(ServerLevel var1, Animal var2) {
      ItemStack var3 = new ItemStack(Items.SNIFFER_EGG);
      ItemEntity var4 = new ItemEntity(var1, this.position().x(), this.position().y(), this.position().z(), var3);
      var4.setDefaultPickUpDelay();
      this.finalizeSpawnChildFromBreeding(var1, var2, (AgeableMob)null);
      this.playSound(SoundEvents.SNIFFER_EGG_PLOP, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.5F);
      var1.addFreshEntity(var4);
   }

   public void die(DamageSource var1) {
      this.transitionTo(Sniffer.State.IDLING);
      super.die(var1);
   }

   public void tick() {
      switch(this.getState()) {
      case DIGGING:
         this.emitDiggingParticles(this.diggingAnimationState).dropSeed();
         break;
      case SEARCHING:
         this.playSearchingSound();
      }

      super.tick();
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      boolean var4 = this.isFood(var3);
      InteractionResult var5 = super.mobInteract(var1, var2);
      if (var5.consumesAction() && var4) {
         this.level().playSound((Player)null, (Entity)this, this.getEatingSound(var3), SoundSource.NEUTRAL, 1.0F, Mth.randomBetween(this.level().random, 0.8F, 1.2F));
      }

      return var5;
   }

   public double getPassengersRidingOffset() {
      return 1.8D;
   }

   public float getNameTagOffsetY() {
      return super.getNameTagOffsetY() + 0.3F;
   }

   private void playSearchingSound() {
      if (this.level().isClientSide() && this.tickCount % 20 == 0) {
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.SNIFFER_SEARCHING, this.getSoundSource(), 1.0F, 1.0F, false);
      }

   }

   protected void playStepSound(BlockPos var1, BlockState var2) {
      this.playSound(SoundEvents.SNIFFER_STEP, 0.15F, 1.0F);
   }

   public SoundEvent getEatingSound(ItemStack var1) {
      return SoundEvents.SNIFFER_EAT;
   }

   protected SoundEvent getAmbientSound() {
      return Set.of(Sniffer.State.DIGGING, Sniffer.State.SEARCHING).contains(this.getState()) ? null : SoundEvents.SNIFFER_IDLE;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.SNIFFER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.SNIFFER_DEATH;
   }

   public int getMaxHeadYRot() {
      return 50;
   }

   public void setBaby(boolean var1) {
      this.setAge(var1 ? -48000 : 0);
   }

   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return (AgeableMob)EntityType.SNIFFER.create(var1);
   }

   public boolean canMate(Animal var1) {
      if (!(var1 instanceof Sniffer)) {
         return false;
      } else {
         Sniffer var2 = (Sniffer)var1;
         Set var3 = Set.of(Sniffer.State.IDLING, Sniffer.State.SCENTING, Sniffer.State.FEELING_HAPPY);
         return var3.contains(this.getState()) && var3.contains(var2.getState()) && super.canMate(var1);
      }
   }

   public AABB getBoundingBoxForCulling() {
      return super.getBoundingBoxForCulling().inflate(0.6000000238418579D);
   }

   public boolean isFood(ItemStack var1) {
      return var1.is(ItemTags.SNIFFER_FOOD);
   }

   protected Brain<?> makeBrain(Dynamic<?> var1) {
      return SnifferAi.makeBrain(this.brainProvider().makeBrain(var1));
   }

   public Brain<Sniffer> getBrain() {
      return super.getBrain();
   }

   protected Brain.Provider<Sniffer> brainProvider() {
      return Brain.provider(SnifferAi.MEMORY_TYPES, SnifferAi.SENSOR_TYPES);
   }

   protected void customServerAiStep() {
      this.level().getProfiler().push("snifferBrain");
      this.getBrain().tick((ServerLevel)this.level(), this);
      this.level().getProfiler().popPush("snifferActivityUpdate");
      SnifferAi.updateActivity(this);
      this.level().getProfiler().pop();
      super.customServerAiStep();
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   static {
      DIGGING_DIMENSIONS = EntityDimensions.scalable(EntityType.SNIFFER.getWidth(), EntityType.SNIFFER.getHeight() - 0.4F);
      DATA_STATE = SynchedEntityData.defineId(Sniffer.class, EntityDataSerializers.SNIFFER_STATE);
      DATA_DROP_SEED_AT_TICK = SynchedEntityData.defineId(Sniffer.class, EntityDataSerializers.INT);
   }

   public static enum State {
      IDLING,
      FEELING_HAPPY,
      SCENTING,
      SNIFFING,
      SEARCHING,
      DIGGING,
      RISING;

      private State() {
      }

      // $FF: synthetic method
      private static Sniffer.State[] $values() {
         return new Sniffer.State[]{IDLING, FEELING_HAPPY, SCENTING, SNIFFING, SEARCHING, DIGGING, RISING};
      }
   }
}
