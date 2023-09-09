package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationSupplier;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;

public abstract class ObjectSelectionList<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   private static final Component USAGE_NARRATION = Component.translatable("narration.selection.usage");

   public ObjectSelectionList(Minecraft var1, int var2, int var3, int var4, int var5, int var6) {
      super(var1, var2, var3, var4, var5, var6);
   }

   @Nullable
   public ComponentPath nextFocusPath(FocusNavigationEvent var1) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (this.isFocused() && var1 instanceof FocusNavigationEvent.ArrowNavigation) {
         FocusNavigationEvent.ArrowNavigation var4 = (FocusNavigationEvent.ArrowNavigation)var1;
         ObjectSelectionList.Entry var3 = (ObjectSelectionList.Entry)this.nextEntry(var4.direction());
         return var3 != null ? ComponentPath.path((ContainerEventHandler)this, (ComponentPath)ComponentPath.leaf(var3)) : null;
      } else if (!this.isFocused()) {
         ObjectSelectionList.Entry var2 = (ObjectSelectionList.Entry)this.getSelected();
         if (var2 == null) {
            var2 = (ObjectSelectionList.Entry)this.nextEntry(var1.getVerticalDirectionForInitialFocus());
         }

         return var2 == null ? null : ComponentPath.path((ContainerEventHandler)this, (ComponentPath)ComponentPath.leaf(var2));
      } else {
         return null;
      }
   }

   public void updateNarration(NarrationElementOutput var1) {
      ObjectSelectionList.Entry var2 = (ObjectSelectionList.Entry)this.getHovered();
      if (var2 != null) {
         this.narrateListElementPosition(var1.nest(), var2);
         var2.updateNarration(var1);
      } else {
         ObjectSelectionList.Entry var3 = (ObjectSelectionList.Entry)this.getSelected();
         if (var3 != null) {
            this.narrateListElementPosition(var1.nest(), var3);
            var3.updateNarration(var1);
         }
      }

      if (this.isFocused()) {
         var1.add(NarratedElementType.USAGE, USAGE_NARRATION);
      }

   }

   public abstract static class Entry<E extends ObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements NarrationSupplier {
      public Entry() {
      }

      public abstract Component getNarration();

      public void updateNarration(NarrationElementOutput var1) {
         var1.add(NarratedElementType.TITLE, this.getNarration());
      }

      // $FF: synthetic method
      // $FF: bridge method
      public boolean isMouseOver(double var1, double var3) {
         return super.isMouseOver(var1, var3);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void renderBack(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         super.renderBack(var1, var2, var3, var4, var5, var6, var7, var8, var9, var10);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public boolean isFocused() {
         return super.isFocused();
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setFocused(boolean var1) {
         super.setFocused(var1);
      }
   }
}
