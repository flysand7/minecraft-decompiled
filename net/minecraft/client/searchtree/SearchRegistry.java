package net.minecraft.client.searchtree;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;

public class SearchRegistry implements ResourceManagerReloadListener {
   public static final SearchRegistry.Key<ItemStack> CREATIVE_NAMES = new SearchRegistry.Key();
   public static final SearchRegistry.Key<ItemStack> CREATIVE_TAGS = new SearchRegistry.Key();
   public static final SearchRegistry.Key<RecipeCollection> RECIPE_COLLECTIONS = new SearchRegistry.Key();
   private final Map<SearchRegistry.Key<?>, SearchRegistry.TreeEntry<?>> searchTrees = new HashMap();

   public SearchRegistry() {
   }

   public void onResourceManagerReload(ResourceManager var1) {
      Iterator var2 = this.searchTrees.values().iterator();

      while(var2.hasNext()) {
         SearchRegistry.TreeEntry var3 = (SearchRegistry.TreeEntry)var2.next();
         var3.refresh();
      }

   }

   public <T> void register(SearchRegistry.Key<T> var1, SearchRegistry.TreeBuilderSupplier<T> var2) {
      this.searchTrees.put(var1, new SearchRegistry.TreeEntry(var2));
   }

   private <T> SearchRegistry.TreeEntry<T> getSupplier(SearchRegistry.Key<T> var1) {
      SearchRegistry.TreeEntry var2 = (SearchRegistry.TreeEntry)this.searchTrees.get(var1);
      if (var2 == null) {
         throw new IllegalStateException("Tree builder not registered");
      } else {
         return var2;
      }
   }

   public <T> void populate(SearchRegistry.Key<T> var1, List<T> var2) {
      this.getSupplier(var1).populate(var2);
   }

   public <T> SearchTree<T> getTree(SearchRegistry.Key<T> var1) {
      return this.getSupplier(var1).tree;
   }

   static class TreeEntry<T> {
      private final SearchRegistry.TreeBuilderSupplier<T> factory;
      RefreshableSearchTree<T> tree = RefreshableSearchTree.empty();

      TreeEntry(SearchRegistry.TreeBuilderSupplier<T> var1) {
         this.factory = var1;
      }

      void populate(List<T> var1) {
         this.tree = (RefreshableSearchTree)this.factory.apply(var1);
         this.tree.refresh();
      }

      void refresh() {
         this.tree.refresh();
      }
   }

   public interface TreeBuilderSupplier<T> extends Function<List<T>, RefreshableSearchTree<T>> {
   }

   public static class Key<T> {
      public Key() {
      }
   }
}
