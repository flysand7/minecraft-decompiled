package net.minecraft.world.entity.animal;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.tuple.Pair;

public class MushroomCow extends Cow implements Shearable, VariantHolder<MushroomCow.MushroomType> {
   private static final EntityDataAccessor<String> DATA_TYPE;
   private static final int MUTATE_CHANCE = 1024;
   @Nullable
   private MobEffect effect;
   private int effectDuration;
   @Nullable
   private UUID lastLightningBoltUUID;

   public MushroomCow(EntityType<? extends MushroomCow> var1, Level var2) {
      super(var1, var2);
   }

   public float getWalkTargetValue(BlockPos var1, LevelReader var2) {
      return var2.getBlockState(var1.below()).is(Blocks.MYCELIUM) ? 10.0F : var2.getPathfindingCostFromLightLevels(var1);
   }

   public static boolean checkMushroomSpawnRules(EntityType<MushroomCow> var0, LevelAccessor var1, MobSpawnType var2, BlockPos var3, RandomSource var4) {
      return var1.getBlockState(var3.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(var1, var3);
   }

   public void thunderHit(ServerLevel var1, LightningBolt var2) {
      UUID var3 = var2.getUUID();
      if (!var3.equals(this.lastLightningBoltUUID)) {
         this.setVariant(this.getVariant() == MushroomCow.MushroomType.RED ? MushroomCow.MushroomType.BROWN : MushroomCow.MushroomType.RED);
         this.lastLightningBoltUUID = var3;
         this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
      }

   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_TYPE, MushroomCow.MushroomType.RED.type);
   }

   public InteractionResult mobInteract(Player var1, InteractionHand var2) {
      ItemStack var3 = var1.getItemInHand(var2);
      if (var3.is(Items.BOWL) && !this.isBaby()) {
         boolean var10 = false;
         ItemStack var9;
         if (this.effect != null) {
            var10 = true;
            var9 = new ItemStack(Items.SUSPICIOUS_STEW);
            SuspiciousStewItem.saveMobEffect(var9, this.effect, this.effectDuration);
            this.effect = null;
            this.effectDuration = 0;
         } else {
            var9 = new ItemStack(Items.MUSHROOM_STEW);
         }

         ItemStack var11 = ItemUtils.createFilledResult(var3, var1, var9, false);
         var1.setItemInHand(var2, var11);
         SoundEvent var7;
         if (var10) {
            var7 = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
         } else {
            var7 = SoundEvents.MOOSHROOM_MILK;
         }

         this.playSound(var7, 1.0F, 1.0F);
         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (var3.is(Items.SHEARS) && this.readyForShearing()) {
         this.shear(SoundSource.PLAYERS);
         this.gameEvent(GameEvent.SHEAR, var1);
         if (!this.level().isClientSide) {
            var3.hurtAndBreak(1, var1, (var1x) -> {
               var1x.broadcastBreakEvent(var2);
            });
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else if (this.getVariant() == MushroomCow.MushroomType.BROWN && var3.is(ItemTags.SMALL_FLOWERS)) {
         if (this.effect != null) {
            for(int var4 = 0; var4 < 2; ++var4) {
               this.level().addParticle(ParticleTypes.SMOKE, this.getX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.getZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
            }
         } else {
            Optional var8 = this.getEffectFromItemStack(var3);
            if (!var8.isPresent()) {
               return InteractionResult.PASS;
            }

            Pair var5 = (Pair)var8.get();
            if (!var1.getAbilities().instabuild) {
               var3.shrink(1);
            }

            for(int var6 = 0; var6 < 4; ++var6) {
               this.level().addParticle(ParticleTypes.EFFECT, this.getX() + this.random.nextDouble() / 2.0D, this.getY(0.5D), this.getZ() + this.random.nextDouble() / 2.0D, 0.0D, this.random.nextDouble() / 5.0D, 0.0D);
            }

            this.effect = (MobEffect)var5.getLeft();
            this.effectDuration = (Integer)var5.getRight();
            this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide);
      } else {
         return super.mobInteract(var1, var2);
      }
   }

   public void shear(SoundSource var1) {
      this.level().playSound((Player)null, (Entity)this, SoundEvents.MOOSHROOM_SHEAR, var1, 1.0F, 1.0F);
      if (!this.level().isClientSide()) {
         Cow var2 = (Cow)EntityType.COW.create(this.level());
         if (var2 != null) {
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5D), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            this.discard();
            var2.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            var2.setHealth(this.getHealth());
            var2.yBodyRot = this.yBodyRot;
            if (this.hasCustomName()) {
               var2.setCustomName(this.getCustomName());
               var2.setCustomNameVisible(this.isCustomNameVisible());
            }

            if (this.isPersistenceRequired()) {
               var2.setPersistenceRequired();
            }

            var2.setInvulnerable(this.isInvulnerable());
            this.level().addFreshEntity(var2);

            for(int var3 = 0; var3 < 5; ++var3) {
               this.level().addFreshEntity(new ItemEntity(this.level(), this.getX(), this.getY(1.0D), this.getZ(), new ItemStack(this.getVariant().blockState.getBlock())));
            }
         }
      }

   }

   public boolean readyForShearing() {
      return this.isAlive() && !this.isBaby();
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putString("Type", this.getVariant().getSerializedName());
      if (this.effect != null) {
         var1.putInt("EffectId", MobEffect.getId(this.effect));
         var1.putInt("EffectDuration", this.effectDuration);
      }

   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setVariant(MushroomCow.MushroomType.byType(var1.getString("Type")));
      if (var1.contains("EffectId", 99)) {
         this.effect = MobEffect.byId(var1.getInt("EffectId"));
      }

      if (var1.contains("EffectDuration", 99)) {
         this.effectDuration = var1.getInt("EffectDuration");
      }

   }

   private Optional<Pair<MobEffect, Integer>> getEffectFromItemStack(ItemStack var1) {
      SuspiciousEffectHolder var2 = SuspiciousEffectHolder.tryGet(var1.getItem());
      return var2 != null ? Optional.of(Pair.of(var2.getSuspiciousEffect(), var2.getEffectDuration())) : Optional.empty();
   }

   public void setVariant(MushroomCow.MushroomType var1) {
      this.entityData.set(DATA_TYPE, var1.type);
   }

   public MushroomCow.MushroomType getVariant() {
      return MushroomCow.MushroomType.byType((String)this.entityData.get(DATA_TYPE));
   }

   @Nullable
   public MushroomCow getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      MushroomCow var3 = (MushroomCow)EntityType.MOOSHROOM.create(var1);
      if (var3 != null) {
         var3.setVariant(this.getOffspringType((MushroomCow)var2));
      }

      return var3;
   }

   private MushroomCow.MushroomType getOffspringType(MushroomCow var1) {
      MushroomCow.MushroomType var2 = this.getVariant();
      MushroomCow.MushroomType var3 = var1.getVariant();
      MushroomCow.MushroomType var4;
      if (var2 == var3 && this.random.nextInt(1024) == 0) {
         var4 = var2 == MushroomCow.MushroomType.BROWN ? MushroomCow.MushroomType.RED : MushroomCow.MushroomType.BROWN;
      } else {
         var4 = this.random.nextBoolean() ? var2 : var3;
      }

      return var4;
   }

   // $FF: synthetic method
   @Nullable
   public Cow getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return this.getBreedOffspring(var1, var2);
   }

   // $FF: synthetic method
   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel var1, AgeableMob var2) {
      return this.getBreedOffspring(var1, var2);
   }

   // $FF: synthetic method
   public Object getVariant() {
      return this.getVariant();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void setVariant(Object var1) {
      this.setVariant((MushroomCow.MushroomType)var1);
   }

   static {
      DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.STRING);
   }

   public static enum MushroomType implements StringRepresentable {
      RED("red", Blocks.RED_MUSHROOM.defaultBlockState()),
      BROWN("brown", Blocks.BROWN_MUSHROOM.defaultBlockState());

      public static final StringRepresentable.EnumCodec<MushroomCow.MushroomType> CODEC = StringRepresentable.fromEnum(MushroomCow.MushroomType::values);
      final String type;
      final BlockState blockState;

      private MushroomType(String var3, BlockState var4) {
         this.type = var3;
         this.blockState = var4;
      }

      public BlockState getBlockState() {
         return this.blockState;
      }

      public String getSerializedName() {
         return this.type;
      }

      static MushroomCow.MushroomType byType(String var0) {
         return (MushroomCow.MushroomType)CODEC.byName(var0, RED);
      }

      // $FF: synthetic method
      private static MushroomCow.MushroomType[] $values() {
         return new MushroomCow.MushroomType[]{RED, BROWN};
      }
   }
}
