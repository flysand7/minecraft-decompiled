package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;

public abstract class PlacementModifier {
   public static final Codec<PlacementModifier> CODEC;

   public PlacementModifier() {
   }

   public abstract Stream<BlockPos> getPositions(PlacementContext var1, RandomSource var2, BlockPos var3);

   public abstract PlacementModifierType<?> type();

   static {
      CODEC = BuiltInRegistries.PLACEMENT_MODIFIER_TYPE.byNameCodec().dispatch(PlacementModifier::type, PlacementModifierType::codec);
   }
}
