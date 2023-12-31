package net.minecraft.client.gui.screens.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TelemetryEventWidget extends AbstractScrollWidget {
   private static final int HEADER_HORIZONTAL_PADDING = 32;
   private static final String TELEMETRY_REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
   private static final String TELEMETRY_OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
   private static final Component PROPERTY_TITLE;
   private final Font font;
   private TelemetryEventWidget.Content content;
   @Nullable
   private DoubleConsumer onScrolledListener;

   public TelemetryEventWidget(int var1, int var2, int var3, int var4, Font var5) {
      super(var1, var2, var3, var4, Component.empty());
      this.font = var5;
      this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
   }

   public void onOptInChanged(boolean var1) {
      this.content = this.buildContent(var1);
      this.setScrollAmount(this.scrollAmount());
   }

   private TelemetryEventWidget.Content buildContent(boolean var1) {
      TelemetryEventWidget.ContentBuilder var2 = new TelemetryEventWidget.ContentBuilder(this.containerWidth());
      ArrayList var3 = new ArrayList(TelemetryEventType.values());
      var3.sort(Comparator.comparing(TelemetryEventType::isOptIn));
      if (!var1) {
         var3.removeIf(TelemetryEventType::isOptIn);
      }

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         TelemetryEventType var5 = (TelemetryEventType)var3.get(var4);
         this.addEventType(var2, var5);
         if (var4 < var3.size() - 1) {
            Objects.requireNonNull(this.font);
            var2.addSpacer(9);
         }
      }

      return var2.build();
   }

   public void setOnScrolledListener(@Nullable DoubleConsumer var1) {
      this.onScrolledListener = var1;
   }

   protected void setScrollAmount(double var1) {
      super.setScrollAmount(var1);
      if (this.onScrolledListener != null) {
         this.onScrolledListener.accept(this.scrollAmount());
      }

   }

   protected int getInnerHeight() {
      return this.content.container().getHeight();
   }

   protected double scrollRate() {
      Objects.requireNonNull(this.font);
      return 9.0D;
   }

   protected void renderContents(GuiGraphics var1, int var2, int var3, float var4) {
      int var5 = this.getY() + this.innerPadding();
      int var6 = this.getX() + this.innerPadding();
      var1.pose().pushPose();
      var1.pose().translate((double)var6, (double)var5, 0.0D);
      this.content.container().visitWidgets((var4x) -> {
         var4x.render(var1, var2, var3, var4);
      });
      var1.pose().popPose();
   }

   protected void updateWidgetNarration(NarrationElementOutput var1) {
      var1.add(NarratedElementType.TITLE, this.content.narration());
   }

   private void addEventType(TelemetryEventWidget.ContentBuilder var1, TelemetryEventType var2) {
      String var3 = var2.isOptIn() ? "telemetry.event.optional" : "telemetry.event.required";
      var1.addHeader(this.font, Component.translatable(var3, var2.title()));
      var1.addHeader(this.font, var2.description().withStyle(ChatFormatting.GRAY));
      Objects.requireNonNull(this.font);
      var1.addSpacer(9 / 2);
      var1.addLine(this.font, PROPERTY_TITLE, 2);
      this.addEventTypeProperties(var2, var1);
   }

   private void addEventTypeProperties(TelemetryEventType var1, TelemetryEventWidget.ContentBuilder var2) {
      Iterator var3 = var1.properties().iterator();

      while(var3.hasNext()) {
         TelemetryProperty var4 = (TelemetryProperty)var3.next();
         var2.addLine(this.font, var4.title());
      }

   }

   private int containerWidth() {
      return this.width - this.totalInnerPadding();
   }

   static {
      PROPERTY_TITLE = Component.translatable("telemetry_info.property_title").withStyle(ChatFormatting.UNDERLINE);
   }

   private static record Content(GridLayout a, Component b) {
      private final GridLayout container;
      private final Component narration;

      Content(GridLayout var1, Component var2) {
         this.container = var1;
         this.narration = var2;
      }

      public GridLayout container() {
         return this.container;
      }

      public Component narration() {
         return this.narration;
      }
   }

   private static class ContentBuilder {
      private final int width;
      private final GridLayout grid;
      private final GridLayout.RowHelper helper;
      private final LayoutSettings alignHeader;
      private final MutableComponent narration = Component.empty();

      public ContentBuilder(int var1) {
         this.width = var1;
         this.grid = new GridLayout();
         this.grid.defaultCellSetting().alignHorizontallyLeft();
         this.helper = this.grid.createRowHelper(1);
         this.helper.addChild(SpacerElement.width(var1));
         this.alignHeader = this.helper.newCellSettings().alignHorizontallyCenter().paddingHorizontal(32);
      }

      public void addLine(Font var1, Component var2) {
         this.addLine(var1, var2, 0);
      }

      public void addLine(Font var1, Component var2, int var3) {
         this.helper.addChild((new MultiLineTextWidget(var2, var1)).setMaxWidth(this.width), this.helper.newCellSettings().paddingBottom(var3));
         this.narration.append(var2).append("\n");
      }

      public void addHeader(Font var1, Component var2) {
         this.helper.addChild((new MultiLineTextWidget(var2, var1)).setMaxWidth(this.width - 64).setCentered(true), this.alignHeader);
         this.narration.append(var2).append("\n");
      }

      public void addSpacer(int var1) {
         this.helper.addChild(SpacerElement.height(var1));
      }

      public TelemetryEventWidget.Content build() {
         this.grid.arrangeElements();
         return new TelemetryEventWidget.Content(this.grid, this.narration);
      }
   }
}
