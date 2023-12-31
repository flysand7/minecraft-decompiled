package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(ImmutableMap.of(), ImmutableList.of());
   public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      Codec var10001 = GenerationStep.Carving.CODEC;
      Codec var10002 = ConfiguredWorldCarver.LIST_CODEC;
      Logger var10004 = LOGGER;
      Objects.requireNonNull(var10004);
      RecordCodecBuilder var1 = Codec.simpleMap(var10001, var10002.promotePartial(Util.prefix("Carver: ", var10004::error)), StringRepresentable.keys(GenerationStep.Carving.values())).fieldOf("carvers").forGetter((var0x) -> {
         return var0x.carvers;
      });
      var10002 = PlacedFeature.LIST_OF_LISTS_CODEC;
      var10004 = LOGGER;
      Objects.requireNonNull(var10004);
      return var0.group(var1, var10002.promotePartial(Util.prefix("Features: ", var10004::error)).fieldOf("features").forGetter((var0x) -> {
         return var0x.features;
      })).apply(var0, BiomeGenerationSettings::new);
   });
   private final Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> carvers;
   private final List<HolderSet<PlacedFeature>> features;
   private final Supplier<List<ConfiguredFeature<?, ?>>> flowerFeatures;
   private final Supplier<Set<PlacedFeature>> featureSet;

   BiomeGenerationSettings(Map<GenerationStep.Carving, HolderSet<ConfiguredWorldCarver<?>>> var1, List<HolderSet<PlacedFeature>> var2) {
      this.carvers = var1;
      this.features = var2;
      this.flowerFeatures = Suppliers.memoize(() -> {
         return (List)var2.stream().flatMap(HolderSet::stream).map(Holder::value).flatMap(PlacedFeature::getFeatures).filter((var0) -> {
            return var0.feature() == Feature.FLOWER;
         }).collect(ImmutableList.toImmutableList());
      });
      this.featureSet = Suppliers.memoize(() -> {
         return (Set)var2.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet());
      });
   }

   public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers(GenerationStep.Carving var1) {
      return (Iterable)Objects.requireNonNullElseGet((Iterable)this.carvers.get(var1), List::of);
   }

   public List<ConfiguredFeature<?, ?>> getFlowerFeatures() {
      return (List)this.flowerFeatures.get();
   }

   public List<HolderSet<PlacedFeature>> features() {
      return this.features;
   }

   public boolean hasFeature(PlacedFeature var1) {
      return ((Set)this.featureSet.get()).contains(var1);
   }

   public static class Builder extends BiomeGenerationSettings.PlainBuilder {
      private final HolderGetter<PlacedFeature> placedFeatures;
      private final HolderGetter<ConfiguredWorldCarver<?>> worldCarvers;

      public Builder(HolderGetter<PlacedFeature> var1, HolderGetter<ConfiguredWorldCarver<?>> var2) {
         this.placedFeatures = var1;
         this.worldCarvers = var2;
      }

      public BiomeGenerationSettings.Builder addFeature(GenerationStep.Decoration var1, ResourceKey<PlacedFeature> var2) {
         this.addFeature(var1.ordinal(), this.placedFeatures.getOrThrow(var2));
         return this;
      }

      public BiomeGenerationSettings.Builder addCarver(GenerationStep.Carving var1, ResourceKey<ConfiguredWorldCarver<?>> var2) {
         this.addCarver(var1, this.worldCarvers.getOrThrow(var2));
         return this;
      }
   }

   public static class PlainBuilder {
      private final Map<GenerationStep.Carving, List<Holder<ConfiguredWorldCarver<?>>>> carvers = Maps.newLinkedHashMap();
      private final List<List<Holder<PlacedFeature>>> features = Lists.newArrayList();

      public PlainBuilder() {
      }

      public BiomeGenerationSettings.PlainBuilder addFeature(GenerationStep.Decoration var1, Holder<PlacedFeature> var2) {
         return this.addFeature(var1.ordinal(), var2);
      }

      public BiomeGenerationSettings.PlainBuilder addFeature(int var1, Holder<PlacedFeature> var2) {
         this.addFeatureStepsUpTo(var1);
         ((List)this.features.get(var1)).add(var2);
         return this;
      }

      public BiomeGenerationSettings.PlainBuilder addCarver(GenerationStep.Carving var1, Holder<ConfiguredWorldCarver<?>> var2) {
         ((List)this.carvers.computeIfAbsent(var1, (var0) -> {
            return Lists.newArrayList();
         })).add(var2);
         return this;
      }

      private void addFeatureStepsUpTo(int var1) {
         while(this.features.size() <= var1) {
            this.features.add(Lists.newArrayList());
         }

      }

      public BiomeGenerationSettings build() {
         return new BiomeGenerationSettings((Map)this.carvers.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (var0) -> {
            return HolderSet.direct((List)var0.getValue());
         })), (List)this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList()));
      }
   }
}
