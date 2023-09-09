package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class LinearLayout extends AbstractLayout {
   private final LinearLayout.Orientation orientation;
   private final List<LinearLayout.ChildContainer> children;
   private final LayoutSettings defaultChildLayoutSettings;

   public LinearLayout(int var1, int var2, LinearLayout.Orientation var3) {
      this(0, 0, var1, var2, var3);
   }

   public LinearLayout(int var1, int var2, int var3, int var4, LinearLayout.Orientation var5) {
      super(var1, var2, var3, var4);
      this.children = new ArrayList();
      this.defaultChildLayoutSettings = LayoutSettings.defaults();
      this.orientation = var5;
   }

   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int var1 = 0;
         int var2 = this.orientation.getSecondaryLength((LayoutElement)this);

         LinearLayout.ChildContainer var4;
         for(Iterator var3 = this.children.iterator(); var3.hasNext(); var2 = Math.max(var2, this.orientation.getSecondaryLength(var4))) {
            var4 = (LinearLayout.ChildContainer)var3.next();
            var1 += this.orientation.getPrimaryLength(var4);
         }

         int var10 = this.orientation.getPrimaryLength((LayoutElement)this) - var1;
         int var11 = this.orientation.getPrimaryPosition(this);
         Iterator var5 = this.children.iterator();
         LinearLayout.ChildContainer var6 = (LinearLayout.ChildContainer)var5.next();
         this.orientation.setPrimaryPosition(var6, var11);
         var11 += this.orientation.getPrimaryLength(var6);
         LinearLayout.ChildContainer var8;
         if (this.children.size() >= 2) {
            for(Divisor var7 = new Divisor(var10, this.children.size() - 1); var7.hasNext(); var11 += this.orientation.getPrimaryLength(var8)) {
               var11 += var7.nextInt();
               var8 = (LinearLayout.ChildContainer)var5.next();
               this.orientation.setPrimaryPosition(var8, var11);
            }
         }

         int var12 = this.orientation.getSecondaryPosition(this);
         Iterator var13 = this.children.iterator();

         while(var13.hasNext()) {
            LinearLayout.ChildContainer var9 = (LinearLayout.ChildContainer)var13.next();
            this.orientation.setSecondaryPosition(var9, var12, var2);
         }

         switch(this.orientation) {
         case HORIZONTAL:
            this.height = var2;
            break;
         case VERTICAL:
            this.width = var2;
         }

      }
   }

   public void visitChildren(Consumer<LayoutElement> var1) {
      this.children.forEach((var1x) -> {
         var1.accept(var1x.child);
      });
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T var1) {
      return this.addChild(var1, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T var1, LayoutSettings var2) {
      this.children.add(new LinearLayout.ChildContainer(var1, var2));
      return var1;
   }

   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      private Orientation() {
      }

      int getPrimaryLength(LayoutElement var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getWidth();
            break;
         case VERTICAL:
            var10000 = var1.getHeight();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      int getPrimaryLength(LinearLayout.ChildContainer var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getWidth();
            break;
         case VERTICAL:
            var10000 = var1.getHeight();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      int getSecondaryLength(LayoutElement var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getHeight();
            break;
         case VERTICAL:
            var10000 = var1.getWidth();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      int getSecondaryLength(LinearLayout.ChildContainer var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getHeight();
            break;
         case VERTICAL:
            var10000 = var1.getWidth();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      void setPrimaryPosition(LinearLayout.ChildContainer var1, int var2) {
         switch(this) {
         case HORIZONTAL:
            var1.setX(var2, var1.getWidth());
            break;
         case VERTICAL:
            var1.setY(var2, var1.getHeight());
         }

      }

      void setSecondaryPosition(LinearLayout.ChildContainer var1, int var2, int var3) {
         switch(this) {
         case HORIZONTAL:
            var1.setY(var2, var3);
            break;
         case VERTICAL:
            var1.setX(var2, var3);
         }

      }

      int getPrimaryPosition(LayoutElement var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getX();
            break;
         case VERTICAL:
            var10000 = var1.getY();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      int getSecondaryPosition(LayoutElement var1) {
         int var10000;
         switch(this) {
         case HORIZONTAL:
            var10000 = var1.getY();
            break;
         case VERTICAL:
            var10000 = var1.getX();
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var10000;
      }

      // $FF: synthetic method
      private static LinearLayout.Orientation[] $values() {
         return new LinearLayout.Orientation[]{HORIZONTAL, VERTICAL};
      }
   }

   private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement var1, LayoutSettings var2) {
         super(var1, var2);
      }
   }
}
