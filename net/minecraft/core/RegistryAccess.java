package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public interface RegistryAccess extends HolderLookup.Provider {
   Logger LOGGER = LogUtils.getLogger();
   RegistryAccess.Frozen EMPTY = (new RegistryAccess.ImmutableRegistryAccess(Map.of())).freeze();

   <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> var1);

   default <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1) {
      return this.registry(var1).map(Registry::asLookup);
   }

   default <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> var1) {
      return (Registry)this.registry(var1).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + var1);
      });
   }

   Stream<RegistryAccess.RegistryEntry<?>> registries();

   static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> var0) {
      return new RegistryAccess.Frozen() {
         public <T> Optional<Registry<T>> registry(ResourceKey<? extends Registry<? extends T>> var1) {
            Registry var2 = var0;
            return var2.getOptional(var1);
         }

         public Stream<RegistryAccess.RegistryEntry<?>> registries() {
            return var0.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
         }

         public RegistryAccess.Frozen freeze() {
            return this;
         }
      };
   }

   default RegistryAccess.Frozen freeze() {
      class FrozenAccess extends RegistryAccess.ImmutableRegistryAccess implements RegistryAccess.Frozen {
         protected FrozenAccess(Stream<RegistryAccess.RegistryEntry<?>> var2) {
            super(var2);
         }
      }

      return new FrozenAccess(this.registries().map(RegistryAccess.RegistryEntry::freeze));
   }

   default Lifecycle allRegistriesLifecycle() {
      return (Lifecycle)this.registries().map((var0) -> {
         return var0.value.registryLifecycle();
      }).reduce(Lifecycle.stable(), Lifecycle::add);
   }

   public static record RegistryEntry<T>(ResourceKey<? extends Registry<T>> a, Registry<T> b) {
      private final ResourceKey<? extends Registry<T>> key;
      final Registry<T> value;

      public RegistryEntry(ResourceKey<? extends Registry<T>> var1, Registry<T> var2) {
         this.key = var1;
         this.value = var2;
      }

      private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(Entry<? extends ResourceKey<? extends Registry<?>>, R> var0) {
         return fromUntyped((ResourceKey)var0.getKey(), (Registry)var0.getValue());
      }

      private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> var0, Registry<?> var1) {
         return new RegistryAccess.RegistryEntry(var0, var1);
      }

      private RegistryAccess.RegistryEntry<T> freeze() {
         return new RegistryAccess.RegistryEntry(this.key, this.value.freeze());
      }

      public ResourceKey<? extends Registry<T>> key() {
         return this.key;
      }

      public Registry<T> value() {
         return this.value;
      }
   }

   public static class ImmutableRegistryAccess implements RegistryAccess {
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

      public ImmutableRegistryAccess(List<? extends Registry<?>> var1) {
         this.registries = (Map)var1.stream().collect(Collectors.toUnmodifiableMap(Registry::key, (var0) -> {
            return var0;
         }));
      }

      public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> var1) {
         this.registries = Map.copyOf(var1);
      }

      public ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> var1) {
         this.registries = (Map)var1.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
      }

      public <E> Optional<Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> var1) {
         return Optional.ofNullable((Registry)this.registries.get(var1)).map((var0) -> {
            return var0;
         });
      }

      public Stream<RegistryAccess.RegistryEntry<?>> registries() {
         return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
      }
   }

   public interface Frozen extends RegistryAccess {
   }
}
