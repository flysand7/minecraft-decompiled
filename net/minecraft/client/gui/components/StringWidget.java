package net.minecraft.client.gui.components;

import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

public class StringWidget extends AbstractStringWidget {
   private float alignX;

   public StringWidget(Component var1, Font var2) {
      int var10003 = var2.width(var1.getVisualOrderText());
      Objects.requireNonNull(var2);
      this(0, 0, var10003, 9, var1, var2);
   }

   public StringWidget(int var1, int var2, Component var3, Font var4) {
      this(0, 0, var1, var2, var3, var4);
   }

   public StringWidget(int var1, int var2, int var3, int var4, Component var5, Font var6) {
      super(var1, var2, var3, var4, var5, var6);
      this.alignX = 0.5F;
      this.active = false;
   }

   public StringWidget setColor(int var1) {
      super.setColor(var1);
      return this;
   }

   private StringWidget horizontalAlignment(float var1) {
      this.alignX = var1;
      return this;
   }

   public StringWidget alignLeft() {
      return this.horizontalAlignment(0.0F);
   }

   public StringWidget alignCenter() {
      return this.horizontalAlignment(0.5F);
   }

   public StringWidget alignRight() {
      return this.horizontalAlignment(1.0F);
   }

   public void renderWidget(GuiGraphics var1, int var2, int var3, float var4) {
      Component var5 = this.getMessage();
      Font var6 = this.getFont();
      int var7 = this.getX() + Math.round(this.alignX * (float)(this.getWidth() - var6.width((FormattedText)var5)));
      int var10000 = this.getY();
      int var10001 = this.getHeight();
      Objects.requireNonNull(var6);
      int var8 = var10000 + (var10001 - 9) / 2;
      var1.drawString(var6, var5, var7, var8, this.getColor());
   }

   // $FF: synthetic method
   public AbstractStringWidget setColor(int var1) {
      return this.setColor(var1);
   }
}
