package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public abstract class AbstractWidget implements Renderable, GuiEventListener, LayoutElement, NarratableEntry {
   public static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/widgets.png");
   public static final ResourceLocation ACCESSIBILITY_TEXTURE = new ResourceLocation("textures/gui/accessibility.png");
   private static final double PERIOD_PER_SCROLLED_PIXEL = 0.5D;
   private static final double MIN_SCROLL_PERIOD = 3.0D;
   protected int width;
   protected int height;
   private int x;
   private int y;
   private Component message;
   protected boolean isHovered;
   public boolean active = true;
   public boolean visible = true;
   protected float alpha = 1.0F;
   private int tabOrderGroup;
   private boolean focused;
   @Nullable
   private Tooltip tooltip;
   private int tooltipMsDelay;
   private long hoverOrFocusedStartTime;
   private boolean wasHoveredOrFocused;

   public AbstractWidget(int var1, int var2, int var3, int var4, Component var5) {
      this.x = var1;
      this.y = var2;
      this.width = var3;
      this.height = var4;
      this.message = var5;
   }

   public int getHeight() {
      return this.height;
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      if (this.visible) {
         this.isHovered = var2 >= this.getX() && var3 >= this.getY() && var2 < this.getX() + this.width && var3 < this.getY() + this.height;
         this.renderWidget(var1, var2, var3, var4);
         this.updateTooltip();
      }
   }

   private void updateTooltip() {
      if (this.tooltip != null) {
         boolean var1 = this.isHovered || this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard();
         if (var1 != this.wasHoveredOrFocused) {
            if (var1) {
               this.hoverOrFocusedStartTime = Util.getMillis();
            }

            this.wasHoveredOrFocused = var1;
         }

         if (var1 && Util.getMillis() - this.hoverOrFocusedStartTime > (long)this.tooltipMsDelay) {
            Screen var2 = Minecraft.getInstance().screen;
            if (var2 != null) {
               var2.setTooltipForNextRenderPass(this.tooltip, this.createTooltipPositioner(), this.isFocused());
            }
         }

      }
   }

   protected ClientTooltipPositioner createTooltipPositioner() {
      return (ClientTooltipPositioner)(!this.isHovered && this.isFocused() && Minecraft.getInstance().getLastInputType().isKeyboard() ? new BelowOrAboveWidgetTooltipPositioner(this) : new MenuTooltipPositioner(this));
   }

   public void setTooltip(@Nullable Tooltip var1) {
      this.tooltip = var1;
   }

   @Nullable
   public Tooltip getTooltip() {
      return this.tooltip;
   }

   public void setTooltipDelay(int var1) {
      this.tooltipMsDelay = var1;
   }

   protected MutableComponent createNarrationMessage() {
      return wrapDefaultNarrationMessage(this.getMessage());
   }

   public static MutableComponent wrapDefaultNarrationMessage(Component var0) {
      return Component.translatable("gui.narrate.button", var0);
   }

   protected abstract void renderWidget(GuiGraphics var1, int var2, int var3, float var4);

   protected static void renderScrollingString(GuiGraphics var0, Font var1, Component var2, int var3, int var4, int var5, int var6, int var7) {
      int var8 = var1.width((FormattedText)var2);
      int var10000 = var4 + var6;
      Objects.requireNonNull(var1);
      int var9 = (var10000 - 9) / 2 + 1;
      int var10 = var5 - var3;
      if (var8 > var10) {
         int var11 = var8 - var10;
         double var12 = (double)Util.getMillis() / 1000.0D;
         double var14 = Math.max((double)var11 * 0.5D, 3.0D);
         double var16 = Math.sin(1.5707963267948966D * Math.cos(6.283185307179586D * var12 / var14)) / 2.0D + 0.5D;
         double var18 = Mth.lerp(var16, 0.0D, (double)var11);
         var0.enableScissor(var3, var4, var5, var6);
         var0.drawString(var1, var2, var3 - (int)var18, var9, var7);
         var0.disableScissor();
      } else {
         var0.drawCenteredString(var1, var2, (var3 + var5) / 2, var9, var7);
      }

   }

   protected void renderScrollingString(GuiGraphics var1, Font var2, int var3, int var4) {
      int var5 = this.getX() + var3;
      int var6 = this.getX() + this.getWidth() - var3;
      renderScrollingString(var1, var2, this.getMessage(), var5, this.getY(), var6, this.getY() + this.getHeight(), var4);
   }

   public void renderTexture(GuiGraphics var1, ResourceLocation var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11) {
      int var12 = var6;
      if (!this.isActive()) {
         var12 = var6 + var7 * 2;
      } else if (this.isHoveredOrFocused()) {
         var12 = var6 + var7;
      }

      RenderSystem.enableDepthTest();
      var1.blit(var2, var3, var4, (float)var5, (float)var12, var8, var9, var10, var11);
   }

   public void onClick(double var1, double var3) {
   }

   public void onRelease(double var1, double var3) {
   }

   protected void onDrag(double var1, double var3, double var5, double var7) {
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (this.active && this.visible) {
         if (this.isValidClickButton(var5)) {
            boolean var6 = this.clicked(var1, var3);
            if (var6) {
               this.playDownSound(Minecraft.getInstance().getSoundManager());
               this.onClick(var1, var3);
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean mouseReleased(double var1, double var3, int var5) {
      if (this.isValidClickButton(var5)) {
         this.onRelease(var1, var3);
         return true;
      } else {
         return false;
      }
   }

   protected boolean isValidClickButton(int var1) {
      return var1 == 0;
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (this.isValidClickButton(var5)) {
         this.onDrag(var1, var3, var6, var8);
         return true;
      } else {
         return false;
      }
   }

   protected boolean clicked(double var1, double var3) {
      return this.active && this.visible && var1 >= (double)this.getX() && var3 >= (double)this.getY() && var1 < (double)(this.getX() + this.width) && var3 < (double)(this.getY() + this.height);
   }

   @Nullable
   public ComponentPath nextFocusPath(FocusNavigationEvent var1) {
      if (this.active && this.visible) {
         return !this.isFocused() ? ComponentPath.leaf(this) : null;
      } else {
         return null;
      }
   }

   public boolean isMouseOver(double var1, double var3) {
      return this.active && this.visible && var1 >= (double)this.getX() && var3 >= (double)this.getY() && var1 < (double)(this.getX() + this.width) && var3 < (double)(this.getY() + this.height);
   }

   public void playDownSound(SoundManager var1) {
      var1.play(SimpleSoundInstance.forUI((Holder)SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public int getWidth() {
      return this.width;
   }

   public void setWidth(int var1) {
      this.width = var1;
   }

   public void setAlpha(float var1) {
      this.alpha = var1;
   }

   public void setMessage(Component var1) {
      this.message = var1;
   }

   public Component getMessage() {
      return this.message;
   }

   public boolean isFocused() {
      return this.focused;
   }

   public boolean isHovered() {
      return this.isHovered;
   }

   public boolean isHoveredOrFocused() {
      return this.isHovered() || this.isFocused();
   }

   public boolean isActive() {
      return this.visible && this.active;
   }

   public void setFocused(boolean var1) {
      this.focused = var1;
   }

   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   public final void updateNarration(NarrationElementOutput var1) {
      this.updateWidgetNarration(var1);
      if (this.tooltip != null) {
         this.tooltip.updateNarration(var1);
      }

   }

   protected abstract void updateWidgetNarration(NarrationElementOutput var1);

   protected void defaultButtonNarrationText(NarrationElementOutput var1) {
      var1.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.button.usage.focused"));
         } else {
            var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.button.usage.hovered"));
         }
      }

   }

   public int getX() {
      return this.x;
   }

   public void setX(int var1) {
      this.x = var1;
   }

   public int getY() {
      return this.y;
   }

   public void setY(int var1) {
      this.y = var1;
   }

   public void visitWidgets(Consumer<AbstractWidget> var1) {
      var1.accept(this);
   }

   public ScreenRectangle getRectangle() {
      return LayoutElement.super.getRectangle();
   }

   public int getTabOrderGroup() {
      return this.tabOrderGroup;
   }

   public void setTabOrderGroup(int var1) {
      this.tabOrderGroup = var1;
   }
}
