package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class MappedRegistry<T> implements WritableRegistry<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   final ResourceKey<? extends Registry<T>> key;
   private final ObjectList<Holder.Reference<T>> byId;
   private final Object2IntMap<T> toId;
   private final Map<ResourceLocation, Holder.Reference<T>> byLocation;
   private final Map<ResourceKey<T>, Holder.Reference<T>> byKey;
   private final Map<T, Holder.Reference<T>> byValue;
   private final Map<T, Lifecycle> lifecycles;
   private Lifecycle registryLifecycle;
   private volatile Map<TagKey<T>, HolderSet.Named<T>> tags;
   private boolean frozen;
   @Nullable
   private Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
   @Nullable
   private List<Holder.Reference<T>> holdersInOrder;
   private int nextId;
   private final HolderLookup.RegistryLookup<T> lookup;

   public MappedRegistry(ResourceKey<? extends Registry<T>> var1, Lifecycle var2) {
      this(var1, var2, false);
   }

   public MappedRegistry(ResourceKey<? extends Registry<T>> var1, Lifecycle var2, boolean var3) {
      this.byId = new ObjectArrayList(256);
      this.toId = (Object2IntMap)Util.make(new Object2IntOpenCustomHashMap(Util.identityStrategy()), (var0) -> {
         var0.defaultReturnValue(-1);
      });
      this.byLocation = new HashMap();
      this.byKey = new HashMap();
      this.byValue = new IdentityHashMap();
      this.lifecycles = new IdentityHashMap();
      this.tags = new IdentityHashMap();
      this.lookup = new HolderLookup.RegistryLookup<T>() {
         public ResourceKey<? extends Registry<? extends T>> key() {
            return MappedRegistry.this.key;
         }

         public Lifecycle registryLifecycle() {
            return MappedRegistry.this.registryLifecycle();
         }

         public Optional<Holder.Reference<T>> get(ResourceKey<T> var1) {
            return MappedRegistry.this.getHolder(var1);
         }

         public Stream<Holder.Reference<T>> listElements() {
            return MappedRegistry.this.holders();
         }

         public Optional<HolderSet.Named<T>> get(TagKey<T> var1) {
            return MappedRegistry.this.getTag(var1);
         }

         public Stream<HolderSet.Named<T>> listTags() {
            return MappedRegistry.this.getTags().map(Pair::getSecond);
         }
      };
      Bootstrap.checkBootstrapCalled(() -> {
         return "registry " + var1;
      });
      this.key = var1;
      this.registryLifecycle = var2;
      if (var3) {
         this.unregisteredIntrusiveHolders = new IdentityHashMap();
      }

   }

   public ResourceKey<? extends Registry<T>> key() {
      return this.key;
   }

   public String toString() {
      return "Registry[" + this.key + " (" + this.registryLifecycle + ")]";
   }

   private List<Holder.Reference<T>> holdersInOrder() {
      if (this.holdersInOrder == null) {
         this.holdersInOrder = this.byId.stream().filter(Objects::nonNull).toList();
      }

      return this.holdersInOrder;
   }

   private void validateWrite() {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen");
      }
   }

   private void validateWrite(ResourceKey<T> var1) {
      if (this.frozen) {
         throw new IllegalStateException("Registry is already frozen (trying to add key " + var1 + ")");
      }
   }

   public Holder.Reference<T> registerMapping(int var1, ResourceKey<T> var2, T var3, Lifecycle var4) {
      this.validateWrite(var2);
      Validate.notNull(var2);
      Validate.notNull(var3);
      if (this.byLocation.containsKey(var2.location())) {
         Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + var2 + "' to registry"));
      }

      if (this.byValue.containsKey(var3)) {
         Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + var3 + "' to registry"));
      }

      Holder.Reference var5;
      if (this.unregisteredIntrusiveHolders != null) {
         var5 = (Holder.Reference)this.unregisteredIntrusiveHolders.remove(var3);
         if (var5 == null) {
            throw new AssertionError("Missing intrusive holder for " + var2 + ":" + var3);
         }

         var5.bindKey(var2);
      } else {
         var5 = (Holder.Reference)this.byKey.computeIfAbsent(var2, (var1x) -> {
            return Holder.Reference.createStandAlone(this.holderOwner(), var1x);
         });
      }

      this.byKey.put(var2, var5);
      this.byLocation.put(var2.location(), var5);
      this.byValue.put(var3, var5);
      this.byId.size(Math.max(this.byId.size(), var1 + 1));
      this.byId.set(var1, var5);
      this.toId.put(var3, var1);
      if (this.nextId <= var1) {
         this.nextId = var1 + 1;
      }

      this.lifecycles.put(var3, var4);
      this.registryLifecycle = this.registryLifecycle.add(var4);
      this.holdersInOrder = null;
      return var5;
   }

   public Holder.Reference<T> register(ResourceKey<T> var1, T var2, Lifecycle var3) {
      return this.registerMapping(this.nextId, var1, var2, var3);
   }

   @Nullable
   public ResourceLocation getKey(T var1) {
      Holder.Reference var2 = (Holder.Reference)this.byValue.get(var1);
      return var2 != null ? var2.key().location() : null;
   }

   public Optional<ResourceKey<T>> getResourceKey(T var1) {
      return Optional.ofNullable((Holder.Reference)this.byValue.get(var1)).map(Holder.Reference::key);
   }

   public int getId(@Nullable T var1) {
      return this.toId.getInt(var1);
   }

   @Nullable
   public T get(@Nullable ResourceKey<T> var1) {
      return getValueFromNullable((Holder.Reference)this.byKey.get(var1));
   }

   @Nullable
   public T byId(int var1) {
      return var1 >= 0 && var1 < this.byId.size() ? getValueFromNullable((Holder.Reference)this.byId.get(var1)) : null;
   }

   public Optional<Holder.Reference<T>> getHolder(int var1) {
      return var1 >= 0 && var1 < this.byId.size() ? Optional.ofNullable((Holder.Reference)this.byId.get(var1)) : Optional.empty();
   }

   public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> var1) {
      return Optional.ofNullable((Holder.Reference)this.byKey.get(var1));
   }

   public Holder<T> wrapAsHolder(T var1) {
      Holder.Reference var2 = (Holder.Reference)this.byValue.get(var1);
      return (Holder)(var2 != null ? var2 : Holder.direct(var1));
   }

   Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> var1) {
      return (Holder.Reference)this.byKey.computeIfAbsent(var1, (var1x) -> {
         if (this.unregisteredIntrusiveHolders != null) {
            throw new IllegalStateException("This registry can't create new holders without value");
         } else {
            this.validateWrite(var1x);
            return Holder.Reference.createStandAlone(this.holderOwner(), var1x);
         }
      });
   }

   public int size() {
      return this.byKey.size();
   }

   public Lifecycle lifecycle(T var1) {
      return (Lifecycle)this.lifecycles.get(var1);
   }

   public Lifecycle registryLifecycle() {
      return this.registryLifecycle;
   }

   public Iterator<T> iterator() {
      return Iterators.transform(this.holdersInOrder().iterator(), Holder::value);
   }

   @Nullable
   public T get(@Nullable ResourceLocation var1) {
      Holder.Reference var2 = (Holder.Reference)this.byLocation.get(var1);
      return getValueFromNullable(var2);
   }

   @Nullable
   private static <T> T getValueFromNullable(@Nullable Holder.Reference<T> var0) {
      return var0 != null ? var0.value() : null;
   }

   public Set<ResourceLocation> keySet() {
      return Collections.unmodifiableSet(this.byLocation.keySet());
   }

   public Set<ResourceKey<T>> registryKeySet() {
      return Collections.unmodifiableSet(this.byKey.keySet());
   }

   public Set<Entry<ResourceKey<T>, T>> entrySet() {
      return Collections.unmodifiableSet(Maps.transformValues(this.byKey, Holder::value).entrySet());
   }

   public Stream<Holder.Reference<T>> holders() {
      return this.holdersInOrder().stream();
   }

   public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
      return this.tags.entrySet().stream().map((var0) -> {
         return Pair.of((TagKey)var0.getKey(), (HolderSet.Named)var0.getValue());
      });
   }

   public HolderSet.Named<T> getOrCreateTag(TagKey<T> var1) {
      HolderSet.Named var2 = (HolderSet.Named)this.tags.get(var1);
      if (var2 == null) {
         var2 = this.createTag(var1);
         IdentityHashMap var3 = new IdentityHashMap(this.tags);
         var3.put(var1, var2);
         this.tags = var3;
      }

      return var2;
   }

   private HolderSet.Named<T> createTag(TagKey<T> var1) {
      return new HolderSet.Named(this.holderOwner(), var1);
   }

   public Stream<TagKey<T>> getTagNames() {
      return this.tags.keySet().stream();
   }

   public boolean isEmpty() {
      return this.byKey.isEmpty();
   }

   public Optional<Holder.Reference<T>> getRandom(RandomSource var1) {
      return Util.getRandomSafe(this.holdersInOrder(), var1);
   }

   public boolean containsKey(ResourceLocation var1) {
      return this.byLocation.containsKey(var1);
   }

   public boolean containsKey(ResourceKey<T> var1) {
      return this.byKey.containsKey(var1);
   }

   public Registry<T> freeze() {
      if (this.frozen) {
         return this;
      } else {
         this.frozen = true;
         this.byValue.forEach((var0, var1x) -> {
            var1x.bindValue(var0);
         });
         List var1 = this.byKey.entrySet().stream().filter((var0) -> {
            return !((Holder.Reference)var0.getValue()).isBound();
         }).map((var0) -> {
            return ((ResourceKey)var0.getKey()).location();
         }).sorted().toList();
         if (!var1.isEmpty()) {
            ResourceKey var10002 = this.key();
            throw new IllegalStateException("Unbound values in registry " + var10002 + ": " + var1);
         } else {
            if (this.unregisteredIntrusiveHolders != null) {
               if (!this.unregisteredIntrusiveHolders.isEmpty()) {
                  throw new IllegalStateException("Some intrusive holders were not registered: " + this.unregisteredIntrusiveHolders.values());
               }

               this.unregisteredIntrusiveHolders = null;
            }

            return this;
         }
      }
   }

   public Holder.Reference<T> createIntrusiveHolder(T var1) {
      if (this.unregisteredIntrusiveHolders == null) {
         throw new IllegalStateException("This registry can't create intrusive holders");
      } else {
         this.validateWrite();
         return (Holder.Reference)this.unregisteredIntrusiveHolders.computeIfAbsent(var1, (var1x) -> {
            return Holder.Reference.createIntrusive(this.asLookup(), var1x);
         });
      }
   }

   public Optional<HolderSet.Named<T>> getTag(TagKey<T> var1) {
      return Optional.ofNullable((HolderSet.Named)this.tags.get(var1));
   }

   public void bindTags(Map<TagKey<T>, List<Holder<T>>> var1) {
      IdentityHashMap var2 = new IdentityHashMap();
      this.byKey.values().forEach((var1x) -> {
         var2.put(var1x, new ArrayList());
      });
      var1.forEach((var2x, var3x) -> {
         Iterator var4 = var3x.iterator();

         while(var4.hasNext()) {
            Holder var5 = (Holder)var4.next();
            if (!var5.canSerializeIn(this.asLookup())) {
               throw new IllegalStateException("Can't create named set " + var2x + " containing value " + var5 + " from outside registry " + this);
            }

            if (!(var5 instanceof Holder.Reference)) {
               throw new IllegalStateException("Found direct holder " + var5 + " value in tag " + var2x);
            }

            Holder.Reference var6 = (Holder.Reference)var5;
            ((List)var2.get(var6)).add(var2x);
         }

      });
      SetView var3 = Sets.difference(this.tags.keySet(), var1.keySet());
      if (!var3.isEmpty()) {
         LOGGER.warn("Not all defined tags for registry {} are present in data pack: {}", this.key(), var3.stream().map((var0) -> {
            return var0.location().toString();
         }).sorted().collect(Collectors.joining(", ")));
      }

      IdentityHashMap var4 = new IdentityHashMap(this.tags);
      var1.forEach((var2x, var3x) -> {
         ((HolderSet.Named)var4.computeIfAbsent(var2x, this::createTag)).bind(var3x);
      });
      var2.forEach(Holder.Reference::bindTags);
      this.tags = var4;
   }

   public void resetTags() {
      this.tags.values().forEach((var0) -> {
         var0.bind(List.of());
      });
      this.byKey.values().forEach((var0) -> {
         var0.bindTags(Set.of());
      });
   }

   public HolderGetter<T> createRegistrationLookup() {
      this.validateWrite();
      return new HolderGetter<T>() {
         public Optional<Holder.Reference<T>> get(ResourceKey<T> var1) {
            return Optional.of(this.getOrThrow(var1));
         }

         public Holder.Reference<T> getOrThrow(ResourceKey<T> var1) {
            return MappedRegistry.this.getOrCreateHolderOrThrow(var1);
         }

         public Optional<HolderSet.Named<T>> get(TagKey<T> var1) {
            return Optional.of(this.getOrThrow(var1));
         }

         public HolderSet.Named<T> getOrThrow(TagKey<T> var1) {
            return MappedRegistry.this.getOrCreateTag(var1);
         }
      };
   }

   public HolderOwner<T> holderOwner() {
      return this.lookup;
   }

   public HolderLookup.RegistryLookup<T> asLookup() {
      return this.lookup;
   }

   // $FF: synthetic method
   public Holder registerMapping(int var1, ResourceKey var2, Object var3, Lifecycle var4) {
      return this.registerMapping(var1, var2, var3, var4);
   }
}
