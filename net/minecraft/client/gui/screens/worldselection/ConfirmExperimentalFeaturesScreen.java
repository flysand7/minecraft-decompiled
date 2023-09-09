package net.minecraft.client.gui.screens.worldselection;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.flag.FeatureFlags;

public class ConfirmExperimentalFeaturesScreen extends Screen {
   private static final Component TITLE = Component.translatable("selectWorld.experimental.title");
   private static final Component MESSAGE = Component.translatable("selectWorld.experimental.message");
   private static final Component DETAILS_BUTTON = Component.translatable("selectWorld.experimental.details");
   private static final int COLUMN_SPACING = 10;
   private static final int DETAILS_BUTTON_WIDTH = 100;
   private final BooleanConsumer callback;
   final Collection<Pack> enabledPacks;
   private final GridLayout layout = (new GridLayout()).columnSpacing(10).rowSpacing(20);

   public ConfirmExperimentalFeaturesScreen(Collection<Pack> var1, BooleanConsumer var2) {
      super(TITLE);
      this.enabledPacks = var1;
      this.callback = var2;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), MESSAGE);
   }

   protected void init() {
      super.init();
      GridLayout.RowHelper var1 = this.layout.createRowHelper(2);
      LayoutSettings var2 = var1.newCellSettings().alignHorizontallyCenter();
      var1.addChild(new StringWidget(this.title, this.font), 2, var2);
      MultiLineTextWidget var3 = (MultiLineTextWidget)var1.addChild((new MultiLineTextWidget(MESSAGE, this.font)).setCentered(true), 2, var2);
      var3.setMaxWidth(310);
      var1.addChild(Button.builder(DETAILS_BUTTON, (var1x) -> {
         this.minecraft.setScreen(new ConfirmExperimentalFeaturesScreen.DetailsScreen());
      }).width(100).build(), 2, var2);
      var1.addChild(Button.builder(CommonComponents.GUI_PROCEED, (var1x) -> {
         this.callback.accept(true);
      }).build());
      var1.addChild(Button.builder(CommonComponents.GUI_BACK, (var1x) -> {
         this.callback.accept(false);
      }).build());
      this.layout.visitWidgets((var1x) -> {
         AbstractWidget var10000 = (AbstractWidget)this.addRenderableWidget(var1x);
      });
      this.layout.arrangeElements();
      this.repositionElements();
   }

   protected void repositionElements() {
      FrameLayout.alignInRectangle(this.layout, 0, 0, this.width, this.height, 0.5F, 0.5F);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      super.render(var1, var2, var3, var4);
   }

   public void onClose() {
      this.callback.accept(false);
   }

   private class DetailsScreen extends Screen {
      private ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList packList;

      DetailsScreen() {
         super(Component.translatable("selectWorld.experimental.details.title"));
      }

      public void onClose() {
         this.minecraft.setScreen(ConfirmExperimentalFeaturesScreen.this);
      }

      protected void init() {
         super.init();
         this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (var1) -> {
            this.onClose();
         }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 24, 200, 20).build());
         this.packList = new ConfirmExperimentalFeaturesScreen.DetailsScreen.PackList(this.minecraft, ConfirmExperimentalFeaturesScreen.this.enabledPacks);
         this.addWidget(this.packList);
      }

      public void render(GuiGraphics var1, int var2, int var3, float var4) {
         this.renderBackground(var1);
         this.packList.render(var1, var2, var3, var4);
         var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 10, 16777215);
         super.render(var1, var2, var3, var4);
      }

      class PackList extends ObjectSelectionList<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
         public PackList(Minecraft var2, Collection<Pack> var3) {
            int var10002 = DetailsScreen.this.width;
            int var10003 = DetailsScreen.this.height;
            int var10005 = DetailsScreen.this.height - 64;
            Objects.requireNonNull(var2.font);
            super(var2, var10002, var10003, 32, var10005, (9 + 2) * 3);
            Iterator var4 = var3.iterator();

            while(var4.hasNext()) {
               Pack var5 = (Pack)var4.next();
               String var6 = FeatureFlags.printMissingFlags(FeatureFlags.VANILLA_SET, var5.getRequestedFeatures());
               if (!var6.isEmpty()) {
                  MutableComponent var7 = ComponentUtils.mergeStyles(var5.getTitle().copy(), Style.EMPTY.withBold(true));
                  MutableComponent var8 = Component.translatable("selectWorld.experimental.details.entry", var6);
                  this.addEntry(DetailsScreen.this.new PackListEntry(var7, var8, MultiLineLabel.create(DetailsScreen.this.font, var8, this.getRowWidth())));
               }
            }

         }

         public int getRowWidth() {
            return this.width * 3 / 4;
         }
      }

      private class PackListEntry extends ObjectSelectionList.Entry<ConfirmExperimentalFeaturesScreen.DetailsScreen.PackListEntry> {
         private final Component packId;
         private final Component message;
         private final MultiLineLabel splitMessage;

         PackListEntry(Component var2, Component var3, MultiLineLabel var4) {
            this.packId = var2;
            this.message = var3;
            this.splitMessage = var4;
         }

         public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            var1.drawString(DetailsScreen.this.minecraft.font, this.packId, var4, var3, 16777215);
            MultiLineLabel var10000 = this.splitMessage;
            int var10003 = var3 + 12;
            Objects.requireNonNull(DetailsScreen.this.font);
            var10000.renderLeftAligned(var1, var4, var10003, 9, 16777215);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", CommonComponents.joinForNarration(this.packId, this.message));
         }
      }
   }
}
