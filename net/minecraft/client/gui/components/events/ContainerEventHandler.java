package net.minecraft.client.gui.components.events;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2i;

public interface ContainerEventHandler extends GuiEventListener {
   List<? extends GuiEventListener> children();

   default Optional<GuiEventListener> getChildAt(double var1, double var3) {
      Iterator var5 = this.children().iterator();

      GuiEventListener var6;
      do {
         if (!var5.hasNext()) {
            return Optional.empty();
         }

         var6 = (GuiEventListener)var5.next();
      } while(!var6.isMouseOver(var1, var3));

      return Optional.of(var6);
   }

   default boolean mouseClicked(double var1, double var3, int var5) {
      Iterator var6 = this.children().iterator();

      GuiEventListener var7;
      do {
         if (!var6.hasNext()) {
            return false;
         }

         var7 = (GuiEventListener)var6.next();
      } while(!var7.mouseClicked(var1, var3, var5));

      this.setFocused(var7);
      if (var5 == 0) {
         this.setDragging(true);
      }

      return true;
   }

   default boolean mouseReleased(double var1, double var3, int var5) {
      this.setDragging(false);
      return this.getChildAt(var1, var3).filter((var5x) -> {
         return var5x.mouseReleased(var1, var3, var5);
      }).isPresent();
   }

   default boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      return this.getFocused() != null && this.isDragging() && var5 == 0 ? this.getFocused().mouseDragged(var1, var3, var5, var6, var8) : false;
   }

   boolean isDragging();

   void setDragging(boolean var1);

   default boolean mouseScrolled(double var1, double var3, double var5) {
      return this.getChildAt(var1, var3).filter((var6) -> {
         return var6.mouseScrolled(var1, var3, var5);
      }).isPresent();
   }

   default boolean keyPressed(int var1, int var2, int var3) {
      return this.getFocused() != null && this.getFocused().keyPressed(var1, var2, var3);
   }

   default boolean keyReleased(int var1, int var2, int var3) {
      return this.getFocused() != null && this.getFocused().keyReleased(var1, var2, var3);
   }

   default boolean charTyped(char var1, int var2) {
      return this.getFocused() != null && this.getFocused().charTyped(var1, var2);
   }

   @Nullable
   GuiEventListener getFocused();

   void setFocused(@Nullable GuiEventListener var1);

   default void setFocused(boolean var1) {
   }

   default boolean isFocused() {
      return this.getFocused() != null;
   }

   @Nullable
   default ComponentPath getCurrentFocusPath() {
      GuiEventListener var1 = this.getFocused();
      return var1 != null ? ComponentPath.path(this, var1.getCurrentFocusPath()) : null;
   }

   default void magicalSpecialHackyFocus(@Nullable GuiEventListener var1) {
      this.setFocused(var1);
   }

   @Nullable
   default ComponentPath nextFocusPath(FocusNavigationEvent var1) {
      GuiEventListener var2 = this.getFocused();
      if (var2 != null) {
         ComponentPath var3 = var2.nextFocusPath(var1);
         if (var3 != null) {
            return ComponentPath.path(this, var3);
         }
      }

      if (var1 instanceof FocusNavigationEvent.TabNavigation) {
         FocusNavigationEvent.TabNavigation var5 = (FocusNavigationEvent.TabNavigation)var1;
         return this.handleTabNavigation(var5);
      } else if (var1 instanceof FocusNavigationEvent.ArrowNavigation) {
         FocusNavigationEvent.ArrowNavigation var4 = (FocusNavigationEvent.ArrowNavigation)var1;
         return this.handleArrowNavigation(var4);
      } else {
         return null;
      }
   }

   @Nullable
   private default ComponentPath handleTabNavigation(FocusNavigationEvent.TabNavigation var1) {
      boolean var2 = var1.forward();
      GuiEventListener var3 = this.getFocused();
      ArrayList var4 = new ArrayList(this.children());
      Collections.sort(var4, Comparator.comparingInt((var0) -> {
         return var0.getTabOrderGroup();
      }));
      int var6 = var4.indexOf(var3);
      int var5;
      if (var3 != null && var6 >= 0) {
         var5 = var6 + (var2 ? 1 : 0);
      } else if (var2) {
         var5 = 0;
      } else {
         var5 = var4.size();
      }

      ListIterator var7 = var4.listIterator(var5);
      BooleanSupplier var10000;
      if (var2) {
         Objects.requireNonNull(var7);
         var10000 = var7::hasNext;
      } else {
         Objects.requireNonNull(var7);
         var10000 = var7::hasPrevious;
      }

      BooleanSupplier var8 = var10000;
      Supplier var12;
      if (var2) {
         Objects.requireNonNull(var7);
         var12 = var7::next;
      } else {
         Objects.requireNonNull(var7);
         var12 = var7::previous;
      }

      Supplier var9 = var12;

      ComponentPath var11;
      do {
         if (!var8.getAsBoolean()) {
            return null;
         }

         GuiEventListener var10 = (GuiEventListener)var9.get();
         var11 = var10.nextFocusPath(var1);
      } while(var11 == null);

      return ComponentPath.path(this, var11);
   }

   @Nullable
   private default ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation var1) {
      GuiEventListener var2 = this.getFocused();
      if (var2 == null) {
         ScreenDirection var5 = var1.direction();
         ScreenRectangle var4 = this.getRectangle().getBorder(var5.getOpposite());
         return ComponentPath.path(this, this.nextFocusPathInDirection(var4, var5, (GuiEventListener)null, var1));
      } else {
         ScreenRectangle var3 = var2.getRectangle();
         return ComponentPath.path(this, this.nextFocusPathInDirection(var3, var1.direction(), var2, var1));
      }
   }

   @Nullable
   private default ComponentPath nextFocusPathInDirection(ScreenRectangle var1, ScreenDirection var2, @Nullable GuiEventListener var3, FocusNavigationEvent var4) {
      ScreenAxis var5 = var2.getAxis();
      ScreenAxis var6 = var5.orthogonal();
      ScreenDirection var7 = var6.getPositive();
      int var8 = var1.getBoundInDirection(var2.getOpposite());
      ArrayList var9 = new ArrayList();
      Iterator var10 = this.children().iterator();

      while(var10.hasNext()) {
         GuiEventListener var11 = (GuiEventListener)var10.next();
         if (var11 != var3) {
            ScreenRectangle var12 = var11.getRectangle();
            if (var12.overlapsInAxis(var1, var6)) {
               int var13 = var12.getBoundInDirection(var2.getOpposite());
               if (var2.isAfter(var13, var8)) {
                  var9.add(var11);
               } else if (var13 == var8 && var2.isAfter(var12.getBoundInDirection(var2), var1.getBoundInDirection(var2))) {
                  var9.add(var11);
               }
            }
         }
      }

      Comparator var15 = Comparator.comparing((var1x) -> {
         return var1x.getRectangle().getBoundInDirection(var2.getOpposite());
      }, var2.coordinateValueComparator());
      Comparator var16 = Comparator.comparing((var1x) -> {
         return var1x.getRectangle().getBoundInDirection(var7.getOpposite());
      }, var7.coordinateValueComparator());
      var9.sort(var15.thenComparing(var16));
      Iterator var17 = var9.iterator();

      ComponentPath var14;
      do {
         if (!var17.hasNext()) {
            return this.nextFocusPathVaguelyInDirection(var1, var2, var3, var4);
         }

         GuiEventListener var18 = (GuiEventListener)var17.next();
         var14 = var18.nextFocusPath(var4);
      } while(var14 == null);

      return var14;
   }

   @Nullable
   private default ComponentPath nextFocusPathVaguelyInDirection(ScreenRectangle var1, ScreenDirection var2, @Nullable GuiEventListener var3, FocusNavigationEvent var4) {
      ScreenAxis var5 = var2.getAxis();
      ScreenAxis var6 = var5.orthogonal();
      ArrayList var7 = new ArrayList();
      ScreenPosition var8 = ScreenPosition.of(var5, var1.getBoundInDirection(var2), var1.getCenterInAxis(var6));
      Iterator var9 = this.children().iterator();

      while(var9.hasNext()) {
         GuiEventListener var10 = (GuiEventListener)var9.next();
         if (var10 != var3) {
            ScreenRectangle var11 = var10.getRectangle();
            ScreenPosition var12 = ScreenPosition.of(var5, var11.getBoundInDirection(var2.getOpposite()), var11.getCenterInAxis(var6));
            if (var2.isAfter(var12.getCoordinate(var5), var8.getCoordinate(var5))) {
               long var13 = Vector2i.distanceSquared(var8.x(), var8.y(), var12.x(), var12.y());
               var7.add(Pair.of(var10, var13));
            }
         }
      }

      var7.sort(Comparator.comparingDouble(Pair::getSecond));
      var9 = var7.iterator();

      ComponentPath var16;
      do {
         if (!var9.hasNext()) {
            return null;
         }

         Pair var15 = (Pair)var9.next();
         var16 = ((GuiEventListener)var15.getFirst()).nextFocusPath(var4);
      } while(var16 == null);

      return var16;
   }
}
