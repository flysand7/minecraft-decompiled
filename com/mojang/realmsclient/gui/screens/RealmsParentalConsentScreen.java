package com.mojang.realmsclient.gui.screens;

import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.realms.RealmsScreen;

public class RealmsParentalConsentScreen extends RealmsScreen {
   private static final Component MESSAGE = Component.translatable("mco.account.privacyinfo");
   private final Screen nextScreen;
   private MultiLineLabel messageLines;

   public RealmsParentalConsentScreen(Screen var1) {
      super(GameNarrator.NO_TITLE);
      this.messageLines = MultiLineLabel.EMPTY;
      this.nextScreen = var1;
   }

   public void init() {
      MutableComponent var1 = Component.translatable("mco.account.update");
      Component var2 = CommonComponents.GUI_BACK;
      int var3 = Math.max(this.font.width((FormattedText)var1), this.font.width((FormattedText)var2)) + 30;
      MutableComponent var4 = Component.translatable("mco.account.privacy.info");
      int var5 = (int)((double)this.font.width((FormattedText)var4) * 1.2D);
      this.addRenderableWidget(Button.builder(var4, (var0) -> {
         Util.getPlatform().openUri("https://aka.ms/MinecraftGDPR");
      }).bounds(this.width / 2 - var5 / 2, row(11), var5, 20).build());
      this.addRenderableWidget(Button.builder(var1, (var0) -> {
         Util.getPlatform().openUri("https://aka.ms/UpdateMojangAccount");
      }).bounds(this.width / 2 - (var3 + 5), row(13), var3, 20).build());
      this.addRenderableWidget(Button.builder(var2, (var1x) -> {
         this.minecraft.setScreen(this.nextScreen);
      }).bounds(this.width / 2 + 5, row(13), var3, 20).build());
      this.messageLines = MultiLineLabel.create(this.font, MESSAGE, (int)Math.round((double)this.width * 0.9D));
   }

   public Component getNarrationMessage() {
      return MESSAGE;
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.messageLines.renderCentered(var1, this.width / 2, 15, 15, 16777215);
      super.render(var1, var2, var3, var4);
   }
}
