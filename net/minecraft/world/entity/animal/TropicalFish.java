package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class TropicalFish extends AbstractSchoolingFish implements VariantHolder<TropicalFish.Pattern> {
   public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT;
   public static final List<TropicalFish.Variant> COMMON_VARIANTS;
   private boolean isSchool = true;

   public TropicalFish(EntityType<? extends TropicalFish> var1, Level var2) {
      super(var1, var2);
   }

   public static String getPredefinedName(int var0) {
      return "entity.minecraft.tropical_fish.predefined." + var0;
   }

   static int packVariant(TropicalFish.Pattern var0, DyeColor var1, DyeColor var2) {
      return var0.getPackedId() & '\uffff' | (var1.getId() & 255) << 16 | (var2.getId() & 255) << 24;
   }

   public static DyeColor getBaseColor(int var0) {
      return DyeColor.byId(var0 >> 16 & 255);
   }

   public static DyeColor getPatternColor(int var0) {
      return DyeColor.byId(var0 >> 24 & 255);
   }

   public static TropicalFish.Pattern getPattern(int var0) {
      return TropicalFish.Pattern.byId(var0 & '\uffff');
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
   }

   public void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      var1.putInt("Variant", this.getPackedVariant());
   }

   public void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.setPackedVariant(var1.getInt("Variant"));
   }

   private void setPackedVariant(int var1) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, var1);
   }

   public boolean isMaxGroupSizeReached(int var1) {
      return !this.isSchool;
   }

   private int getPackedVariant() {
      return (Integer)this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   public DyeColor getBaseColor() {
      return getBaseColor(this.getPackedVariant());
   }

   public DyeColor getPatternColor() {
      return getPatternColor(this.getPackedVariant());
   }

   public TropicalFish.Pattern getVariant() {
      return getPattern(this.getPackedVariant());
   }

   public void setVariant(TropicalFish.Pattern var1) {
      int var2 = this.getPackedVariant();
      DyeColor var3 = getBaseColor(var2);
      DyeColor var4 = getPatternColor(var2);
      this.setPackedVariant(packVariant(var1, var3, var4));
   }

   public void saveToBucketTag(ItemStack var1) {
      super.saveToBucketTag(var1);
      CompoundTag var2 = var1.getOrCreateTag();
      var2.putInt("BucketVariantTag", this.getPackedVariant());
   }

   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.TROPICAL_FISH_BUCKET);
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.TROPICAL_FISH_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.TROPICAL_FISH_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource var1) {
      return SoundEvents.TROPICAL_FISH_HURT;
   }

   protected SoundEvent getFlopSound() {
      return SoundEvents.TROPICAL_FISH_FLOP;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor var1, DifficultyInstance var2, MobSpawnType var3, @Nullable SpawnGroupData var4, @Nullable CompoundTag var5) {
      Object var14 = super.finalizeSpawn(var1, var2, var3, var4, var5);
      if (var3 == MobSpawnType.BUCKET && var5 != null && var5.contains("BucketVariantTag", 3)) {
         this.setPackedVariant(var5.getInt("BucketVariantTag"));
         return (SpawnGroupData)var14;
      } else {
         RandomSource var7 = var1.getRandom();
         TropicalFish.Variant var6;
         if (var14 instanceof TropicalFish.TropicalFishGroupData) {
            TropicalFish.TropicalFishGroupData var8 = (TropicalFish.TropicalFishGroupData)var14;
            var6 = var8.variant;
         } else if ((double)var7.nextFloat() < 0.9D) {
            var6 = (TropicalFish.Variant)Util.getRandom(COMMON_VARIANTS, var7);
            var14 = new TropicalFish.TropicalFishGroupData(this, var6);
         } else {
            this.isSchool = false;
            TropicalFish.Pattern[] var9 = TropicalFish.Pattern.values();
            DyeColor[] var10 = DyeColor.values();
            TropicalFish.Pattern var11 = (TropicalFish.Pattern)Util.getRandom((Object[])var9, var7);
            DyeColor var12 = (DyeColor)Util.getRandom((Object[])var10, var7);
            DyeColor var13 = (DyeColor)Util.getRandom((Object[])var10, var7);
            var6 = new TropicalFish.Variant(var11, var12, var13);
         }

         this.setPackedVariant(var6.getPackedId());
         return (SpawnGroupData)var14;
      }
   }

   public static boolean checkTropicalFishSpawnRules(EntityType<TropicalFish> var0, LevelAccessor var1, MobSpawnType var2, BlockPos var3, RandomSource var4) {
      return var1.getFluidState(var3.below()).is(FluidTags.WATER) && var1.getBlockState(var3.above()).is(Blocks.WATER) && (var1.getBiome(var3).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT) || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(var0, var1, var2, var3, var4));
   }

   // $FF: synthetic method
   public Object getVariant() {
      return this.getVariant();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void setVariant(Object var1) {
      this.setVariant((TropicalFish.Pattern)var1);
   }

   static {
      DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
      COMMON_VARIANTS = List.of(new TropicalFish.Variant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY), new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY), new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE), new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY), new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY), new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE), new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE), new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW), new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED), new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW), new TropicalFish.Variant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY), new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE), new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK), new TropicalFish.Variant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE), new TropicalFish.Variant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE), new TropicalFish.Variant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED), new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE), new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW), new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE), new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE), new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW), new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW));
   }

   public static enum Pattern implements StringRepresentable {
      KOB("kob", TropicalFish.Base.SMALL, 0),
      SUNSTREAK("sunstreak", TropicalFish.Base.SMALL, 1),
      SNOOPER("snooper", TropicalFish.Base.SMALL, 2),
      DASHER("dasher", TropicalFish.Base.SMALL, 3),
      BRINELY("brinely", TropicalFish.Base.SMALL, 4),
      SPOTTY("spotty", TropicalFish.Base.SMALL, 5),
      FLOPPER("flopper", TropicalFish.Base.LARGE, 0),
      STRIPEY("stripey", TropicalFish.Base.LARGE, 1),
      GLITTER("glitter", TropicalFish.Base.LARGE, 2),
      BLOCKFISH("blockfish", TropicalFish.Base.LARGE, 3),
      BETTY("betty", TropicalFish.Base.LARGE, 4),
      CLAYFISH("clayfish", TropicalFish.Base.LARGE, 5);

      public static final Codec<TropicalFish.Pattern> CODEC = StringRepresentable.fromEnum(TropicalFish.Pattern::values);
      private static final IntFunction<TropicalFish.Pattern> BY_ID = ByIdMap.sparse(TropicalFish.Pattern::getPackedId, values(), KOB);
      private final String name;
      private final Component displayName;
      private final TropicalFish.Base base;
      private final int packedId;

      private Pattern(String var3, TropicalFish.Base var4, int var5) {
         this.name = var3;
         this.base = var4;
         this.packedId = var4.id | var5 << 8;
         this.displayName = Component.translatable("entity.minecraft.tropical_fish.type." + this.name);
      }

      public static TropicalFish.Pattern byId(int var0) {
         return (TropicalFish.Pattern)BY_ID.apply(var0);
      }

      public TropicalFish.Base base() {
         return this.base;
      }

      public int getPackedId() {
         return this.packedId;
      }

      public String getSerializedName() {
         return this.name;
      }

      public Component displayName() {
         return this.displayName;
      }

      // $FF: synthetic method
      private static TropicalFish.Pattern[] $values() {
         return new TropicalFish.Pattern[]{KOB, SUNSTREAK, SNOOPER, DASHER, BRINELY, SPOTTY, FLOPPER, STRIPEY, GLITTER, BLOCKFISH, BETTY, CLAYFISH};
      }
   }

   private static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
      final TropicalFish.Variant variant;

      TropicalFishGroupData(TropicalFish var1, TropicalFish.Variant var2) {
         super(var1);
         this.variant = var2;
      }
   }

   public static record Variant(TropicalFish.Pattern a, DyeColor b, DyeColor c) {
      private final TropicalFish.Pattern pattern;
      private final DyeColor baseColor;
      private final DyeColor patternColor;

      public Variant(TropicalFish.Pattern var1, DyeColor var2, DyeColor var3) {
         this.pattern = var1;
         this.baseColor = var2;
         this.patternColor = var3;
      }

      public int getPackedId() {
         return TropicalFish.packVariant(this.pattern, this.baseColor, this.patternColor);
      }

      public TropicalFish.Pattern pattern() {
         return this.pattern;
      }

      public DyeColor baseColor() {
         return this.baseColor;
      }

      public DyeColor patternColor() {
         return this.patternColor;
      }
   }

   public static enum Base {
      SMALL(0),
      LARGE(1);

      final int id;

      private Base(int var3) {
         this.id = var3;
      }

      // $FF: synthetic method
      private static TropicalFish.Base[] $values() {
         return new TropicalFish.Base[]{SMALL, LARGE};
      }
   }
}
