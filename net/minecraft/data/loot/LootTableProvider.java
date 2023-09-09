package net.minecraft.data.loot;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataResolver;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootTableProvider implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput.PathProvider pathProvider;
   private final Set<ResourceLocation> requiredTables;
   private final List<LootTableProvider.SubProviderEntry> subProviders;

   public LootTableProvider(PackOutput var1, Set<ResourceLocation> var2, List<LootTableProvider.SubProviderEntry> var3) {
      this.pathProvider = var1.createPathProvider(PackOutput.Target.DATA_PACK, "loot_tables");
      this.subProviders = var3;
      this.requiredTables = var2;
   }

   public CompletableFuture<?> run(CachedOutput var1) {
      final HashMap var2 = Maps.newHashMap();
      Object2ObjectOpenHashMap var3 = new Object2ObjectOpenHashMap();
      this.subProviders.forEach((var2x) -> {
         ((LootTableSubProvider)var2x.provider().get()).generate((var3x, var4) -> {
            ResourceLocation var5 = (ResourceLocation)var3.put(RandomSequence.seedForKey(var3x), var3x);
            if (var5 != null) {
               Util.logAndPauseIfInIde("Loot table random sequence seed collision on " + var5 + " and " + var3x);
            }

            var4.setRandomSequence(var3x);
            if (var2.put(var3x, var4.setParamSet(var2x.paramSet).build()) != null) {
               throw new IllegalStateException("Duplicate loot table " + var3x);
            }
         });
      });
      ValidationContext var4 = new ValidationContext(LootContextParamSets.ALL_PARAMS, new LootDataResolver() {
         @Nullable
         public <T> T getElement(LootDataId<T> var1) {
            return var1.type() == LootDataType.TABLE ? var2.get(var1.location()) : null;
         }
      });
      SetView var5 = Sets.difference(this.requiredTables, var2.keySet());
      Iterator var6 = var5.iterator();

      while(var6.hasNext()) {
         ResourceLocation var7 = (ResourceLocation)var6.next();
         var4.reportProblem("Missing built-in table: " + var7);
      }

      var2.forEach((var1x, var2x) -> {
         var2x.validate(var4.setParams(var2x.getParamSet()).enterElement("{" + var1x + "}", new LootDataId(LootDataType.TABLE, var1x)));
      });
      Multimap var8 = var4.getProblems();
      if (!var8.isEmpty()) {
         var8.forEach((var0, var1x) -> {
            LOGGER.warn("Found validation problem in {}: {}", var0, var1x);
         });
         throw new IllegalStateException("Failed to validate loot tables, see logs");
      } else {
         return CompletableFuture.allOf((CompletableFuture[])var2.entrySet().stream().map((var2x) -> {
            ResourceLocation var3 = (ResourceLocation)var2x.getKey();
            LootTable var4 = (LootTable)var2x.getValue();
            Path var5 = this.pathProvider.json(var3);
            return DataProvider.saveStable(var1, LootDataType.TABLE.parser().toJsonTree(var4), var5);
         }).toArray((var0) -> {
            return new CompletableFuture[var0];
         }));
      }
   }

   public final String getName() {
      return "Loot Tables";
   }

   public static record SubProviderEntry(Supplier<LootTableSubProvider> a, LootContextParamSet b) {
      private final Supplier<LootTableSubProvider> provider;
      final LootContextParamSet paramSet;

      public SubProviderEntry(Supplier<LootTableSubProvider> var1, LootContextParamSet var2) {
         this.provider = var1;
         this.paramSet = var2;
      }

      public Supplier<LootTableSubProvider> provider() {
         return this.provider;
      }

      public LootContextParamSet paramSet() {
         return this.paramSet;
      }
   }
}
