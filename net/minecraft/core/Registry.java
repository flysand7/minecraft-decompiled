package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public interface Registry<T> extends Keyable, IdMap<T> {
   ResourceKey<? extends Registry<T>> key();

   default Codec<T> byNameCodec() {
      Codec var1 = ResourceLocation.CODEC.flatXmap((var1x) -> {
         return (DataResult)Optional.ofNullable(this.get(var1x)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               ResourceKey var10000 = this.key();
               return "Unknown registry key in " + var10000 + ": " + var1x;
            });
         });
      }, (var1x) -> {
         return (DataResult)this.getResourceKey(var1x).map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               ResourceKey var10000 = this.key();
               return "Unknown registry element in " + var10000 + ":" + var1x;
            });
         });
      });
      Codec var2 = ExtraCodecs.idResolverCodec((var1x) -> {
         return this.getResourceKey(var1x).isPresent() ? this.getId(var1x) : -1;
      }, this::byId, -1);
      return ExtraCodecs.overrideLifecycle(ExtraCodecs.orCompressed(var1, var2), this::lifecycle, this::lifecycle);
   }

   default Codec<Holder<T>> holderByNameCodec() {
      Codec var1 = ResourceLocation.CODEC.flatXmap((var1x) -> {
         return (DataResult)this.getHolder(ResourceKey.create(this.key(), var1x)).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               ResourceKey var10000 = this.key();
               return "Unknown registry key in " + var10000 + ": " + var1x;
            });
         });
      }, (var1x) -> {
         return (DataResult)var1x.unwrapKey().map(ResourceKey::location).map(DataResult::success).orElseGet(() -> {
            return DataResult.error(() -> {
               ResourceKey var10000 = this.key();
               return "Unknown registry element in " + var10000 + ":" + var1x;
            });
         });
      });
      return ExtraCodecs.overrideLifecycle(var1, (var1x) -> {
         return this.lifecycle(var1x.value());
      }, (var1x) -> {
         return this.lifecycle(var1x.value());
      });
   }

   default <U> Stream<U> keys(DynamicOps<U> var1) {
      return this.keySet().stream().map((var1x) -> {
         return var1.createString(var1x.toString());
      });
   }

   @Nullable
   ResourceLocation getKey(T var1);

   Optional<ResourceKey<T>> getResourceKey(T var1);

   int getId(@Nullable T var1);

   @Nullable
   T get(@Nullable ResourceKey<T> var1);

   @Nullable
   T get(@Nullable ResourceLocation var1);

   Lifecycle lifecycle(T var1);

   Lifecycle registryLifecycle();

   default Optional<T> getOptional(@Nullable ResourceLocation var1) {
      return Optional.ofNullable(this.get(var1));
   }

   default Optional<T> getOptional(@Nullable ResourceKey<T> var1) {
      return Optional.ofNullable(this.get(var1));
   }

   default T getOrThrow(ResourceKey<T> var1) {
      Object var2 = this.get(var1);
      if (var2 == null) {
         ResourceKey var10002 = this.key();
         throw new IllegalStateException("Missing key in " + var10002 + ": " + var1);
      } else {
         return var2;
      }
   }

   Set<ResourceLocation> keySet();

   Set<Entry<ResourceKey<T>, T>> entrySet();

   Set<ResourceKey<T>> registryKeySet();

   Optional<Holder.Reference<T>> getRandom(RandomSource var1);

   default Stream<T> stream() {
      return StreamSupport.stream(this.spliterator(), false);
   }

   boolean containsKey(ResourceLocation var1);

   boolean containsKey(ResourceKey<T> var1);

   static <T> T register(Registry<? super T> var0, String var1, T var2) {
      return register(var0, new ResourceLocation(var1), var2);
   }

   static <V, T extends V> T register(Registry<V> var0, ResourceLocation var1, T var2) {
      return register(var0, ResourceKey.create(var0.key(), var1), var2);
   }

   static <V, T extends V> T register(Registry<V> var0, ResourceKey<V> var1, T var2) {
      ((WritableRegistry)var0).register(var1, var2, Lifecycle.stable());
      return var2;
   }

   static <T> Holder.Reference<T> registerForHolder(Registry<T> var0, ResourceKey<T> var1, T var2) {
      return ((WritableRegistry)var0).register(var1, var2, Lifecycle.stable());
   }

   static <T> Holder.Reference<T> registerForHolder(Registry<T> var0, ResourceLocation var1, T var2) {
      return registerForHolder(var0, ResourceKey.create(var0.key(), var1), var2);
   }

   static <V, T extends V> T registerMapping(Registry<V> var0, int var1, String var2, T var3) {
      ((WritableRegistry)var0).registerMapping(var1, ResourceKey.create(var0.key(), new ResourceLocation(var2)), var3, Lifecycle.stable());
      return var3;
   }

   Registry<T> freeze();

   Holder.Reference<T> createIntrusiveHolder(T var1);

   Optional<Holder.Reference<T>> getHolder(int var1);

   Optional<Holder.Reference<T>> getHolder(ResourceKey<T> var1);

   Holder<T> wrapAsHolder(T var1);

   default Holder.Reference<T> getHolderOrThrow(ResourceKey<T> var1) {
      return (Holder.Reference)this.getHolder(var1).orElseThrow(() -> {
         ResourceKey var10002 = this.key();
         return new IllegalStateException("Missing key in " + var10002 + ": " + var1);
      });
   }

   Stream<Holder.Reference<T>> holders();

   Optional<HolderSet.Named<T>> getTag(TagKey<T> var1);

   default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> var1) {
      return (Iterable)DataFixUtils.orElse(this.getTag(var1), List.of());
   }

   HolderSet.Named<T> getOrCreateTag(TagKey<T> var1);

   Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags();

   Stream<TagKey<T>> getTagNames();

   void resetTags();

   void bindTags(Map<TagKey<T>, List<Holder<T>>> var1);

   default IdMap<Holder<T>> asHolderIdMap() {
      return new IdMap<Holder<T>>() {
         public int getId(Holder<T> var1) {
            return Registry.this.getId(var1.value());
         }

         @Nullable
         public Holder<T> byId(int var1) {
            return (Holder)Registry.this.getHolder(var1).orElse((Object)null);
         }

         public int size() {
            return Registry.this.size();
         }

         public Iterator<Holder<T>> iterator() {
            return Registry.this.holders().map((var0) -> {
               return var0;
            }).iterator();
         }

         // $FF: synthetic method
         @Nullable
         public Object byId(int var1) {
            return this.byId(var1);
         }

         // $FF: synthetic method
         // $FF: bridge method
         public int getId(Object var1) {
            return this.getId((Holder)var1);
         }
      };
   }

   HolderOwner<T> holderOwner();

   HolderLookup.RegistryLookup<T> asLookup();

   default HolderLookup.RegistryLookup<T> asTagAddingLookup() {
      return new HolderLookup.RegistryLookup.Delegate<T>() {
         protected HolderLookup.RegistryLookup<T> parent() {
            return Registry.this.asLookup();
         }

         public Optional<HolderSet.Named<T>> get(TagKey<T> var1) {
            return Optional.of(this.getOrThrow(var1));
         }

         public HolderSet.Named<T> getOrThrow(TagKey<T> var1) {
            return Registry.this.getOrCreateTag(var1);
         }
      };
   }
}
