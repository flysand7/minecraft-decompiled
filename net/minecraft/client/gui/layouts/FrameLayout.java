package net.minecraft.client.gui.layouts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;

public class FrameLayout extends AbstractLayout {
   private final List<FrameLayout.ChildContainer> children;
   private int minWidth;
   private int minHeight;
   private final LayoutSettings defaultChildLayoutSettings;

   public FrameLayout() {
      this(0, 0, 0, 0);
   }

   public FrameLayout(int var1, int var2) {
      this(0, 0, var1, var2);
   }

   public FrameLayout(int var1, int var2, int var3, int var4) {
      super(var1, var2, var3, var4);
      this.children = new ArrayList();
      this.defaultChildLayoutSettings = LayoutSettings.defaults().align(0.5F, 0.5F);
      this.setMinDimensions(var3, var4);
   }

   public FrameLayout setMinDimensions(int var1, int var2) {
      return this.setMinWidth(var1).setMinHeight(var2);
   }

   public FrameLayout setMinHeight(int var1) {
      this.minHeight = var1;
      return this;
   }

   public FrameLayout setMinWidth(int var1) {
      this.minWidth = var1;
      return this;
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public void arrangeElements() {
      super.arrangeElements();
      int var1 = this.minWidth;
      int var2 = this.minHeight;

      Iterator var3;
      FrameLayout.ChildContainer var4;
      for(var3 = this.children.iterator(); var3.hasNext(); var2 = Math.max(var2, var4.getHeight())) {
         var4 = (FrameLayout.ChildContainer)var3.next();
         var1 = Math.max(var1, var4.getWidth());
      }

      var3 = this.children.iterator();

      while(var3.hasNext()) {
         var4 = (FrameLayout.ChildContainer)var3.next();
         var4.setX(this.getX(), var1);
         var4.setY(this.getY(), var2);
      }

      this.width = var1;
      this.height = var2;
   }

   public <T extends LayoutElement> T addChild(T var1) {
      return this.addChild(var1, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T var1, LayoutSettings var2) {
      this.children.add(new FrameLayout.ChildContainer(var1, var2));
      return var1;
   }

   public void visitChildren(Consumer<LayoutElement> var1) {
      this.children.forEach((var1x) -> {
         var1.accept(var1x.child);
      });
   }

   public static void centerInRectangle(LayoutElement var0, int var1, int var2, int var3, int var4) {
      alignInRectangle(var0, var1, var2, var3, var4, 0.5F, 0.5F);
   }

   public static void centerInRectangle(LayoutElement var0, ScreenRectangle var1) {
      centerInRectangle(var0, var1.position().x(), var1.position().y(), var1.width(), var1.height());
   }

   public static void alignInRectangle(LayoutElement var0, ScreenRectangle var1, float var2, float var3) {
      alignInRectangle(var0, var1.left(), var1.top(), var1.width(), var1.height(), var2, var3);
   }

   public static void alignInRectangle(LayoutElement var0, int var1, int var2, int var3, int var4, float var5, float var6) {
      int var10002 = var0.getWidth();
      Objects.requireNonNull(var0);
      alignInDimension(var1, var3, var10002, var0::setX, var5);
      var10002 = var0.getHeight();
      Objects.requireNonNull(var0);
      alignInDimension(var2, var4, var10002, var0::setY, var6);
   }

   public static void alignInDimension(int var0, int var1, int var2, Consumer<Integer> var3, float var4) {
      int var5 = (int)Mth.lerp(var4, 0.0F, (float)(var1 - var2));
      var3.accept(var0 + var5);
   }

   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement var1, LayoutSettings var2) {
         super(var1, var2);
      }
   }
}
