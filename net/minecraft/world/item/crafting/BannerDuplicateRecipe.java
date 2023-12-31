package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;

public class BannerDuplicateRecipe extends CustomRecipe {
   public BannerDuplicateRecipe(ResourceLocation var1, CraftingBookCategory var2) {
      super(var1, var2);
   }

   public boolean matches(CraftingContainer var1, Level var2) {
      DyeColor var3 = null;
      ItemStack var4 = null;
      ItemStack var5 = null;

      for(int var6 = 0; var6 < var1.getContainerSize(); ++var6) {
         ItemStack var7 = var1.getItem(var6);
         if (!var7.isEmpty()) {
            Item var8 = var7.getItem();
            if (!(var8 instanceof BannerItem)) {
               return false;
            }

            BannerItem var9 = (BannerItem)var8;
            if (var3 == null) {
               var3 = var9.getColor();
            } else if (var3 != var9.getColor()) {
               return false;
            }

            int var10 = BannerBlockEntity.getPatternCount(var7);
            if (var10 > 6) {
               return false;
            }

            if (var10 > 0) {
               if (var4 != null) {
                  return false;
               }

               var4 = var7;
            } else {
               if (var5 != null) {
                  return false;
               }

               var5 = var7;
            }
         }
      }

      return var4 != null && var5 != null;
   }

   public ItemStack assemble(CraftingContainer var1, RegistryAccess var2) {
      for(int var3 = 0; var3 < var1.getContainerSize(); ++var3) {
         ItemStack var4 = var1.getItem(var3);
         if (!var4.isEmpty()) {
            int var5 = BannerBlockEntity.getPatternCount(var4);
            if (var5 > 0 && var5 <= 6) {
               return var4.copyWithCount(1);
            }
         }
      }

      return ItemStack.EMPTY;
   }

   public NonNullList<ItemStack> getRemainingItems(CraftingContainer var1) {
      NonNullList var2 = NonNullList.withSize(var1.getContainerSize(), ItemStack.EMPTY);

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         ItemStack var4 = var1.getItem(var3);
         if (!var4.isEmpty()) {
            if (var4.getItem().hasCraftingRemainingItem()) {
               var2.set(var3, new ItemStack(var4.getItem().getCraftingRemainingItem()));
            } else if (var4.hasTag() && BannerBlockEntity.getPatternCount(var4) > 0) {
               var2.set(var3, var4.copyWithCount(1));
            }
         }
      }

      return var2;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.BANNER_DUPLICATE;
   }

   public boolean canCraftInDimensions(int var1, int var2) {
      return var1 * var2 >= 2;
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
