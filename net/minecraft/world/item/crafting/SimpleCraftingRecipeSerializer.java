package net.minecraft.world.item.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class SimpleCraftingRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
   private final SimpleCraftingRecipeSerializer.Factory<T> constructor;

   public SimpleCraftingRecipeSerializer(SimpleCraftingRecipeSerializer.Factory<T> var1) {
      this.constructor = var1;
   }

   public T fromJson(ResourceLocation var1, JsonObject var2) {
      CraftingBookCategory var3 = (CraftingBookCategory)CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(var2, "category", (String)null), CraftingBookCategory.MISC);
      return this.constructor.create(var1, var3);
   }

   public T fromNetwork(ResourceLocation var1, FriendlyByteBuf var2) {
      CraftingBookCategory var3 = (CraftingBookCategory)var2.readEnum(CraftingBookCategory.class);
      return this.constructor.create(var1, var3);
   }

   public void toNetwork(FriendlyByteBuf var1, T var2) {
      var1.writeEnum(var2.category());
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void toNetwork(FriendlyByteBuf var1, Recipe var2) {
      this.toNetwork(var1, (CraftingRecipe)var2);
   }

   // $FF: synthetic method
   public Recipe fromNetwork(ResourceLocation var1, FriendlyByteBuf var2) {
      return this.fromNetwork(var1, var2);
   }

   // $FF: synthetic method
   public Recipe fromJson(ResourceLocation var1, JsonObject var2) {
      return this.fromJson(var1, var2);
   }

   @FunctionalInterface
   public interface Factory<T extends CraftingRecipe> {
      T create(ResourceLocation var1, CraftingBookCategory var2);
   }
}
