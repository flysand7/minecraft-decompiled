package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;
   private final double maxValue;

   /** @deprecated */
   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource var0, IntStream var1) {
      return new PerlinNoise(var0, makeAmplitudes(new IntRBTreeSet((Collection)var1.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   /** @deprecated */
   @Deprecated
   public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource var0, int var1, DoubleList var2) {
      return new PerlinNoise(var0, Pair.of(var1, var2), false);
   }

   public static PerlinNoise create(RandomSource var0, IntStream var1) {
      return create(var0, (List)var1.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource var0, List<Integer> var1) {
      return new PerlinNoise(var0, makeAmplitudes(new IntRBTreeSet(var1)), true);
   }

   public static PerlinNoise create(RandomSource var0, int var1, double var2, double... var4) {
      DoubleArrayList var5 = new DoubleArrayList(var4);
      var5.add(0, var2);
      return new PerlinNoise(var0, Pair.of(var1, var5), true);
   }

   public static PerlinNoise create(RandomSource var0, int var1, DoubleList var2) {
      return new PerlinNoise(var0, Pair.of(var1, var2), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet var0) {
      if (var0.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int var1 = -var0.firstInt();
         int var2 = var0.lastInt();
         int var3 = var1 + var2 + 1;
         if (var3 < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleArrayList var4 = new DoubleArrayList(new double[var3]);
            IntBidirectionalIterator var5 = var0.iterator();

            while(var5.hasNext()) {
               int var6 = var5.nextInt();
               var4.set(var6 + var1, 1.0D);
            }

            return Pair.of(-var1, var4);
         }
      }
   }

   protected PerlinNoise(RandomSource var1, Pair<Integer, DoubleList> var2, boolean var3) {
      this.firstOctave = (Integer)var2.getFirst();
      this.amplitudes = (DoubleList)var2.getSecond();
      int var4 = this.amplitudes.size();
      int var5 = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[var4];
      int var7;
      if (var3) {
         PositionalRandomFactory var6 = var1.forkPositional();

         for(var7 = 0; var7 < var4; ++var7) {
            if (this.amplitudes.getDouble(var7) != 0.0D) {
               int var8 = this.firstOctave + var7;
               this.noiseLevels[var7] = new ImprovedNoise(var6.fromHashOf("octave_" + var8));
            }
         }
      } else {
         ImprovedNoise var10 = new ImprovedNoise(var1);
         if (var5 >= 0 && var5 < var4) {
            double var11 = this.amplitudes.getDouble(var5);
            if (var11 != 0.0D) {
               this.noiseLevels[var5] = var10;
            }
         }

         for(var7 = var5 - 1; var7 >= 0; --var7) {
            if (var7 < var4) {
               double var12 = this.amplitudes.getDouble(var7);
               if (var12 != 0.0D) {
                  this.noiseLevels[var7] = new ImprovedNoise(var1);
               } else {
                  skipOctave(var1);
               }
            } else {
               skipOctave(var1);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter((var0) -> {
            return var0 != 0.0D;
         }).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if (var5 < var4 - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0D, (double)(-var5));
      this.lowestFreqValueFactor = Math.pow(2.0D, (double)(var4 - 1)) / (Math.pow(2.0D, (double)var4) - 1.0D);
      this.maxValue = this.edgeValue(2.0D);
   }

   protected double maxValue() {
      return this.maxValue;
   }

   private static void skipOctave(RandomSource var0) {
      var0.consumeCount(262);
   }

   public double getValue(double var1, double var3, double var5) {
      return this.getValue(var1, var3, var5, 0.0D, 0.0D, false);
   }

   /** @deprecated */
   @Deprecated
   public double getValue(double var1, double var3, double var5, double var7, double var9, boolean var11) {
      double var12 = 0.0D;
      double var14 = this.lowestFreqInputFactor;
      double var16 = this.lowestFreqValueFactor;

      for(int var18 = 0; var18 < this.noiseLevels.length; ++var18) {
         ImprovedNoise var19 = this.noiseLevels[var18];
         if (var19 != null) {
            double var20 = var19.noise(wrap(var1 * var14), var11 ? -var19.yo : wrap(var3 * var14), wrap(var5 * var14), var7 * var14, var9 * var14);
            var12 += this.amplitudes.getDouble(var18) * var20 * var16;
         }

         var14 *= 2.0D;
         var16 /= 2.0D;
      }

      return var12;
   }

   public double maxBrokenValue(double var1) {
      return this.edgeValue(var1 + 2.0D);
   }

   private double edgeValue(double var1) {
      double var3 = 0.0D;
      double var5 = this.lowestFreqValueFactor;

      for(int var7 = 0; var7 < this.noiseLevels.length; ++var7) {
         ImprovedNoise var8 = this.noiseLevels[var7];
         if (var8 != null) {
            var3 += this.amplitudes.getDouble(var7) * var1 * var5;
         }

         var5 /= 2.0D;
      }

      return var3;
   }

   @Nullable
   public ImprovedNoise getOctaveNoise(int var1) {
      return this.noiseLevels[this.noiseLevels.length - 1 - var1];
   }

   public static double wrap(double var0) {
      return var0 - (double)Mth.lfloor(var0 / 3.3554432E7D + 0.5D) * 3.3554432E7D;
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder var1) {
      var1.append("PerlinNoise{");
      List var2 = this.amplitudes.stream().map((var0) -> {
         return String.format(Locale.ROOT, "%.2f", var0);
      }).toList();
      var1.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(var2).append(", noise levels: [");

      for(int var3 = 0; var3 < this.noiseLevels.length; ++var3) {
         var1.append(var3).append(": ");
         ImprovedNoise var4 = this.noiseLevels[var3];
         if (var4 == null) {
            var1.append("null");
         } else {
            var4.parityConfigString(var1);
         }

         var1.append(", ");
      }

      var1.append("]");
      var1.append("}");
   }
}
