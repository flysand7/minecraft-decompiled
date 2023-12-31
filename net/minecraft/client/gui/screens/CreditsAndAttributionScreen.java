package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class CreditsAndAttributionScreen extends Screen {
   private static final int BUTTON_SPACING = 8;
   private static final int BUTTON_WIDTH = 210;
   private static final Component TITLE = Component.translatable("credits_and_attribution.screen.title");
   private static final Component CREDITS_BUTTON = Component.translatable("credits_and_attribution.button.credits");
   private static final Component ATTRIBUTION_BUTTON = Component.translatable("credits_and_attribution.button.attribution");
   private static final Component LICENSES_BUTTON = Component.translatable("credits_and_attribution.button.licenses");
   private final Screen lastScreen;
   private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

   public CreditsAndAttributionScreen(Screen var1) {
      super(TITLE);
      this.lastScreen = var1;
   }

   protected void init() {
      this.layout.addToHeader(new StringWidget(this.getTitle(), this.font));
      GridLayout var1 = ((GridLayout)this.layout.addToContents(new GridLayout())).spacing(8);
      var1.defaultCellSetting().alignHorizontallyCenter();
      GridLayout.RowHelper var2 = var1.createRowHelper(1);
      var2.addChild(Button.builder(CREDITS_BUTTON, (var1x) -> {
         this.openCreditsScreen();
      }).width(210).build());
      var2.addChild(Button.builder(ATTRIBUTION_BUTTON, ConfirmLinkScreen.confirmLink("https://aka.ms/MinecraftJavaAttribution", this, true)).width(210).build());
      var2.addChild(Button.builder(LICENSES_BUTTON, ConfirmLinkScreen.confirmLink("https://aka.ms/MinecraftJavaLicenses", this, true)).width(210).build());
      this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, (var1x) -> {
         this.onClose();
      }).build());
      this.layout.arrangeElements();
      this.layout.visitWidgets(this::addRenderableWidget);
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
   }

   private void openCreditsScreen() {
      this.minecraft.setScreen(new WinScreen(false, () -> {
         this.minecraft.setScreen(this);
      }));
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      super.render(var1, var2, var3, var4);
   }
}
