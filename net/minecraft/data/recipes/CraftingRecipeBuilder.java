package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.CraftingBookCategory;

public abstract class CraftingRecipeBuilder {
   public CraftingRecipeBuilder() {
   }

   protected static CraftingBookCategory determineBookCategory(RecipeCategory var0) {
      CraftingBookCategory var10000;
      switch(var0) {
      case BUILDING_BLOCKS:
         var10000 = CraftingBookCategory.BUILDING;
         break;
      case TOOLS:
      case COMBAT:
         var10000 = CraftingBookCategory.EQUIPMENT;
         break;
      case REDSTONE:
         var10000 = CraftingBookCategory.REDSTONE;
         break;
      default:
         var10000 = CraftingBookCategory.MISC;
      }

      return var10000;
   }

   protected abstract static class CraftingResult implements FinishedRecipe {
      private final CraftingBookCategory category;

      protected CraftingResult(CraftingBookCategory var1) {
         this.category = var1;
      }

      public void serializeRecipeData(JsonObject var1) {
         var1.addProperty("category", this.category.getSerializedName());
      }
   }
}
