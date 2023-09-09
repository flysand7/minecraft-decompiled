package net.minecraft.world.item;

public interface TooltipFlag {
   TooltipFlag.Default NORMAL = new TooltipFlag.Default(false, false);
   TooltipFlag.Default ADVANCED = new TooltipFlag.Default(true, false);

   boolean isAdvanced();

   boolean isCreative();

   public static record Default(boolean c, boolean d) implements TooltipFlag {
      private final boolean advanced;
      private final boolean creative;

      public Default(boolean var1, boolean var2) {
         this.advanced = var1;
         this.creative = var2;
      }

      public boolean isAdvanced() {
         return this.advanced;
      }

      public boolean isCreative() {
         return this.creative;
      }

      public TooltipFlag.Default asCreative() {
         return new TooltipFlag.Default(this.advanced, true);
      }

      public boolean advanced() {
         return this.advanced;
      }

      public boolean creative() {
         return this.creative;
      }
   }
}
