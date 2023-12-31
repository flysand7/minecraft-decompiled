package net.minecraft.world.entity.animal;

import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Ocelot extends Animal {
   public static final double CROUCH_SPEED_MOD = 0.6D;
   public static final double WALK_SPEED_MOD = 0.8D;
   public static final double SPRINT_SPEED_MOD = 1.33D;
   private static final Ingredient TEMPT_INGREDIENT;
   private static final EntityDataAccessor<Boolean> DATA_TRUSTING;
   @Nullable
   private Ocelot.OcelotAvoidEntityGoal<Player> ocelotAvoidPlayersGoal;
   @Nullable
   private Ocelot.OcelotTemptGoal temptGoal;

   public Ocelot(EntityType<? extends Ocelot> var1, Level var2) {
      super(var1, var2);
      this.reassessTrustingGoals();
   }

   boolean isTrusting() {
      return (Boolean)this.entityData.get(DATA_TRUSTING);
   }

   private void setTrusting(boolean var1) {
      this.entityData.set(DATA_TRUSTING, var1);
      this.reassessTrustingGoals();
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putBoolean("Trusting", this.isTrusting());
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setTrusting(var1.getBoolean("Trusting"));
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_TRUSTING, false);
   }

   protected void registerGoals() {
      this.temptGoal = new Ocelot.OcelotTemptGoal(this, 0.6D, TEMPT_INGREDIENT, true);
      this.goalSelector.addGoal(1, new FloatGoal(this));
      this.goalSelector.addGoal(3, this.temptGoal);
      this.goalSelector.addGoal(7, new LeapAtTargetGoal(this, 0.3F));
      this.goalSelector.addGoal(8, new OcelotAttackGoal(this));
      this.goalSelector.addGoal(9, new BreedGoal(this, 0.8D));
      this.goalSelector.addGoal(10, new WaterAvoidingRandomStrollGoal(this, 0.8D, 1.0000001E-5F));
      this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10.0F));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Chicken.class, false));
      this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Turtle.class, 10, false, false, Turtle.BABY_ON_LAND_SELECTOR));
   }

   public void customServerAiStep() {
      if (this.getMoveControl().hasWanted()) {
         double var1 = this.getMoveControl().getSpeedModifier();
         if (var1 == 0.6D) {
            this.setPose(Pose.CROUCHING);
            this.setSprinting(false);
         } else if (var1 == 1.33D) {
            this.setPose(Pose.STANDING);
            this.setSprinting(true);
         } else {
            this.setPose(Pose.STANDING);
            this.setSprinting(false);
         }
      } else {
         this.setPose(Pose.STANDING);
         this.setSprinting(false);
      }

   }

   public boolean removeWhenFarAway(double var1) {
      return !this.isTrusting() && this.tickCount > 2400;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.30000001192092896D).add(Attributes.ATTACK_DAMAGE, 3.0D);
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return SoundEvents.OCELOT_AMBIENT;
   }

   public int getAmbientSoundInterval() {
      return 900;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.OCELOT_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.OCELOT_DEATH;
   }

   private float getAttackDamage() {
      return (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
   }

   public boolean doHurtTarget(Entity var1) {
      return var1.hurt(this.damageSources().mobAttack(this), this.getAttackDamage());
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      if ((this.temptGoal == null || this.temptGoal.isRunning()) && !this.isTrusting() && this.isFood(var3) && var1.distanceToSqr(this) < 9.0D) {
         this.usePlayerItem(var1, var2, var3);
         if (!this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
               this.setTrusting(true);
               this.spawnTrustingParticles(true);
               this.level().broadcastEntityEvent(this, (byte)41);
            } else {
               this.spawnTrustingParticles(false);
               this.level().broadcastEntityEvent(this, (byte)40);
            }
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(var1, var2);
      }
   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 41) {
         this.spawnTrustingParticles(true);
      } else if (var1 == 40) {
         this.spawnTrustingParticles(false);
      } else {
         super.handleEntityEvent(var1);
      }

   }

   private void spawnTrustingParticles(boolean var1) {
      SimpleParticleType var2 = ParticleTypes.HEART;
      if (!var1) {
         var2 = ParticleTypes.SMOKE;
      }

      for(int var3 = 0; var3 < 7; ++var3) {
         double var4 = this.random.nextGaussian() * 0.02D;
         double var6 = this.random.nextGaussian() * 0.02D;
         double var8 = this.random.nextGaussian() * 0.02D;
         this.level().addParticle(var2, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), var4, var6, var8);
      }

   }

   protected void reassessTrustingGoals() {
      if (this.ocelotAvoidPlayersGoal == null) {
         this.ocelotAvoidPlayersGoal = new Ocelot.OcelotAvoidEntityGoal(this, Player.class, 16.0F, 0.8D, 1.33D);
      }

      this.goalSelector.removeGoal(this.ocelotAvoidPlayersGoal);
      if (!this.isTrusting()) {
         this.goalSelector.addGoal(4, this.ocelotAvoidPlayersGoal);
      }

   }

   @Nullable
   public Ocelot getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return (Ocelot)EntityType.OCELOT.create(var1);
   }

   public boolean isFood(ItemStack var1) {
      return TEMPT_INGREDIENT.test(var1);
   }

   public static boolean checkOcelotSpawnRules(EntityType<Ocelot> var0, LevelAccessor var1, MobSpawnType var2, BlockPos var3, RandomSource var4) {
      return var4.nextInt(3) != 0;
   }

   public boolean checkSpawnObstruction(LevelReader var1) {
      if (var1.isUnobstructed(this) && !var1.containsAnyLiquid(this.getBoundingBox())) {
         BlockPos var2 = this.blockPosition();
         if (var2.getY() < var1.getSeaLevel()) {
            return false;
         }

         BlockState var3 = var1.getBlockState(var2.below());
         if (var3.is(Blocks.GRASS_BLOCK) || var3.is(BlockTags.LEAVES)) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      if (var4 == null) {
         var4 = new AgeableMob.AgeableMobGroupData(1.0F);
      }

      return super.finalizeSpawn(var1, var2, var3, (SpawnGroupData)var4, var5);
   }

   public Vec3 getLeashOffset() {
      return new Vec3(0.0D, (double)(0.5F * this.getEyeHeight()), (double)(this.getBbWidth() * 0.4F));
   }

   public boolean isSteppingCarefully() {
      return this.isCrouching() || super.isSteppingCarefully();
   }

   // $FF: synthetic method
   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return this.getBreedOffspring(var1, var2);
   }

   static {
      TEMPT_INGREDIENT = Ingredient.of(Items.COD, Items.SALMON);
      DATA_TRUSTING = SynchedEntityData.defineId(Ocelot.class, EntityDataSerializers.BOOLEAN);
   }

   static class OcelotTemptGoal extends TemptGoal {
      private final Ocelot ocelot;

      public OcelotTemptGoal(Ocelot var1, double var2, Ingredient var4, boolean var5) {
         super(var1, var2, var4, var5);
         this.ocelot = var1;
      }

      protected boolean canScare() {
         return super.canScare() && !this.ocelot.isTrusting();
      }
   }

   static class OcelotAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
      private final Ocelot ocelot;

      public OcelotAvoidEntityGoal(Ocelot var1, Class<T> var2, float var3, double var4, double var6) {
         Predicate var10006 = EntitySelector.NO_CREATIVE_OR_SPECTATOR;
         Objects.requireNonNull(var10006);
         super(var1, var2, var3, var4, var6, var10006::test);
         this.ocelot = var1;
      }

      public boolean canUse() {
         return !this.ocelot.isTrusting() && super.canUse();
      }

      public boolean canContinueToUse() {
         return !this.ocelot.isTrusting() && super.canContinueToUse();
      }
   }
}
