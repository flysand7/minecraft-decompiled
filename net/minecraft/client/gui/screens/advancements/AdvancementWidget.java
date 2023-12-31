package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class AdvancementWidget {
   private static final ResourceLocation WIDGETS_LOCATION = new ResourceLocation("textures/gui/advancements/widgets.png");
   private static final int HEIGHT = 26;
   private static final int BOX_X = 0;
   private static final int BOX_WIDTH = 200;
   private static final int FRAME_WIDTH = 26;
   private static final int ICON_X = 8;
   private static final int ICON_Y = 5;
   private static final int ICON_WIDTH = 26;
   private static final int TITLE_PADDING_LEFT = 3;
   private static final int TITLE_PADDING_RIGHT = 5;
   private static final int TITLE_X = 32;
   private static final int TITLE_Y = 9;
   private static final int TITLE_MAX_WIDTH = 163;
   private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final Advancement advancement;
   private final DisplayInfo display;
   private final FormattedCharSequence title;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   @Nullable
   private AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   @Nullable
   private AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab var1, Minecraft var2, Advancement var3, DisplayInfo var4) {
      this.tab = var1;
      this.advancement = var3;
      this.display = var4;
      this.minecraft = var2;
      this.title = Language.getInstance().getVisualOrder(var2.font.substrByWidth(var4.getTitle(), 163));
      this.x = Mth.floor(var4.getX() * 28.0F);
      this.y = Mth.floor(var4.getY() * 27.0F);
      int var5 = var3.getMaxCriteraRequired();
      int var6 = String.valueOf(var5).length();
      int var7 = var5 > 1 ? var2.font.width("  ") + var2.font.width("0") * var6 * 2 + var2.font.width("/") : 0;
      int var8 = 29 + var2.font.width(this.title) + var7;
      this.description = Language.getInstance().getVisualOrder(this.findOptimalLines(ComponentUtils.mergeStyles(var4.getDescription().copy(), Style.EMPTY.withColor(var4.getFrame().getChatColor())), var8));

      FormattedCharSequence var10;
      for(Iterator var9 = this.description.iterator(); var9.hasNext(); var8 = Math.max(var8, var2.font.width(var10))) {
         var10 = (FormattedCharSequence)var9.next();
      }

      this.width = var8 + 3 + 5;
   }

   private static float getMaxWidth(StringSplitter var0, List<FormattedText> var1) {
      Stream var10000 = var1.stream();
      Objects.requireNonNull(var0);
      return (float)var10000.mapToDouble(var0::stringWidth).max().orElse(0.0D);
   }

   private List<FormattedText> findOptimalLines(Component var1, int var2) {
      StringSplitter var3 = this.minecraft.font.getSplitter();
      List var4 = null;
      float var5 = Float.MAX_VALUE;
      int[] var6 = TEST_SPLIT_OFFSETS;
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         int var9 = var6[var8];
         List var10 = var3.splitLines((FormattedText)var1, var2 - var9, Style.EMPTY);
         float var11 = Math.abs(getMaxWidth(var3, var10) - (float)var2);
         if (var11 <= 10.0F) {
            return var10;
         }

         if (var11 < var5) {
            var5 = var11;
            var4 = var10;
         }
      }

      return var4;
   }

   @Nullable
   private AdvancementWidget getFirstVisibleParent(Advancement var1) {
      do {
         var1 = var1.getParent();
      } while(var1 != null && var1.getDisplay() == null);

      if (var1 != null && var1.getDisplay() != null) {
         return this.tab.getWidget(var1);
      } else {
         return null;
      }
   }

   public void drawConnectivity(GuiGraphics var1, int var2, int var3, boolean var4) {
      if (this.parent != null) {
         int var5 = var2 + this.parent.x + 13;
         int var6 = var2 + this.parent.x + 26 + 4;
         int var7 = var3 + this.parent.y + 13;
         int var8 = var2 + this.x + 13;
         int var9 = var3 + this.y + 13;
         int var10 = var4 ? -16777216 : -1;
         if (var4) {
            var1.hLine(var6, var5, var7 - 1, var10);
            var1.hLine(var6 + 1, var5, var7, var10);
            var1.hLine(var6, var5, var7 + 1, var10);
            var1.hLine(var8, var6 - 1, var9 - 1, var10);
            var1.hLine(var8, var6 - 1, var9, var10);
            var1.hLine(var8, var6 - 1, var9 + 1, var10);
            var1.vLine(var6 - 1, var9, var7, var10);
            var1.vLine(var6 + 1, var9, var7, var10);
         } else {
            var1.hLine(var6, var5, var7, var10);
            var1.hLine(var8, var6, var9, var10);
            var1.vLine(var6, var9, var7, var10);
         }
      }

      Iterator var11 = this.children.iterator();

      while(var11.hasNext()) {
         AdvancementWidget var12 = (AdvancementWidget)var11.next();
         var12.drawConnectivity(var1, var2, var3, var4);
      }

   }

   public void draw(GuiGraphics var1, int var2, int var3) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float var4 = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType var5;
         if (var4 >= 1.0F) {
            var5 = AdvancementWidgetType.OBTAINED;
         } else {
            var5 = AdvancementWidgetType.UNOBTAINED;
         }

         var1.blit(WIDGETS_LOCATION, var2 + this.x + 3, var3 + this.y, this.display.getFrame().getTexture(), 128 + var5.getIndex() * 26, 26, 26);
         var1.renderFakeItem(this.display.getIcon(), var2 + this.x + 8, var3 + this.y + 5);
      }

      Iterator var6 = this.children.iterator();

      while(var6.hasNext()) {
         AdvancementWidget var7 = (AdvancementWidget)var6.next();
         var7.draw(var1, var2, var3);
      }

   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress var1) {
      this.progress = var1;
   }

   public void addChild(AdvancementWidget var1) {
      this.children.add(var1);
   }

   public void drawHover(GuiGraphics var1, int var2, int var3, float var4, int var5, int var6) {
      boolean var7 = var5 + var2 + this.x + this.width + 26 >= this.tab.getScreen().width;
      String var8 = this.progress == null ? null : this.progress.getProgressText();
      int var9 = var8 == null ? 0 : this.minecraft.font.width(var8);
      int var10000 = 113 - var3 - this.y - 26;
      int var10002 = this.description.size();
      Objects.requireNonNull(this.minecraft.font);
      boolean var10 = var10000 <= 6 + var10002 * 9;
      float var11 = this.progress == null ? 0.0F : this.progress.getPercent();
      int var15 = Mth.floor(var11 * (float)this.width);
      AdvancementWidgetType var12;
      AdvancementWidgetType var13;
      AdvancementWidgetType var14;
      if (var11 >= 1.0F) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.OBTAINED;
         var14 = AdvancementWidgetType.OBTAINED;
      } else if (var15 < 2) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.UNOBTAINED;
         var13 = AdvancementWidgetType.UNOBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      } else if (var15 > this.width - 2) {
         var15 = this.width / 2;
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.OBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      } else {
         var12 = AdvancementWidgetType.OBTAINED;
         var13 = AdvancementWidgetType.UNOBTAINED;
         var14 = AdvancementWidgetType.UNOBTAINED;
      }

      int var16 = this.width - var15;
      RenderSystem.enableBlend();
      int var17 = var3 + this.y;
      int var18;
      if (var7) {
         var18 = var2 + this.x - this.width + 26 + 6;
      } else {
         var18 = var2 + this.x;
      }

      int var10001 = this.description.size();
      Objects.requireNonNull(this.minecraft.font);
      int var19 = 32 + var10001 * 9;
      if (!this.description.isEmpty()) {
         if (var10) {
            var1.blitNineSliced(WIDGETS_LOCATION, var18, var17 + 26 - var19, this.width, var19, 10, 200, 26, 0, 52);
         } else {
            var1.blitNineSliced(WIDGETS_LOCATION, var18, var17, this.width, var19, 10, 200, 26, 0, 52);
         }
      }

      var1.blit(WIDGETS_LOCATION, var18, var17, 0, var12.getIndex() * 26, var15, 26);
      var1.blit(WIDGETS_LOCATION, var18 + var15, var17, 200 - var16, var13.getIndex() * 26, var16, 26);
      var1.blit(WIDGETS_LOCATION, var2 + this.x + 3, var3 + this.y, this.display.getFrame().getTexture(), 128 + var14.getIndex() * 26, 26, 26);
      if (var7) {
         var1.drawString(this.minecraft.font, (FormattedCharSequence)this.title, var18 + 5, var3 + this.y + 9, -1);
         if (var8 != null) {
            var1.drawString(this.minecraft.font, (String)var8, var2 + this.x - var9, var3 + this.y + 9, -1);
         }
      } else {
         var1.drawString(this.minecraft.font, (FormattedCharSequence)this.title, var2 + this.x + 32, var3 + this.y + 9, -1);
         if (var8 != null) {
            var1.drawString(this.minecraft.font, (String)var8, var2 + this.x + this.width - var9 - 5, var3 + this.y + 9, -1);
         }
      }

      int var10003;
      int var20;
      int var10004;
      Font var21;
      FormattedCharSequence var22;
      if (var10) {
         for(var20 = 0; var20 < this.description.size(); ++var20) {
            var21 = this.minecraft.font;
            var22 = (FormattedCharSequence)this.description.get(var20);
            var10003 = var18 + 5;
            var10004 = var17 + 26 - var19 + 7;
            Objects.requireNonNull(this.minecraft.font);
            var1.drawString(var21, var22, var10003, var10004 + var20 * 9, -5592406, false);
         }
      } else {
         for(var20 = 0; var20 < this.description.size(); ++var20) {
            var21 = this.minecraft.font;
            var22 = (FormattedCharSequence)this.description.get(var20);
            var10003 = var18 + 5;
            var10004 = var3 + this.y + 9 + 17;
            Objects.requireNonNull(this.minecraft.font);
            var1.drawString(var21, var22, var10003, var10004 + var20 * 9, -5592406, false);
         }
      }

      var1.renderFakeItem(this.display.getIcon(), var2 + this.x + 8, var3 + this.y + 5);
   }

   public boolean isMouseOver(int var1, int var2, int var3, int var4) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int var5 = var1 + this.x;
         int var6 = var5 + 26;
         int var7 = var2 + this.y;
         int var8 = var7 + 26;
         return var3 >= var5 && var3 <= var6 && var4 >= var7 && var4 <= var8;
      } else {
         return false;
      }
   }

   public void attachToParent() {
      if (this.parent == null && this.advancement.getParent() != null) {
         this.parent = this.getFirstVisibleParent(this.advancement);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }

   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
