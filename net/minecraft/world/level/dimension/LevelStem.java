package net.minecraft.world.level.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record LevelStem(Holder<DimensionType> e, ChunkGenerator f) {
   private final Holder<DimensionType> type;
   private final ChunkGenerator generator;
   public static final Codec<LevelStem> CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(DimensionType.CODEC.fieldOf("type").forGetter(LevelStem::type), ChunkGenerator.CODEC.fieldOf("generator").forGetter(LevelStem::generator)).apply(var0, var0.stable(LevelStem::new));
   });
   public static final ResourceKey<LevelStem> OVERWORLD;
   public static final ResourceKey<LevelStem> NETHER;
   public static final ResourceKey<LevelStem> END;

   public LevelStem(Holder<DimensionType> var1, ChunkGenerator var2) {
      this.type = var1;
      this.generator = var2;
   }

   public Holder<DimensionType> type() {
      return this.type;
   }

   public ChunkGenerator generator() {
      return this.generator;
   }

   static {
      OVERWORLD = ResourceKey.create(Registries.LEVEL_STEM, new ResourceLocation("overworld"));
      NETHER = ResourceKey.create(Registries.LEVEL_STEM, new ResourceLocation("the_nether"));
      END = ResourceKey.create(Registries.LEVEL_STEM, new ResourceLocation("the_end"));
   }
}
