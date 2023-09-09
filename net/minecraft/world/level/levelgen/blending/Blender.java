package net.minecraft.world.level.levelgen.blending;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableMap.Builder;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.data.worldgen.NoiseData;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableObject;

public class Blender {
   private static final Blender EMPTY = new Blender(new Long2ObjectOpenHashMap(), new Long2ObjectOpenHashMap()) {
      public Blender.BlendingOutput blendOffsetAndFactor(int var1, int var2) {
         return new Blender.BlendingOutput(1.0D, 0.0D);
      }

      public double blendDensity(DensityFunction.FunctionContext var1, double var2) {
         return var2;
      }

      public BiomeResolver getBiomeResolver(BiomeResolver var1) {
         return var1;
      }
   };
   private static final NormalNoise SHIFT_NOISE;
   private static final int HEIGHT_BLENDING_RANGE_CELLS;
   private static final int HEIGHT_BLENDING_RANGE_CHUNKS;
   private static final int DENSITY_BLENDING_RANGE_CELLS = 2;
   private static final int DENSITY_BLENDING_RANGE_CHUNKS;
   private static final double OLD_CHUNK_XZ_RADIUS = 8.0D;
   private final Long2ObjectOpenHashMap<BlendingData> heightAndBiomeBlendingData;
   private final Long2ObjectOpenHashMap<BlendingData> densityBlendingData;

   public static Blender empty() {
      return EMPTY;
   }

   public static Blender of(@Nullable WorldGenRegion var0) {
      if (var0 == null) {
         return EMPTY;
      } else {
         ChunkPos var1 = var0.getCenter();
         if (!var0.isOldChunkAround(var1, HEIGHT_BLENDING_RANGE_CHUNKS)) {
            return EMPTY;
         } else {
            Long2ObjectOpenHashMap var2 = new Long2ObjectOpenHashMap();
            Long2ObjectOpenHashMap var3 = new Long2ObjectOpenHashMap();
            int var4 = Mth.square(HEIGHT_BLENDING_RANGE_CHUNKS + 1);

            for(int var5 = -HEIGHT_BLENDING_RANGE_CHUNKS; var5 <= HEIGHT_BLENDING_RANGE_CHUNKS; ++var5) {
               for(int var6 = -HEIGHT_BLENDING_RANGE_CHUNKS; var6 <= HEIGHT_BLENDING_RANGE_CHUNKS; ++var6) {
                  if (var5 * var5 + var6 * var6 <= var4) {
                     int var7 = var1.x + var5;
                     int var8 = var1.z + var6;
                     BlendingData var9 = BlendingData.getOrUpdateBlendingData(var0, var7, var8);
                     if (var9 != null) {
                        var2.put(ChunkPos.asLong(var7, var8), var9);
                        if (var5 >= -DENSITY_BLENDING_RANGE_CHUNKS && var5 <= DENSITY_BLENDING_RANGE_CHUNKS && var6 >= -DENSITY_BLENDING_RANGE_CHUNKS && var6 <= DENSITY_BLENDING_RANGE_CHUNKS) {
                           var3.put(ChunkPos.asLong(var7, var8), var9);
                        }
                     }
                  }
               }
            }

            if (var2.isEmpty() && var3.isEmpty()) {
               return EMPTY;
            } else {
               return new Blender(var2, var3);
            }
         }
      }
   }

   Blender(Long2ObjectOpenHashMap<BlendingData> var1, Long2ObjectOpenHashMap<BlendingData> var2) {
      this.heightAndBiomeBlendingData = var1;
      this.densityBlendingData = var2;
   }

   public Blender.BlendingOutput blendOffsetAndFactor(int var1, int var2) {
      int var3 = QuartPos.fromBlock(var1);
      int var4 = QuartPos.fromBlock(var2);
      double var5 = this.getBlendingDataValue(var3, 0, var4, BlendingData::getHeight);
      if (var5 != Double.MAX_VALUE) {
         return new Blender.BlendingOutput(0.0D, heightToOffset(var5));
      } else {
         MutableDouble var7 = new MutableDouble(0.0D);
         MutableDouble var8 = new MutableDouble(0.0D);
         MutableDouble var9 = new MutableDouble(Double.POSITIVE_INFINITY);
         this.heightAndBiomeBlendingData.forEach((var5x, var6) -> {
            var6.iterateHeights(QuartPos.fromSection(ChunkPos.getX(var5x)), QuartPos.fromSection(ChunkPos.getZ(var5x)), (var5, var6x, var7x) -> {
               double var9x = Mth.length((double)(var3 - var5), (double)(var4 - var6x));
               if (!(var9x > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
                  if (var9x < var9.doubleValue()) {
                     var9.setValue(var9x);
                  }

                  double var11 = 1.0D / (var9x * var9x * var9x * var9x);
                  var8.add(var7x * var11);
                  var7.add(var11);
               }
            });
         });
         if (var9.doubleValue() == Double.POSITIVE_INFINITY) {
            return new Blender.BlendingOutput(1.0D, 0.0D);
         } else {
            double var10 = var8.doubleValue() / var7.doubleValue();
            double var12 = Mth.clamp(var9.doubleValue() / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
            var12 = 3.0D * var12 * var12 - 2.0D * var12 * var12 * var12;
            return new Blender.BlendingOutput(var12, heightToOffset(var10));
         }
      }
   }

   private static double heightToOffset(double var0) {
      double var2 = 1.0D;
      double var4 = var0 + 0.5D;
      double var6 = Mth.positiveModulo(var4, 8.0D);
      return 1.0D * (32.0D * (var4 - 128.0D) - 3.0D * (var4 - 120.0D) * var6 + 3.0D * var6 * var6) / (128.0D * (32.0D - 3.0D * var6));
   }

   public double blendDensity(DensityFunction.FunctionContext var1, double var2) {
      int var4 = QuartPos.fromBlock(var1.blockX());
      int var5 = var1.blockY() / 8;
      int var6 = QuartPos.fromBlock(var1.blockZ());
      double var7 = this.getBlendingDataValue(var4, var5, var6, BlendingData::getDensity);
      if (var7 != Double.MAX_VALUE) {
         return var7;
      } else {
         MutableDouble var9 = new MutableDouble(0.0D);
         MutableDouble var10 = new MutableDouble(0.0D);
         MutableDouble var11 = new MutableDouble(Double.POSITIVE_INFINITY);
         this.densityBlendingData.forEach((var6x, var7x) -> {
            var7x.iterateDensities(QuartPos.fromSection(ChunkPos.getX(var6x)), QuartPos.fromSection(ChunkPos.getZ(var6x)), var5 - 1, var5 + 1, (var6xx, var7, var8, var9x) -> {
               double var11x = Mth.length((double)(var4 - var6xx), (double)((var5 - var7) * 2), (double)(var6 - var8));
               if (!(var11x > 2.0D)) {
                  if (var11x < var11.doubleValue()) {
                     var11.setValue(var11x);
                  }

                  double var13 = 1.0D / (var11x * var11x * var11x * var11x);
                  var10.add(var9x * var13);
                  var9.add(var13);
               }
            });
         });
         if (var11.doubleValue() == Double.POSITIVE_INFINITY) {
            return var2;
         } else {
            double var12 = var10.doubleValue() / var9.doubleValue();
            double var14 = Mth.clamp(var11.doubleValue() / 3.0D, 0.0D, 1.0D);
            return Mth.lerp(var14, var12, var2);
         }
      }
   }

   private double getBlendingDataValue(int var1, int var2, int var3, Blender.CellValueGetter var4) {
      int var5 = QuartPos.toSection(var1);
      int var6 = QuartPos.toSection(var3);
      boolean var7 = (var1 & 3) == 0;
      boolean var8 = (var3 & 3) == 0;
      double var9 = this.getBlendingDataValue(var4, var5, var6, var1, var2, var3);
      if (var9 == Double.MAX_VALUE) {
         if (var7 && var8) {
            var9 = this.getBlendingDataValue(var4, var5 - 1, var6 - 1, var1, var2, var3);
         }

         if (var9 == Double.MAX_VALUE) {
            if (var7) {
               var9 = this.getBlendingDataValue(var4, var5 - 1, var6, var1, var2, var3);
            }

            if (var9 == Double.MAX_VALUE && var8) {
               var9 = this.getBlendingDataValue(var4, var5, var6 - 1, var1, var2, var3);
            }
         }
      }

      return var9;
   }

   private double getBlendingDataValue(Blender.CellValueGetter var1, int var2, int var3, int var4, int var5, int var6) {
      BlendingData var7 = (BlendingData)this.heightAndBiomeBlendingData.get(ChunkPos.asLong(var2, var3));
      return var7 != null ? var1.get(var7, var4 - QuartPos.fromSection(var2), var5, var6 - QuartPos.fromSection(var3)) : Double.MAX_VALUE;
   }

   public BiomeResolver getBiomeResolver(BiomeResolver var1) {
      return (var2, var3, var4, var5) -> {
         Holder var6 = this.blendBiome(var2, var3, var4);
         return var6 == null ? var1.getNoiseBiome(var2, var3, var4, var5) : var6;
      };
   }

   @Nullable
   private Holder<Biome> blendBiome(int var1, int var2, int var3) {
      MutableDouble var4 = new MutableDouble(Double.POSITIVE_INFINITY);
      MutableObject var5 = new MutableObject();
      this.heightAndBiomeBlendingData.forEach((var5x, var6x) -> {
         var6x.iterateBiomes(QuartPos.fromSection(ChunkPos.getX(var5x)), var2, QuartPos.fromSection(ChunkPos.getZ(var5x)), (var4x, var5xx, var6) -> {
            double var7 = Mth.length((double)(var1 - var4x), (double)(var3 - var5xx));
            if (!(var7 > (double)HEIGHT_BLENDING_RANGE_CELLS)) {
               if (var7 < var4.doubleValue()) {
                  var5.setValue(var6);
                  var4.setValue(var7);
               }

            }
         });
      });
      if (var4.doubleValue() == Double.POSITIVE_INFINITY) {
         return null;
      } else {
         double var6 = SHIFT_NOISE.getValue((double)var1, 0.0D, (double)var3) * 12.0D;
         double var8 = Mth.clamp((var4.doubleValue() + var6) / (double)(HEIGHT_BLENDING_RANGE_CELLS + 1), 0.0D, 1.0D);
         return var8 > 0.5D ? null : (Holder)var5.getValue();
      }
   }

   public static void generateBorderTicks(WorldGenRegion var0, ChunkAccess var1) {
      ChunkPos var2 = var1.getPos();
      boolean var3 = var1.isOldNoiseGeneration();
      BlockPos.MutableBlockPos var4 = new BlockPos.MutableBlockPos();
      BlockPos var5 = new BlockPos(var2.getMinBlockX(), 0, var2.getMinBlockZ());
      BlendingData var6 = var1.getBlendingData();
      if (var6 != null) {
         int var7 = var6.getAreaWithOldGeneration().getMinBuildHeight();
         int var8 = var6.getAreaWithOldGeneration().getMaxBuildHeight() - 1;
         if (var3) {
            for(int var9 = 0; var9 < 16; ++var9) {
               for(int var10 = 0; var10 < 16; ++var10) {
                  generateBorderTick(var1, var4.setWithOffset(var5, var9, var7 - 1, var10));
                  generateBorderTick(var1, var4.setWithOffset(var5, var9, var7, var10));
                  generateBorderTick(var1, var4.setWithOffset(var5, var9, var8, var10));
                  generateBorderTick(var1, var4.setWithOffset(var5, var9, var8 + 1, var10));
               }
            }
         }

         Iterator var19 = Direction.Plane.HORIZONTAL.iterator();

         while(true) {
            Direction var20;
            do {
               if (!var19.hasNext()) {
                  return;
               }

               var20 = (Direction)var19.next();
            } while(var0.getChunk(var2.x + var20.getStepX(), var2.z + var20.getStepZ()).isOldNoiseGeneration() == var3);

            int var11 = var20 == Direction.EAST ? 15 : 0;
            int var12 = var20 == Direction.WEST ? 0 : 15;
            int var13 = var20 == Direction.SOUTH ? 15 : 0;
            int var14 = var20 == Direction.NORTH ? 0 : 15;

            for(int var15 = var11; var15 <= var12; ++var15) {
               for(int var16 = var13; var16 <= var14; ++var16) {
                  int var17 = Math.min(var8, var1.getHeight(Heightmap.Types.MOTION_BLOCKING, var15, var16)) + 1;

                  for(int var18 = var7; var18 < var17; ++var18) {
                     generateBorderTick(var1, var4.setWithOffset(var5, var15, var18, var16));
                  }
               }
            }
         }
      }
   }

   private static void generateBorderTick(ChunkAccess var0, BlockPos var1) {
      BlockState var2 = var0.getBlockState(var1);
      if (var2.is(BlockTags.LEAVES)) {
         var0.markPosForPostprocessing(var1);
      }

      FluidState var3 = var0.getFluidState(var1);
      if (!var3.isEmpty()) {
         var0.markPosForPostprocessing(var1);
      }

   }

   public static void addAroundOldChunksCarvingMaskFilter(WorldGenLevel var0, ProtoChunk var1) {
      ChunkPos var2 = var1.getPos();
      Builder var3 = ImmutableMap.builder();
      Direction8[] var4 = Direction8.values();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Direction8 var7 = var4[var6];
         int var8 = var2.x + var7.getStepX();
         int var9 = var2.z + var7.getStepZ();
         BlendingData var10 = var0.getChunk(var8, var9).getBlendingData();
         if (var10 != null) {
            var3.put(var7, var10);
         }
      }

      ImmutableMap var11 = var3.build();
      if (var1.isOldNoiseGeneration() || !var11.isEmpty()) {
         Blender.DistanceGetter var12 = makeOldChunkDistanceGetter(var1.getBlendingData(), var11);
         CarvingMask.Mask var13 = (var1x, var2x, var3x) -> {
            double var4 = (double)var1x + 0.5D + SHIFT_NOISE.getValue((double)var1x, (double)var2x, (double)var3x) * 4.0D;
            double var6 = (double)var2x + 0.5D + SHIFT_NOISE.getValue((double)var2x, (double)var3x, (double)var1x) * 4.0D;
            double var8 = (double)var3x + 0.5D + SHIFT_NOISE.getValue((double)var3x, (double)var1x, (double)var2x) * 4.0D;
            return var12.getDistance(var4, var6, var8) < 4.0D;
         };
         Stream var10000 = Stream.of(GenerationStep.Carving.values());
         Objects.requireNonNull(var1);
         var10000.map(var1::getOrCreateCarvingMask).forEach((var1x) -> {
            var1x.setAdditionalMask(var13);
         });
      }
   }

   public static Blender.DistanceGetter makeOldChunkDistanceGetter(@Nullable BlendingData var0, Map<Direction8, BlendingData> var1) {
      ArrayList var2 = Lists.newArrayList();
      if (var0 != null) {
         var2.add(makeOffsetOldChunkDistanceGetter((Direction8)null, var0));
      }

      var1.forEach((var1x, var2x) -> {
         var2.add(makeOffsetOldChunkDistanceGetter(var1x, var2x));
      });
      return (var1x, var3, var5) -> {
         double var7 = Double.POSITIVE_INFINITY;
         Iterator var9 = var2.iterator();

         while(var9.hasNext()) {
            Blender.DistanceGetter var10 = (Blender.DistanceGetter)var9.next();
            double var11 = var10.getDistance(var1x, var3, var5);
            if (var11 < var7) {
               var7 = var11;
            }
         }

         return var7;
      };
   }

   private static Blender.DistanceGetter makeOffsetOldChunkDistanceGetter(@Nullable Direction8 var0, BlendingData var1) {
      double var2 = 0.0D;
      double var4 = 0.0D;
      Direction var7;
      if (var0 != null) {
         for(Iterator var6 = var0.getDirections().iterator(); var6.hasNext(); var4 += (double)(var7.getStepZ() * 16)) {
            var7 = (Direction)var6.next();
            var2 += (double)(var7.getStepX() * 16);
         }
      }

      double var10 = (double)var1.getAreaWithOldGeneration().getHeight() / 2.0D;
      double var12 = (double)var1.getAreaWithOldGeneration().getMinBuildHeight() + var10;
      return (var8, var10x, var12x) -> {
         return distanceToCube(var8 - 8.0D - var2, var10x - var12, var12x - 8.0D - var4, 8.0D, var10, 8.0D);
      };
   }

   private static double distanceToCube(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = Math.abs(var0) - var6;
      double var14 = Math.abs(var2) - var8;
      double var16 = Math.abs(var4) - var10;
      return Mth.length(Math.max(0.0D, var12), Math.max(0.0D, var14), Math.max(0.0D, var16));
   }

   static {
      SHIFT_NOISE = NormalNoise.create(new XoroshiroRandomSource(42L), NoiseData.DEFAULT_SHIFT);
      HEIGHT_BLENDING_RANGE_CELLS = QuartPos.fromSection(7) - 1;
      HEIGHT_BLENDING_RANGE_CHUNKS = QuartPos.toSection(HEIGHT_BLENDING_RANGE_CELLS + 3);
      DENSITY_BLENDING_RANGE_CHUNKS = QuartPos.toSection(5);
   }

   interface CellValueGetter {
      double get(BlendingData var1, int var2, int var3, int var4);
   }

   public static record BlendingOutput(double a, double b) {
      private final double alpha;
      private final double blendingOffset;

      public BlendingOutput(double var1, double var3) {
         this.alpha = var1;
         this.blendingOffset = var3;
      }

      public double alpha() {
         return this.alpha;
      }

      public double blendingOffset() {
         return this.blendingOffset;
      }
   }

   public interface DistanceGetter {
      double getDistance(double var1, double var3, double var5);
   }
}
