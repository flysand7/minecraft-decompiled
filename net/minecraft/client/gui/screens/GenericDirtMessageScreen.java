package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class GenericDirtMessageScreen extends Screen {
   public GenericDirtMessageScreen(Component var1) {
      super(var1);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderDirtBackground(var1);
      var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 70, 16777215);
      super.render(var1, var2, var3, var4);
   }
}
