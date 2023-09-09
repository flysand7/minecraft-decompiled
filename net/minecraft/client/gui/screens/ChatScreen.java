package net.minecraft.client.gui.screens;

import javax.annotation.Nullable;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class ChatScreen extends Screen {
   public static final double MOUSE_SCROLL_SPEED = 7.0D;
   private static final Component USAGE_TEXT = Component.translatable("chat_screen.usage");
   private static final int TOOLTIP_MAX_WIDTH = 210;
   private String historyBuffer = "";
   private int historyPos = -1;
   protected EditBox input;
   private String initial;
   CommandSuggestions commandSuggestions;

   public ChatScreen(String var1) {
      super(Component.translatable("chat_screen.title"));
      this.initial = var1;
   }

   protected void init() {
      this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
      this.input = new EditBox(this.minecraft.fontFilterFishy, 4, this.height - 12, this.width - 4, 12, Component.translatable("chat.editBox")) {
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.input.setMaxLength(256);
      this.input.setBordered(false);
      this.input.setValue(this.initial);
      this.input.setResponder(this::onEdited);
      this.input.setCanLoseFocus(false);
      this.addWidget(this.input);
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
      this.commandSuggestions.updateCommandInfo();
      this.setInitialFocus(this.input);
   }

   public void resize(Minecraft var1, int var2, int var3) {
      String var4 = this.input.getValue();
      this.init(var1, var2, var3);
      this.setChatLine(var4);
      this.commandSuggestions.updateCommandInfo();
   }

   public void removed() {
      this.minecraft.gui.getChat().resetChatScroll();
   }

   public void tick() {
      this.input.tick();
   }

   private void onEdited(String var1) {
      String var2 = this.input.getValue();
      this.commandSuggestions.setAllowSuggestions(!var2.equals(this.initial));
      this.commandSuggestions.updateCommandInfo();
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (this.commandSuggestions.keyPressed(var1, var2, var3)) {
         return true;
      } else if (super.keyPressed(var1, var2, var3)) {
         return true;
      } else if (var1 == 256) {
         this.minecraft.setScreen((Screen)null);
         return true;
      } else if (var1 != 257 && var1 != 335) {
         if (var1 == 265) {
            this.moveInHistory(-1);
            return true;
         } else if (var1 == 264) {
            this.moveInHistory(1);
            return true;
         } else if (var1 == 266) {
            this.minecraft.gui.getChat().scrollChat(this.minecraft.gui.getChat().getLinesPerPage() - 1);
            return true;
         } else if (var1 == 267) {
            this.minecraft.gui.getChat().scrollChat(-this.minecraft.gui.getChat().getLinesPerPage() + 1);
            return true;
         } else {
            return false;
         }
      } else {
         if (this.handleChatInput(this.input.getValue(), true)) {
            this.minecraft.setScreen((Screen)null);
         }

         return true;
      }
   }

   public boolean mouseScrolled(double var1, double var3, double var5) {
      var5 = Mth.clamp(var5, -1.0D, 1.0D);
      if (this.commandSuggestions.mouseScrolled(var5)) {
         return true;
      } else {
         if (!hasShiftDown()) {
            var5 *= 7.0D;
         }

         this.minecraft.gui.getChat().scrollChat((int)var5);
         return true;
      }
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (this.commandSuggestions.mouseClicked((double)((int)var1), (double)((int)var3), var5)) {
         return true;
      } else {
         if (var5 == 0) {
            ChatComponent var6 = this.minecraft.gui.getChat();
            if (var6.handleChatQueueClicked(var1, var3)) {
               return true;
            }

            Style var7 = this.getComponentStyleAt(var1, var3);
            if (var7 != null && this.handleComponentClicked(var7)) {
               this.initial = this.input.getValue();
               return true;
            }
         }

         return this.input.mouseClicked(var1, var3, var5) ? true : super.mouseClicked(var1, var3, var5);
      }
   }

   protected void insertText(String var1, boolean var2) {
      if (var2) {
         this.input.setValue(var1);
      } else {
         this.input.insertText(var1);
      }

   }

   public void moveInHistory(int var1) {
      int var2 = this.historyPos + var1;
      int var3 = this.minecraft.gui.getChat().getRecentChat().size();
      var2 = Mth.clamp(var2, 0, var3);
      if (var2 != this.historyPos) {
         if (var2 == var3) {
            this.historyPos = var3;
            this.input.setValue(this.historyBuffer);
         } else {
            if (this.historyPos == var3) {
               this.historyBuffer = this.input.getValue();
            }

            this.input.setValue((String)this.minecraft.gui.getChat().getRecentChat().get(var2));
            this.commandSuggestions.setAllowSuggestions(false);
            this.historyPos = var2;
         }
      }
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      var1.fill(2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
      this.input.render(var1, var2, var3, var4);
      super.render(var1, var2, var3, var4);
      this.commandSuggestions.render(var1, var2, var3);
      GuiMessageTag var5 = this.minecraft.gui.getChat().getMessageTagAt((double)var2, (double)var3);
      if (var5 != null && var5.text() != null) {
         var1.renderTooltip(this.font, this.font.split(var5.text(), 210), var2, var3);
      } else {
         Style var6 = this.getComponentStyleAt((double)var2, (double)var3);
         if (var6 != null && var6.getHoverEvent() != null) {
            var1.renderComponentHoverEffect(this.font, var6, var2, var3);
         }
      }

   }

   public boolean isPauseScreen() {
      return false;
   }

   private void setChatLine(String var1) {
      this.input.setValue(var1);
   }

   protected void updateNarrationState(NarrationElementOutput var1) {
      var1.add(NarratedElementType.TITLE, this.getTitle());
      var1.add(NarratedElementType.USAGE, USAGE_TEXT);
      String var2 = this.input.getValue();
      if (!var2.isEmpty()) {
         var1.nest().add(NarratedElementType.TITLE, (Component)Component.translatable("chat_screen.message", var2));
      }

   }

   @Nullable
   private Style getComponentStyleAt(double var1, double var3) {
      return this.minecraft.gui.getChat().getClickedComponentStyleAt(var1, var3);
   }

   public boolean handleChatInput(String var1, boolean var2) {
      var1 = this.normalizeChatMessage(var1);
      if (var1.isEmpty()) {
         return true;
      } else {
         if (var2) {
            this.minecraft.gui.getChat().addRecentChat(var1);
         }

         if (var1.startsWith("/")) {
            this.minecraft.player.connection.sendCommand(var1.substring(1));
         } else {
            this.minecraft.player.connection.sendChat(var1);
         }

         return true;
      }
   }

   public String normalizeChatMessage(String var1) {
      return StringUtil.trimChatMessage(StringUtils.normalizeSpace(var1.trim()));
   }
}
