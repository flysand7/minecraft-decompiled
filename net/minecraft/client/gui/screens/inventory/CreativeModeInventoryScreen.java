package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.HotbarManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public class CreativeModeInventoryScreen extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
   private static final ResourceLocation CREATIVE_TABS_LOCATION = new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");
   private static final String GUI_CREATIVE_TAB_PREFIX = "textures/gui/container/creative_inventory/tab_";
   private static final String CUSTOM_SLOT_LOCK = "CustomCreativeLock";
   private static final int NUM_ROWS = 5;
   private static final int NUM_COLS = 9;
   private static final int TAB_WIDTH = 26;
   private static final int TAB_HEIGHT = 32;
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   static final SimpleContainer CONTAINER = new SimpleContainer(45);
   private static final Component TRASH_SLOT_TOOLTIP = Component.translatable("inventory.binSlot");
   private static final int TEXT_COLOR = 16777215;
   private static CreativeModeTab selectedTab = CreativeModeTabs.getDefaultTab();
   private float scrollOffs;
   private boolean scrolling;
   private EditBox searchBox;
   @Nullable
   private List<Slot> originalSlots;
   @Nullable
   private Slot destroyItemSlot;
   private CreativeInventoryListener listener;
   private boolean ignoreTextInput;
   private boolean hasClickedOutside;
   private final Set<TagKey<Item>> visibleTags = new HashSet();
   private final boolean displayOperatorCreativeTab;

   public CreativeModeInventoryScreen(Player var1, FeatureFlagSet var2, boolean var3) {
      super(new CreativeModeInventoryScreen.ItemPickerMenu(var1), var1.getInventory(), CommonComponents.EMPTY);
      var1.containerMenu = this.menu;
      this.imageHeight = 136;
      this.imageWidth = 195;
      this.displayOperatorCreativeTab = var3;
      CreativeModeTabs.tryRebuildTabContents(var2, this.hasPermissions(var1), var1.level().registryAccess());
   }

   private boolean hasPermissions(Player var1) {
      return var1.canUseGameMasterBlocks() && this.displayOperatorCreativeTab;
   }

   private void tryRefreshInvalidatedTabs(FeatureFlagSet var1, boolean var2, HolderLookup.Provider var3) {
      if (CreativeModeTabs.tryRebuildTabContents(var1, var2, var3)) {
         Iterator var4 = CreativeModeTabs.allTabs().iterator();

         while(true) {
            while(true) {
               CreativeModeTab var5;
               Collection var6;
               do {
                  if (!var4.hasNext()) {
                     return;
                  }

                  var5 = (CreativeModeTab)var4.next();
                  var6 = var5.getDisplayItems();
               } while(var5 != selectedTab);

               if (var5.getType() == CreativeModeTab.Type.CATEGORY && var6.isEmpty()) {
                  this.selectTab(CreativeModeTabs.getDefaultTab());
               } else {
                  this.refreshCurrentTabContents(var6);
               }
            }
         }
      }
   }

   private void refreshCurrentTabContents(Collection<ItemStack> var1) {
      int var2 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getRowIndexForScroll(this.scrollOffs);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
         this.refreshSearchResults();
      } else {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(var1);
      }

      this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getScrollForRowIndex(var2);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
   }

   public void containerTick() {
      super.containerTick();
      if (this.minecraft != null) {
         if (this.minecraft.player != null) {
            this.tryRefreshInvalidatedTabs(this.minecraft.player.connection.enabledFeatures(), this.hasPermissions(this.minecraft.player), this.minecraft.player.level().registryAccess());
         }

         if (!this.minecraft.gameMode.hasInfiniteItems()) {
            this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
         } else {
            this.searchBox.tick();
         }

      }
   }

   protected void slotClicked(@Nullable Slot var1, int var2, int var3, ClickType var4) {
      if (this.isCreativeSlot(var1)) {
         this.searchBox.moveCursorToEnd();
         this.searchBox.setHighlightPos(0);
      }

      boolean var5 = var4 == ClickType.QUICK_MOVE;
      var4 = var2 == -999 && var4 == ClickType.PICKUP ? ClickType.THROW : var4;
      ItemStack var6;
      if (var1 == null && selectedTab.getType() != CreativeModeTab.Type.INVENTORY && var4 != ClickType.QUICK_CRAFT) {
         if (!((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && this.hasClickedOutside) {
            if (var3 == 0) {
               this.minecraft.player.drop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried(), true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried());
               ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
            }

            if (var3 == 1) {
               var6 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().split(1);
               this.minecraft.player.drop(var6, true);
               this.minecraft.gameMode.handleCreativeModeItemDrop(var6);
            }
         }
      } else {
         if (var1 != null && !var1.mayPickup(this.minecraft.player)) {
            return;
         }

         if (var1 == this.destroyItemSlot && var5) {
            for(int var10 = 0; var10 < this.minecraft.player.inventoryMenu.getItems().size(); ++var10) {
               this.minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, var10);
            }
         } else {
            ItemStack var7;
            if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
               if (var1 == this.destroyItemSlot) {
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
               } else if (var4 == ClickType.THROW && var1 != null && var1.hasItem()) {
                  var6 = var1.remove(var3 == 0 ? 1 : var1.getItem().getMaxStackSize());
                  var7 = var1.getItem();
                  this.minecraft.player.drop(var6, true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(var6);
                  this.minecraft.gameMode.handleCreativeModeItemAdd(var7, ((CreativeModeInventoryScreen.SlotWrapper)var1).target.index);
               } else if (var4 == ClickType.THROW && !((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                  this.minecraft.player.drop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried(), true);
                  this.minecraft.gameMode.handleCreativeModeItemDrop(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried());
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
               } else {
                  this.minecraft.player.inventoryMenu.clicked(var1 == null ? var2 : ((CreativeModeInventoryScreen.SlotWrapper)var1).target.index, var3, var4, this.minecraft.player);
                  this.minecraft.player.inventoryMenu.broadcastChanges();
               }
            } else {
               int var8;
               if (var4 != ClickType.QUICK_CRAFT && var1.container == CONTAINER) {
                  var6 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried();
                  var7 = var1.getItem();
                  if (var4 == ClickType.SWAP) {
                     if (!var7.isEmpty()) {
                        this.minecraft.player.getInventory().setItem(var3, var7.copyWithCount(var7.getMaxStackSize()));
                        this.minecraft.player.inventoryMenu.broadcastChanges();
                     }

                     return;
                  }

                  ItemStack var12;
                  if (var4 == ClickType.CLONE) {
                     if (((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty() && var1.hasItem()) {
                        var12 = var1.getItem();
                        ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(var12.copyWithCount(var12.getMaxStackSize()));
                     }

                     return;
                  }

                  if (var4 == ClickType.THROW) {
                     if (!var7.isEmpty()) {
                        var12 = var7.copyWithCount(var3 == 0 ? 1 : var7.getMaxStackSize());
                        this.minecraft.player.drop(var12, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(var12);
                     }

                     return;
                  }

                  if (!var6.isEmpty() && !var7.isEmpty() && ItemStack.isSameItemSameTags(var6, var7)) {
                     if (var3 == 0) {
                        if (var5) {
                           var6.setCount(var6.getMaxStackSize());
                        } else if (var6.getCount() < var6.getMaxStackSize()) {
                           var6.grow(1);
                        }
                     } else {
                        var6.shrink(1);
                     }
                  } else if (!var7.isEmpty() && var6.isEmpty()) {
                     var8 = var5 ? var7.getMaxStackSize() : var7.getCount();
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(var7.copyWithCount(var8));
                  } else if (var3 == 0) {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).setCarried(ItemStack.EMPTY);
                  } else if (!((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().isEmpty()) {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getCarried().shrink(1);
                  }
               } else if (this.menu != null) {
                  var6 = var1 == null ? ItemStack.EMPTY : ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(var1.index).getItem();
                  ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).clicked(var1 == null ? var2 : var1.index, var3, var4, this.minecraft.player);
                  if (AbstractContainerMenu.getQuickcraftHeader(var3) == 2) {
                     for(int var11 = 0; var11 < 9; ++var11) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(45 + var11).getItem(), 36 + var11);
                     }
                  } else if (var1 != null) {
                     var7 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getSlot(var1.index).getItem();
                     this.minecraft.gameMode.handleCreativeModeItemAdd(var7, var1.index - ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.size() + 9 + 36);
                     var8 = 45 + var3;
                     if (var4 == ClickType.SWAP) {
                        this.minecraft.gameMode.handleCreativeModeItemAdd(var6, var8 - ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.size() + 9 + 36);
                     } else if (var4 == ClickType.THROW && !var6.isEmpty()) {
                        ItemStack var9 = var6.copyWithCount(var3 == 0 ? 1 : var6.getMaxStackSize());
                        this.minecraft.player.drop(var9, true);
                        this.minecraft.gameMode.handleCreativeModeItemDrop(var9);
                     }

                     this.minecraft.player.inventoryMenu.broadcastChanges();
                  }
               }
            }
         }
      }

   }

   private boolean isCreativeSlot(@Nullable Slot var1) {
      return var1 != null && var1.container == CONTAINER;
   }

   protected void init() {
      if (this.minecraft.gameMode.hasInfiniteItems()) {
         super.init();
         Font var10003 = this.font;
         int var10004 = this.leftPos + 82;
         int var10005 = this.topPos + 6;
         Objects.requireNonNull(this.font);
         this.searchBox = new EditBox(var10003, var10004, var10005, 80, 9, Component.translatable("itemGroup.search"));
         this.searchBox.setMaxLength(50);
         this.searchBox.setBordered(false);
         this.searchBox.setVisible(false);
         this.searchBox.setTextColor(16777215);
         this.addWidget(this.searchBox);
         CreativeModeTab var1 = selectedTab;
         selectedTab = CreativeModeTabs.getDefaultTab();
         this.selectTab(var1);
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
         this.listener = new CreativeInventoryListener(this.minecraft);
         this.minecraft.player.inventoryMenu.addSlotListener(this.listener);
         if (!selectedTab.shouldDisplay()) {
            this.selectTab(CreativeModeTabs.getDefaultTab());
         }
      } else {
         this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
      }

   }

   public void resize(Minecraft var1, int var2, int var3) {
      int var4 = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getRowIndexForScroll(this.scrollOffs);
      String var5 = this.searchBox.getValue();
      this.init(var1, var2, var3);
      this.searchBox.setValue(var5);
      if (!this.searchBox.getValue().isEmpty()) {
         this.refreshSearchResults();
      }

      this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).getScrollForRowIndex(var4);
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
   }

   public void removed() {
      super.removed();
      if (this.minecraft.player != null && this.minecraft.player.getInventory() != null) {
         this.minecraft.player.inventoryMenu.removeSlotListener(this.listener);
      }

   }

   public boolean charTyped(char var1, int var2) {
      if (this.ignoreTextInput) {
         return false;
      } else if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
         return false;
      } else {
         String var3 = this.searchBox.getValue();
         if (this.searchBox.charTyped(var1, var2)) {
            if (!Objects.equals(var3, this.searchBox.getValue())) {
               this.refreshSearchResults();
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public boolean keyPressed(int var1, int var2, int var3) {
      this.ignoreTextInput = false;
      if (selectedTab.getType() != CreativeModeTab.Type.SEARCH) {
         if (this.minecraft.options.keyChat.matches(var1, var2)) {
            this.ignoreTextInput = true;
            this.selectTab(CreativeModeTabs.searchTab());
            return true;
         } else {
            return super.keyPressed(var1, var2, var3);
         }
      } else {
         boolean var4 = !this.isCreativeSlot(this.hoveredSlot) || this.hoveredSlot.hasItem();
         boolean var5 = InputConstants.getKey(var1, var2).getNumericKeyValue().isPresent();
         if (var4 && var5 && this.checkHotbarKeyPressed(var1, var2)) {
            this.ignoreTextInput = true;
            return true;
         } else {
            String var6 = this.searchBox.getValue();
            if (this.searchBox.keyPressed(var1, var2, var3)) {
               if (!Objects.equals(var6, this.searchBox.getValue())) {
                  this.refreshSearchResults();
               }

               return true;
            } else {
               return this.searchBox.isFocused() && this.searchBox.isVisible() && var1 != 256 ? true : super.keyPressed(var1, var2, var3);
            }
         }
      }
   }

   public boolean keyReleased(int var1, int var2, int var3) {
      this.ignoreTextInput = false;
      return super.keyReleased(var1, var2, var3);
   }

   private void refreshSearchResults() {
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      this.visibleTags.clear();
      String var1 = this.searchBox.getValue();
      if (var1.isEmpty()) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(selectedTab.getDisplayItems());
      } else {
         SearchTree var2;
         if (var1.startsWith("#")) {
            var1 = var1.substring(1);
            var2 = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_TAGS);
            this.updateVisibleTags(var1);
         } else {
            var2 = this.minecraft.getSearchTree(SearchRegistry.CREATIVE_NAMES);
         }

         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(var2.search(var1.toLowerCase(Locale.ROOT)));
      }

      this.scrollOffs = 0.0F;
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(0.0F);
   }

   private void updateVisibleTags(String var1) {
      int var2 = var1.indexOf(58);
      Predicate var3;
      if (var2 == -1) {
         var3 = (var1x) -> {
            return var1x.getPath().contains(var1);
         };
      } else {
         String var4 = var1.substring(0, var2).trim();
         String var5 = var1.substring(var2 + 1).trim();
         var3 = (var2x) -> {
            return var2x.getNamespace().contains(var4) && var2x.getPath().contains(var5);
         };
      }

      Stream var10000 = BuiltInRegistries.ITEM.getTagNames().filter((var1x) -> {
         return var3.test(var1x.location());
      });
      Set var10001 = this.visibleTags;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::add);
   }

   protected void renderLabels(GuiGraphics var1, int var2, int var3) {
      if (selectedTab.showTitle()) {
         var1.drawString(this.font, (Component)selectedTab.getDisplayName(), 8, 6, 4210752, false);
      }

   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      if (var5 == 0) {
         double var6 = var1 - (double)this.leftPos;
         double var8 = var3 - (double)this.topPos;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab var11 = (CreativeModeTab)var10.next();
            if (this.checkTabClicked(var11, var6, var8)) {
               return true;
            }
         }

         if (selectedTab.getType() != CreativeModeTab.Type.INVENTORY && this.insideScrollbar(var1, var3)) {
            this.scrolling = this.canScroll();
            return true;
         }
      }

      return super.mouseClicked(var1, var3, var5);
   }

   public boolean mouseReleased(double var1, double var3, int var5) {
      if (var5 == 0) {
         double var6 = var1 - (double)this.leftPos;
         double var8 = var3 - (double)this.topPos;
         this.scrolling = false;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab var11 = (CreativeModeTab)var10.next();
            if (this.checkTabClicked(var11, var6, var8)) {
               this.selectTab(var11);
               return true;
            }
         }
      }

      return super.mouseReleased(var1, var3, var5);
   }

   private boolean canScroll() {
      return selectedTab.canScroll() && ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).canScroll();
   }

   private void selectTab(CreativeModeTab var1) {
      CreativeModeTab var2 = selectedTab;
      selectedTab = var1;
      this.quickCraftSlots.clear();
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.clear();
      this.clearDraggingState();
      int var4;
      int var6;
      if (selectedTab.getType() == CreativeModeTab.Type.HOTBAR) {
         HotbarManager var3 = this.minecraft.getHotbarManager();

         for(var4 = 0; var4 < 9; ++var4) {
            Hotbar var5 = var3.get(var4);
            if (var5.isEmpty()) {
               for(var6 = 0; var6 < 9; ++var6) {
                  if (var6 == var4) {
                     ItemStack var7 = new ItemStack(Items.PAPER);
                     var7.getOrCreateTagElement("CustomCreativeLock");
                     Component var8 = this.minecraft.options.keyHotbarSlots[var4].getTranslatedKeyMessage();
                     Component var9 = this.minecraft.options.keySaveHotbarActivator.getTranslatedKeyMessage();
                     var7.setHoverName(Component.translatable("inventory.hotbarInfo", var9, var8));
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.add(var7);
                  } else {
                     ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.add(ItemStack.EMPTY);
                  }
               }
            } else {
               ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(var5);
            }
         }
      } else if (selectedTab.getType() == CreativeModeTab.Type.CATEGORY) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).items.addAll(selectedTab.getDisplayItems());
      }

      if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
         InventoryMenu var10 = this.minecraft.player.inventoryMenu;
         if (this.originalSlots == null) {
            this.originalSlots = ImmutableList.copyOf(((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots);
         }

         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.clear();

         for(var4 = 0; var4 < var10.slots.size(); ++var4) {
            int var11;
            int var12;
            int var14;
            int var15;
            if (var4 >= 5 && var4 < 9) {
               var12 = var4 - 5;
               var14 = var12 / 2;
               var15 = var12 % 2;
               var11 = 54 + var14 * 54;
               var6 = 6 + var15 * 27;
            } else if (var4 >= 0 && var4 < 5) {
               var11 = -2000;
               var6 = -2000;
            } else if (var4 == 45) {
               var11 = 35;
               var6 = 20;
            } else {
               var12 = var4 - 9;
               var14 = var12 % 9;
               var15 = var12 / 9;
               var11 = 9 + var14 * 18;
               if (var4 >= 36) {
                  var6 = 112;
               } else {
                  var6 = 54 + var15 * 18;
               }
            }

            CreativeModeInventoryScreen.SlotWrapper var13 = new CreativeModeInventoryScreen.SlotWrapper((Slot)var10.slots.get(var4), var4, var11, var6);
            ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.add(var13);
         }

         this.destroyItemSlot = new Slot(CONTAINER, 0, 173, 112);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.add(this.destroyItemSlot);
      } else if (var2.getType() == CreativeModeTab.Type.INVENTORY) {
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.clear();
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).slots.addAll(this.originalSlots);
         this.originalSlots = null;
      }

      if (selectedTab.getType() == CreativeModeTab.Type.SEARCH) {
         this.searchBox.setVisible(true);
         this.searchBox.setCanLoseFocus(false);
         this.searchBox.setFocused(true);
         if (var2 != var1) {
            this.searchBox.setValue("");
         }

         this.refreshSearchResults();
      } else {
         this.searchBox.setVisible(false);
         this.searchBox.setCanLoseFocus(true);
         this.searchBox.setFocused(false);
         this.searchBox.setValue("");
      }

      this.scrollOffs = 0.0F;
      ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(0.0F);
   }

   public boolean mouseScrolled(double var1, double var3, double var5) {
      if (!this.canScroll()) {
         return false;
      } else {
         this.scrollOffs = ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).subtractInputFromScroll(this.scrollOffs, var5);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
         return true;
      }
   }

   protected boolean hasClickedOutside(double var1, double var3, int var5, int var6, int var7) {
      boolean var8 = var1 < (double)var5 || var3 < (double)var6 || var1 >= (double)(var5 + this.imageWidth) || var3 >= (double)(var6 + this.imageHeight);
      this.hasClickedOutside = var8 && !this.checkTabClicked(selectedTab, var1, var3);
      return this.hasClickedOutside;
   }

   protected boolean insideScrollbar(double var1, double var3) {
      int var5 = this.leftPos;
      int var6 = this.topPos;
      int var7 = var5 + 175;
      int var8 = var6 + 18;
      int var9 = var7 + 14;
      int var10 = var8 + 112;
      return var1 >= (double)var7 && var3 >= (double)var8 && var1 < (double)var9 && var3 < (double)var10;
   }

   public boolean mouseDragged(double var1, double var3, int var5, double var6, double var8) {
      if (this.scrolling) {
         int var10 = this.topPos + 18;
         int var11 = var10 + 112;
         this.scrollOffs = ((float)var3 - (float)var10 - 7.5F) / ((float)(var11 - var10) - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         ((CreativeModeInventoryScreen.ItemPickerMenu)this.menu).scrollTo(this.scrollOffs);
         return true;
      } else {
         return super.mouseDragged(var1, var3, var5, var6, var8);
      }
   }

   public void render(GuiGraphics var1, int var2, int var3, float var4) {
      this.renderBackground(var1);
      super.render(var1, var2, var3, var4);
      Iterator var5 = CreativeModeTabs.tabs().iterator();

      while(var5.hasNext()) {
         CreativeModeTab var6 = (CreativeModeTab)var5.next();
         if (this.checkTabHovering(var1, var6, var2, var3)) {
            break;
         }
      }

      if (this.destroyItemSlot != null && selectedTab.getType() == CreativeModeTab.Type.INVENTORY && this.isHovering(this.destroyItemSlot.x, this.destroyItemSlot.y, 16, 16, (double)var2, (double)var3)) {
         var1.renderTooltip(this.font, TRASH_SLOT_TOOLTIP, var2, var3);
      }

      this.renderTooltip(var1, var2, var3);
   }

   public List<Component> getTooltipFromContainerItem(ItemStack var1) {
      boolean var2 = this.hoveredSlot != null && this.hoveredSlot instanceof CreativeModeInventoryScreen.CustomCreativeSlot;
      boolean var3 = selectedTab.getType() == CreativeModeTab.Type.CATEGORY;
      boolean var4 = selectedTab.getType() == CreativeModeTab.Type.SEARCH;
      TooltipFlag.Default var5 = this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
      TooltipFlag.Default var6 = var2 ? var5.asCreative() : var5;
      List var7 = var1.getTooltipLines(this.minecraft.player, var6);
      if (var3 && var2) {
         return var7;
      } else {
         ArrayList var8 = Lists.newArrayList(var7);
         if (var4 && var2) {
            this.visibleTags.forEach((var2x) -> {
               if (var1.is(var2x)) {
                  var8.add(1, Component.literal("#" + var2x.location()).withStyle(ChatFormatting.DARK_PURPLE));
               }

            });
         }

         int var9 = 1;
         Iterator var10 = CreativeModeTabs.tabs().iterator();

         while(var10.hasNext()) {
            CreativeModeTab var11 = (CreativeModeTab)var10.next();
            if (var11.getType() != CreativeModeTab.Type.SEARCH && var11.contains(var1)) {
               var8.add(var9++, var11.getDisplayName().copy().withStyle(ChatFormatting.BLUE));
            }
         }

         return var8;
      }
   }

   protected void renderBg(GuiGraphics var1, float var2, int var3, int var4) {
      Iterator var5 = CreativeModeTabs.tabs().iterator();

      while(var5.hasNext()) {
         CreativeModeTab var6 = (CreativeModeTab)var5.next();
         if (var6 != selectedTab) {
            this.renderTabButton(var1, var6);
         }
      }

      var1.blit(new ResourceLocation("textures/gui/container/creative_inventory/tab_" + selectedTab.getBackgroundSuffix()), this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
      this.searchBox.render(var1, var3, var4, var2);
      int var8 = this.leftPos + 175;
      int var9 = this.topPos + 18;
      int var7 = var9 + 112;
      if (selectedTab.canScroll()) {
         var1.blit(CREATIVE_TABS_LOCATION, var8, var9 + (int)((float)(var7 - var9 - 17) * this.scrollOffs), 232 + (this.canScroll() ? 0 : 12), 0, 12, 15);
      }

      this.renderTabButton(var1, selectedTab);
      if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
         InventoryScreen.renderEntityInInventoryFollowsMouse(var1, this.leftPos + 88, this.topPos + 45, 20, (float)(this.leftPos + 88 - var3), (float)(this.topPos + 45 - 30 - var4), this.minecraft.player);
      }

   }

   private int getTabX(CreativeModeTab var1) {
      int var2 = var1.column();
      boolean var3 = true;
      int var4 = 27 * var2;
      if (var1.isAlignedRight()) {
         var4 = this.imageWidth - 27 * (7 - var2) + 1;
      }

      return var4;
   }

   private int getTabY(CreativeModeTab var1) {
      byte var2 = 0;
      int var3;
      if (var1.row() == CreativeModeTab.Row.TOP) {
         var3 = var2 - 32;
      } else {
         var3 = var2 + this.imageHeight;
      }

      return var3;
   }

   protected boolean checkTabClicked(CreativeModeTab var1, double var2, double var4) {
      int var6 = this.getTabX(var1);
      int var7 = this.getTabY(var1);
      return var2 >= (double)var6 && var2 <= (double)(var6 + 26) && var4 >= (double)var7 && var4 <= (double)(var7 + 32);
   }

   protected boolean checkTabHovering(GuiGraphics var1, CreativeModeTab var2, int var3, int var4) {
      int var5 = this.getTabX(var2);
      int var6 = this.getTabY(var2);
      if (this.isHovering(var5 + 3, var6 + 3, 21, 27, (double)var3, (double)var4)) {
         var1.renderTooltip(this.font, var2.getDisplayName(), var3, var4);
         return true;
      } else {
         return false;
      }
   }

   protected void renderTabButton(GuiGraphics var1, CreativeModeTab var2) {
      boolean var3 = var2 == selectedTab;
      boolean var4 = var2.row() == CreativeModeTab.Row.TOP;
      int var5 = var2.column();
      int var6 = var5 * 26;
      int var7 = 0;
      int var8 = this.leftPos + this.getTabX(var2);
      int var9 = this.topPos;
      boolean var10 = true;
      if (var3) {
         var7 += 32;
      }

      if (var4) {
         var9 -= 28;
      } else {
         var7 += 64;
         var9 += this.imageHeight - 4;
      }

      var1.blit(CREATIVE_TABS_LOCATION, var8, var9, var6, var7, 26, 32);
      var1.pose().pushPose();
      var1.pose().translate(0.0F, 0.0F, 100.0F);
      var8 += 5;
      var9 += 8 + (var4 ? 1 : -1);
      ItemStack var11 = var2.getIconItem();
      var1.renderItem(var11, var8, var9);
      var1.renderItemDecorations(this.font, var11, var8, var9);
      var1.pose().popPose();
   }

   public boolean isInventoryOpen() {
      return selectedTab.getType() == CreativeModeTab.Type.INVENTORY;
   }

   public static void handleHotbarLoadOrSave(Minecraft var0, int var1, boolean var2, boolean var3) {
      LocalPlayer var4 = var0.player;
      HotbarManager var5 = var0.getHotbarManager();
      Hotbar var6 = var5.get(var1);
      int var7;
      if (var2) {
         for(var7 = 0; var7 < Inventory.getSelectionSize(); ++var7) {
            ItemStack var8 = (ItemStack)var6.get(var7);
            ItemStack var9 = var8.isItemEnabled(var4.level().enabledFeatures()) ? var8.copy() : ItemStack.EMPTY;
            var4.getInventory().setItem(var7, var9);
            var0.gameMode.handleCreativeModeItemAdd(var9, 36 + var7);
         }

         var4.inventoryMenu.broadcastChanges();
      } else if (var3) {
         for(var7 = 0; var7 < Inventory.getSelectionSize(); ++var7) {
            var6.set(var7, var4.getInventory().getItem(var7).copy());
         }

         Component var10 = var0.options.keyHotbarSlots[var1].getTranslatedKeyMessage();
         Component var11 = var0.options.keyLoadHotbarActivator.getTranslatedKeyMessage();
         MutableComponent var12 = Component.translatable("inventory.hotbarSaved", var11, var10);
         var0.gui.setOverlayMessage(var12, false);
         var0.getNarrator().sayNow((Component)var12);
         var5.save();
      }

   }

   public static class ItemPickerMenu extends AbstractContainerMenu {
      public final NonNullList<ItemStack> items = NonNullList.create();
      private final AbstractContainerMenu inventoryMenu;

      public ItemPickerMenu(Player var1) {
         super((MenuType)null, 0);
         this.inventoryMenu = var1.inventoryMenu;
         Inventory var2 = var1.getInventory();

         int var3;
         for(var3 = 0; var3 < 5; ++var3) {
            for(int var4 = 0; var4 < 9; ++var4) {
               this.addSlot(new CreativeModeInventoryScreen.CustomCreativeSlot(CreativeModeInventoryScreen.CONTAINER, var3 * 9 + var4, 9 + var4 * 18, 18 + var3 * 18));
            }
         }

         for(var3 = 0; var3 < 9; ++var3) {
            this.addSlot(new Slot(var2, var3, 9 + var3 * 18, 112));
         }

         this.scrollTo(0.0F);
      }

      public boolean stillValid(Player var1) {
         return true;
      }

      protected int calculateRowCount() {
         return Mth.positiveCeilDiv(this.items.size(), 9) - 5;
      }

      protected int getRowIndexForScroll(float var1) {
         return Math.max((int)((double)(var1 * (float)this.calculateRowCount()) + 0.5D), 0);
      }

      protected float getScrollForRowIndex(int var1) {
         return Mth.clamp((float)var1 / (float)this.calculateRowCount(), 0.0F, 1.0F);
      }

      protected float subtractInputFromScroll(float var1, double var2) {
         return Mth.clamp(var1 - (float)(var2 / (double)this.calculateRowCount()), 0.0F, 1.0F);
      }

      public void scrollTo(float var1) {
         int var2 = this.getRowIndexForScroll(var1);

         for(int var3 = 0; var3 < 5; ++var3) {
            for(int var4 = 0; var4 < 9; ++var4) {
               int var5 = var4 + (var3 + var2) * 9;
               if (var5 >= 0 && var5 < this.items.size()) {
                  CreativeModeInventoryScreen.CONTAINER.setItem(var4 + var3 * 9, (ItemStack)this.items.get(var5));
               } else {
                  CreativeModeInventoryScreen.CONTAINER.setItem(var4 + var3 * 9, ItemStack.EMPTY);
               }
            }
         }

      }

      public boolean canScroll() {
         return this.items.size() > 45;
      }

      public ItemStack quickMoveStack(Player var1, int var2) {
         if (var2 >= this.slots.size() - 9 && var2 < this.slots.size()) {
            Slot var3 = (Slot)this.slots.get(var2);
            if (var3 != null && var3.hasItem()) {
               var3.setByPlayer(ItemStack.EMPTY);
            }
         }

         return ItemStack.EMPTY;
      }

      public boolean canTakeItemForPickAll(ItemStack var1, Slot var2) {
         return var2.container != CreativeModeInventoryScreen.CONTAINER;
      }

      public boolean canDragTo(Slot var1) {
         return var1.container != CreativeModeInventoryScreen.CONTAINER;
      }

      public ItemStack getCarried() {
         return this.inventoryMenu.getCarried();
      }

      public void setCarried(ItemStack var1) {
         this.inventoryMenu.setCarried(var1);
      }
   }

   static class SlotWrapper extends Slot {
      final Slot target;

      public SlotWrapper(Slot var1, int var2, int var3, int var4) {
         super(var1.container, var2, var3, var4);
         this.target = var1;
      }

      public void onTake(Player var1, ItemStack var2) {
         this.target.onTake(var1, var2);
      }

      public boolean mayPlace(ItemStack var1) {
         return this.target.mayPlace(var1);
      }

      public ItemStack getItem() {
         return this.target.getItem();
      }

      public boolean hasItem() {
         return this.target.hasItem();
      }

      public void setByPlayer(ItemStack var1) {
         this.target.setByPlayer(var1);
      }

      public void set(ItemStack var1) {
         this.target.set(var1);
      }

      public void setChanged() {
         this.target.setChanged();
      }

      public int getMaxStackSize() {
         return this.target.getMaxStackSize();
      }

      public int getMaxStackSize(ItemStack var1) {
         return this.target.getMaxStackSize(var1);
      }

      @Nullable
      public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
         return this.target.getNoItemIcon();
      }

      public ItemStack remove(int var1) {
         return this.target.remove(var1);
      }

      public boolean isActive() {
         return this.target.isActive();
      }

      public boolean mayPickup(Player var1) {
         return this.target.mayPickup(var1);
      }
   }

   private static class CustomCreativeSlot extends Slot {
      public CustomCreativeSlot(Container var1, int var2, int var3, int var4) {
         super(var1, var2, var3, var4);
      }

      public boolean mayPickup(Player var1) {
         ItemStack var2 = this.getItem();
         if (super.mayPickup(var1) && !var2.isEmpty()) {
            return var2.isItemEnabled(var1.level().enabledFeatures()) && var2.getTagElement("CustomCreativeLock") == null;
         } else {
            return var2.isEmpty();
         }
      }
   }
}
