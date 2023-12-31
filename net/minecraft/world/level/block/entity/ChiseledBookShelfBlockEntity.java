package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.slf4j.Logger;

public class ChiseledBookShelfBlockEntity extends BlockEntity implements Container {
   public static final int MAX_BOOKS_IN_STORAGE = 6;
   private static final Logger LOGGER = LogUtils.getLogger();
   private final NonNullList<ItemStack> items;
   private int lastInteractedSlot;

   public ChiseledBookShelfBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.CHISELED_BOOKSHELF, var1, var2);
      this.items = NonNullList.withSize(6, ItemStack.EMPTY);
      this.lastInteractedSlot = -1;
   }

   private void updateState(int var1) {
      if (var1 >= 0 && var1 < 6) {
         this.lastInteractedSlot = var1;
         BlockState var2 = this.getBlockState();

         for(int var3 = 0; var3 < ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.size(); ++var3) {
            boolean var4 = !this.getItem(var3).isEmpty();
            BooleanProperty var5 = (BooleanProperty)ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(var3);
            var2 = (BlockState)var2.setValue(var5, var4);
         }

         ((Level)Objects.requireNonNull(this.level)).setBlock(this.worldPosition, var2, 3);
      } else {
         LOGGER.error("Expected slot 0-5, got {}", var1);
      }
   }

   public void load(CompoundTag var1) {
      this.items.clear();
      ContainerHelper.loadAllItems(var1, this.items);
      this.lastInteractedSlot = var1.getInt("last_interacted_slot");
   }

   protected void saveAdditional(CompoundTag var1) {
      ContainerHelper.saveAllItems(var1, this.items, true);
      var1.putInt("last_interacted_slot", this.lastInteractedSlot);
   }

   public int count() {
      return (int)this.items.stream().filter(Predicate.not(ItemStack::isEmpty)).count();
   }

   public void clearContent() {
      this.items.clear();
   }

   public int getContainerSize() {
      return 6;
   }

   public boolean isEmpty() {
      return this.items.stream().allMatch(ItemStack::isEmpty);
   }

   public ItemStack getItem(int var1) {
      return (ItemStack)this.items.get(var1);
   }

   public ItemStack removeItem(int var1, int var2) {
      ItemStack var3 = (ItemStack)Objects.requireNonNullElse((ItemStack)this.items.get(var1), ItemStack.EMPTY);
      this.items.set(var1, ItemStack.EMPTY);
      if (!var3.isEmpty()) {
         this.updateState(var1);
      }

      return var3;
   }

   public ItemStack removeItemNoUpdate(int var1) {
      return this.removeItem(var1, 1);
   }

   public void setItem(int var1, ItemStack var2) {
      if (var2.is(ItemTags.BOOKSHELF_BOOKS)) {
         this.items.set(var1, var2);
         this.updateState(var1);
      }

   }

   public boolean canTakeItem(Container var1, int var2, ItemStack var3) {
      return var1.hasAnyMatching((var2x) -> {
         if (var2x.isEmpty()) {
            return true;
         } else {
            return ItemStack.isSameItemSameTags(var3, var2x) && var2x.getCount() + var3.getCount() <= Math.min(var2x.getMaxStackSize(), var1.getMaxStackSize());
         }
      });
   }

   public int getMaxStackSize() {
      return 1;
   }

   public boolean stillValid(Player var1) {
      return Container.stillValidBlockEntity(this, var1);
   }

   public boolean canPlaceItem(int var1, ItemStack var2) {
      return var2.is(ItemTags.BOOKSHELF_BOOKS) && this.getItem(var1).isEmpty();
   }

   public int getLastInteractedSlot() {
      return this.lastInteractedSlot;
   }
}
