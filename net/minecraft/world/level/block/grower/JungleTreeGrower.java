package net.minecraft.world.level.block.grower;

import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class JungleTreeGrower extends AbstractMegaTreeGrower {
   public JungleTreeGrower() {
   }

   protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource var1, boolean var2) {
      return TreeFeatures.JUNGLE_TREE_NO_VINE;
   }

   protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource var1) {
      return TreeFeatures.MEGA_JUNGLE_TREE;
   }
}
