package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

public class BookEditScreen extends Screen {
   private static final int TEXT_WIDTH = 114;
   private static final int TEXT_HEIGHT = 128;
   private static final int IMAGE_WIDTH = 192;
   private static final int IMAGE_HEIGHT = 192;
   private static final Component EDIT_TITLE_LABEL = Component.translatable("book.editTitle");
   private static final Component FINALIZE_WARNING_LABEL = Component.translatable("book.finalizeWarning");
   private static final FormattedCharSequence BLACK_CURSOR;
   private static final FormattedCharSequence GRAY_CURSOR;
   private final Player owner;
   private final ItemStack book;
   private boolean isModified;
   private boolean isSigning;
   private int frameTick;
   private int currentPage;
   private final List<String> pages = Lists.newArrayList();
   private String title = "";
   private final TextFieldHelper pageEdit = new TextFieldHelper(this::getCurrentPageText, this::setCurrentPageText, this::getClipboard, this::setClipboard, (var1x) -> {
      return var1x.length() < 1024 && this.font.wordWrapHeight((String)var1x, 114) <= 128;
   });
   private final TextFieldHelper titleEdit = new TextFieldHelper(() -> {
      return this.title;
   }, (var1x) -> {
      this.title = var1x;
   }, this::getClipboard, this::setClipboard, (var0) -> {
      return var0.length() < 16;
   });
   private long lastClickTime;
   private int lastIndex = -1;
   private PageButton forwardButton;
   private PageButton backButton;
   private Button doneButton;
   private Button signButton;
   private Button finalizeButton;
   private Button cancelButton;
   private final InteractionHand hand;
   @Nullable
   private BookEditScreen.DisplayCache displayCache;
   private Component pageMsg;
   private final Component ownerText;

   public BookEditScreen(Player var1, ItemStack var2, InteractionHand var3) {
      super(GameNarrator.NO_TITLE);
      this.displayCache = BookEditScreen.DisplayCache.EMPTY;
      this.pageMsg = CommonComponents.EMPTY;
      this.owner = var1;
      this.book = var2;
      this.hand = var3;
      CompoundTag var4 = var2.getTag();
      if (var4 != null) {
         List var10001 = this.pages;
         Objects.requireNonNull(var10001);
         BookViewScreen.loadPages(var4, var10001::add);
      }

      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      this.ownerText = Component.translatable("book.byAuthor", var1.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   private void setClipboard(String var1) {
      if (this.minecraft != null) {
         TextFieldHelper.setClipboardContents(this.minecraft, var1);
      }

   }

   private String getClipboard() {
      return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
   }

   private int getNumPages() {
      return this.pages.size();
   }

   public void tick() {
      super.tick();
      ++this.frameTick;
   }

   protected void init() {
      this.clearDisplayCache();
      this.signButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), (var1x) -> {
         this.isSigning = true;
         this.updateButtonVisibility();
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.doneButton = (Button)this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (var1x) -> {
         this.minecraft.setScreen((Screen)null);
         this.saveChanges(false);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      this.finalizeButton = (Button)this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), (var1x) -> {
         if (this.isSigning) {
            this.saveChanges(true);
            this.minecraft.setScreen((Screen)null);
         }

      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.cancelButton = (Button)this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (var1x) -> {
         if (this.isSigning) {
            this.isSigning = false;
         }

         this.updateButtonVisibility();
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      int var1 = (this.width - 192) / 2;
      boolean var2 = true;
      this.forwardButton = (PageButton)this.addRenderableWidget(new PageButton(var1 + 116, 159, true, (var1x) -> {
         this.pageForward();
      }, true));
      this.backButton = (PageButton)this.addRenderableWidget(new PageButton(var1 + 43, 159, false, (var1x) -> {
         this.pageBack();
      }, true));
      this.updateButtonVisibility();
   }

   private void pageBack() {
      if (this.currentPage > 0) {
         --this.currentPage;
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         ++this.currentPage;
      } else {
         this.appendPageToBook();
         if (this.currentPage < this.getNumPages() - 1) {
            ++this.currentPage;
         }
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void updateButtonVisibility() {
      this.backButton.visible = !this.isSigning && this.currentPage > 0;
      this.forwardButton.visible = !this.isSigning;
      this.doneButton.visible = !this.isSigning;
      this.signButton.visible = !this.isSigning;
      this.cancelButton.visible = this.isSigning;
      this.finalizeButton.visible = this.isSigning;
      this.finalizeButton.active = !this.title.trim().isEmpty();
   }

   private void eraseEmptyTrailingPages() {
      ListIterator var1 = this.pages.listIterator(this.pages.size());

      while(var1.hasPrevious() && ((String)var1.previous()).isEmpty()) {
         var1.remove();
      }

   }

   private void saveChanges(boolean var1) {
      if (this.isModified) {
         this.eraseEmptyTrailingPages();
         this.updateLocalCopy(var1);
         int var2 = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().selected : 40;
         this.minecraft.getConnection().send((Packet)(new ServerboundEditBookPacket(var2, this.pages, var1 ? Optional.of(this.title.trim()) : Optional.empty())));
      }
   }

   private void updateLocalCopy(boolean var1) {
      ListTag var2 = new ListTag();
      Stream var10000 = this.pages.stream().map(StringTag::valueOf);
      Objects.requireNonNull(var2);
      var10000.forEach(var2::add);
      if (!this.pages.isEmpty()) {
         this.book.addTagElement("pages", var2);
      }

      if (var1) {
         this.book.addTagElement("author", StringTag.valueOf(this.owner.getGameProfile().getName()));
         this.book.addTagElement("title", StringTag.valueOf(this.title.trim()));
      }

   }

   private void appendPageToBook() {
      if (this.getNumPages() < 100) {
         this.pages.add("");
         this.isModified = true;
      }
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      if (super.keyPressed(var1, var2, var3)) {
         return true;
      } else if (this.isSigning) {
         return this.titleKeyPressed(var1, var2, var3);
      } else {
         boolean var4 = this.bookKeyPressed(var1, var2, var3);
         if (var4) {
            this.clearDisplayCache();
            return true;
         } else {
            return false;
         }
      }
   }

   public boolean charTyped(char var1, int var2) {
      if (super.charTyped(var1, var2)) {
         return true;
      } else if (this.isSigning) {
         boolean var3 = this.titleEdit.charTyped(var1);
         if (var3) {
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         } else {
            return false;
         }
      } else if (SharedConstants.isAllowedChatCharacter(var1)) {
         this.pageEdit.insertText(Character.toString(var1));
         this.clearDisplayCache();
         return true;
      } else {
         return false;
      }
   }

   private boolean bookKeyPressed(int var1, int var2, int var3) {
      if (Screen.isSelectAll(var1)) {
         this.pageEdit.selectAll();
         return true;
      } else if (Screen.isCopy(var1)) {
         this.pageEdit.copy();
         return true;
      } else if (Screen.isPaste(var1)) {
         this.pageEdit.paste();
         return true;
      } else if (Screen.isCut(var1)) {
         this.pageEdit.cut();
         return true;
      } else {
         TextFieldHelper.CursorStep var4 = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
         switch(var1) {
         case 257:
         case 335:
            this.pageEdit.insertText("\n");
            return true;
         case 259:
            this.pageEdit.removeFromCursor(-1, var4);
            return true;
         case 261:
            this.pageEdit.removeFromCursor(1, var4);
            return true;
         case 262:
            this.pageEdit.moveBy(1, Screen.hasShiftDown(), var4);
            return true;
         case 263:
            this.pageEdit.moveBy(-1, Screen.hasShiftDown(), var4);
            return true;
         case 264:
            this.keyDown();
            return true;
         case 265:
            this.keyUp();
            return true;
         case 266:
            this.backButton.onPress();
            return true;
         case 267:
            this.forwardButton.onPress();
            return true;
         case 268:
            this.keyHome();
            return true;
         case 269:
            this.keyEnd();
            return true;
         default:
            return false;
         }
      }
   }

   private void keyUp() {
      this.changeLine(-1);
   }

   private void keyDown() {
      this.changeLine(1);
   }

   private void changeLine(int var1) {
      int var2 = this.pageEdit.getCursorPos();
      int var3 = this.getDisplayCache().changeLine(var2, var1);
      this.pageEdit.setCursorPos(var3, Screen.hasShiftDown());
   }

   private void keyHome() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToStart(Screen.hasShiftDown());
      } else {
         int var1 = this.pageEdit.getCursorPos();
         int var2 = this.getDisplayCache().findLineStart(var1);
         this.pageEdit.setCursorPos(var2, Screen.hasShiftDown());
      }

   }

   private void keyEnd() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToEnd(Screen.hasShiftDown());
      } else {
         BookEditScreen.DisplayCache var1 = this.getDisplayCache();
         int var2 = this.pageEdit.getCursorPos();
         int var3 = var1.findLineEnd(var2);
         this.pageEdit.setCursorPos(var3, Screen.hasShiftDown());
      }

   }

   private boolean titleKeyPressed(int var1, int var2, int var3) {
      switch(var1) {
      case 257:
      case 335:
         if (!this.title.isEmpty()) {
            this.saveChanges(true);
            this.minecraft.setScreen((Screen)null);
         }

         return true;
      case 259:
         this.titleEdit.removeCharsFromCursor(-1);
         this.updateButtonVisibility();
         this.isModified = true;
         return true;
      default:
         return false;
      }
   }

   private String getCurrentPageText() {
      return this.currentPage >= 0 && this.currentPage < this.pages.size() ? (String)this.pages.get(this.currentPage) : "";
   }

   private void setCurrentPageText(String var1) {
      if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
         this.pages.set(this.currentPage, var1);
         this.isModified = true;
         this.clearDisplayCache();
      }

   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      this.setFocused((GuiEventListener)null);
      int var5 = (this.width - 192) / 2;
      boolean var6 = true;
      var1.blit(BookViewScreen.BOOK_LOCATION, var5, 2, 0, 0, 192, 192);
      int var10;
      int var11;
      if (this.isSigning) {
         boolean var7 = this.frameTick / 6 % 2 == 0;
         FormattedCharSequence var8 = FormattedCharSequence.composite(FormattedCharSequence.forward(this.title, Style.EMPTY), var7 ? BLACK_CURSOR : GRAY_CURSOR);
         int var9 = this.font.width((FormattedText)EDIT_TITLE_LABEL);
         var1.drawString(this.font, (Component)EDIT_TITLE_LABEL, var5 + 36 + (114 - var9) / 2, 34, 0, false);
         var10 = this.font.width(var8);
         var1.drawString(this.font, (FormattedCharSequence)var8, var5 + 36 + (114 - var10) / 2, 50, 0, false);
         var11 = this.font.width((FormattedText)this.ownerText);
         var1.drawString(this.font, (Component)this.ownerText, var5 + 36 + (114 - var11) / 2, 60, 0, false);
         var1.drawWordWrap(this.font, FINALIZE_WARNING_LABEL, var5 + 36, 82, 114, 0);
      } else {
         int var13 = this.font.width((FormattedText)this.pageMsg);
         var1.drawString(this.font, (Component)this.pageMsg, var5 - var13 + 192 - 44, 18, 0, false);
         BookEditScreen.DisplayCache var14 = this.getDisplayCache();
         BookEditScreen.LineInfo[] var15 = var14.lines;
         var10 = var15.length;

         for(var11 = 0; var11 < var10; ++var11) {
            BookEditScreen.LineInfo var12 = var15[var11];
            var1.drawString(this.font, var12.asComponent, var12.x, var12.y, -16777216, false);
         }

         this.renderHighlight(var1, var14.selection);
         this.renderCursor(var1, var14.cursor, var14.cursorAtEnd);
      }

      super.render(var1, var2, var3, var4);
   }

   private void renderCursor(GuiGraphics var1, BookEditScreen.Pos2i var2, boolean var3) {
      if (this.frameTick / 6 % 2 == 0) {
         var2 = this.convertLocalToScreen(var2);
         if (!var3) {
            int var10001 = var2.x;
            int var10002 = var2.y - 1;
            int var10003 = var2.x + 1;
            int var10004 = var2.y;
            Objects.requireNonNull(this.font);
            var1.fill(var10001, var10002, var10003, var10004 + 9, -16777216);
         } else {
            var1.drawString(this.font, (String)"_", var2.x, var2.y, 0, false);
         }
      }

   }

   private void renderHighlight(GuiGraphics var1, Rect2i[] var2) {
      Rect2i[] var3 = var2;
      int var4 = var2.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Rect2i var6 = var3[var5];
         int var7 = var6.getX();
         int var8 = var6.getY();
         int var9 = var7 + var6.getWidth();
         int var10 = var8 + var6.getHeight();
         var1.fill(RenderType.guiTextHighlight(), var7, var8, var9, var10, -16776961);
      }

   }

   private BookEditScreen.Pos2i convertScreenToLocal(BookEditScreen.Pos2i var1) {
      return new BookEditScreen.Pos2i(var1.x - (this.width - 192) / 2 - 36, var1.y - 32);
   }

   private BookEditScreen.Pos2i convertLocalToScreen(BookEditScreen.Pos2i var1) {
      return new BookEditScreen.Pos2i(var1.x + (this.width - 192) / 2 + 36, var1.y + 32);
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (super.mouseClicked(var1, var3, var5)) {
         return true;
      } else {
         if (var5 == 0) {
            long var6 = Util.getMillis();
            BookEditScreen.DisplayCache var8 = this.getDisplayCache();
            int var9 = var8.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)var1, (int)var3)));
            if (var9 >= 0) {
               if (var9 == this.lastIndex && var6 - this.lastClickTime < 250L) {
                  if (!this.pageEdit.isSelecting()) {
                     this.selectWord(var9);
                  } else {
                     this.pageEdit.selectAll();
                  }
               } else {
                  this.pageEdit.setCursorPos(var9, Screen.hasShiftDown());
               }

               this.clearDisplayCache();
            }

            this.lastIndex = var9;
            this.lastClickTime = var6;
         }

         return true;
      }
   }

   private void selectWord(int var1) {
      String var2 = this.getCurrentPageText();
      this.pageEdit.setSelectionRange(StringSplitter.getWordPosition(var2, -1, var1, false), StringSplitter.getWordPosition(var2, 1, var1, false));
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (super.mouseDragged(var1, var3, var5, var6, var8)) {
         return true;
      } else {
         if (var5 == 0) {
            BookEditScreen.DisplayCache var10 = this.getDisplayCache();
            int var11 = var10.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)var1, (int)var3)));
            this.pageEdit.setCursorPos(var11, true);
            this.clearDisplayCache();
         }

         return true;
      }
   }

   private BookEditScreen.DisplayCache getDisplayCache() {
      if (this.displayCache == null) {
         this.displayCache = this.rebuildDisplayCache();
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages());
      }

      return this.displayCache;
   }

   private void clearDisplayCache() {
      this.displayCache = null;
   }

   private void clearDisplayCacheAfterPageChange() {
      this.pageEdit.setCursorToEnd();
      this.clearDisplayCache();
   }

   private BookEditScreen.DisplayCache rebuildDisplayCache() {
      String var1 = this.getCurrentPageText();
      if (var1.isEmpty()) {
         return BookEditScreen.DisplayCache.EMPTY;
      } else {
         int var2 = this.pageEdit.getCursorPos();
         int var3 = this.pageEdit.getSelectionPos();
         IntArrayList var4 = new IntArrayList();
         ArrayList var5 = Lists.newArrayList();
         MutableInt var6 = new MutableInt();
         MutableBoolean var7 = new MutableBoolean();
         StringSplitter var8 = this.font.getSplitter();
         var8.splitLines(var1, 114, Style.EMPTY, true, (var6x, var7x, var8x) -> {
            int var9 = var6.getAndIncrement();
            String var10 = var1.substring(var7x, var8x);
            var7.setValue(var10.endsWith("\n"));
            String var11 = StringUtils.stripEnd(var10, " \n");
            Objects.requireNonNull(this.font);
            int var12 = var9 * 9;
            BookEditScreen.Pos2i var13 = this.convertLocalToScreen(new BookEditScreen.Pos2i(0, var12));
            var4.add(var7x);
            var5.add(new BookEditScreen.LineInfo(var6x, var11, var13.x, var13.y));
         });
         int[] var9 = var4.toIntArray();
         boolean var10 = var2 == var1.length();
         BookEditScreen.Pos2i var11;
         int var13;
         if (var10 && var7.isTrue()) {
            int var10003 = var5.size();
            Objects.requireNonNull(this.font);
            var11 = new BookEditScreen.Pos2i(0, var10003 * 9);
         } else {
            int var12 = findLineFromPos(var9, var2);
            var13 = this.font.width(var1.substring(var9[var12], var2));
            Objects.requireNonNull(this.font);
            var11 = new BookEditScreen.Pos2i(var13, var12 * 9);
         }

         ArrayList var22 = Lists.newArrayList();
         if (var2 != var3) {
            var13 = Math.min(var2, var3);
            int var14 = Math.max(var2, var3);
            int var15 = findLineFromPos(var9, var13);
            int var16 = findLineFromPos(var9, var14);
            int var17;
            int var18;
            if (var15 == var16) {
               Objects.requireNonNull(this.font);
               var17 = var15 * 9;
               var18 = var9[var15];
               var22.add(this.createPartialLineSelection(var1, var8, var13, var14, var17, var18));
            } else {
               var17 = var15 + 1 > var9.length ? var1.length() : var9[var15 + 1];
               Objects.requireNonNull(this.font);
               var22.add(this.createPartialLineSelection(var1, var8, var13, var17, var15 * 9, var9[var15]));

               for(var18 = var15 + 1; var18 < var16; ++var18) {
                  Objects.requireNonNull(this.font);
                  int var19 = var18 * 9;
                  String var20 = var1.substring(var9[var18], var9[var18 + 1]);
                  int var21 = (int)var8.stringWidth(var20);
                  BookEditScreen.Pos2i var10002 = new BookEditScreen.Pos2i(0, var19);
                  Objects.requireNonNull(this.font);
                  var22.add(this.createSelection(var10002, new BookEditScreen.Pos2i(var21, var19 + 9)));
               }

               int var10004 = var9[var16];
               Objects.requireNonNull(this.font);
               var22.add(this.createPartialLineSelection(var1, var8, var10004, var14, var16 * 9, var9[var16]));
            }
         }

         return new BookEditScreen.DisplayCache(var1, var11, var10, var9, (BookEditScreen.LineInfo[])var5.toArray(new BookEditScreen.LineInfo[0]), (Rect2i[])var22.toArray(new Rect2i[0]));
      }
   }

   static int findLineFromPos(int[] var0, int var1) {
      int var2 = Arrays.binarySearch(var0, var1);
      return var2 < 0 ? -(var2 + 2) : var2;
   }

   private Rect2i createPartialLineSelection(String var1, StringSplitter var2, int var3, int var4, int var5, int var6) {
      String var7 = var1.substring(var6, var3);
      String var8 = var1.substring(var6, var4);
      BookEditScreen.Pos2i var9 = new BookEditScreen.Pos2i((int)var2.stringWidth(var7), var5);
      int var10002 = (int)var2.stringWidth(var8);
      Objects.requireNonNull(this.font);
      BookEditScreen.Pos2i var10 = new BookEditScreen.Pos2i(var10002, var5 + 9);
      return this.createSelection(var9, var10);
   }

   private Rect2i createSelection(BookEditScreen.Pos2i var1, BookEditScreen.Pos2i var2) {
      BookEditScreen.Pos2i var3 = this.convertLocalToScreen(var1);
      BookEditScreen.Pos2i var4 = this.convertLocalToScreen(var2);
      int var5 = Math.min(var3.x, var4.x);
      int var6 = Math.max(var3.x, var4.x);
      int var7 = Math.min(var3.y, var4.y);
      int var8 = Math.max(var3.y, var4.y);
      return new Rect2i(var5, var7, var6 - var5, var8 - var7);
   }

   static {
      BLACK_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.BLACK));
      GRAY_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.GRAY));
   }

   static class DisplayCache {
      static final BookEditScreen.DisplayCache EMPTY;
      private final String fullText;
      final BookEditScreen.Pos2i cursor;
      final boolean cursorAtEnd;
      private final int[] lineStarts;
      final BookEditScreen.LineInfo[] lines;
      final Rect2i[] selection;

      public DisplayCache(String var1, BookEditScreen.Pos2i var2, boolean var3, int[] var4, BookEditScreen.LineInfo[] var5, Rect2i[] var6) {
         this.fullText = var1;
         this.cursor = var2;
         this.cursorAtEnd = var3;
         this.lineStarts = var4;
         this.lines = var5;
         this.selection = var6;
      }

      public int getIndexAtPosition(Font var1, BookEditScreen.Pos2i var2) {
         int var10000 = var2.y;
         Objects.requireNonNull(var1);
         int var3 = var10000 / 9;
         if (var3 < 0) {
            return 0;
         } else if (var3 >= this.lines.length) {
            return this.fullText.length();
         } else {
            BookEditScreen.LineInfo var4 = this.lines[var3];
            return this.lineStarts[var3] + var1.getSplitter().plainIndexAtWidth(var4.contents, var2.x, var4.style);
         }
      }

      public int changeLine(int var1, int var2) {
         int var3 = BookEditScreen.findLineFromPos(this.lineStarts, var1);
         int var4 = var3 + var2;
         int var5;
         if (0 <= var4 && var4 < this.lineStarts.length) {
            int var6 = var1 - this.lineStarts[var3];
            int var7 = this.lines[var4].contents.length();
            var5 = this.lineStarts[var4] + Math.min(var6, var7);
         } else {
            var5 = var1;
         }

         return var5;
      }

      public int findLineStart(int var1) {
         int var2 = BookEditScreen.findLineFromPos(this.lineStarts, var1);
         return this.lineStarts[var2];
      }

      public int findLineEnd(int var1) {
         int var2 = BookEditScreen.findLineFromPos(this.lineStarts, var1);
         return this.lineStarts[var2] + this.lines[var2].contents.length();
      }

      static {
         EMPTY = new BookEditScreen.DisplayCache("", new BookEditScreen.Pos2i(0, 0), true, new int[]{0}, new BookEditScreen.LineInfo[]{new BookEditScreen.LineInfo(Style.EMPTY, "", 0, 0)}, new Rect2i[0]);
      }
   }

   private static class LineInfo {
      final Style style;
      final String contents;
      final Component asComponent;
      final int x;
      final int y;

      public LineInfo(Style var1, String var2, int var3, int var4) {
         this.style = var1;
         this.contents = var2;
         this.x = var3;
         this.y = var4;
         this.asComponent = Component.literal(var2).setStyle(var1);
      }
   }

   private static class Pos2i {
      public final int x;
      public final int y;

      Pos2i(int var1, int var2) {
         this.x = var1;
         this.y = var2;
      }
   }
}
