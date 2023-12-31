package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
   private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001D, 1000.0D);
   private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(SCALE_RANGE.fieldOf("xz_scale").forGetter((var0x) -> {
         return var0x.xzScale;
      }), SCALE_RANGE.fieldOf("y_scale").forGetter((var0x) -> {
         return var0x.yScale;
      }), SCALE_RANGE.fieldOf("xz_factor").forGetter((var0x) -> {
         return var0x.xzFactor;
      }), SCALE_RANGE.fieldOf("y_factor").forGetter((var0x) -> {
         return var0x.yFactor;
      }), Codec.doubleRange(1.0D, 8.0D).fieldOf("smear_scale_multiplier").forGetter((var0x) -> {
         return var0x.smearScaleMultiplier;
      })).apply(var0, BlendedNoise::createUnseeded);
   });
   public static final KeyDispatchDataCodec<BlendedNoise> CODEC;
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;
   private final double xzMultiplier;
   private final double yMultiplier;
   private final double xzFactor;
   private final double yFactor;
   private final double smearScaleMultiplier;
   private final double maxValue;
   private final double xzScale;
   private final double yScale;

   public static BlendedNoise createUnseeded(double var0, double var2, double var4, double var6, double var8) {
      return new BlendedNoise(new XoroshiroRandomSource(0L), var0, var2, var4, var6, var8);
   }

   private BlendedNoise(PerlinNoise var1, PerlinNoise var2, PerlinNoise var3, double var4, double var6, double var8, double var10, double var12) {
      this.minLimitNoise = var1;
      this.maxLimitNoise = var2;
      this.mainNoise = var3;
      this.xzScale = var4;
      this.yScale = var6;
      this.xzFactor = var8;
      this.yFactor = var10;
      this.smearScaleMultiplier = var12;
      this.xzMultiplier = 684.412D * this.xzScale;
      this.yMultiplier = 684.412D * this.yScale;
      this.maxValue = var1.maxBrokenValue(this.yMultiplier);
   }

   @VisibleForTesting
   public BlendedNoise(RandomSource var1, double var2, double var4, double var6, double var8, double var10) {
      this(PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-15, 0)), PerlinNoise.createLegacyForBlendedNoise(var1, IntStream.rangeClosed(-7, 0)), var2, var4, var6, var8, var10);
   }

   public BlendedNoise withNewRandom(RandomSource var1) {
      return new BlendedNoise(var1, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
   }

   public double compute(DensityFunction.FunctionContext var1) {
      double var2 = (double)var1.blockX() * this.xzMultiplier;
      double var4 = (double)var1.blockY() * this.yMultiplier;
      double var6 = (double)var1.blockZ() * this.xzMultiplier;
      double var8 = var2 / this.xzFactor;
      double var10 = var4 / this.yFactor;
      double var12 = var6 / this.xzFactor;
      double var14 = this.yMultiplier * this.smearScaleMultiplier;
      double var16 = var14 / this.yFactor;
      double var18 = 0.0D;
      double var20 = 0.0D;
      double var22 = 0.0D;
      boolean var24 = true;
      double var25 = 1.0D;

      for(int var27 = 0; var27 < 8; ++var27) {
         ImprovedNoise var28 = this.mainNoise.getOctaveNoise(var27);
         if (var28 != null) {
            var22 += var28.noise(PerlinNoise.wrap(var8 * var25), PerlinNoise.wrap(var10 * var25), PerlinNoise.wrap(var12 * var25), var16 * var25, var10 * var25) / var25;
         }

         var25 /= 2.0D;
      }

      double var41 = (var22 / 10.0D + 1.0D) / 2.0D;
      boolean var29 = var41 >= 1.0D;
      boolean var30 = var41 <= 0.0D;
      var25 = 1.0D;

      for(int var31 = 0; var31 < 16; ++var31) {
         double var32 = PerlinNoise.wrap(var2 * var25);
         double var34 = PerlinNoise.wrap(var4 * var25);
         double var36 = PerlinNoise.wrap(var6 * var25);
         double var38 = var14 * var25;
         ImprovedNoise var40;
         if (!var29) {
            var40 = this.minLimitNoise.getOctaveNoise(var31);
            if (var40 != null) {
               var18 += var40.noise(var32, var34, var36, var38, var4 * var25) / var25;
            }
         }

         if (!var30) {
            var40 = this.maxLimitNoise.getOctaveNoise(var31);
            if (var40 != null) {
               var20 += var40.noise(var32, var34, var36, var38, var4 * var25) / var25;
            }
         }

         var25 /= 2.0D;
      }

      return Mth.clampedLerp(var18 / 512.0D, var20 / 512.0D, var41) / 128.0D;
   }

   public double minValue() {
      return -this.maxValue();
   }

   public double maxValue() {
      return this.maxValue;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder var1) {
      var1.append("BlendedNoise{minLimitNoise=");
      this.minLimitNoise.parityConfigString(var1);
      var1.append(", maxLimitNoise=");
      this.maxLimitNoise.parityConfigString(var1);
      var1.append(", mainNoise=");
      this.mainNoise.parityConfigString(var1);
      var1.append(String.format(Locale.ROOT, ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8", 684.412D, 684.412D, 8.555150000000001D, 4.277575000000001D)).append('}');
   }

   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return CODEC;
   }

   static {
      CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
   }
}
