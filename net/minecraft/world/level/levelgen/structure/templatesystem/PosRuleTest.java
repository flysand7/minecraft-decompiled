package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public abstract class PosRuleTest {
   public static final Codec<PosRuleTest> CODEC;

   public PosRuleTest() {
   }

   public abstract boolean test(BlockPos var1, BlockPos var2, BlockPos var3, RandomSource var4);

   protected abstract PosRuleTestType<?> getType();

   static {
      CODEC = BuiltInRegistries.POS_RULE_TEST.byNameCodec().dispatch("predicate_type", PosRuleTest::getType, PosRuleTestType::codec);
   }
}
