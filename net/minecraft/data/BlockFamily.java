package net.minecraft.data;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

public class BlockFamily {
   private final Block baseBlock;
   final Map<BlockFamily.Variant, Block> variants = Maps.newHashMap();
   FeatureFlagSet requiredFeatures;
   boolean generateModel;
   boolean generateRecipe;
   @Nullable
   String recipeGroupPrefix;
   @Nullable
   String recipeUnlockedBy;

   BlockFamily(Block var1) {
      this.requiredFeatures = FeatureFlags.VANILLA_SET;
      this.generateModel = true;
      this.generateRecipe = true;
      this.baseBlock = var1;
   }

   public Block getBaseBlock() {
      return this.baseBlock;
   }

   public Map<BlockFamily.Variant, Block> getVariants() {
      return this.variants;
   }

   public Block get(BlockFamily.Variant var1) {
      return (Block)this.variants.get(var1);
   }

   public boolean shouldGenerateModel() {
      return this.generateModel;
   }

   public boolean shouldGenerateRecipe(FeatureFlagSet var1) {
      return this.generateRecipe && this.requiredFeatures.isSubsetOf(var1);
   }

   public Optional<String> getRecipeGroupPrefix() {
      return Util.isBlank(this.recipeGroupPrefix) ? Optional.empty() : Optional.of(this.recipeGroupPrefix);
   }

   public Optional<String> getRecipeUnlockedBy() {
      return Util.isBlank(this.recipeUnlockedBy) ? Optional.empty() : Optional.of(this.recipeUnlockedBy);
   }

   public static class Builder {
      private final BlockFamily family;

      public Builder(Block var1) {
         this.family = new BlockFamily(var1);
      }

      public BlockFamily getFamily() {
         return this.family;
      }

      public BlockFamily.Builder button(Block var1) {
         this.family.variants.put(BlockFamily.Variant.BUTTON, var1);
         return this;
      }

      public BlockFamily.Builder chiseled(Block var1) {
         this.family.variants.put(BlockFamily.Variant.CHISELED, var1);
         return this;
      }

      public BlockFamily.Builder mosaic(Block var1) {
         this.family.variants.put(BlockFamily.Variant.MOSAIC, var1);
         return this;
      }

      public BlockFamily.Builder cracked(Block var1) {
         this.family.variants.put(BlockFamily.Variant.CRACKED, var1);
         return this;
      }

      public BlockFamily.Builder cut(Block var1) {
         this.family.variants.put(BlockFamily.Variant.CUT, var1);
         return this;
      }

      public BlockFamily.Builder door(Block var1) {
         this.family.variants.put(BlockFamily.Variant.DOOR, var1);
         return this;
      }

      public BlockFamily.Builder customFence(Block var1) {
         this.family.variants.put(BlockFamily.Variant.CUSTOM_FENCE, var1);
         return this;
      }

      public BlockFamily.Builder fence(Block var1) {
         this.family.variants.put(BlockFamily.Variant.FENCE, var1);
         return this;
      }

      public BlockFamily.Builder customFenceGate(Block var1) {
         this.family.variants.put(BlockFamily.Variant.CUSTOM_FENCE_GATE, var1);
         return this;
      }

      public BlockFamily.Builder fenceGate(Block var1) {
         this.family.variants.put(BlockFamily.Variant.FENCE_GATE, var1);
         return this;
      }

      public BlockFamily.Builder sign(Block var1, Block var2) {
         this.family.variants.put(BlockFamily.Variant.SIGN, var1);
         this.family.variants.put(BlockFamily.Variant.WALL_SIGN, var2);
         return this;
      }

      public BlockFamily.Builder slab(Block var1) {
         this.family.variants.put(BlockFamily.Variant.SLAB, var1);
         return this;
      }

      public BlockFamily.Builder stairs(Block var1) {
         this.family.variants.put(BlockFamily.Variant.STAIRS, var1);
         return this;
      }

      public BlockFamily.Builder pressurePlate(Block var1) {
         this.family.variants.put(BlockFamily.Variant.PRESSURE_PLATE, var1);
         return this;
      }

      public BlockFamily.Builder polished(Block var1) {
         this.family.variants.put(BlockFamily.Variant.POLISHED, var1);
         return this;
      }

      public BlockFamily.Builder trapdoor(Block var1) {
         this.family.variants.put(BlockFamily.Variant.TRAPDOOR, var1);
         return this;
      }

      public BlockFamily.Builder wall(Block var1) {
         this.family.variants.put(BlockFamily.Variant.WALL, var1);
         return this;
      }

      public BlockFamily.Builder dontGenerateModel() {
         this.family.generateModel = false;
         return this;
      }

      public BlockFamily.Builder dontGenerateRecipe() {
         this.family.generateRecipe = false;
         return this;
      }

      public BlockFamily.Builder featureLockedBehind(FeatureFlag... var1) {
         this.family.requiredFeatures = FeatureFlags.REGISTRY.subset(var1);
         return this;
      }

      public BlockFamily.Builder recipeGroupPrefix(String var1) {
         this.family.recipeGroupPrefix = var1;
         return this;
      }

      public BlockFamily.Builder recipeUnlockedBy(String var1) {
         this.family.recipeUnlockedBy = var1;
         return this;
      }
   }

   public static enum Variant {
      BUTTON("button"),
      CHISELED("chiseled"),
      CRACKED("cracked"),
      CUT("cut"),
      DOOR("door"),
      CUSTOM_FENCE("custom_fence"),
      FENCE("fence"),
      CUSTOM_FENCE_GATE("custom_fence_gate"),
      FENCE_GATE("fence_gate"),
      MOSAIC("mosaic"),
      SIGN("sign"),
      SLAB("slab"),
      STAIRS("stairs"),
      PRESSURE_PLATE("pressure_plate"),
      POLISHED("polished"),
      TRAPDOOR("trapdoor"),
      WALL("wall"),
      WALL_SIGN("wall_sign");

      private final String name;

      private Variant(String var3) {
         this.name = var3;
      }

      public String getName() {
         return this.name;
      }

      // $FF: synthetic method
      private static BlockFamily.Variant[] $values() {
         return new BlockFamily.Variant[]{BUTTON, CHISELED, CRACKED, CUT, DOOR, CUSTOM_FENCE, FENCE, CUSTOM_FENCE_GATE, FENCE_GATE, MOSAIC, SIGN, SLAB, STAIRS, PRESSURE_PLATE, POLISHED, TRAPDOOR, WALL, WALL_SIGN};
      }
   }
}
