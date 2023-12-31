package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractButton extends AbstractWidget {
   protected static final int TEXTURE_Y_OFFSET = 46;
   protected static final int TEXTURE_WIDTH = 200;
   protected static final int TEXTURE_HEIGHT = 20;
   protected static final int TEXTURE_BORDER_X = 20;
   protected static final int TEXTURE_BORDER_Y = 4;
   protected static final int TEXT_MARGIN = 2;

   public AbstractButton(int var1, int var2, int var3, int var4, Component var5) {
      super(var1, var2, var3, var4, var5);
   }

   public abstract void onPress();

   protected void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
      Minecraft var5 = Minecraft.getInstance();
      var1.setColor(1.0F, 1.0F, 1.0F, this.alpha);
      RenderSystem.enableBlend();
      RenderSystem.enableDepthTest();
      var1.blitNineSliced(WIDGETS_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
      var1.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      int var6 = this.active ? 16777215 : 10526880;
      this.renderString(var1, var5.font, var6 | Mth.ceil(this.alpha * 255.0F) << 24);
   }

   public void renderString(GuiGraphics var1, Font var2, int var3) {
      this.renderScrollingString(var1, var2, 2, var3);
   }

   private int getTextureY() {
      byte var1 = 1;
      if (!this.active) {
         var1 = 0;
      } else if (this.isHoveredOrFocused()) {
         var1 = 2;
      }

      return 46 + var1 * 20;
   }

   public void onClick(double var1, double var3) {
      this.onPress();
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (this.active && this.visible) {
         if (CommonInputs.selected(var1)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
