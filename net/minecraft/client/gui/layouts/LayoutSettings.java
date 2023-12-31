package net.minecraft.client.gui.layouts;

public interface LayoutSettings {
   LayoutSettings padding(int var1);

   LayoutSettings padding(int var1, int var2);

   LayoutSettings padding(int var1, int var2, int var3, int var4);

   LayoutSettings paddingLeft(int var1);

   LayoutSettings paddingTop(int var1);

   LayoutSettings paddingRight(int var1);

   LayoutSettings paddingBottom(int var1);

   LayoutSettings paddingHorizontal(int var1);

   LayoutSettings paddingVertical(int var1);

   LayoutSettings align(float var1, float var2);

   LayoutSettings alignHorizontally(float var1);

   LayoutSettings alignVertically(float var1);

   default LayoutSettings alignHorizontallyLeft() {
      return this.alignHorizontally(0.0F);
   }

   default LayoutSettings alignHorizontallyCenter() {
      return this.alignHorizontally(0.5F);
   }

   default LayoutSettings alignHorizontallyRight() {
      return this.alignHorizontally(1.0F);
   }

   default LayoutSettings alignVerticallyTop() {
      return this.alignVertically(0.0F);
   }

   default LayoutSettings alignVerticallyMiddle() {
      return this.alignVertically(0.5F);
   }

   default LayoutSettings alignVerticallyBottom() {
      return this.alignVertically(1.0F);
   }

   LayoutSettings copy();

   LayoutSettings.LayoutSettingsImpl getExposed();

   static LayoutSettings defaults() {
      return new LayoutSettings.LayoutSettingsImpl();
   }

   public static class LayoutSettingsImpl implements LayoutSettings {
      public int paddingLeft;
      public int paddingTop;
      public int paddingRight;
      public int paddingBottom;
      public float xAlignment;
      public float yAlignment;

      public LayoutSettingsImpl() {
      }

      public LayoutSettingsImpl(LayoutSettings.LayoutSettingsImpl var1) {
         this.paddingLeft = var1.paddingLeft;
         this.paddingTop = var1.paddingTop;
         this.paddingRight = var1.paddingRight;
         this.paddingBottom = var1.paddingBottom;
         this.xAlignment = var1.xAlignment;
         this.yAlignment = var1.yAlignment;
      }

      public LayoutSettings.LayoutSettingsImpl padding(int var1) {
         return this.padding(var1, var1);
      }

      public LayoutSettings.LayoutSettingsImpl padding(int var1, int var2) {
         return this.paddingHorizontal(var1).paddingVertical(var2);
      }

      public LayoutSettings.LayoutSettingsImpl padding(int var1, int var2, int var3, int var4) {
         return this.paddingLeft(var1).paddingRight(var3).paddingTop(var2).paddingBottom(var4);
      }

      public LayoutSettings.LayoutSettingsImpl paddingLeft(int var1) {
         this.paddingLeft = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl paddingTop(int var1) {
         this.paddingTop = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl paddingRight(int var1) {
         this.paddingRight = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl paddingBottom(int var1) {
         this.paddingBottom = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl paddingHorizontal(int var1) {
         return this.paddingLeft(var1).paddingRight(var1);
      }

      public LayoutSettings.LayoutSettingsImpl paddingVertical(int var1) {
         return this.paddingTop(var1).paddingBottom(var1);
      }

      public LayoutSettings.LayoutSettingsImpl align(float var1, float var2) {
         this.xAlignment = var1;
         this.yAlignment = var2;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl alignHorizontally(float var1) {
         this.xAlignment = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl alignVertically(float var1) {
         this.yAlignment = var1;
         return this;
      }

      public LayoutSettings.LayoutSettingsImpl copy() {
         return new LayoutSettings.LayoutSettingsImpl(this);
      }

      public LayoutSettings.LayoutSettingsImpl getExposed() {
         return this;
      }

      // $FF: synthetic method
      public LayoutSettings copy() {
         return this.copy();
      }

      // $FF: synthetic method
      public LayoutSettings alignVertically(float var1) {
         return this.alignVertically(var1);
      }

      // $FF: synthetic method
      public LayoutSettings alignHorizontally(float var1) {
         return this.alignHorizontally(var1);
      }

      // $FF: synthetic method
      public LayoutSettings align(float var1, float var2) {
         return this.align(var1, var2);
      }

      // $FF: synthetic method
      public LayoutSettings paddingVertical(int var1) {
         return this.paddingVertical(var1);
      }

      // $FF: synthetic method
      public LayoutSettings paddingHorizontal(int var1) {
         return this.paddingHorizontal(var1);
      }

      // $FF: synthetic method
      public LayoutSettings paddingBottom(int var1) {
         return this.paddingBottom(var1);
      }

      // $FF: synthetic method
      public LayoutSettings paddingRight(int var1) {
         return this.paddingRight(var1);
      }

      // $FF: synthetic method
      public LayoutSettings paddingTop(int var1) {
         return this.paddingTop(var1);
      }

      // $FF: synthetic method
      public LayoutSettings paddingLeft(int var1) {
         return this.paddingLeft(var1);
      }

      // $FF: synthetic method
      public LayoutSettings padding(int var1, int var2, int var3, int var4) {
         return this.padding(var1, var2, var3, var4);
      }

      // $FF: synthetic method
      public LayoutSettings padding(int var1, int var2) {
         return this.padding(var1, var2);
      }

      // $FF: synthetic method
      public LayoutSettings padding(int var1) {
         return this.padding(var1);
      }
   }
}
