package net.minecraft.world.flag;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

public final class FeatureFlagSet {
   private static final FeatureFlagSet EMPTY = new FeatureFlagSet((FeatureFlagUniverse)null, 0L);
   public static final int MAX_CONTAINER_SIZE = 64;
   @Nullable
   private final FeatureFlagUniverse universe;
   private final long mask;

   private FeatureFlagSet(@Nullable FeatureFlagUniverse var1, long var2) {
      this.universe = var1;
      this.mask = var2;
   }

   static FeatureFlagSet create(FeatureFlagUniverse var0, Collection<FeatureFlag> var1) {
      if (var1.isEmpty()) {
         return EMPTY;
      } else {
         long var2 = computeMask(var0, 0L, var1);
         return new FeatureFlagSet(var0, var2);
      }
   }

   public static FeatureFlagSet of() {
      return EMPTY;
   }

   public static FeatureFlagSet of(FeatureFlag var0) {
      return new FeatureFlagSet(var0.universe, var0.mask);
   }

   public static FeatureFlagSet of(FeatureFlag var0, FeatureFlag... var1) {
      long var2 = var1.length == 0 ? var0.mask : computeMask(var0.universe, var0.mask, Arrays.asList(var1));
      return new FeatureFlagSet(var0.universe, var2);
   }

   private static long computeMask(FeatureFlagUniverse var0, long var1, Iterable<FeatureFlag> var3) {
      FeatureFlag var5;
      for(Iterator var4 = var3.iterator(); var4.hasNext(); var1 |= var5.mask) {
         var5 = (FeatureFlag)var4.next();
         if (var0 != var5.universe) {
            throw new IllegalStateException("Mismatched feature universe, expected '" + var0 + "', but got '" + var5.universe + "'");
         }
      }

      return var1;
   }

   public boolean contains(FeatureFlag var1) {
      if (this.universe != var1.universe) {
         return false;
      } else {
         return (this.mask & var1.mask) != 0L;
      }
   }

   public boolean isSubsetOf(FeatureFlagSet var1) {
      if (this.universe == null) {
         return true;
      } else if (this.universe != var1.universe) {
         return false;
      } else {
         return (this.mask & ~var1.mask) == 0L;
      }
   }

   public FeatureFlagSet join(FeatureFlagSet var1) {
      if (this.universe == null) {
         return var1;
      } else if (var1.universe == null) {
         return this;
      } else if (this.universe != var1.universe) {
         throw new IllegalArgumentException("Mismatched set elements: '" + this.universe + "' != '" + var1.universe + "'");
      } else {
         return new FeatureFlagSet(this.universe, this.mask | var1.mask);
      }
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else {
         boolean var10000;
         if (var1 instanceof FeatureFlagSet) {
            FeatureFlagSet var2 = (FeatureFlagSet)var1;
            if (this.universe == var2.universe && this.mask == var2.mask) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   }

   public int hashCode() {
      return (int)HashCommon.mix(this.mask);
   }
}
