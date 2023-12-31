package net.minecraft.world.entity.animal;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public abstract class Animal extends AgeableMob {
   protected static final int PARENT_AGE_AFTER_BREEDING = 6000;
   private int inLove;
   @Nullable
   private UUID loveCause;

   protected Animal(EntityType<? extends Animal> var1, Level var2) {
      super(var1, var2);
      this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 16.0F);
      this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, -1.0F);
   }

   protected void customServerAiStep() {
      if (this.getAge() != 0) {
         this.inLove = 0;
      }

      super.customServerAiStep();
   }

   public void aiStep() {
      super.aiStep();
      if (this.getAge() != 0) {
         this.inLove = 0;
      }

      if (this.inLove > 0) {
         --this.inLove;
         if (this.inLove % 10 == 0) {
            double var1 = this.random.nextGaussian() * 0.02D;
            double var3 = this.random.nextGaussian() * 0.02D;
            double var5 = this.random.nextGaussian() * 0.02D;
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), var1, var3, var5);
         }
      }

   }

   public boolean hurt(DamageSource var1, float var2) {
      if (this.isInvulnerableTo(var1)) {
         return false;
      } else {
         this.inLove = 0;
         return super.hurt(var1, var2);
      }
   }

   public float getWalkTargetValue(BlockPos var1, LevelReader var2) {
      return var2.getBlockState(var1.below()).is(Blocks.GRASS_BLOCK) ? 10.0F : var2.getPathfindingCostFromLightLevels(var1);
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("InLove", this.inLove);
      if (this.loveCause != null) {
         var1.putUUID("LoveCause", this.loveCause);
      }

   }

   public double getMyRidingOffset() {
      return 0.14D;
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.inLove = var1.getInt("InLove");
      this.loveCause = var1.hasUUID("LoveCause") ? var1.getUUID("LoveCause") : null;
   }

   public static boolean checkAnimalSpawnRules(EntityType<? extends Animal> var0, LevelAccessor var1, MobSpawnType var2, BlockPos var3, RandomSource var4) {
      return var1.getBlockState(var3.below()).is(BlockTags.ANIMALS_SPAWNABLE_ON) && isBrightEnoughToSpawn(var1, var3);
   }

   protected static boolean isBrightEnoughToSpawn(BlockAndTintGetter var0, BlockPos var1) {
      return var0.getRawBrightness(var1, 0) > 8;
   }

   public int getAmbientSoundInterval() {
      return 120;
   }

   public boolean removeWhenFarAway(double var1) {
      return false;
   }

   public int getExperienceReward() {
      return 1 + this.level().random.nextInt(3);
   }

   public boolean isFood(ItemStack var1) {
      return var1.is(Items.WHEAT);
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      if (this.isFood(var3)) {
         int var4 = this.getAge();
         if (!this.level().isClientSide && var4 == 0 && this.canFallInLove()) {
            this.usePlayerItem(var1, var2, var3);
            this.setInLove(var1);
            return InteractionResult.SUCCESS;
         }

         if (this.isBaby()) {
            this.usePlayerItem(var1, var2, var3);
            this.ageUp(getSpeedUpSecondsWhenFeeding(-var4), true);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
         }

         if (this.level().isClientSide) {
            return InteractionResult.CONSUME;
         }
      }

      return super.mobInteract(var1, var2);
   }

   protected void usePlayerItem(Player var1, InteractionHand var2, ItemStack var3) {
      if (!var1.getAbilities().instabuild) {
         var3.shrink(1);
      }

   }

   public boolean canFallInLove() {
      return this.inLove <= 0;
   }

   public void setInLove(@Nullable Player var1) {
      this.inLove = 600;
      if (var1 != null) {
         this.loveCause = var1.getUUID();
      }

      this.level().broadcastEntityEvent(this, (byte)18);
   }

   public void setInLoveTime(int var1) {
      this.inLove = var1;
   }

   public int getInLoveTime() {
      return this.inLove;
   }

   @Nullable
   public ServerPlayer getLoveCause() {
      if (this.loveCause == null) {
         return null;
      } else {
         Player var1 = this.level().getPlayerByUUID(this.loveCause);
         return var1 instanceof ServerPlayer ? (ServerPlayer)var1 : null;
      }
   }

   public boolean isInLove() {
      return this.inLove > 0;
   }

   public void resetLove() {
      this.inLove = 0;
   }

   public boolean canMate(Animal var1) {
      if (var1 == this) {
         return false;
      } else if (var1.getClass() != this.getClass()) {
         return false;
      } else {
         return this.isInLove() && var1.isInLove();
      }
   }

   public void spawnChildFromBreeding(ServerLevel var1, Animal var2) {
      AgeableMob var3 = this.getBreedOffspring(var1, var2);
      if (var3 != null) {
         var3.setBaby(true);
         var3.moveTo(this.getX(), this.getY(), this.getZ(), 0.0F, 0.0F);
         this.finalizeSpawnChildFromBreeding(var1, var2, var3);
         var1.addFreshEntityWithPassengers(var3);
      }
   }

   public void finalizeSpawnChildFromBreeding(ServerLevel var1, Animal var2, @Nullable AgeableMob var3) {
      Optional.ofNullable(this.getLoveCause()).or(() -> {
         return Optional.ofNullable(var2.getLoveCause());
      }).ifPresent((var3x) -> {
         var3x.awardStat(Stats.ANIMALS_BRED);
         CriteriaTriggers.BRED_ANIMALS.trigger(var3x, this, var2, var3);
      });
      this.setAge(6000);
      var2.setAge(6000);
      this.resetLove();
      var2.resetLove();
      var1.broadcastEntityEvent(this, (byte)18);
      if (var1.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         var1.addFreshEntity(new ExperienceOrb(var1, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
      }

   }

   public void handleEntityEvent(byte var1) {
      if (var1 == 18) {
         for(int var2 = 0; var2 < 7; ++var2) {
            double var3 = this.random.nextGaussian() * 0.02D;
            double var5 = this.random.nextGaussian() * 0.02D;
            double var7 = this.random.nextGaussian() * 0.02D;
            this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), var3, var5, var7);
         }
      } else {
         super.handleEntityEvent(var1);
      }

   }
}
