package net.minecraft.client.gui.components;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;

public class MultiLineEditBox extends AbstractScrollWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int TEXT_COLOR = -2039584;
   private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private int frame;

   public MultiLineEditBox(Font var1, int var2, int var3, int var4, int var5, Component var6, Component var7) {
      super(var2, var3, var4, var5, var7);
      this.font = var1;
      this.placeholder = var6;
      this.textField = new MultilineTextField(var1, var4 - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int var1) {
      this.textField.setCharacterLimit(var1);
   }

   public void setValueListener(Consumer<String> var1) {
      this.textField.setValueListener(var1);
   }

   public void setValue(String var1) {
      this.textField.setValue(var1);
   }

   public String getValue() {
      return this.textField.value();
   }

   public void tick() {
      ++this.frame;
   }

   public void updateWidgetNarration(NarrationElementOutput var1) {
      var1.add(NarratedElementType.TITLE, (Component)Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (super.mouseClicked(var1, var3, var5)) {
         return true;
      } else if (this.withinContentAreaPoint(var1, var3) && var5 == 0) {
         this.textField.setSelecting(Screen.hasShiftDown());
         this.seekCursorScreen(var1, var3);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (super.mouseDragged(var1, var3, var5, var6, var8)) {
         return true;
      } else if (this.withinContentAreaPoint(var1, var3) && var5 == 0) {
         this.textField.setSelecting(true);
         this.seekCursorScreen(var1, var3);
         this.textField.setSelecting(Screen.hasShiftDown());
         return true;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      return this.textField.keyPressed(var1);
   }

   public boolean charTyped(char var1, int var2) {
      if (this.visible && this.isFocused() && SharedConstants.isAllowedChatCharacter(var1)) {
         this.textField.insertText(Character.toString(var1));
         return true;
      } else {
         return false;
      }
   }

   protected void renderContents(GuiGraphics var1, int var2, int var3, float var4) {
      String var5 = this.textField.value();
      if (var5.isEmpty() && !this.isFocused()) {
         var1.drawWordWrap(this.font, this.placeholder, this.getX() + this.innerPadding(), this.getY() + this.innerPadding(), this.width - this.totalInnerPadding(), -857677600);
      } else {
         int var6 = this.textField.cursor();
         boolean var7 = this.isFocused() && this.frame / 6 % 2 == 0;
         boolean var8 = var6 < var5.length();
         int var9 = 0;
         int var10 = 0;
         int var11 = this.getY() + this.innerPadding();

         int var10002;
         int var10004;
         for(Iterator var12 = this.textField.iterateLines().iterator(); var12.hasNext(); var11 += 9) {
            MultilineTextField.StringView var13 = (MultilineTextField.StringView)var12.next();
            Objects.requireNonNull(this.font);
            boolean var14 = this.withinContentAreaTopBottom(var11, var11 + 9);
            if (var7 && var8 && var6 >= var13.beginIndex() && var6 <= var13.endIndex()) {
               if (var14) {
                  var9 = var1.drawString(this.font, var5.substring(var13.beginIndex(), var6), this.getX() + this.innerPadding(), var11, -2039584) - 1;
                  var10002 = var11 - 1;
                  int var10003 = var9 + 1;
                  var10004 = var11 + 1;
                  Objects.requireNonNull(this.font);
                  var1.fill(var9, var10002, var10003, var10004 + 9, -3092272);
                  var1.drawString(this.font, var5.substring(var6, var13.endIndex()), var9, var11, -2039584);
               }
            } else {
               if (var14) {
                  var9 = var1.drawString(this.font, var5.substring(var13.beginIndex(), var13.endIndex()), this.getX() + this.innerPadding(), var11, -2039584) - 1;
               }

               var10 = var11;
            }

            Objects.requireNonNull(this.font);
         }

         if (var7 && !var8) {
            Objects.requireNonNull(this.font);
            if (this.withinContentAreaTopBottom(var10, var10 + 9)) {
               var1.drawString(this.font, "_", var9, var10, -3092272);
            }
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView var18 = this.textField.getSelected();
            int var19 = this.getX() + this.innerPadding();
            var11 = this.getY() + this.innerPadding();
            Iterator var20 = this.textField.iterateLines().iterator();

            while(var20.hasNext()) {
               MultilineTextField.StringView var15 = (MultilineTextField.StringView)var20.next();
               if (var18.beginIndex() > var15.endIndex()) {
                  Objects.requireNonNull(this.font);
                  var11 += 9;
               } else {
                  if (var15.beginIndex() > var18.endIndex()) {
                     break;
                  }

                  Objects.requireNonNull(this.font);
                  if (this.withinContentAreaTopBottom(var11, var11 + 9)) {
                     int var16 = this.font.width(var5.substring(var15.beginIndex(), Math.max(var18.beginIndex(), var15.beginIndex())));
                     int var17;
                     if (var18.endIndex() > var15.endIndex()) {
                        var17 = this.width - this.innerPadding();
                     } else {
                        var17 = this.font.width(var5.substring(var15.beginIndex(), var18.endIndex()));
                     }

                     var10002 = var19 + var16;
                     var10004 = var19 + var17;
                     Objects.requireNonNull(this.font);
                     this.renderHighlight(var1, var10002, var11, var10004, var11 + 9);
                  }

                  Objects.requireNonNull(this.font);
                  var11 += 9;
               }
            }
         }

      }
   }

   protected void renderDecorations(GuiGraphics var1) {
      super.renderDecorations(var1);
      if (this.textField.hasCharacterLimit()) {
         int var2 = this.textField.characterLimit();
         MutableComponent var3 = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), var2);
         var1.drawString(this.font, (Component)var3, this.getX() + this.width - this.font.width((FormattedText)var3), this.getY() + this.height + 4, 10526880);
      }

   }

   public int getInnerHeight() {
      Objects.requireNonNull(this.font);
      return 9 * this.textField.getLineCount();
   }

   protected boolean scrollbarVisible() {
      return (double)this.textField.getLineCount() > this.getDisplayableLineCount();
   }

   protected double scrollRate() {
      Objects.requireNonNull(this.font);
      return 9.0D / 2.0D;
   }

   private void renderHighlight(GuiGraphics var1, int var2, int var3, int var4, int var5) {
      var1.fill(RenderType.guiTextHighlight(), var2, var3, var4, var5, -16776961);
   }

   private void scrollToCursor() {
      double var1 = this.scrollAmount();
      MultilineTextField var10000 = this.textField;
      Objects.requireNonNull(this.font);
      MultilineTextField.StringView var3 = var10000.getLineView((int)(var1 / 9.0D));
      int var5;
      if (this.textField.cursor() <= var3.beginIndex()) {
         var5 = this.textField.getLineAtCursor();
         Objects.requireNonNull(this.font);
         var1 = (double)(var5 * 9);
      } else {
         var10000 = this.textField;
         double var10001 = var1 + (double)this.height;
         Objects.requireNonNull(this.font);
         MultilineTextField.StringView var4 = var10000.getLineView((int)(var10001 / 9.0D) - 1);
         if (this.textField.cursor() > var4.endIndex()) {
            var5 = this.textField.getLineAtCursor();
            Objects.requireNonNull(this.font);
            var5 = var5 * 9 - this.height;
            Objects.requireNonNull(this.font);
            var1 = (double)(var5 + 9 + this.totalInnerPadding());
         }
      }

      this.setScrollAmount(var1);
   }

   private double getDisplayableLineCount() {
      double var10000 = (double)(this.height - this.totalInnerPadding());
      Objects.requireNonNull(this.font);
      return var10000 / 9.0D;
   }

   private void seekCursorScreen(double var1, double var3) {
      double var5 = var1 - (double)this.getX() - (double)this.innerPadding();
      double var7 = var3 - (double)this.getY() - (double)this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint(var5, var7);
   }
}
