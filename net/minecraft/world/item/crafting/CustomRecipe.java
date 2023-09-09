package net.minecraft.world.item.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public abstract class CustomRecipe implements CraftingRecipe {
   private final ResourceLocation id;
   private final CraftingBookCategory category;

   public CustomRecipe(ResourceLocation var1, CraftingBookCategory var2) {
      this.id = var1;
      this.category = var2;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public boolean isSpecial() {
      return true;
   }

   public ItemStack getResultItem(RegistryAccess var1) {
      return ItemStack.EMPTY;
   }

   public CraftingBookCategory category() {
      return this.category;
   }
}
