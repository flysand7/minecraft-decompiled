package net.minecraft.world.level.levelgen;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class SurfaceRules {
   public static final SurfaceRules.ConditionSource ON_FLOOR;
   public static final SurfaceRules.ConditionSource UNDER_FLOOR;
   public static final SurfaceRules.ConditionSource DEEP_UNDER_FLOOR;
   public static final SurfaceRules.ConditionSource VERY_DEEP_UNDER_FLOOR;
   public static final SurfaceRules.ConditionSource ON_CEILING;
   public static final SurfaceRules.ConditionSource UNDER_CEILING;

   public SurfaceRules() {
   }

   public static SurfaceRules.ConditionSource stoneDepthCheck(int var0, boolean var1, CaveSurface var2) {
      return new SurfaceRules.StoneDepthCheck(var0, var1, 0, var2);
   }

   public static SurfaceRules.ConditionSource stoneDepthCheck(int var0, boolean var1, int var2, CaveSurface var3) {
      return new SurfaceRules.StoneDepthCheck(var0, var1, var2, var3);
   }

   public static SurfaceRules.ConditionSource not(SurfaceRules.ConditionSource var0) {
      return new SurfaceRules.NotConditionSource(var0);
   }

   public static SurfaceRules.ConditionSource yBlockCheck(VerticalAnchor var0, int var1) {
      return new SurfaceRules.YConditionSource(var0, var1, false);
   }

   public static SurfaceRules.ConditionSource yStartCheck(VerticalAnchor var0, int var1) {
      return new SurfaceRules.YConditionSource(var0, var1, true);
   }

   public static SurfaceRules.ConditionSource waterBlockCheck(int var0, int var1) {
      return new SurfaceRules.WaterConditionSource(var0, var1, false);
   }

   public static SurfaceRules.ConditionSource waterStartCheck(int var0, int var1) {
      return new SurfaceRules.WaterConditionSource(var0, var1, true);
   }

   @SafeVarargs
   public static SurfaceRules.ConditionSource isBiome(ResourceKey<Biome>... var0) {
      return isBiome(List.of(var0));
   }

   private static SurfaceRules.BiomeConditionSource isBiome(List<ResourceKey<Biome>> var0) {
      return new SurfaceRules.BiomeConditionSource(var0);
   }

   public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> var0, double var1) {
      return noiseCondition(var0, var1, Double.MAX_VALUE);
   }

   public static SurfaceRules.ConditionSource noiseCondition(ResourceKey<NormalNoise.NoiseParameters> var0, double var1, double var3) {
      return new SurfaceRules.NoiseThresholdConditionSource(var0, var1, var3);
   }

   public static SurfaceRules.ConditionSource verticalGradient(String var0, VerticalAnchor var1, VerticalAnchor var2) {
      return new SurfaceRules.VerticalGradientConditionSource(new ResourceLocation(var0), var1, var2);
   }

   public static SurfaceRules.ConditionSource steep() {
      return SurfaceRules.Steep.INSTANCE;
   }

   public static SurfaceRules.ConditionSource hole() {
      return SurfaceRules.Hole.INSTANCE;
   }

   public static SurfaceRules.ConditionSource abovePreliminarySurface() {
      return SurfaceRules.AbovePreliminarySurface.INSTANCE;
   }

   public static SurfaceRules.ConditionSource temperature() {
      return SurfaceRules.Temperature.INSTANCE;
   }

   public static SurfaceRules.RuleSource ifTrue(SurfaceRules.ConditionSource var0, SurfaceRules.RuleSource var1) {
      return new SurfaceRules.TestRuleSource(var0, var1);
   }

   public static SurfaceRules.RuleSource sequence(SurfaceRules.RuleSource... var0) {
      if (var0.length == 0) {
         throw new IllegalArgumentException("Need at least 1 rule for a sequence");
      } else {
         return new SurfaceRules.SequenceRuleSource(Arrays.asList(var0));
      }
   }

   public static SurfaceRules.RuleSource state(BlockState var0) {
      return new SurfaceRules.BlockRuleSource(var0);
   }

   public static SurfaceRules.RuleSource bandlands() {
      return SurfaceRules.Bandlands.INSTANCE;
   }

   static <A> Codec<? extends A> register(Registry<Codec<? extends A>> var0, String var1, KeyDispatchDataCodec<? extends A> var2) {
      return (Codec)Registry.register(var0, (String)var1, var2.codec());
   }

   static {
      ON_FLOOR = stoneDepthCheck(0, false, CaveSurface.FLOOR);
      UNDER_FLOOR = stoneDepthCheck(0, true, CaveSurface.FLOOR);
      DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 6, CaveSurface.FLOOR);
      VERY_DEEP_UNDER_FLOOR = stoneDepthCheck(0, true, 30, CaveSurface.FLOOR);
      ON_CEILING = stoneDepthCheck(0, false, CaveSurface.CEILING);
      UNDER_CEILING = stoneDepthCheck(0, true, CaveSurface.CEILING);
   }

   private static record StoneDepthCheck(int a, boolean c, int d, CaveSurface e) implements SurfaceRules.ConditionSource {
      final int offset;
      final boolean addSurfaceDepth;
      final int secondaryDepthRange;
      private final CaveSurface surfaceType;
      static final KeyDispatchDataCodec<SurfaceRules.StoneDepthCheck> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.INT.fieldOf("offset").forGetter(SurfaceRules.StoneDepthCheck::offset), Codec.BOOL.fieldOf("add_surface_depth").forGetter(SurfaceRules.StoneDepthCheck::addSurfaceDepth), Codec.INT.fieldOf("secondary_depth_range").forGetter(SurfaceRules.StoneDepthCheck::secondaryDepthRange), CaveSurface.CODEC.fieldOf("surface_type").forGetter(SurfaceRules.StoneDepthCheck::surfaceType)).apply(var0, SurfaceRules.StoneDepthCheck::new);
      }));

      StoneDepthCheck(int var1, boolean var2, int var3, CaveSurface var4) {
         this.offset = var1;
         this.addSurfaceDepth = var2;
         this.secondaryDepthRange = var3;
         this.surfaceType = var4;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         final boolean var2 = this.surfaceType == CaveSurface.CEILING;

         class StoneDepthCondition extends SurfaceRules.LazyYCondition {
            StoneDepthCondition() {
               super(var1);
            }

            protected boolean compute() {
               int var1x = var2 ? this.context.stoneDepthBelow : this.context.stoneDepthAbove;
               int var2x = StoneDepthCheck.this.addSurfaceDepth ? this.context.surfaceDepth : 0;
               int var3 = StoneDepthCheck.this.secondaryDepthRange == 0 ? 0 : (int)Mth.map(this.context.getSurfaceSecondary(), -1.0D, 1.0D, 0.0D, (double)StoneDepthCheck.this.secondaryDepthRange);
               return var1x <= 1 + StoneDepthCheck.this.offset + var2x + var3;
            }
         }

         return new StoneDepthCondition();
      }

      public int offset() {
         return this.offset;
      }

      public boolean addSurfaceDepth() {
         return this.addSurfaceDepth;
      }

      public int secondaryDepthRange() {
         return this.secondaryDepthRange;
      }

      public CaveSurface surfaceType() {
         return this.surfaceType;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   static record NotConditionSource(SurfaceRules.ConditionSource a) implements SurfaceRules.ConditionSource {
      private final SurfaceRules.ConditionSource target;
      static final KeyDispatchDataCodec<SurfaceRules.NotConditionSource> CODEC;

      NotConditionSource(SurfaceRules.ConditionSource var1) {
         this.target = var1;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context var1) {
         return new SurfaceRules.NotCondition((SurfaceRules.Condition)this.target.apply(var1));
      }

      public SurfaceRules.ConditionSource target() {
         return this.target;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      static {
         CODEC = KeyDispatchDataCodec.of(SurfaceRules.ConditionSource.CODEC.xmap(SurfaceRules.NotConditionSource::new, SurfaceRules.NotConditionSource::target).fieldOf("invert"));
      }
   }

   public interface ConditionSource extends Function<SurfaceRules.Context, SurfaceRules.Condition> {
      Codec<SurfaceRules.ConditionSource> CODEC = BuiltInRegistries.MATERIAL_CONDITION.byNameCodec().dispatch((var0) -> {
         return var0.codec().codec();
      }, Function.identity());

      static Codec<? extends SurfaceRules.ConditionSource> bootstrap(Registry<Codec<? extends SurfaceRules.ConditionSource>> var0) {
         SurfaceRules.register(var0, "biome", SurfaceRules.BiomeConditionSource.CODEC);
         SurfaceRules.register(var0, "noise_threshold", SurfaceRules.NoiseThresholdConditionSource.CODEC);
         SurfaceRules.register(var0, "vertical_gradient", SurfaceRules.VerticalGradientConditionSource.CODEC);
         SurfaceRules.register(var0, "y_above", SurfaceRules.YConditionSource.CODEC);
         SurfaceRules.register(var0, "water", SurfaceRules.WaterConditionSource.CODEC);
         SurfaceRules.register(var0, "temperature", SurfaceRules.Temperature.CODEC);
         SurfaceRules.register(var0, "steep", SurfaceRules.Steep.CODEC);
         SurfaceRules.register(var0, "not", SurfaceRules.NotConditionSource.CODEC);
         SurfaceRules.register(var0, "hole", SurfaceRules.Hole.CODEC);
         SurfaceRules.register(var0, "above_preliminary_surface", SurfaceRules.AbovePreliminarySurface.CODEC);
         return SurfaceRules.register(var0, "stone_depth", SurfaceRules.StoneDepthCheck.CODEC);
      }

      KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec();
   }

   private static record YConditionSource(VerticalAnchor a, int c, boolean d) implements SurfaceRules.ConditionSource {
      final VerticalAnchor anchor;
      final int surfaceDepthMultiplier;
      final boolean addStoneDepth;
      static final KeyDispatchDataCodec<SurfaceRules.YConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(VerticalAnchor.CODEC.fieldOf("anchor").forGetter(SurfaceRules.YConditionSource::anchor), Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.YConditionSource::surfaceDepthMultiplier), Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.YConditionSource::addStoneDepth)).apply(var0, SurfaceRules.YConditionSource::new);
      }));

      YConditionSource(VerticalAnchor var1, int var2, boolean var3) {
         this.anchor = var1;
         this.surfaceDepthMultiplier = var2;
         this.addStoneDepth = var3;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         class YCondition extends SurfaceRules.LazyYCondition {
            YCondition() {
               super(var1);
            }

            protected boolean compute() {
               return this.context.blockY + (YConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0) >= YConditionSource.this.anchor.resolveY(this.context.context) + this.context.surfaceDepth * YConditionSource.this.surfaceDepthMultiplier;
            }
         }

         return new YCondition();
      }

      public VerticalAnchor anchor() {
         return this.anchor;
      }

      public int surfaceDepthMultiplier() {
         return this.surfaceDepthMultiplier;
      }

      public boolean addStoneDepth() {
         return this.addStoneDepth;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   private static record WaterConditionSource(int a, int c, boolean d) implements SurfaceRules.ConditionSource {
      final int offset;
      final int surfaceDepthMultiplier;
      final boolean addStoneDepth;
      static final KeyDispatchDataCodec<SurfaceRules.WaterConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(Codec.INT.fieldOf("offset").forGetter(SurfaceRules.WaterConditionSource::offset), Codec.intRange(-20, 20).fieldOf("surface_depth_multiplier").forGetter(SurfaceRules.WaterConditionSource::surfaceDepthMultiplier), Codec.BOOL.fieldOf("add_stone_depth").forGetter(SurfaceRules.WaterConditionSource::addStoneDepth)).apply(var0, SurfaceRules.WaterConditionSource::new);
      }));

      WaterConditionSource(int var1, int var2, boolean var3) {
         this.offset = var1;
         this.surfaceDepthMultiplier = var2;
         this.addStoneDepth = var3;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         class WaterCondition extends SurfaceRules.LazyYCondition {
            WaterCondition() {
               super(var1);
            }

            protected boolean compute() {
               return this.context.waterHeight == Integer.MIN_VALUE || this.context.blockY + (WaterConditionSource.this.addStoneDepth ? this.context.stoneDepthAbove : 0) >= this.context.waterHeight + WaterConditionSource.this.offset + this.context.surfaceDepth * WaterConditionSource.this.surfaceDepthMultiplier;
            }
         }

         return new WaterCondition();
      }

      public int offset() {
         return this.offset;
      }

      public int surfaceDepthMultiplier() {
         return this.surfaceDepthMultiplier;
      }

      public boolean addStoneDepth() {
         return this.addStoneDepth;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   static final class BiomeConditionSource implements SurfaceRules.ConditionSource {
      static final KeyDispatchDataCodec<SurfaceRules.BiomeConditionSource> CODEC;
      private final List<ResourceKey<Biome>> biomes;
      final Predicate<ResourceKey<Biome>> biomeNameTest;

      BiomeConditionSource(List<ResourceKey<Biome>> var1) {
         this.biomes = var1;
         Set var10001 = Set.copyOf(var1);
         Objects.requireNonNull(var10001);
         this.biomeNameTest = var10001::contains;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         class BiomeCondition extends SurfaceRules.LazyYCondition {
            BiomeCondition() {
               super(var1);
            }

            protected boolean compute() {
               return ((Holder)this.context.biome.get()).is(BiomeConditionSource.this.biomeNameTest);
            }
         }

         return new BiomeCondition();
      }

      public boolean equals(Object var1) {
         if (this == var1) {
            return true;
         } else if (var1 instanceof SurfaceRules.BiomeConditionSource) {
            SurfaceRules.BiomeConditionSource var2 = (SurfaceRules.BiomeConditionSource)var1;
            return this.biomes.equals(var2.biomes);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.biomes.hashCode();
      }

      public String toString() {
         return "BiomeConditionSource[biomes=" + this.biomes + "]";
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      static {
         CODEC = KeyDispatchDataCodec.of(ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biome_is").xmap(SurfaceRules::isBiome, (var0) -> {
            return var0.biomes;
         }));
      }
   }

   private static record NoiseThresholdConditionSource(ResourceKey<NormalNoise.NoiseParameters> a, double c, double d) implements SurfaceRules.ConditionSource {
      private final ResourceKey<NormalNoise.NoiseParameters> noise;
      final double minThreshold;
      final double maxThreshold;
      static final KeyDispatchDataCodec<SurfaceRules.NoiseThresholdConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(SurfaceRules.NoiseThresholdConditionSource::noise), Codec.DOUBLE.fieldOf("min_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::minThreshold), Codec.DOUBLE.fieldOf("max_threshold").forGetter(SurfaceRules.NoiseThresholdConditionSource::maxThreshold)).apply(var0, SurfaceRules.NoiseThresholdConditionSource::new);
      }));

      NoiseThresholdConditionSource(ResourceKey<NormalNoise.NoiseParameters> var1, double var2, double var4) {
         this.noise = var1;
         this.minThreshold = var2;
         this.maxThreshold = var4;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         final NormalNoise var2 = var1.randomState.getOrCreateNoise(this.noise);

         class NoiseThresholdCondition extends SurfaceRules.LazyXZCondition {
            NoiseThresholdCondition() {
               super(var1);
            }

            protected boolean compute() {
               double var1x = var2.getValue((double)this.context.blockX, 0.0D, (double)this.context.blockZ);
               return var1x >= NoiseThresholdConditionSource.this.minThreshold && var1x <= NoiseThresholdConditionSource.this.maxThreshold;
            }
         }

         return new NoiseThresholdCondition();
      }

      public ResourceKey<NormalNoise.NoiseParameters> noise() {
         return this.noise;
      }

      public double minThreshold() {
         return this.minThreshold;
      }

      public double maxThreshold() {
         return this.maxThreshold;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   static record VerticalGradientConditionSource(ResourceLocation a, VerticalAnchor c, VerticalAnchor d) implements SurfaceRules.ConditionSource {
      private final ResourceLocation randomName;
      private final VerticalAnchor trueAtAndBelow;
      private final VerticalAnchor falseAtAndAbove;
      static final KeyDispatchDataCodec<SurfaceRules.VerticalGradientConditionSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ResourceLocation.CODEC.fieldOf("random_name").forGetter(SurfaceRules.VerticalGradientConditionSource::randomName), VerticalAnchor.CODEC.fieldOf("true_at_and_below").forGetter(SurfaceRules.VerticalGradientConditionSource::trueAtAndBelow), VerticalAnchor.CODEC.fieldOf("false_at_and_above").forGetter(SurfaceRules.VerticalGradientConditionSource::falseAtAndAbove)).apply(var0, SurfaceRules.VerticalGradientConditionSource::new);
      }));

      VerticalGradientConditionSource(ResourceLocation var1, VerticalAnchor var2, VerticalAnchor var3) {
         this.randomName = var1;
         this.trueAtAndBelow = var2;
         this.falseAtAndAbove = var3;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(final SurfaceRules.Context var1) {
         final int var2 = this.trueAtAndBelow().resolveY(var1.context);
         final int var3 = this.falseAtAndAbove().resolveY(var1.context);
         final PositionalRandomFactory var4 = var1.randomState.getOrCreateRandomFactory(this.randomName());

         class VerticalGradientCondition extends SurfaceRules.LazyYCondition {
            VerticalGradientCondition() {
               super(var1);
            }

            protected boolean compute() {
               int var1x = this.context.blockY;
               if (var1x <= var2) {
                  return true;
               } else if (var1x >= var3) {
                  return false;
               } else {
                  double var2x = Mth.map((double)var1x, (double)var2, (double)var3, 1.0D, 0.0D);
                  RandomSource var4x = var4.at(this.context.blockX, var1x, this.context.blockZ);
                  return (double)var4x.nextFloat() < var2x;
               }
            }
         }

         return new VerticalGradientCondition();
      }

      public ResourceLocation randomName() {
         return this.randomName;
      }

      public VerticalAnchor trueAtAndBelow() {
         return this.trueAtAndBelow;
      }

      public VerticalAnchor falseAtAndAbove() {
         return this.falseAtAndAbove;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   static enum Steep implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Steep> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private Steep() {
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context var1) {
         return var1.steep;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      // $FF: synthetic method
      private static SurfaceRules.Steep[] $values() {
         return new SurfaceRules.Steep[]{INSTANCE};
      }
   }

   static enum Hole implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Hole> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private Hole() {
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context var1) {
         return var1.hole;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      // $FF: synthetic method
      private static SurfaceRules.Hole[] $values() {
         return new SurfaceRules.Hole[]{INSTANCE};
      }
   }

   static enum AbovePreliminarySurface implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.AbovePreliminarySurface> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private AbovePreliminarySurface() {
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context var1) {
         return var1.abovePreliminarySurface;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      // $FF: synthetic method
      private static SurfaceRules.AbovePreliminarySurface[] $values() {
         return new SurfaceRules.AbovePreliminarySurface[]{INSTANCE};
      }
   }

   static enum Temperature implements SurfaceRules.ConditionSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Temperature> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private Temperature() {
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
         return CODEC;
      }

      public SurfaceRules.Condition apply(SurfaceRules.Context var1) {
         return var1.temperature;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      // $FF: synthetic method
      private static SurfaceRules.Temperature[] $values() {
         return new SurfaceRules.Temperature[]{INSTANCE};
      }
   }

   private static record TestRuleSource(SurfaceRules.ConditionSource a, SurfaceRules.RuleSource c) implements SurfaceRules.RuleSource {
      private final SurfaceRules.ConditionSource ifTrue;
      private final SurfaceRules.RuleSource thenRun;
      static final KeyDispatchDataCodec<SurfaceRules.TestRuleSource> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(SurfaceRules.ConditionSource.CODEC.fieldOf("if_true").forGetter(SurfaceRules.TestRuleSource::ifTrue), SurfaceRules.RuleSource.CODEC.fieldOf("then_run").forGetter(SurfaceRules.TestRuleSource::thenRun)).apply(var0, SurfaceRules.TestRuleSource::new);
      }));

      TestRuleSource(SurfaceRules.ConditionSource var1, SurfaceRules.RuleSource var2) {
         this.ifTrue = var1;
         this.thenRun = var2;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context var1) {
         return new SurfaceRules.TestRule((SurfaceRules.Condition)this.ifTrue.apply(var1), (SurfaceRules.SurfaceRule)this.thenRun.apply(var1));
      }

      public SurfaceRules.ConditionSource ifTrue() {
         return this.ifTrue;
      }

      public SurfaceRules.RuleSource thenRun() {
         return this.thenRun;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }
   }

   public interface RuleSource extends Function<SurfaceRules.Context, SurfaceRules.SurfaceRule> {
      Codec<SurfaceRules.RuleSource> CODEC = BuiltInRegistries.MATERIAL_RULE.byNameCodec().dispatch((var0) -> {
         return var0.codec().codec();
      }, Function.identity());

      static Codec<? extends SurfaceRules.RuleSource> bootstrap(Registry<Codec<? extends SurfaceRules.RuleSource>> var0) {
         SurfaceRules.register(var0, "bandlands", SurfaceRules.Bandlands.CODEC);
         SurfaceRules.register(var0, "block", SurfaceRules.BlockRuleSource.CODEC);
         SurfaceRules.register(var0, "sequence", SurfaceRules.SequenceRuleSource.CODEC);
         return SurfaceRules.register(var0, "condition", SurfaceRules.TestRuleSource.CODEC);
      }

      KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec();
   }

   private static record SequenceRuleSource(List<SurfaceRules.RuleSource> a) implements SurfaceRules.RuleSource {
      private final List<SurfaceRules.RuleSource> sequence;
      static final KeyDispatchDataCodec<SurfaceRules.SequenceRuleSource> CODEC;

      SequenceRuleSource(List<SurfaceRules.RuleSource> var1) {
         this.sequence = var1;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context var1) {
         if (this.sequence.size() == 1) {
            return (SurfaceRules.SurfaceRule)((SurfaceRules.RuleSource)this.sequence.get(0)).apply(var1);
         } else {
            Builder var2 = ImmutableList.builder();
            Iterator var3 = this.sequence.iterator();

            while(var3.hasNext()) {
               SurfaceRules.RuleSource var4 = (SurfaceRules.RuleSource)var3.next();
               var2.add((SurfaceRules.SurfaceRule)var4.apply(var1));
            }

            return new SurfaceRules.SequenceRule(var2.build());
         }
      }

      public List<SurfaceRules.RuleSource> sequence() {
         return this.sequence;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      static {
         CODEC = KeyDispatchDataCodec.of(SurfaceRules.RuleSource.CODEC.listOf().xmap(SurfaceRules.SequenceRuleSource::new, SurfaceRules.SequenceRuleSource::sequence).fieldOf("sequence"));
      }
   }

   static record BlockRuleSource(BlockState a, SurfaceRules.StateRule c) implements SurfaceRules.RuleSource {
      private final BlockState resultState;
      private final SurfaceRules.StateRule rule;
      static final KeyDispatchDataCodec<SurfaceRules.BlockRuleSource> CODEC;

      BlockRuleSource(BlockState var1) {
         this(var1, new SurfaceRules.StateRule(var1));
      }

      private BlockRuleSource(BlockState var1, SurfaceRules.StateRule var2) {
         this.resultState = var1;
         this.rule = var2;
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context var1) {
         return this.rule;
      }

      public BlockState resultState() {
         return this.resultState;
      }

      public SurfaceRules.StateRule rule() {
         return this.rule;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      static {
         CODEC = KeyDispatchDataCodec.of(BlockState.CODEC.xmap(SurfaceRules.BlockRuleSource::new, SurfaceRules.BlockRuleSource::resultState).fieldOf("result_state"));
      }
   }

   private static enum Bandlands implements SurfaceRules.RuleSource {
      INSTANCE;

      static final KeyDispatchDataCodec<SurfaceRules.Bandlands> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      private Bandlands() {
      }

      public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
         return CODEC;
      }

      public SurfaceRules.SurfaceRule apply(SurfaceRules.Context var1) {
         SurfaceSystem var10000 = var1.system;
         Objects.requireNonNull(var10000);
         return var10000::getBand;
      }

      // $FF: synthetic method
      public Object apply(Object var1) {
         return this.apply((SurfaceRules.Context)var1);
      }

      // $FF: synthetic method
      private static SurfaceRules.Bandlands[] $values() {
         return new SurfaceRules.Bandlands[]{INSTANCE};
      }
   }

   private static record SequenceRule(List<SurfaceRules.SurfaceRule> a) implements SurfaceRules.SurfaceRule {
      private final List<SurfaceRules.SurfaceRule> rules;

      SequenceRule(List<SurfaceRules.SurfaceRule> var1) {
         this.rules = var1;
      }

      @Nullable
      public BlockState tryApply(int var1, int var2, int var3) {
         Iterator var4 = this.rules.iterator();

         BlockState var6;
         do {
            if (!var4.hasNext()) {
               return null;
            }

            SurfaceRules.SurfaceRule var5 = (SurfaceRules.SurfaceRule)var4.next();
            var6 = var5.tryApply(var1, var2, var3);
         } while(var6 == null);

         return var6;
      }

      public List<SurfaceRules.SurfaceRule> rules() {
         return this.rules;
      }
   }

   private static record TestRule(SurfaceRules.Condition a, SurfaceRules.SurfaceRule b) implements SurfaceRules.SurfaceRule {
      private final SurfaceRules.Condition condition;
      private final SurfaceRules.SurfaceRule followup;

      TestRule(SurfaceRules.Condition var1, SurfaceRules.SurfaceRule var2) {
         this.condition = var1;
         this.followup = var2;
      }

      @Nullable
      public BlockState tryApply(int var1, int var2, int var3) {
         return !this.condition.test() ? null : this.followup.tryApply(var1, var2, var3);
      }

      public SurfaceRules.Condition condition() {
         return this.condition;
      }

      public SurfaceRules.SurfaceRule followup() {
         return this.followup;
      }
   }

   static record StateRule(BlockState a) implements SurfaceRules.SurfaceRule {
      private final BlockState state;

      StateRule(BlockState var1) {
         this.state = var1;
      }

      public BlockState tryApply(int var1, int var2, int var3) {
         return this.state;
      }

      public BlockState state() {
         return this.state;
      }
   }

   protected interface SurfaceRule {
      @Nullable
      BlockState tryApply(int var1, int var2, int var3);
   }

   private static record NotCondition(SurfaceRules.Condition a) implements SurfaceRules.Condition {
      private final SurfaceRules.Condition target;

      NotCondition(SurfaceRules.Condition var1) {
         this.target = var1;
      }

      public boolean test() {
         return !this.target.test();
      }

      public SurfaceRules.Condition target() {
         return this.target;
      }
   }

   private abstract static class LazyYCondition extends SurfaceRules.LazyCondition {
      protected LazyYCondition(SurfaceRules.Context var1) {
         super(var1);
      }

      protected long getContextLastUpdate() {
         return this.context.lastUpdateY;
      }
   }

   private abstract static class LazyXZCondition extends SurfaceRules.LazyCondition {
      protected LazyXZCondition(SurfaceRules.Context var1) {
         super(var1);
      }

      protected long getContextLastUpdate() {
         return this.context.lastUpdateXZ;
      }
   }

   private abstract static class LazyCondition implements SurfaceRules.Condition {
      protected final SurfaceRules.Context context;
      private long lastUpdate;
      @Nullable
      Boolean result;

      protected LazyCondition(SurfaceRules.Context var1) {
         this.context = var1;
         this.lastUpdate = this.getContextLastUpdate() - 1L;
      }

      public boolean test() {
         long var1 = this.getContextLastUpdate();
         if (var1 == this.lastUpdate) {
            if (this.result == null) {
               throw new IllegalStateException("Update triggered but the result is null");
            } else {
               return this.result;
            }
         } else {
            this.lastUpdate = var1;
            this.result = this.compute();
            return this.result;
         }
      }

      protected abstract long getContextLastUpdate();

      protected abstract boolean compute();
   }

   private interface Condition {
      boolean test();
   }

   protected static final class Context {
      private static final int HOW_FAR_BELOW_PRELIMINARY_SURFACE_LEVEL_TO_BUILD_SURFACE = 8;
      private static final int SURFACE_CELL_BITS = 4;
      private static final int SURFACE_CELL_SIZE = 16;
      private static final int SURFACE_CELL_MASK = 15;
      final SurfaceSystem system;
      final SurfaceRules.Condition temperature = new SurfaceRules.Context.TemperatureHelperCondition(this);
      final SurfaceRules.Condition steep = new SurfaceRules.Context.SteepMaterialCondition(this);
      final SurfaceRules.Condition hole = new SurfaceRules.Context.HoleCondition(this);
      final SurfaceRules.Condition abovePreliminarySurface = new SurfaceRules.Context.AbovePreliminarySurfaceCondition();
      final RandomState randomState;
      final ChunkAccess chunk;
      private final NoiseChunk noiseChunk;
      private final Function<BlockPos, Holder<Biome>> biomeGetter;
      final WorldGenerationContext context;
      private long lastPreliminarySurfaceCellOrigin = Long.MAX_VALUE;
      private final int[] preliminarySurfaceCache = new int[4];
      long lastUpdateXZ = -9223372036854775807L;
      int blockX;
      int blockZ;
      int surfaceDepth;
      private long lastSurfaceDepth2Update;
      private double surfaceSecondary;
      private long lastMinSurfaceLevelUpdate;
      private int minSurfaceLevel;
      long lastUpdateY;
      final BlockPos.MutableBlockPos pos;
      Supplier<Holder<Biome>> biome;
      int blockY;
      int waterHeight;
      int stoneDepthBelow;
      int stoneDepthAbove;

      protected Context(SurfaceSystem var1, RandomState var2, ChunkAccess var3, NoiseChunk var4, Function<BlockPos, Holder<Biome>> var5, Registry<Biome> var6, WorldGenerationContext var7) {
         this.lastSurfaceDepth2Update = this.lastUpdateXZ - 1L;
         this.lastMinSurfaceLevelUpdate = this.lastUpdateXZ - 1L;
         this.lastUpdateY = -9223372036854775807L;
         this.pos = new BlockPos.MutableBlockPos();
         this.system = var1;
         this.randomState = var2;
         this.chunk = var3;
         this.noiseChunk = var4;
         this.biomeGetter = var5;
         this.context = var7;
      }

      protected void updateXZ(int var1, int var2) {
         ++this.lastUpdateXZ;
         ++this.lastUpdateY;
         this.blockX = var1;
         this.blockZ = var2;
         this.surfaceDepth = this.system.getSurfaceDepth(var1, var2);
      }

      protected void updateY(int var1, int var2, int var3, int var4, int var5, int var6) {
         ++this.lastUpdateY;
         this.biome = Suppliers.memoize(() -> {
            return (Holder)this.biomeGetter.apply(this.pos.set(var4, var5, var6));
         });
         this.blockY = var5;
         this.waterHeight = var3;
         this.stoneDepthBelow = var2;
         this.stoneDepthAbove = var1;
      }

      protected double getSurfaceSecondary() {
         if (this.lastSurfaceDepth2Update != this.lastUpdateXZ) {
            this.lastSurfaceDepth2Update = this.lastUpdateXZ;
            this.surfaceSecondary = this.system.getSurfaceSecondary(this.blockX, this.blockZ);
         }

         return this.surfaceSecondary;
      }

      private static int blockCoordToSurfaceCell(int var0) {
         return var0 >> 4;
      }

      private static int surfaceCellToBlockCoord(int var0) {
         return var0 << 4;
      }

      protected int getMinSurfaceLevel() {
         if (this.lastMinSurfaceLevelUpdate != this.lastUpdateXZ) {
            this.lastMinSurfaceLevelUpdate = this.lastUpdateXZ;
            int var1 = blockCoordToSurfaceCell(this.blockX);
            int var2 = blockCoordToSurfaceCell(this.blockZ);
            long var3 = ChunkPos.asLong(var1, var2);
            if (this.lastPreliminarySurfaceCellOrigin != var3) {
               this.lastPreliminarySurfaceCellOrigin = var3;
               this.preliminarySurfaceCache[0] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(var1), surfaceCellToBlockCoord(var2));
               this.preliminarySurfaceCache[1] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(var1 + 1), surfaceCellToBlockCoord(var2));
               this.preliminarySurfaceCache[2] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(var1), surfaceCellToBlockCoord(var2 + 1));
               this.preliminarySurfaceCache[3] = this.noiseChunk.preliminarySurfaceLevel(surfaceCellToBlockCoord(var1 + 1), surfaceCellToBlockCoord(var2 + 1));
            }

            int var5 = Mth.floor(Mth.lerp2((double)((float)(this.blockX & 15) / 16.0F), (double)((float)(this.blockZ & 15) / 16.0F), (double)this.preliminarySurfaceCache[0], (double)this.preliminarySurfaceCache[1], (double)this.preliminarySurfaceCache[2], (double)this.preliminarySurfaceCache[3]));
            this.minSurfaceLevel = var5 + this.surfaceDepth - 8;
         }

         return this.minSurfaceLevel;
      }

      private static class TemperatureHelperCondition extends SurfaceRules.LazyYCondition {
         TemperatureHelperCondition(SurfaceRules.Context var1) {
            super(var1);
         }

         protected boolean compute() {
            return ((Biome)((Holder)this.context.biome.get()).value()).coldEnoughToSnow(this.context.pos.set(this.context.blockX, this.context.blockY, this.context.blockZ));
         }
      }

      static class SteepMaterialCondition extends SurfaceRules.LazyXZCondition {
         SteepMaterialCondition(SurfaceRules.Context var1) {
            super(var1);
         }

         protected boolean compute() {
            int var1 = this.context.blockX & 15;
            int var2 = this.context.blockZ & 15;
            int var3 = Math.max(var2 - 1, 0);
            int var4 = Math.min(var2 + 1, 15);
            ChunkAccess var5 = this.context.chunk;
            int var6 = var5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var1, var3);
            int var7 = var5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var1, var4);
            if (var7 >= var6 + 4) {
               return true;
            } else {
               int var8 = Math.max(var1 - 1, 0);
               int var9 = Math.min(var1 + 1, 15);
               int var10 = var5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var8, var2);
               int var11 = var5.getHeight(Heightmap.Types.WORLD_SURFACE_WG, var9, var2);
               return var10 >= var11 + 4;
            }
         }
      }

      private static final class HoleCondition extends SurfaceRules.LazyXZCondition {
         HoleCondition(SurfaceRules.Context var1) {
            super(var1);
         }

         protected boolean compute() {
            return this.context.surfaceDepth <= 0;
         }
      }

      final class AbovePreliminarySurfaceCondition implements SurfaceRules.Condition {
         AbovePreliminarySurfaceCondition() {
         }

         public boolean test() {
            return Context.this.blockY >= Context.this.getMinSurfaceLevel();
         }
      }
   }
}
