package net.minecraft.client;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.slf4j.Logger;

public class ClientRecipeBook extends RecipeBook {
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<RecipeBookCategories, List<RecipeCollection>> collectionsByTab = ImmutableMap.of();
   private List<RecipeCollection> allCollections = ImmutableList.of();

   public ClientRecipeBook() {
   }

   public void setupCollections(Iterable<Recipe<?>> var1, RegistryAccess var2) {
      Map var3 = categorizeAndGroupRecipes(var1);
      HashMap var4 = Maps.newHashMap();
      Builder var5 = ImmutableList.builder();
      var3.forEach((var3x, var4x) -> {
         Stream var10002 = var4x.stream().map((var1) -> {
            return new RecipeCollection(var2, var1);
         });
         Objects.requireNonNull(var5);
         var4.put(var3x, (List)var10002.peek(var5::add).collect(ImmutableList.toImmutableList()));
      });
      RecipeBookCategories.AGGREGATE_CATEGORIES.forEach((var1x, var2x) -> {
         var4.put(var1x, (List)var2x.stream().flatMap((var1) -> {
            return ((List)var4.getOrDefault(var1, ImmutableList.of())).stream();
         }).collect(ImmutableList.toImmutableList()));
      });
      this.collectionsByTab = ImmutableMap.copyOf(var4);
      this.allCollections = var5.build();
   }

   private static Map<RecipeBookCategories, List<List<Recipe<?>>>> categorizeAndGroupRecipes(Iterable<Recipe<?>> var0) {
      HashMap var1 = Maps.newHashMap();
      HashBasedTable var2 = HashBasedTable.create();
      Iterator var3 = var0.iterator();

      while(var3.hasNext()) {
         Recipe var4 = (Recipe)var3.next();
         if (!var4.isSpecial() && !var4.isIncomplete()) {
            RecipeBookCategories var5 = getCategory(var4);
            String var6 = var4.getGroup();
            if (var6.isEmpty()) {
               ((List)var1.computeIfAbsent(var5, (var0x) -> {
                  return Lists.newArrayList();
               })).add(ImmutableList.of(var4));
            } else {
               Object var7 = (List)var2.get(var5, var6);
               if (var7 == null) {
                  var7 = Lists.newArrayList();
                  var2.put(var5, var6, var7);
                  ((List)var1.computeIfAbsent(var5, (var0x) -> {
                     return Lists.newArrayList();
                  })).add(var7);
               }

               ((List)var7).add(var4);
            }
         }
      }

      return var1;
   }

   private static RecipeBookCategories getCategory(Recipe<?> var0) {
      RecipeBookCategories var4;
      if (var0 instanceof CraftingRecipe) {
         CraftingRecipe var5 = (CraftingRecipe)var0;
         switch(var5.category()) {
         case BUILDING:
            var4 = RecipeBookCategories.CRAFTING_BUILDING_BLOCKS;
            break;
         case EQUIPMENT:
            var4 = RecipeBookCategories.CRAFTING_EQUIPMENT;
            break;
         case REDSTONE:
            var4 = RecipeBookCategories.CRAFTING_REDSTONE;
            break;
         case MISC:
            var4 = RecipeBookCategories.CRAFTING_MISC;
            break;
         default:
            throw new IncompatibleClassChangeError();
         }

         return var4;
      } else {
         RecipeType var1 = var0.getType();
         if (var0 instanceof AbstractCookingRecipe) {
            AbstractCookingRecipe var2 = (AbstractCookingRecipe)var0;
            CookingBookCategory var3 = var2.category();
            if (var1 == RecipeType.SMELTING) {
               switch(var3) {
               case BLOCKS:
                  var4 = RecipeBookCategories.FURNACE_BLOCKS;
                  break;
               case FOOD:
                  var4 = RecipeBookCategories.FURNACE_FOOD;
                  break;
               case MISC:
                  var4 = RecipeBookCategories.FURNACE_MISC;
                  break;
               default:
                  throw new IncompatibleClassChangeError();
               }

               return var4;
            }

            if (var1 == RecipeType.BLASTING) {
               return var3 == CookingBookCategory.BLOCKS ? RecipeBookCategories.BLAST_FURNACE_BLOCKS : RecipeBookCategories.BLAST_FURNACE_MISC;
            }

            if (var1 == RecipeType.SMOKING) {
               return RecipeBookCategories.SMOKER_FOOD;
            }

            if (var1 == RecipeType.CAMPFIRE_COOKING) {
               return RecipeBookCategories.CAMPFIRE;
            }
         }

         if (var1 == RecipeType.STONECUTTING) {
            return RecipeBookCategories.STONECUTTER;
         } else if (var1 == RecipeType.SMITHING) {
            return RecipeBookCategories.SMITHING;
         } else {
            Logger var10000 = LOGGER;
            Object var10002 = LogUtils.defer(() -> {
               return BuiltInRegistries.RECIPE_TYPE.getKey(var0.getType());
            });
            Objects.requireNonNull(var0);
            var10000.warn("Unknown recipe category: {}/{}", var10002, LogUtils.defer(var0::getId));
            return RecipeBookCategories.UNKNOWN;
         }
      }
   }

   public List<RecipeCollection> getCollections() {
      return this.allCollections;
   }

   public List<RecipeCollection> getCollection(RecipeBookCategories var1) {
      return (List)this.collectionsByTab.getOrDefault(var1, Collections.emptyList());
   }
}
