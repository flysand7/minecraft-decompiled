package net.minecraft.client.gui.screens.controls;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.ArrayUtils;

public class KeyBindsList extends ContainerObjectSelectionList<KeyBindsList.Entry> {
   final KeyBindsScreen keyBindsScreen;
   int maxNameWidth;

   public KeyBindsList(KeyBindsScreen var1, Minecraft var2) {
      super(var2, var1.width + 45, var1.height, 20, var1.height - 32, 20);
      this.keyBindsScreen = var1;
      KeyMapping[] var3 = (KeyMapping[])ArrayUtils.clone(var2.options.keyMappings);
      Arrays.sort(var3);
      String var4 = null;
      KeyMapping[] var5 = var3;
      int var6 = var3.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         KeyMapping var8 = var5[var7];
         String var9 = var8.getCategory();
         if (!var9.equals(var4)) {
            var4 = var9;
            this.addEntry(new KeyBindsList.CategoryEntry(Component.translatable(var9)));
         }

         MutableComponent var10 = Component.translatable(var8.getName());
         int var11 = var2.font.width((FormattedText)var10);
         if (var11 > this.maxNameWidth) {
            this.maxNameWidth = var11;
         }

         this.addEntry(new KeyBindsList.KeyEntry(var8, var10));
      }

   }

   public void resetMappingAndUpdateButtons() {
      KeyMapping.resetMapping();
      this.refreshEntries();
   }

   public void refreshEntries() {
      this.children().forEach(KeyBindsList.Entry::refreshEntry);
   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 15;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 32;
   }

   public class CategoryEntry extends KeyBindsList.Entry {
      final Component name;
      private final int width;

      public CategoryEntry(Component var2) {
         this.name = var2;
         this.width = KeyBindsList.this.minecraft.font.width((FormattedText)this.name);
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         Font var10001 = KeyBindsList.this.minecraft.font;
         Component var10002 = this.name;
         int var10003 = KeyBindsList.this.minecraft.screen.width / 2 - this.width / 2;
         int var10004 = var3 + var6;
         Objects.requireNonNull(KeyBindsList.this.minecraft.font);
         var1.drawString(var10001, var10002, var10003, var10004 - 9 - 1, 16777215, false);
      }

      @Nullable
      public ComponentPath nextFocusPath(FocusNavigationEvent var1) {
         return null;
      }

      public List<? extends GuiEventListener> children() {
         return Collections.emptyList();
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            public void updateNarration(NarrationElementOutput var1) {
               var1.add(NarratedElementType.TITLE, CategoryEntry.this.name);
            }
         });
      }

      protected void refreshEntry() {
      }
   }

   public class KeyEntry extends KeyBindsList.Entry {
      private final KeyMapping key;
      private final Component name;
      private final Button changeButton;
      private final Button resetButton;
      private boolean hasCollision = false;

      KeyEntry(KeyMapping var2, Component var3) {
         this.key = var2;
         this.name = var3;
         this.changeButton = Button.builder(var3, (var2x) -> {
            KeyBindsList.this.keyBindsScreen.selectedKey = var2;
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 75, 20).createNarration((var2x) -> {
            return var2.isUnbound() ? Component.translatable("narrator.controls.unbound", var3) : Component.translatable("narrator.controls.bound", var3, var2x.get());
         }).build();
         this.resetButton = Button.builder(Component.translatable("controls.reset"), (var2x) -> {
            KeyBindsList.this.minecraft.options.setKey(var2, var2.getDefaultKey());
            KeyBindsList.this.resetMappingAndUpdateButtons();
         }).bounds(0, 0, 50, 20).createNarration((var1x) -> {
            return Component.translatable("narrator.controls.reset", var3);
         }).build();
         this.refreshEntry();
      }

      public void render(GuiGraphics var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10) {
         Font var10001 = KeyBindsList.this.minecraft.font;
         Component var10002 = this.name;
         int var10003 = var4 + 90 - KeyBindsList.this.maxNameWidth;
         int var10004 = var3 + var6 / 2;
         Objects.requireNonNull(KeyBindsList.this.minecraft.font);
         var1.drawString(var10001, var10002, var10003, var10004 - 9 / 2, 16777215, false);
         this.resetButton.setX(var4 + 190);
         this.resetButton.setY(var3);
         this.resetButton.render(var1, var7, var8, var10);
         this.changeButton.setX(var4 + 105);
         this.changeButton.setY(var3);
         if (this.hasCollision) {
            boolean var11 = true;
            int var12 = this.changeButton.getX() - 6;
            var1.fill(var12, var3 + 2, var12 + 3, var3 + var6 + 2, ChatFormatting.RED.getColor() | -16777216);
         }

         this.changeButton.render(var1, var7, var8, var10);
      }

      public List<? extends GuiEventListener> children() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(this.changeButton, this.resetButton);
      }

      protected void refreshEntry() {
         this.changeButton.setMessage(this.key.getTranslatedKeyMessage());
         this.resetButton.active = !this.key.isDefault();
         this.hasCollision = false;
         MutableComponent var1 = Component.empty();
         if (!this.key.isUnbound()) {
            KeyMapping[] var2 = KeyBindsList.this.minecraft.options.keyMappings;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               KeyMapping var5 = var2[var4];
               if (var5 != this.key && this.key.same(var5)) {
                  if (this.hasCollision) {
                     var1.append(", ");
                  }

                  this.hasCollision = true;
                  var1.append((Component)Component.translatable(var5.getName()));
               }
            }
         }

         if (this.hasCollision) {
            this.changeButton.setMessage(Component.literal("[ ").append((Component)this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE)).append(" ]").withStyle(ChatFormatting.RED));
            this.changeButton.setTooltip(Tooltip.create(Component.translatable("controls.keybinds.duplicateKeybinds", var1)));
         } else {
            this.changeButton.setTooltip((Tooltip)null);
         }

         if (KeyBindsList.this.keyBindsScreen.selectedKey == this.key) {
            this.changeButton.setMessage(Component.literal("> ").append((Component)this.changeButton.getMessage().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.UNDERLINE)).append(" <").withStyle(ChatFormatting.YELLOW));
         }

      }
   }

   public abstract static class Entry extends ContainerObjectSelectionList.Entry<KeyBindsList.Entry> {
      public Entry() {
      }

      abstract void refreshEntry();
   }
}
