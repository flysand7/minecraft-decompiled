package net.minecraft.world.level.block.grower;

import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class OakTreeGrower extends AbstractTreeGrower {
   public OakTreeGrower() {
   }

   protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource var1, boolean var2) {
      if (var1.nextInt(10) == 0) {
         return var2 ? TreeFeatures.FANCY_OAK_BEES_005 : TreeFeatures.FANCY_OAK;
      } else {
         return var2 ? TreeFeatures.OAK_BEES_005 : TreeFeatures.OAK;
      }
   }
}
