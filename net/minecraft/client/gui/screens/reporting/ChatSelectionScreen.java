package net.minecraft.client.gui.screens.reporting;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.ChatTrustLevel;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.client.multiplayer.chat.report.ChatReportBuilder;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class ChatSelectionScreen extends Screen {
   private static final Component TITLE = Component.translatable("gui.chatSelection.title");
   private static final Component CONTEXT_INFO;
   @Nullable
   private final Screen lastScreen;
   private final ReportingContext reportingContext;
   private Button confirmSelectedButton;
   private MultiLineLabel contextInfoLabel;
   @Nullable
   private ChatSelectionScreen.ChatSelectionList chatSelectionList;
   final ChatReportBuilder report;
   private final Consumer<ChatReportBuilder> onSelected;
   private ChatSelectionLogFiller chatLogFiller;

   public ChatSelectionScreen(@Nullable Screen var1, ReportingContext var2, ChatReportBuilder var3, Consumer<ChatReportBuilder> var4) {
      super(TITLE);
      this.lastScreen = var1;
      this.reportingContext = var2;
      this.report = var3.copy();
      this.onSelected = var4;
   }

   protected void init() {
      this.chatLogFiller = new ChatSelectionLogFiller(this.reportingContext, this::canReport);
      this.contextInfoLabel = MultiLineLabel.create(this.font, CONTEXT_INFO, this.width - 16);
      Minecraft var10004 = this.minecraft;
      int var10005 = this.contextInfoLabel.getLineCount() + 1;
      Objects.requireNonNull(this.font);
      this.chatSelectionList = new ChatSelectionScreen.ChatSelectionList(var10004, var10005 * 9);
      this.chatSelectionList.setRenderBackground(false);
      this.addWidget(this.chatSelectionList);
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (var1) -> {
         this.onClose();
      }).bounds(this.width / 2 - 155, this.height - 32, 150, 20).build());
      this.confirmSelectedButton = (Button)this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (var1) -> {
         this.onSelected.accept(this.report);
         this.onClose();
      }).bounds(this.width / 2 - 155 + 160, this.height - 32, 150, 20).build());
      this.updateConfirmSelectedButton();
      this.extendLog();
      this.chatSelectionList.setScrollAmount((double)this.chatSelectionList.getMaxScroll());
   }

   private boolean canReport(LoggedChatMessage var1) {
      return var1.canReport(this.report.reportedProfileId());
   }

   private void extendLog() {
      int var1 = this.chatSelectionList.getMaxVisibleEntries();
      this.chatLogFiller.fillNextPage(var1, this.chatSelectionList);
   }

   void onReachedScrollTop() {
      this.extendLog();
   }

   void updateConfirmSelectedButton() {
      this.confirmSelectedButton.active = !this.report.reportedMessages().isEmpty();
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.chatSelectionList.render(var1, var2, var3, var4);
      var1.drawCenteredString(this.font, (Component)this.title, this.width / 2, 16, 16777215);
      AbuseReportLimits var5 = this.reportingContext.sender().reportLimits();
      int var6 = this.report.reportedMessages().size();
      int var7 = var5.maxReportedMessageCount();
      MutableComponent var8 = Component.translatable("gui.chatSelection.selected", var6, var7);
      Font var10001 = this.font;
      int var10003 = this.width / 2;
      Objects.requireNonNull(this.font);
      var1.drawCenteredString(var10001, (Component)var8, var10003, 16 + 9 * 3 / 2, 10526880);
      this.contextInfoLabel.renderCentered(var1, this.width / 2, this.chatSelectionList.getFooterTop());
      super.render(var1, var2, var3, var4);
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), CONTEXT_INFO);
   }

   static {
      CONTEXT_INFO = Component.translatable("gui.chatSelection.context").withStyle(ChatFormatting.GRAY);
   }

   public class ChatSelectionList extends ObjectSelectionList<ChatSelectionScreen.ChatSelectionList.Entry> implements ChatSelectionLogFiller.Output {
      @Nullable
      private ChatSelectionScreen.ChatSelectionList.Heading previousHeading;

      public ChatSelectionList(Minecraft var2, int var3) {
         super(var2, ChatSelectionScreen.this.width, ChatSelectionScreen.this.height, 40, ChatSelectionScreen.this.height - 40 - var3, 16);
      }

      public void setScrollAmount(double var1) {
         double var3 = this.getScrollAmount();
         super.setScrollAmount(var1);
         if ((float)this.getMaxScroll() > 1.0E-5F && var1 <= 9.999999747378752E-6D && !Mth.equal(var1, var3)) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }

      }

      public void acceptMessage(int var1, LoggedChatMessage.Player var2) {
         boolean var3 = var2.canReport(ChatSelectionScreen.this.report.reportedProfileId());
         ChatTrustLevel var4 = var2.trustLevel();
         GuiMessageTag var5 = var4.createTag(var2.message());
         ChatSelectionScreen.ChatSelectionList.MessageEntry var6 = new ChatSelectionScreen.ChatSelectionList.MessageEntry(var1, var2.toContentComponent(), var2.toNarrationComponent(), var5, var3, true);
         this.addEntryToTop(var6);
         this.updateHeading(var2, var3);
      }

      private void updateHeading(LoggedChatMessage.Player var1, boolean var2) {
         ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry var3 = new ChatSelectionScreen.ChatSelectionList.MessageHeadingEntry(var1.profile(), var1.toHeadingComponent(), var2);
         this.addEntryToTop(var3);
         ChatSelectionScreen.ChatSelectionList.Heading var4 = new ChatSelectionScreen.ChatSelectionList.Heading(var1.profileId(), var3);
         if (this.previousHeading != null && this.previousHeading.canCombine(var4)) {
            this.removeEntryFromTop(this.previousHeading.entry());
         }

         this.previousHeading = var4;
      }

      public void acceptDivider(Component var1) {
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.DividerEntry(var1));
         this.addEntryToTop(new ChatSelectionScreen.ChatSelectionList.PaddingEntry());
         this.previousHeading = null;
      }

      protected int getScrollbarPosition() {
         return (this.width + this.getRowWidth()) / 2;
      }

      public int getRowWidth() {
         return Math.min(350, this.width - 50);
      }

      public int getMaxVisibleEntries() {
         return Mth.positiveCeilDiv(this.y1 - this.y0, this.itemHeight);
      }

      protected void renderItem(GuiGraphics var1, int var2, int var3, float var4, int var5, int var6, int var7, int var8, int var9) {
         ChatSelectionScreen.ChatSelectionList.Entry var10 = (ChatSelectionScreen.ChatSelectionList.Entry)this.getEntry(var5);
         if (this.shouldHighlightEntry(var10)) {
            boolean var11 = this.getSelected() == var10;
            int var12 = this.isFocused() && var11 ? -1 : -8355712;
            this.renderSelection(var1, var7, var8, var9, var12, -16777216);
         }

         var10.render(var1, var5, var7, var6, var8, var9, var2, var3, this.getHovered() == var10, var4);
      }

      private boolean shouldHighlightEntry(ChatSelectionScreen.ChatSelectionList.Entry var1) {
         if (var1.canSelect()) {
            boolean var2 = this.getSelected() == var1;
            boolean var3 = this.getSelected() == null;
            boolean var4 = this.getHovered() == var1;
            return var2 || var3 && var4 && var1.canReport();
         } else {
            return false;
         }
      }

      @Nullable
      protected ChatSelectionScreen.ChatSelectionList.Entry nextEntry(ScreenDirection var1) {
         return (ChatSelectionScreen.ChatSelectionList.Entry)this.nextEntry(var1, ChatSelectionScreen.ChatSelectionList.Entry::canSelect);
      }

      public void setSelected(@Nullable ChatSelectionScreen.ChatSelectionList.Entry var1) {
         super.setSelected(var1);
         ChatSelectionScreen.ChatSelectionList.Entry var2 = this.nextEntry(ScreenDirection.UP);
         if (var2 == null) {
            ChatSelectionScreen.this.onReachedScrollTop();
         }

      }

      public boolean keyPressed(int var1, int var2, int var3) {
         ChatSelectionScreen.ChatSelectionList.Entry var4 = (ChatSelectionScreen.ChatSelectionList.Entry)this.getSelected();
         return var4 != null && var4.keyPressed(var1, var2, var3) ? true : super.keyPressed(var1, var2, var3);
      }

      public int getFooterTop() {
         int var10000 = this.y1;
         Objects.requireNonNull(ChatSelectionScreen.this.font);
         return var10000 + 9;
      }

      // $FF: synthetic method
      @Nullable
      protected AbstractSelectionList.Entry nextEntry(ScreenDirection var1) {
         return this.nextEntry(var1);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void setSelected(@Nullable AbstractSelectionList.Entry var1) {
         this.setSelected((ChatSelectionScreen.ChatSelectionList.Entry)var1);
      }

      public class MessageEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final ResourceLocation CHECKMARK_TEXTURE = new ResourceLocation("minecraft", "textures/gui/checkmark.png");
         private static final int CHECKMARK_WIDTH = 9;
         private static final int CHECKMARK_HEIGHT = 8;
         private static final int INDENT_AMOUNT = 11;
         private static final int TAG_MARGIN_LEFT = 4;
         private final int chatId;
         private final FormattedText text;
         private final Component narration;
         @Nullable
         private final List<FormattedCharSequence> hoverText;
         @Nullable
         private final GuiMessageTag.Icon tagIcon;
         @Nullable
         private final List<FormattedCharSequence> tagHoverText;
         private final boolean canReport;
         private final boolean playerMessage;

         public MessageEntry(int var2, Component var3, Component var4, @Nullable GuiMessageTag var5, boolean var6, boolean var7) {
            super();
            this.chatId = var2;
            this.tagIcon = (GuiMessageTag.Icon)Optionull.map(var5, GuiMessageTag::icon);
            this.tagHoverText = var5 != null && var5.text() != null ? ChatSelectionScreen.this.font.split(var5.text(), ChatSelectionList.this.getRowWidth()) : null;
            this.canReport = var6;
            this.playerMessage = var7;
            FormattedText var8 = ChatSelectionScreen.this.font.substrByWidth(var3, this.getMaximumTextWidth() - ChatSelectionScreen.this.font.width((FormattedText)CommonComponents.ELLIPSIS));
            if (var3 != var8) {
               this.text = FormattedText.composite(var8, CommonComponents.ELLIPSIS);
               this.hoverText = ChatSelectionScreen.this.font.split(var3, ChatSelectionList.this.getRowWidth());
            } else {
               this.text = var3;
               this.hoverText = null;
            }

            this.narration = var4;
         }

         public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            if (this.isSelected() && this.canReport) {
               this.renderSelectedCheckmark(var1, var3, var4, var6);
            }

            int var11 = var4 + this.getTextIndent();
            int var10000 = var3 + 1;
            Objects.requireNonNull(ChatSelectionScreen.this.font);
            int var12 = var10000 + (var6 - 9) / 2;
            var1.drawString(ChatSelectionScreen.this.font, Language.getInstance().getVisualOrder(this.text), var11, var12, this.canReport ? -1 : -1593835521);
            if (this.hoverText != null && var9) {
               ChatSelectionScreen.this.setTooltipForNextRenderPass(this.hoverText);
            }

            int var13 = ChatSelectionScreen.this.font.width(this.text);
            this.renderTag(var1, var11 + var13 + 4, var3, var6, var7, var8);
         }

         private void renderTag(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6) {
            if (this.tagIcon != null) {
               int var7 = var3 + (var4 - this.tagIcon.height) / 2;
               this.tagIcon.draw(var1, var2, var7);
               if (this.tagHoverText != null && var5 >= var2 && var5 <= var2 + this.tagIcon.width && var6 >= var7 && var6 <= var7 + this.tagIcon.height) {
                  ChatSelectionScreen.this.setTooltipForNextRenderPass(this.tagHoverText);
               }
            }

         }

         private void renderSelectedCheckmark(GuiGraphics var1, int var2, int var3, int var4) {
            int var6 = var2 + (var4 - 8) / 2;
            RenderSystem.enableBlend();
            var1.blit(CHECKMARK_TEXTURE, var3, var6, 0.0F, 0.0F, 9, 8, 9, 8);
            RenderSystem.disableBlend();
         }

         private int getMaximumTextWidth() {
            int var1 = this.tagIcon != null ? this.tagIcon.width + 4 : 0;
            return ChatSelectionList.this.getRowWidth() - this.getTextIndent() - 4 - var1;
         }

         private int getTextIndent() {
            return this.playerMessage ? 11 : 0;
         }

         public Component getNarration() {
            return (Component)(this.isSelected() ? Component.translatable("narrator.select", this.narration) : this.narration);
         }

         public boolean mouseClicked(double var1, double var3, int var5) {
            if (var5 == 0) {
               ChatSelectionList.this.setSelected((ChatSelectionScreen.ChatSelectionList.Entry)null);
               return this.toggleReport();
            } else {
               return false;
            }
         }

         public boolean keyPressed(int var1, int var2, int var3) {
            return CommonInputs.selected(var1) ? this.toggleReport() : false;
         }

         public boolean isSelected() {
            return ChatSelectionScreen.this.report.isReported(this.chatId);
         }

         public boolean canSelect() {
            return true;
         }

         public boolean canReport() {
            return this.canReport;
         }

         private boolean toggleReport() {
            if (this.canReport) {
               ChatSelectionScreen.this.report.toggleReported(this.chatId);
               ChatSelectionScreen.this.updateConfirmSelectedButton();
               return true;
            } else {
               return false;
            }
         }
      }

      public class MessageHeadingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int FACE_SIZE = 12;
         private final Component heading;
         private final ResourceLocation skin;
         private final boolean canReport;

         public MessageHeadingEntry(GameProfile var2, Component var3, boolean var4) {
            super();
            this.heading = var3;
            this.canReport = var4;
            this.skin = ChatSelectionList.this.minecraft.getSkinManager().getInsecureSkinLocation(var2);
         }

         public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            int var11 = var4 - 12 - 4;
            int var12 = var3 + (var6 - 12) / 2;
            PlayerFaceRenderer.draw(var1, this.skin, var11, var12, 12);
            int var10000 = var3 + 1;
            Objects.requireNonNull(ChatSelectionScreen.this.font);
            int var13 = var10000 + (var6 - 9) / 2;
            var1.drawString(ChatSelectionScreen.this.font, this.heading, var4, var13, this.canReport ? -1 : -1593835521);
         }
      }

      private static record Heading(UUID a, ChatSelectionScreen.ChatSelectionList.Entry b) {
         private final UUID sender;
         private final ChatSelectionScreen.ChatSelectionList.Entry entry;

         Heading(UUID var1, ChatSelectionScreen.ChatSelectionList.Entry var2) {
            this.sender = var1;
            this.entry = var2;
         }

         public boolean canCombine(ChatSelectionScreen.ChatSelectionList.Heading var1) {
            return var1.sender.equals(this.sender);
         }

         public UUID sender() {
            return this.sender;
         }

         public ChatSelectionScreen.ChatSelectionList.Entry entry() {
            return this.entry;
         }
      }

      public abstract class Entry extends ObjectSelectionList.Entry<ChatSelectionScreen.ChatSelectionList.Entry> {
         public Entry() {
         }

         public Component getNarration() {
            return CommonComponents.EMPTY;
         }

         public boolean isSelected() {
            return false;
         }

         public boolean canSelect() {
            return false;
         }

         public boolean canReport() {
            return this.canSelect();
         }
      }

      public class PaddingEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         public PaddingEntry() {
            super();
         }

         public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         }
      }

      public class DividerEntry extends ChatSelectionScreen.ChatSelectionList.Entry {
         private static final int COLOR = -6250336;
         private final Component text;

         public DividerEntry(Component var2) {
            super();
            this.text = var2;
         }

         public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
            int var11 = var3 + var6 / 2;
            int var12 = var4 + var5 - 8;
            int var13 = ChatSelectionScreen.this.font.width((FormattedText)this.text);
            int var14 = (var4 + var12 - var13) / 2;
            Objects.requireNonNull(ChatSelectionScreen.this.font);
            int var15 = var11 - 9 / 2;
            var1.drawString(ChatSelectionScreen.this.font, this.text, var14, var15, -6250336);
         }

         public Component getNarration() {
            return this.text;
         }
      }
   }
}
