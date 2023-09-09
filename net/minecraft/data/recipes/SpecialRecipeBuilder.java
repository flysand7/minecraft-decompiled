package net.minecraft.data.recipes;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class SpecialRecipeBuilder extends CraftingRecipeBuilder {
   final RecipeSerializer<?> serializer;

   public SpecialRecipeBuilder(RecipeSerializer<?> var1) {
      this.serializer = var1;
   }

   public static SpecialRecipeBuilder special(RecipeSerializer<? extends CraftingRecipe> var0) {
      return new SpecialRecipeBuilder(var0);
   }

   public void save(Consumer<FinishedRecipe> var1, final String var2) {
      var1.accept(new CraftingRecipeBuilder.CraftingResult(CraftingBookCategory.MISC) {
         public RecipeSerializer<?> getType() {
            return SpecialRecipeBuilder.this.serializer;
         }

         public ResourceLocation getId() {
            return new ResourceLocation(var2);
         }

         @Nullable
         public JsonObject serializeAdvancement() {
            return null;
         }

         public ResourceLocation getAdvancementId() {
            return new ResourceLocation("");
         }
      });
   }
}
