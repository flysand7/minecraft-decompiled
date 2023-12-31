package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class RegistriesDatapackGenerator implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final CompletableFuture<HolderLookup.Provider> registries;

   public RegistriesDatapackGenerator(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2) {
      this.registries = var2;
      this.output = var1;
   }

   public CompletableFuture<?> run(CachedOutput var1) {
      return this.registries.thenCompose((var2) -> {
         RegistryOps var3 = RegistryOps.create(JsonOps.INSTANCE, (HolderLookup.Provider)var2);
         return CompletableFuture.allOf((CompletableFuture[])RegistryDataLoader.WORLDGEN_REGISTRIES.stream().flatMap((var4) -> {
            return this.dumpRegistryCap(var1, var2, var3, var4).stream();
         }).toArray((var0) -> {
            return new CompletableFuture[var0];
         }));
      });
   }

   private <T> Optional<CompletableFuture<?>> dumpRegistryCap(CachedOutput var1, HolderLookup.Provider var2, DynamicOps<JsonElement> var3, RegistryDataLoader.RegistryData<T> var4) {
      ResourceKey var5 = var4.key();
      return var2.lookup(var5).map((var5x) -> {
         PackOutput.PathProvider var6 = this.output.createPathProvider(PackOutput.Target.DATA_PACK, var5.location().getPath());
         return CompletableFuture.allOf((CompletableFuture[])var5x.listElements().map((var4x) -> {
            return dumpValue(var6.json(var4x.key().location()), var1, var3, var4.elementCodec(), var4x.value());
         }).toArray((var0) -> {
            return new CompletableFuture[var0];
         }));
      });
   }

   private static <E> CompletableFuture<?> dumpValue(Path var0, CachedOutput var1, DynamicOps<JsonElement> var2, Encoder<E> var3, E var4) {
      Optional var5 = var3.encodeStart(var2, var4).resultOrPartial((var1x) -> {
         LOGGER.error("Couldn't serialize element {}: {}", var0, var1x);
      });
      return var5.isPresent() ? DataProvider.saveStable(var1, (JsonElement)var5.get(), var0) : CompletableFuture.completedFuture((Object)null);
   }

   public final String getName() {
      return "Registries";
   }
}
