package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;

public class BookCloningRecipe extends CustomRecipe {
   public BookCloningRecipe(ResourceLocation var1, CraftingBookCategory var2) {
      super(var1, var2);
   }

   public boolean matches(CraftingContainer var1, Level var2) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for(int var5 = 0; var5 < var1.getContainerSize(); ++var5) {
         ItemStack var6 = var1.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.WRITTEN_BOOK)) {
               if (!var4.isEmpty()) {
                  return false;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.WRITABLE_BOOK)) {
                  return false;
               }

               ++var3;
            }
         }
      }

      return !var4.isEmpty() && var4.hasTag() && var3 > 0;
   }

   public ItemStack assemble(CraftingContainer var1, RegistryAccess var2) {
      int var3 = 0;
      ItemStack var4 = ItemStack.EMPTY;

      for(int var5 = 0; var5 < var1.getContainerSize(); ++var5) {
         ItemStack var6 = var1.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.is(Items.WRITTEN_BOOK)) {
               if (!var4.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               var4 = var6;
            } else {
               if (!var6.is(Items.WRITABLE_BOOK)) {
                  return ItemStack.EMPTY;
               }

               ++var3;
            }
         }
      }

      if (!var4.isEmpty() && var4.hasTag() && var3 >= 1 && WrittenBookItem.getGeneration(var4) < 2) {
         ItemStack var7 = new ItemStack(Items.WRITTEN_BOOK, var3);
         CompoundTag var8 = var4.getTag().copy();
         var8.putInt("generation", WrittenBookItem.getGeneration(var4) + 1);
         var7.setTag(var8);
         return var7;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public NonNullList<ItemStack> getRemainingItems(CraftingContainer var1) {
      NonNullList var2 = NonNullList.withSize(var1.getContainerSize(), ItemStack.EMPTY);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         ItemStack var4 = var1.getItem(var3);
         if (var4.getItem().hasCraftingRemainingItem()) {
            var2.set(var3, new ItemStack(var4.getItem().getCraftingRemainingItem()));
         } else if (var4.getItem() instanceof WrittenBookItem) {
            var2.set(var3, var4.copyWithCount(1));
            break;
         }
      }

      return var2;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.BOOK_CLONING;
   }

   public boolean canCraftInDimensions(int var1, int var2) {
      return var1 >= 3 && var2 >= 3;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public NonNullList getRemainingItems(Container var1) {
      return this.getRemainingItems((CraftingContainer)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ItemStack assemble(Container var1, RegistryAccess var2) {
      return this.assemble((CraftingContainer)var1, var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public boolean matches(Container var1, Level var2) {
      return this.matches((CraftingContainer)var1, var2);
   }
}
