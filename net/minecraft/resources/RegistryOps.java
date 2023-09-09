package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.util.ExtraCodecs;

public class RegistryOps<T> extends DelegatingOps<T> {
   private final RegistryOps.RegistryInfoLookup lookupProvider;

   private static RegistryOps.RegistryInfoLookup memoizeLookup(final RegistryOps.RegistryInfoLookup var0) {
      return new RegistryOps.RegistryInfoLookup() {
         private final Map<ResourceKey<? extends Registry<?>>, Optional<? extends RegistryOps.RegistryInfo<?>>> lookups = new HashMap();

         public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1) {
            Map var10000 = this.lookups;
            RegistryOps.RegistryInfoLookup var10002 = var0;
            Objects.requireNonNull(var10002);
            return (Optional)var10000.computeIfAbsent(var1, var10002::lookup);
         }
      };
   }

   public static <T> RegistryOps<T> create(DynamicOps<T> var0, final HolderLookup.Provider var1) {
      return create(var0, memoizeLookup(new RegistryOps.RegistryInfoLookup() {
         public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> var1x) {
            return var1.lookup(var1x).map((var0) -> {
               return new RegistryOps.RegistryInfo(var0, var0, var0.registryLifecycle());
            });
         }
      }));
   }

   public static <T> RegistryOps<T> create(DynamicOps<T> var0, RegistryOps.RegistryInfoLookup var1) {
      return new RegistryOps(var0, var1);
   }

   private RegistryOps(DynamicOps<T> var1, RegistryOps.RegistryInfoLookup var2) {
      super(var1);
      this.lookupProvider = var2;
   }

   public <E> Optional<HolderOwner<E>> owner(ResourceKey<? extends Registry<? extends E>> var1) {
      return this.lookupProvider.lookup(var1).map(RegistryOps.RegistryInfo::owner);
   }

   public <E> Optional<HolderGetter<E>> getter(ResourceKey<? extends Registry<? extends E>> var1) {
      return this.lookupProvider.lookup(var1).map(RegistryOps.RegistryInfo::getter);
   }

   public static <E, O> RecordCodecBuilder<O, HolderGetter<E>> retrieveGetter(ResourceKey<? extends Registry<? extends E>> var0) {
      return ExtraCodecs.retrieveContext((var1) -> {
         if (var1 instanceof RegistryOps) {
            RegistryOps var2 = (RegistryOps)var1;
            return (DataResult)var2.lookupProvider.lookup(var0).map((var0x) -> {
               return DataResult.success(var0x.getter(), var0x.elementsLifecycle());
            }).orElseGet(() -> {
               return DataResult.error(() -> {
                  return "Unknown registry: " + var0;
               });
            });
         } else {
            return DataResult.error(() -> {
               return "Not a registry ops";
            });
         }
      }).forGetter((var0x) -> {
         return null;
      });
   }

   public static <E, O> RecordCodecBuilder<O, Holder.Reference<E>> retrieveElement(ResourceKey<E> var0) {
      ResourceKey var1 = ResourceKey.createRegistryKey(var0.registry());
      return ExtraCodecs.retrieveContext((var2) -> {
         if (var2 instanceof RegistryOps) {
            RegistryOps var3 = (RegistryOps)var2;
            return (DataResult)var3.lookupProvider.lookup(var1).flatMap((var1x) -> {
               return var1x.getter().get(var0);
            }).map(DataResult::success).orElseGet(() -> {
               return DataResult.error(() -> {
                  return "Can't find value: " + var0;
               });
            });
         } else {
            return DataResult.error(() -> {
               return "Not a registry ops";
            });
         }
      }).forGetter((var0x) -> {
         return null;
      });
   }

   public interface RegistryInfoLookup {
      <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> var1);
   }

   public static record RegistryInfo<T>(HolderOwner<T> a, HolderGetter<T> b, Lifecycle c) {
      private final HolderOwner<T> owner;
      private final HolderGetter<T> getter;
      private final Lifecycle elementsLifecycle;

      public RegistryInfo(HolderOwner<T> var1, HolderGetter<T> var2, Lifecycle var3) {
         this.owner = var1;
         this.getter = var2;
         this.elementsLifecycle = var3;
      }

      public HolderOwner<T> owner() {
         return this.owner;
      }

      public HolderGetter<T> getter() {
         return this.getter;
      }

      public Lifecycle elementsLifecycle() {
         return this.elementsLifecycle;
      }
   }
}
