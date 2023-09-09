package com.mojang.realmsclient.gui.screens;

import com.mojang.realmsclient.client.RealmsError;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.util.Objects;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;

public class RealmsGenericErrorScreen extends RealmsScreen {
   private final Screen nextScreen;
   private final RealmsGenericErrorScreen.ErrorMessage lines;
   private MultiLineLabel line2Split;

   public RealmsGenericErrorScreen(RealmsServiceException var1, Screen var2) {
      super(GameNarrator.NO_TITLE);
      this.line2Split = MultiLineLabel.EMPTY;
      this.nextScreen = var2;
      this.lines = errorMessage(var1);
   }

   public RealmsGenericErrorScreen(Component var1, Screen var2) {
      super(GameNarrator.NO_TITLE);
      this.line2Split = MultiLineLabel.EMPTY;
      this.nextScreen = var2;
      this.lines = errorMessage(var1);
   }

   public RealmsGenericErrorScreen(Component var1, Component var2, Screen var3) {
      super(GameNarrator.NO_TITLE);
      this.line2Split = MultiLineLabel.EMPTY;
      this.nextScreen = var3;
      this.lines = errorMessage(var1, var2);
   }

   private static RealmsGenericErrorScreen.ErrorMessage errorMessage(RealmsServiceException var0) {
      RealmsError var1 = var0.realmsError;
      if (var1 == null) {
         return errorMessage(Component.translatable("mco.errorMessage.realmsService", var0.httpResultCode), Component.literal(var0.rawResponse));
      } else {
         int var2 = var1.getErrorCode();
         String var3 = "mco.errorMessage." + var2;
         return errorMessage(Component.translatable("mco.errorMessage.realmsService.realmsError", var2), (Component)(I18n.exists(var3) ? Component.translatable(var3) : Component.nullToEmpty(var1.getErrorMessage())));
      }
   }

   private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component var0) {
      return errorMessage(Component.translatable("mco.errorMessage.generic"), var0);
   }

   private static RealmsGenericErrorScreen.ErrorMessage errorMessage(Component var0, Component var1) {
      return new RealmsGenericErrorScreen.ErrorMessage(var0, var1);
   }

   public void init() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_OK, (var1) -> {
         this.minecraft.setScreen(this.nextScreen);
      }).bounds(this.width / 2 - 100, this.height - 52, 200, 20).build());
      this.line2Split = MultiLineLabel.create(this.font, this.lines.detail, this.width * 3 / 4);
   }

   public Component getNarrationMessage() {
      return Component.empty().append(this.lines.title).append(": ").append(this.lines.detail);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      var1.drawCenteredString(this.font, (Component)this.lines.title, this.width / 2, 80, 16777215);
      MultiLineLabel var10000 = this.line2Split;
      int var10002 = this.width / 2;
      Objects.requireNonNull(this.minecraft.font);
      var10000.renderCentered(var1, var10002, 100, 9, 16711680);
      super.render(var1, var2, var3, var4);
   }

   private static record ErrorMessage(Component a, Component b) {
      final Component title;
      final Component detail;

      ErrorMessage(Component var1, Component var2) {
         this.title = var1;
         this.detail = var2;
      }

      public Component title() {
         return this.title;
      }

      public Component detail() {
         return this.detail;
      }
   }
}
