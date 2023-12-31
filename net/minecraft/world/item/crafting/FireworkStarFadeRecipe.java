package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FireworkStarFadeRecipe extends CustomRecipe {
   private static final Ingredient STAR_INGREDIENT;

   public FireworkStarFadeRecipe(ResourceLocation var1, CraftingBookCategory var2) {
      super(var1, var2);
   }

   public boolean matches(CraftingContainer var1, Level var2) {
      boolean var3 = false;
      boolean var4 = false;

      for(int var5 = 0; var5 < var1.getContainerSize(); ++var5) {
         ItemStack var6 = var1.getItem(var5);
         if (!var6.isEmpty()) {
            if (var6.getItem() instanceof DyeItem) {
               var3 = true;
            } else {
               if (!STAR_INGREDIENT.test(var6)) {
                  return false;
               }

               if (var4) {
                  return false;
               }

               var4 = true;
            }
         }
      }

      return var4 && var3;
   }

   public ItemStack assemble(CraftingContainer var1, RegistryAccess var2) {
      ArrayList var3 = Lists.newArrayList();
      ItemStack var4 = null;

      for(int var5 = 0; var5 < var1.getContainerSize(); ++var5) {
         ItemStack var6 = var1.getItem(var5);
         Item var7 = var6.getItem();
         if (var7 instanceof DyeItem) {
            var3.add(((DyeItem)var7).getDyeColor().getFireworkColor());
         } else if (STAR_INGREDIENT.test(var6)) {
            var4 = var6.copyWithCount(1);
         }
      }

      if (var4 != null && !var3.isEmpty()) {
         var4.getOrCreateTagElement("Explosion").putIntArray("FadeColors", (List)var3);
         return var4;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean canCraftInDimensions(int var1, int var2) {
      return var1 * var2 >= 2;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_STAR_FADE;
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

   static {
      STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);
   }
}
