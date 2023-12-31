package net.minecraft.client.gui.navigation;

import it.unimi.dsi.fastutil.ints.IntComparator;

public enum ScreenDirection {
   UP,
   DOWN,
   LEFT,
   RIGHT;

   private final IntComparator coordinateValueComparator = (var1x, var2x) -> {
      return var1x == var2x ? 0 : (this.isBefore(var1x, var2x) ? -1 : 1);
   };

   private ScreenDirection() {
   }

   public ScreenAxis getAxis() {
      ScreenAxis var10000;
      switch(this) {
      case UP:
      case DOWN:
         var10000 = ScreenAxis.VERTICAL;
         break;
      case LEFT:
      case RIGHT:
         var10000 = ScreenAxis.HORIZONTAL;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public ScreenDirection getOpposite() {
      ScreenDirection var10000;
      switch(this) {
      case UP:
         var10000 = DOWN;
         break;
      case DOWN:
         var10000 = UP;
         break;
      case LEFT:
         var10000 = RIGHT;
         break;
      case RIGHT:
         var10000 = LEFT;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public boolean isPositive() {
      boolean var10000;
      switch(this) {
      case UP:
      case LEFT:
         var10000 = false;
         break;
      case DOWN:
      case RIGHT:
         var10000 = true;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public boolean isAfter(int var1, int var2) {
      if (this.isPositive()) {
         return var1 > var2;
      } else {
         return var2 > var1;
      }
   }

   public boolean isBefore(int var1, int var2) {
      if (this.isPositive()) {
         return var1 < var2;
      } else {
         return var2 < var1;
      }
   }

   public IntComparator coordinateValueComparator() {
      return this.coordinateValueComparator;
   }

   // $FF: synthetic method
   private static ScreenDirection[] $values() {
      return new ScreenDirection[]{UP, DOWN, LEFT, RIGHT};
   }
}
