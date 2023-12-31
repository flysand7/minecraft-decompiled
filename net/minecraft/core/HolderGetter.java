package net.minecraft.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface HolderGetter<T> {
   Optional<Holder.Reference<T>> get(ResourceKey<T> var1);

   default Holder.Reference<T> getOrThrow(ResourceKey<T> var1) {
      return (Holder.Reference)this.get(var1).orElseThrow(() -> {
         return new IllegalStateException("Missing element " + var1);
      });
   }

   Optional<HolderSet.Named<T>> get(TagKey<T> var1);

   default HolderSet.Named<T> getOrThrow(TagKey<T> var1) {
      return (HolderSet.Named)this.get(var1).orElseThrow(() -> {
         return new IllegalStateException("Missing tag " + var1);
      });
   }

   public interface Provider {
      <T> Optional<HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);

      default <T> HolderGetter<T> lookupOrThrow(ResourceKey<? extends Registry<? extends T>> var1) {
         return (HolderGetter)this.lookup(var1).orElseThrow(() -> {
            return new IllegalStateException("Registry " + var1.location() + " not found");
         });
      }
   }
}
