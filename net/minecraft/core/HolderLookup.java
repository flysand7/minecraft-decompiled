package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;

public interface HolderLookup<T> extends HolderGetter<T> {
   Stream<Holder.Reference<T>> listElements();

   default Stream<ResourceKey<T>> listElementIds() {
      return this.listElements().map(Holder.Reference::key);
   }

   Stream<HolderSet.Named<T>> listTags();

   default Stream<TagKey<T>> listTagIds() {
      return this.listTags().map(HolderSet.Named::key);
   }

   default HolderLookup<T> filterElements(final Predicate<T> var1) {
      return new HolderLookup.Delegate<T>(this) {
         public Optional<Holder.Reference<T>> get(ResourceKey<T> var1x) {
            return this.parent.get(var1x).filter((var1xx) -> {
               return var1.test(var1xx.value());
            });
         }

         public Stream<Holder.Reference<T>> listElements() {
            return this.parent.listElements().filter((var1x) -> {
               return var1.test(var1x.value());
            });
         }
      };
   }

   public interface Provider {
      <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

      default <T> HolderLookup.RegistryLookup<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> var1) {
         return (HolderLookup.RegistryLookup)this.lookup(var1).orElseThrow(() -> {
            return new IllegalStateException("Registry " + var1.location() + " not found");
         });
      }

      default HolderGetter.Provider asGetterLookup() {
         return new HolderGetter.Provider() {
            public <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1) {
               return Provider.this.lookup(var1).map((var0) -> {
                  return var0;
               });
            }
         };
      }

      static HolderLookup.Provider create(Stream<HolderLookup.RegistryLookup<?>> var0) {
         final Map var1 = (Map)var0.collect(Collectors.toUnmodifiableMap(HolderLookup.RegistryLookup::key, (var0x) -> {
            return var0x;
         }));
         return new HolderLookup.Provider() {
            public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1x) {
               return Optional.ofNullable((HolderLookup.RegistryLookup)var1.get(var1x));
            }
         };
      }
   }

   public static class Delegate<T> implements HolderLookup<T> {
      protected final HolderLookup<T> parent;

      public Delegate(HolderLookup<T> var1) {
         this.parent = var1;
      }

      public Optional<Holder.Reference<T>> get(ResourceKey<T> var1) {
         return this.parent.get(var1);
      }

      public Stream<Holder.Reference<T>> listElements() {
         return this.parent.listElements();
      }

      public Optional<HolderSet.Named<T>> get(TagKey<T> var1) {
         return this.parent.get(var1);
      }

      public Stream<HolderSet.Named<T>> listTags() {
         return this.parent.listTags();
      }
   }

   public interface RegistryLookup<T> extends HolderLookup<T>, HolderOwner<T> {
      ResourceKey<? extends Registry<? extends T>> key();

      Lifecycle registryLifecycle();

      default HolderLookup<T> filterFeatures(FeatureFlagSet var1) {
         return (HolderLookup)(FeatureElement.FILTERED_REGISTRIES.contains(this.key()) ? this.filterElements((var1x) -> {
            return ((FeatureElement)var1x).isEnabled(var1);
         }) : this);
      }

      public abstract static class Delegate<T> implements HolderLookup.RegistryLookup<T> {
         public Delegate() {
         }

         protected abstract HolderLookup.RegistryLookup<T> parent();

         public ResourceKey<? extends Registry<? extends T>> key() {
            return this.parent().key();
         }

         public Lifecycle registryLifecycle() {
            return this.parent().registryLifecycle();
         }

         public Optional<Holder.Reference<T>> get(ResourceKey<T> var1) {
            return this.parent().get(var1);
         }

         public Stream<Holder.Reference<T>> listElements() {
            return this.parent().listElements();
         }

         public Optional<HolderSet.Named<T>> get(TagKey<T> var1) {
            return this.parent().get(var1);
         }

         public Stream<HolderSet.Named<T>> listTags() {
            return this.parent().listTags();
         }
      }
   }
}
