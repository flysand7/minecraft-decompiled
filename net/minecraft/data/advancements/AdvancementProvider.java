package net.minecraft.data.advancements;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class AdvancementProvider implements DataProvider {
   private final PackOutput.PathProvider pathProvider;
   private final List<AdvancementSubProvider> subProviders;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public AdvancementProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2, List<AdvancementSubProvider> var3) {
      this.pathProvider = var1.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
      this.subProviders = var3;
      this.registries = var2;
   }

   public CompletableFuture<?> run(CachedOutput var1) {
      return this.registries.thenCompose((var2) -> {
         HashSet var3 = new HashSet();
         ArrayList var4 = new ArrayList();
         Consumer var5 = (var4x) -> {
            if (!var3.add(var4x.getId())) {
               throw new IllegalStateException("Duplicate advancement " + var4x.getId());
            } else {
               Path var5 = this.pathProvider.json(var4x.getId());
               var4.add(DataProvider.saveStable(var1, var4x.deconstruct().serializeToJson(), var5));
            }
         };
         Iterator var6 = this.subProviders.iterator();

         while(var6.hasNext()) {
            AdvancementSubProvider var7 = (AdvancementSubProvider)var6.next();
            var7.generate(var2, var5);
         }

         return CompletableFuture.allOf((CompletableFuture[])var4.toArray((var0) -> {
            return new CompletableFuture[var0];
         }));
      });
   }

   public final String getName() {
      return "Advancements";
   }
}
