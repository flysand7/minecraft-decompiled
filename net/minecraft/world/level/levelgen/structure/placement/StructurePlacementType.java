package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public interface StructurePlacementType<SP extends StructurePlacement> {
   StructurePlacementType<RandomSpreadStructurePlacement> RANDOM_SPREAD = register("random_spread", RandomSpreadStructurePlacement.CODEC);
   StructurePlacementType<ConcentricRingsStructurePlacement> CONCENTRIC_RINGS = register("concentric_rings", ConcentricRingsStructurePlacement.CODEC);

   Codec<SP> codec();

   private static <SP extends StructurePlacement> StructurePlacementType<SP> register(String var0, Codec<SP> var1) {
      return (StructurePlacementType)Registry.register(BuiltInRegistries.STRUCTURE_PLACEMENT, (String)var0, () -> {
         return var1;
      });
   }
}
