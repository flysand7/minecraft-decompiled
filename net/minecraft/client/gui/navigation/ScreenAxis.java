package net.minecraft.client.gui.navigation;

public enum ScreenAxis {
   HORIZONTAL,
   VERTICAL;

   private ScreenAxis() {
   }

   public ScreenAxis orthogonal() {
      ScreenAxis var10000;
      switch(this) {
      case HORIZONTAL:
         var10000 = VERTICAL;
         break;
      case VERTICAL:
         var10000 = HORIZONTAL;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public ScreenDirection getPositive() {
      ScreenDirection var10000;
      switch(this) {
      case HORIZONTAL:
         var10000 = ScreenDirection.RIGHT;
         break;
      case VERTICAL:
         var10000 = ScreenDirection.DOWN;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public ScreenDirection getNegative() {
      ScreenDirection var10000;
      switch(this) {
      case HORIZONTAL:
         var10000 = ScreenDirection.LEFT;
         break;
      case VERTICAL:
         var10000 = ScreenDirection.UP;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public ScreenDirection getDirection(boolean var1) {
      return var1 ? this.getPositive() : this.getNegative();
   }

   // $FF: synthetic method
   private static ScreenAxis[] $values() {
      return new ScreenAxis[]{HORIZONTAL, VERTICAL};
   }
}
