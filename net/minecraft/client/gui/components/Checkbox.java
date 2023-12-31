package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class Checkbox extends AbstractButton {
   private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");
   private static final int TEXT_COLOR = 14737632;
   private boolean selected;
   private final boolean showLabel;

   public Checkbox(int var1, int var2, int var3, int var4, Component var5, boolean var6) {
      this(var1, var2, var3, var4, var5, var6, true);
   }

   public Checkbox(int var1, int var2, int var3, int var4, Component var5, boolean var6, boolean var7) {
      super(var1, var2, var3, var4, var5);
      this.selected = var6;
      this.showLabel = var7;
   }

   public void onPress() {
      this.selected = !this.selected;
   }

   public boolean selected() {
      return this.selected;
   }

   public void updateWidgetNarration(NarrationElementOutput var1) {
      var1.add(NarratedElementType.TITLE, (Component)this.createNarrationMessage());
      if (this.active) {
         if (this.isFocused()) {
            var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.checkbox.usage.focused"));
         } else {
            var1.add(NarratedElementType.USAGE, (Component)Component.translatable("narration.checkbox.usage.hovered"));
         }
      }

   }

   public void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
      Minecraft var5 = Minecraft.getInstance();
      RenderSystem.enableDepthTest();
      Font var6 = var5.font;
      var1.setColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      var1.blit(TEXTURE, this.getX(), this.getY(), this.isFocused() ? 20.0F : 0.0F, this.selected ? 20.0F : 0.0F, 20, this.height, 64, 64);
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.showLabel) {
         var1.drawString(var6, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
      }

   }
}
