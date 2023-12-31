package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class ShapedRecipe implements CraftingRecipe {
   final int width;
   final int height;
   final NonNullList<Ingredient> recipeItems;
   final ItemStack result;
   private final ResourceLocation id;
   final String group;
   final CraftingBookCategory category;
   final boolean showNotification;

   public ShapedRecipe(ResourceLocation var1, String var2, CraftingBookCategory var3, int var4, int var5, NonNullList<Ingredient> var6, ItemStack var7, boolean var8) {
      this.id = var1;
      this.group = var2;
      this.category = var3;
      this.width = var4;
      this.height = var5;
      this.recipeItems = var6;
      this.result = var7;
      this.showNotification = var8;
   }

   public ShapedRecipe(ResourceLocation var1, String var2, CraftingBookCategory var3, int var4, int var5, NonNullList<Ingredient> var6, ItemStack var7) {
      this(var1, var2, var3, var4, var5, var6, var7, true);
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SHAPED_RECIPE;
   }

   public String getGroup() {
      return this.group;
   }

   public CraftingBookCategory category() {
      return this.category;
   }

   public ItemStack getResultItem(RegistryAccess var1) {
      return this.result;
   }

   public NonNullList<Ingredient> getIngredients() {
      return this.recipeItems;
   }

   public boolean showNotification() {
      return this.showNotification;
   }

   public boolean canCraftInDimensions(int var1, int var2) {
      return var1 >= this.width && var2 >= this.height;
   }

   public boolean matches(CraftingContainer var1, Level var2) {
      for(int var3 = 0; var3 <= var1.getWidth() - this.width; ++var3) {
         for(int var4 = 0; var4 <= var1.getHeight() - this.height; ++var4) {
            if (this.matches(var1, var3, var4, true)) {
               return true;
            }

            if (this.matches(var1, var3, var4, false)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean matches(CraftingContainer var1, int var2, int var3, boolean var4) {
      for(int var5 = 0; var5 < var1.getWidth(); ++var5) {
         for(int var6 = 0; var6 < var1.getHeight(); ++var6) {
            int var7 = var5 - var2;
            int var8 = var6 - var3;
            Ingredient var9 = Ingredient.EMPTY;
            if (var7 >= 0 && var8 >= 0 && var7 < this.width && var8 < this.height) {
               if (var4) {
                  var9 = (Ingredient)this.recipeItems.get(this.width - var7 - 1 + var8 * this.width);
               } else {
                  var9 = (Ingredient)this.recipeItems.get(var7 + var8 * this.width);
               }
            }

            if (!var9.test(var1.getItem(var5 + var6 * var1.getWidth()))) {
               return false;
            }
         }
      }

      return true;
   }

   public ItemStack assemble(CraftingContainer var1, RegistryAccess var2) {
      return this.getResultItem(var2).copy();
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   static NonNullList<Ingredient> dissolvePattern(String[] var0, Map<String, Ingredient> var1, int var2, int var3) {
      NonNullList var4 = NonNullList.withSize(var2 * var3, Ingredient.EMPTY);
      HashSet var5 = Sets.newHashSet(var1.keySet());
      var5.remove(" ");

      for(int var6 = 0; var6 < var0.length; ++var6) {
         for(int var7 = 0; var7 < var0[var6].length(); ++var7) {
            String var8 = var0[var6].substring(var7, var7 + 1);
            Ingredient var9 = (Ingredient)var1.get(var8);
            if (var9 == null) {
               throw new JsonSyntaxException("Pattern references symbol '" + var8 + "' but it's not defined in the key");
            }

            var5.remove(var8);
            var4.set(var7 + var2 * var6, var9);
         }
      }

      if (!var5.isEmpty()) {
         throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + var5);
      } else {
         return var4;
      }
   }

   @VisibleForTesting
   static String[] shrink(String... var0) {
      int var1 = Integer.MAX_VALUE;
      int var2 = 0;
      int var3 = 0;
      int var4 = 0;

      for(int var5 = 0; var5 < var0.length; ++var5) {
         String var6 = var0[var5];
         var1 = Math.min(var1, firstNonSpace(var6));
         int var7 = lastNonSpace(var6);
         var2 = Math.max(var2, var7);
         if (var7 < 0) {
            if (var3 == var5) {
               ++var3;
            }

            ++var4;
         } else {
            var4 = 0;
         }
      }

      if (var0.length == var4) {
         return new String[0];
      } else {
         String[] var8 = new String[var0.length - var4 - var3];

         for(int var9 = 0; var9 < var8.length; ++var9) {
            var8[var9] = var0[var9 + var3].substring(var1, var2 + 1);
         }

         return var8;
      }
   }

   public boolean isIncomplete() {
      NonNullList var1 = this.getIngredients();
      return var1.isEmpty() || var1.stream().filter((var0) -> {
         return !var0.isEmpty();
      }).anyMatch((var0) -> {
         return var0.getItems().length == 0;
      });
   }

   private static int firstNonSpace(String var0) {
      int var1;
      for(var1 = 0; var1 < var0.length() && var0.charAt(var1) == ' '; ++var1) {
      }

      return var1;
   }

   private static int lastNonSpace(String var0) {
      int var1;
      for(var1 = var0.length() - 1; var1 >= 0 && var0.charAt(var1) == ' '; --var1) {
      }

      return var1;
   }

   static String[] patternFromJson(JsonArray var0) {
      String[] var1 = new String[var0.size()];
      if (var1.length > 3) {
         throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
      } else if (var1.length == 0) {
         throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
      } else {
         for(int var2 = 0; var2 < var1.length; ++var2) {
            String var3 = GsonHelper.convertToString(var0.get(var2), "pattern[" + var2 + "]");
            if (var3.length() > 3) {
               throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
            }

            if (var2 > 0 && var1[0].length() != var3.length()) {
               throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            }

            var1[var2] = var3;
         }

         return var1;
      }
   }

   static Map<String, Ingredient> keyFromJson(JsonObject var0) {
      HashMap var1 = Maps.newHashMap();
      Iterator var2 = var0.entrySet().iterator();

      while(var2.hasNext()) {
         Entry var3 = (Entry)var2.next();
         if (((String)var3.getKey()).length() != 1) {
            throw new JsonSyntaxException("Invalid key entry: '" + (String)var3.getKey() + "' is an invalid symbol (must be 1 character only).");
         }

         if (" ".equals(var3.getKey())) {
            throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
         }

         var1.put((String)var3.getKey(), Ingredient.fromJson((JsonElement)var3.getValue(), false));
      }

      var1.put(" ", Ingredient.EMPTY);
      return var1;
   }

   public static ItemStack itemStackFromJson(JsonObject var0) {
      Item var1 = itemFromJson(var0);
      if (var0.has("data")) {
         throw new JsonParseException("Disallowed data tag found");
      } else {
         int var2 = GsonHelper.getAsInt(var0, "count", 1);
         if (var2 < 1) {
            throw new JsonSyntaxException("Invalid output count: " + var2);
         } else {
            return new ItemStack(var1, var2);
         }
      }
   }

   public static Item itemFromJson(JsonObject var0) {
      String var1 = GsonHelper.getAsString(var0, "item");
      Item var2 = (Item)BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(var1)).orElseThrow(() -> {
         return new JsonSyntaxException("Unknown item '" + var1 + "'");
      });
      if (var2 == Items.AIR) {
         throw new JsonSyntaxException("Empty ingredient not allowed here");
      } else {
         return var2;
      }
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

   public static class Serializer implements RecipeSerializer<ShapedRecipe> {
      public Serializer() {
      }

      public ShapedRecipe fromJson(ResourceLocation var1, JsonObject var2) {
         String var3 = GsonHelper.getAsString(var2, "group", "");
         CraftingBookCategory var4 = (CraftingBookCategory)CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(var2, "category", (String)null), CraftingBookCategory.MISC);
         Map var5 = ShapedRecipe.keyFromJson(GsonHelper.getAsJsonObject(var2, "key"));
         String[] var6 = ShapedRecipe.shrink(ShapedRecipe.patternFromJson(GsonHelper.getAsJsonArray(var2, "pattern")));
         int var7 = var6[0].length();
         int var8 = var6.length;
         NonNullList var9 = ShapedRecipe.dissolvePattern(var6, var5, var7, var8);
         ItemStack var10 = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(var2, "result"));
         boolean var11 = GsonHelper.getAsBoolean(var2, "show_notification", true);
         return new ShapedRecipe(var1, var3, var4, var7, var8, var9, var10, var11);
      }

      public ShapedRecipe fromNetwork(ResourceLocation var1, FriendlyByteBuf var2) {
         int var3 = var2.readVarInt();
         int var4 = var2.readVarInt();
         String var5 = var2.readUtf();
         CraftingBookCategory var6 = (CraftingBookCategory)var2.readEnum(CraftingBookCategory.class);
         NonNullList var7 = NonNullList.withSize(var3 * var4, Ingredient.EMPTY);

         for(int var8 = 0; var8 < var7.size(); ++var8) {
            var7.set(var8, Ingredient.fromNetwork(var2));
         }

         ItemStack var10 = var2.readItem();
         boolean var9 = var2.readBoolean();
         return new ShapedRecipe(var1, var5, var6, var3, var4, var7, var10, var9);
      }

      public void toNetwork(FriendlyByteBuf var1, ShapedRecipe var2) {
         var1.writeVarInt(var2.width);
         var1.writeVarInt(var2.height);
         var1.writeUtf(var2.group);
         var1.writeEnum(var2.category);
         Iterator var3 = var2.recipeItems.iterator();

         while(var3.hasNext()) {
            Ingredient var4 = (Ingredient)var3.next();
            var4.toNetwork(var1);
         }

         var1.writeItem(var2.result);
         var1.writeBoolean(var2.showNotification);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void toNetwork(FriendlyByteBuf var1, Recipe var2) {
         this.toNetwork(var1, (ShapedRecipe)var2);
      }

      // $FF: synthetic method
      public Recipe fromNetwork(ResourceLocation var1, FriendlyByteBuf var2) {
         return this.fromNetwork(var1, var2);
      }

      // $FF: synthetic method
      public Recipe fromJson(ResourceLocation var1, JsonObject var2) {
         return this.fromJson(var1, var2);
      }
   }
}
