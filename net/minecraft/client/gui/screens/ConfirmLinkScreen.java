package net.minecraft.client.gui.screens;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ConfirmLinkScreen extends ConfirmScreen {
   private static final Component COPY_BUTTON_TEXT = Component.translatable("chat.copy");
   private static final Component WARNING_TEXT = Component.translatable("chat.link.warning");
   private final String url;
   private final boolean showWarning;

   public ConfirmLinkScreen(BooleanConsumer var1, String var2, boolean var3) {
      this(var1, confirmMessage(var3), Component.literal(var2), var2, var3 ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, var3);
   }

   public ConfirmLinkScreen(BooleanConsumer var1, Component var2, String var3, boolean var4) {
      this(var1, var2, var3, var4 ? CommonComponents.GUI_CANCEL : CommonComponents.GUI_NO, var4);
   }

   public ConfirmLinkScreen(BooleanConsumer var1, Component var2, String var3, Component var4, boolean var5) {
      this(var1, var2, confirmMessage(var5, var3), var3, var4, var5);
   }

   public ConfirmLinkScreen(BooleanConsumer var1, Component var2, Component var3, String var4, Component var5, boolean var6) {
      super(var1, var2, var3);
      this.yesButton = (Component)(var6 ? Component.translatable("chat.link.open") : CommonComponents.GUI_YES);
      this.noButton = var5;
      this.showWarning = !var6;
      this.url = var4;
   }

   protected static MutableComponent confirmMessage(boolean var0, String var1) {
      return confirmMessage(var0).append(CommonComponents.SPACE).append((Component)Component.literal(var1));
   }

   protected static MutableComponent confirmMessage(boolean var0) {
      return Component.translatable(var0 ? "chat.link.confirmTrusted" : "chat.link.confirm");
   }

   protected void addButtons(int var1) {
      this.addRenderableWidget(Button.builder(this.yesButton, (var1x) -> {
         this.callback.accept(true);
      }).bounds(this.width / 2 - 50 - 105, var1, 100, 20).build());
      this.addRenderableWidget(Button.builder(COPY_BUTTON_TEXT, (var1x) -> {
         this.copyToClipboard();
         this.callback.accept(false);
      }).bounds(this.width / 2 - 50, var1, 100, 20).build());
      this.addRenderableWidget(Button.builder(this.noButton, (var1x) -> {
         this.callback.accept(false);
      }).bounds(this.width / 2 - 50 + 105, var1, 100, 20).build());
   }

   public void copyToClipboard() {
      this.minecraft.keyboardHandler.setClipboard(this.url);
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      super.render(var1, var2, var3, var4);
      if (this.showWarning) {
         var1.drawCenteredString(this.font, (Component)WARNING_TEXT, this.width / 2, 110, 16764108);
      }

   }

   public static void confirmLinkNow(String var0, Screen var1, boolean var2) {
      Minecraft var3 = Minecraft.getInstance();
      var3.setScreen(new ConfirmLinkScreen((var3x) -> {
         if (var3x) {
            Util.getPlatform().openUri(var0);
         }

         var3.setScreen(var1);
      }, var0, var2));
   }

   public static Button.OnPress confirmLink(String var0, Screen var1, boolean var2) {
      return (var3) -> {
         confirmLinkNow(var0, var1, var2);
      };
   }
}
