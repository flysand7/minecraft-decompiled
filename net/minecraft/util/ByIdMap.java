package net.minecraft.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public class ByIdMap {
   public ByIdMap() {
   }

   private static <T> IntFunction<T> createMap(ToIntFunction<T> var0, T[] var1) {
      if (var1.length == 0) {
         throw new IllegalArgumentException("Empty value list");
      } else {
         Int2ObjectOpenHashMap var2 = new Int2ObjectOpenHashMap();
         Object[] var3 = var1;
         int var4 = var1.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            Object var6 = var3[var5];
            int var7 = var0.applyAsInt(var6);
            Object var8 = var2.put(var7, var6);
            if (var8 != null) {
               throw new IllegalArgumentException("Duplicate entry on id " + var7 + ": current=" + var6 + ", previous=" + var8);
            }
         }

         return var2;
      }
   }

   public static <T> IntFunction<T> sparse(ToIntFunction<T> var0, T[] var1, T var2) {
      IntFunction var3 = createMap(var0, var1);
      return (var2x) -> {
         return Objects.requireNonNullElse(var3.apply(var2x), var2);
      };
   }

   private static <T> T[] createSortedArray(ToIntFunction<T> var0, T[] var1) {
      int var2 = var1.length;
      if (var2 == 0) {
         throw new IllegalArgumentException("Empty value list");
      } else {
         Object[] var3 = (Object[])var1.clone();
         Arrays.fill(var3, (Object)null);
         Object[] var4 = var1;
         int var5 = var1.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Object var7 = var4[var6];
            int var8 = var0.applyAsInt(var7);
            if (var8 < 0 || var8 >= var2) {
               throw new IllegalArgumentException("Values are not continous, found index " + var8 + " for value " + var7);
            }

            Object var9 = var3[var8];
            if (var9 != null) {
               throw new IllegalArgumentException("Duplicate entry on id " + var8 + ": current=" + var7 + ", previous=" + var9);
            }

            var3[var8] = var7;
         }

         for(int var10 = 0; var10 < var2; ++var10) {
            if (var3[var10] == null) {
               throw new IllegalArgumentException("Missing value at index: " + var10);
            }
         }

         return var3;
      }
   }

   public static <T> IntFunction<T> continuous(ToIntFunction<T> var0, T[] var1, ByIdMap.OutOfBoundsStrategy var2) {
      Object[] var3 = createSortedArray(var0, var1);
      int var4 = var3.length;
      IntFunction var10000;
      switch(var2) {
      case ZERO:
         Object var5 = var3[0];
         var10000 = (var3x) -> {
            return var3x >= 0 && var3x < var4 ? var3[var3x] : var5;
         };
         break;
      case WRAP:
         var10000 = (var2x) -> {
            return var3[Mth.positiveModulo(var2x, var4)];
         };
         break;
      case CLAMP:
         var10000 = (var2x) -> {
            return var3[Mth.clamp(var2x, 0, var4 - 1)];
         };
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public static enum OutOfBoundsStrategy {
      ZERO,
      WRAP,
      CLAMP;

      private OutOfBoundsStrategy() {
      }

      // $FF: synthetic method
      private static ByIdMap.OutOfBoundsStrategy[] $values() {
         return new ByIdMap.OutOfBoundsStrategy[]{ZERO, WRAP, CLAMP};
      }
   }
}
