package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import org.slf4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final Function<ResourceLocation, Optional<? extends T>> idToValue;
   private final String directory;

   public TagLoader(Function<ResourceLocation, Optional<? extends T>> var1, String var2) {
      this.idToValue = var1;
      this.directory = var2;
   }

   public Map<ResourceLocation, List<TagLoader.EntryWithSource>> load(ResourceManager var1) {
      HashMap var2 = Maps.newHashMap();
      FileToIdConverter var3 = FileToIdConverter.json(this.directory);
      Iterator var4 = var3.listMatchingResourceStacks(var1).entrySet().iterator();

      while(var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         ResourceLocation var6 = (ResourceLocation)var5.getKey();
         ResourceLocation var7 = var3.fileToId(var6);
         Iterator var8 = ((List)var5.getValue()).iterator();

         while(var8.hasNext()) {
            Resource var9 = (Resource)var8.next();

            try {
               BufferedReader var10 = var9.openAsReader();

               try {
                  JsonElement var11 = JsonParser.parseReader(var10);
                  List var12 = (List)var2.computeIfAbsent(var7, (var0) -> {
                     return new ArrayList();
                  });
                  DataResult var10000 = TagFile.CODEC.parse(new Dynamic(JsonOps.INSTANCE, var11));
                  Logger var10002 = LOGGER;
                  Objects.requireNonNull(var10002);
                  TagFile var13 = (TagFile)var10000.getOrThrow(false, var10002::error);
                  if (var13.replace()) {
                     var12.clear();
                  }

                  String var14 = var9.sourcePackId();
                  var13.entries().forEach((var2x) -> {
                     var12.add(new TagLoader.EntryWithSource(var2x, var14));
                  });
               } catch (Throwable var16) {
                  if (var10 != null) {
                     try {
                        var10.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (var10 != null) {
                  var10.close();
               }
            } catch (Exception var17) {
               LOGGER.error("Couldn't read tag list {} from {} in data pack {}", new Object[]{var7, var6, var9.sourcePackId(), var17});
            }
         }
      }

      return var2;
   }

   private Either<Collection<TagLoader.EntryWithSource>, Collection<T>> build(TagEntry.Lookup<T> var1, List<TagLoader.EntryWithSource> var2) {
      Builder var3 = ImmutableSet.builder();
      ArrayList var4 = new ArrayList();
      Iterator var5 = var2.iterator();

      while(var5.hasNext()) {
         TagLoader.EntryWithSource var6 = (TagLoader.EntryWithSource)var5.next();
         TagEntry var10000 = var6.entry();
         Objects.requireNonNull(var3);
         if (!var10000.build(var1, var3::add)) {
            var4.add(var6);
         }
      }

      return var4.isEmpty() ? Either.right(var3.build()) : Either.left(var4);
   }

   public Map<ResourceLocation, Collection<T>> build(Map<ResourceLocation, List<TagLoader.EntryWithSource>> var1) {
      final HashMap var2 = Maps.newHashMap();
      TagEntry.Lookup var3 = new TagEntry.Lookup<T>() {
         @Nullable
         public T element(ResourceLocation var1) {
            return ((Optional)TagLoader.this.idToValue.apply(var1)).orElse((Object)null);
         }

         @Nullable
         public Collection<T> tag(ResourceLocation var1) {
            return (Collection)var2.get(var1);
         }
      };
      DependencySorter var4 = new DependencySorter();
      var1.forEach((var1x, var2x) -> {
         var4.addEntry(var1x, new TagLoader.SortingEntry(var2x));
      });
      var4.orderByDependencies((var3x, var4x) -> {
         this.build(var3, var4x.entries).ifLeft((var1) -> {
            LOGGER.error("Couldn't load tag {} as it is missing following references: {}", var3x, var1.stream().map(Objects::toString).collect(Collectors.joining(", ")));
         }).ifRight((var2x) -> {
            var2.put(var3x, var2x);
         });
      });
      return var2;
   }

   public Map<ResourceLocation, Collection<T>> loadAndBuild(ResourceManager var1) {
      return this.build(this.load(var1));
   }

   public static record EntryWithSource(TagEntry a, String b) {
      final TagEntry entry;
      private final String source;

      public EntryWithSource(TagEntry var1, String var2) {
         this.entry = var1;
         this.source = var2;
      }

      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }

      public TagEntry entry() {
         return this.entry;
      }

      public String source() {
         return this.source;
      }
   }

   private static record SortingEntry(List<TagLoader.EntryWithSource> a) implements DependencySorter.Entry<ResourceLocation> {
      final List<TagLoader.EntryWithSource> entries;

      SortingEntry(List<TagLoader.EntryWithSource> var1) {
         this.entries = var1;
      }

      public void visitRequiredDependencies(Consumer<ResourceLocation> var1) {
         this.entries.forEach((var1x) -> {
            var1x.entry.visitRequiredDependencies(var1);
         });
      }

      public void visitOptionalDependencies(Consumer<ResourceLocation> var1) {
         this.entries.forEach((var1x) -> {
            var1x.entry.visitOptionalDependencies(var1);
         });
      }

      public List<TagLoader.EntryWithSource> entries() {
         return this.entries;
      }
   }
}
