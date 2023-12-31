package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.PrimaryLevelData;

public record WorldDimensions(Registry<LevelStem> b) {
   private final Registry<LevelStem> dimensions;
   public static final MapCodec<WorldDimensions> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(RegistryCodecs.fullCodec(Registries.LEVEL_STEM, Lifecycle.stable(), LevelStem.CODEC).fieldOf("dimensions").forGetter(WorldDimensions::dimensions)).apply(var0, var0.stable(WorldDimensions::new));
   });
   private static final Set<ResourceKey<LevelStem>> BUILTIN_ORDER;
   private static final int VANILLA_DIMENSION_COUNT;

   public WorldDimensions(Registry<LevelStem> var1) {
      LevelStem var2 = (LevelStem)var1.get(LevelStem.OVERWORLD);
      if (var2 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         this.dimensions = var1;
      }
   }

   public static Stream<ResourceKey<LevelStem>> keysInOrder(Stream<ResourceKey<LevelStem>> var0) {
      return Stream.concat(BUILTIN_ORDER.stream(), var0.filter((var0x) -> {
         return !BUILTIN_ORDER.contains(var0x);
      }));
   }

   public WorldDimensions replaceOverworldGenerator(RegistryAccess var1, ChunkGenerator var2) {
      Registry var3 = var1.registryOrThrow(Registries.DIMENSION_TYPE);
      Registry var4 = withOverworld(var3, this.dimensions, var2);
      return new WorldDimensions(var4);
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> var0, Registry<LevelStem> var1, ChunkGenerator var2) {
      LevelStem var3 = (LevelStem)var1.get(LevelStem.OVERWORLD);
      Object var4 = var3 == null ? var0.getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD) : var3.type();
      return withOverworld(var1, (Holder)var4, var2);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> var0, Holder<DimensionType> var1, ChunkGenerator var2) {
      MappedRegistry var3 = new MappedRegistry(Registries.LEVEL_STEM, Lifecycle.experimental());
      var3.register(LevelStem.OVERWORLD, new LevelStem(var1, var2), Lifecycle.stable());
      Iterator var4 = var0.entrySet().iterator();

      while(var4.hasNext()) {
         java.util.Map.Entry var5 = (java.util.Map.Entry)var4.next();
         ResourceKey var6 = (ResourceKey)var5.getKey();
         if (var6 != LevelStem.OVERWORLD) {
            var3.register(var6, (LevelStem)var5.getValue(), var0.lifecycle((LevelStem)var5.getValue()));
         }
      }

      return var3.freeze();
   }

   public ChunkGenerator overworld() {
      LevelStem var1 = (LevelStem)this.dimensions.get(LevelStem.OVERWORLD);
      if (var1 == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return var1.generator();
      }
   }

   public Optional<LevelStem> get(ResourceKey<LevelStem> var1) {
      return this.dimensions.getOptional(var1);
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return (ImmutableSet)this.dimensions().entrySet().stream().map(java.util.Map.Entry::getKey).map(Registries::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   private static PrimaryLevelData.SpecialWorldProperty specialWorldProperty(Registry<LevelStem> var0) {
      return (PrimaryLevelData.SpecialWorldProperty)var0.getOptional(LevelStem.OVERWORLD).map((var0x) -> {
         ChunkGenerator var1 = var0x.generator();
         if (var1 instanceof DebugLevelSource) {
            return PrimaryLevelData.SpecialWorldProperty.DEBUG;
         } else {
            return var1 instanceof FlatLevelSource ? PrimaryLevelData.SpecialWorldProperty.FLAT : PrimaryLevelData.SpecialWorldProperty.NONE;
         }
      }).orElse(PrimaryLevelData.SpecialWorldProperty.NONE);
   }

   static Lifecycle checkStability(ResourceKey<LevelStem> var0, LevelStem var1) {
      return isVanillaLike(var0, var1) ? Lifecycle.stable() : Lifecycle.experimental();
   }

   private static boolean isVanillaLike(ResourceKey<LevelStem> var0, LevelStem var1) {
      if (var0 == LevelStem.OVERWORLD) {
         return isStableOverworld(var1);
      } else if (var0 == LevelStem.NETHER) {
         return isStableNether(var1);
      } else {
         return var0 == LevelStem.END ? isStableEnd(var1) : false;
      }
   }

   private static boolean isStableOverworld(LevelStem var0) {
      Holder var1 = var0.type();
      if (!var1.is(BuiltinDimensionTypes.OVERWORLD) && !var1.is(BuiltinDimensionTypes.OVERWORLD_CAVES)) {
         return false;
      } else {
         BiomeSource var3 = var0.generator().getBiomeSource();
         if (var3 instanceof MultiNoiseBiomeSource) {
            MultiNoiseBiomeSource var2 = (MultiNoiseBiomeSource)var3;
            if (!var2.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) {
               return false;
            }
         }

         return true;
      }
   }

   private static boolean isStableNether(LevelStem var0) {
      boolean var10000;
      if (var0.type().is(BuiltinDimensionTypes.NETHER)) {
         ChunkGenerator var3 = var0.generator();
         if (var3 instanceof NoiseBasedChunkGenerator) {
            NoiseBasedChunkGenerator var2 = (NoiseBasedChunkGenerator)var3;
            if (var2.stable(NoiseGeneratorSettings.NETHER)) {
               BiomeSource var4 = var2.getBiomeSource();
               if (var4 instanceof MultiNoiseBiomeSource) {
                  MultiNoiseBiomeSource var1 = (MultiNoiseBiomeSource)var4;
                  if (var1.stable(MultiNoiseBiomeSourceParameterLists.NETHER)) {
                     var10000 = true;
                     return var10000;
                  }
               }
            }
         }
      }

      var10000 = false;
      return var10000;
   }

   private static boolean isStableEnd(LevelStem var0) {
      boolean var10000;
      if (var0.type().is(BuiltinDimensionTypes.END)) {
         ChunkGenerator var2 = var0.generator();
         if (var2 instanceof NoiseBasedChunkGenerator) {
            NoiseBasedChunkGenerator var1 = (NoiseBasedChunkGenerator)var2;
            if (var1.stable(NoiseGeneratorSettings.END) && var1.getBiomeSource() instanceof TheEndBiomeSource) {
               var10000 = true;
               return var10000;
            }
         }
      }

      var10000 = false;
      return var10000;
   }

   public WorldDimensions.Complete bake(Registry<LevelStem> var1) {
      Stream var2 = Stream.concat(var1.registryKeySet().stream(), this.dimensions.registryKeySet().stream()).distinct();
      ArrayList var3 = new ArrayList();
      keysInOrder(var2).forEach((var3x) -> {
         var1.getOptional(var3x).or(() -> {
            return this.dimensions.getOptional(var3x);
         }).ifPresent((var2) -> {
            record Entry(ResourceKey<LevelStem> a, LevelStem b) {
               final ResourceKey<LevelStem> key;
               final LevelStem value;

               Entry(ResourceKey<LevelStem> var1, LevelStem var2) {
                  this.key = var1;
                  this.value = var2;
               }

               Lifecycle lifecycle() {
                  return WorldDimensions.checkStability(this.key, this.value);
               }

               public ResourceKey<LevelStem> key() {
                  return this.key;
               }

               public LevelStem value() {
                  return this.value;
               }
            }

            var3.add(new Entry(var3x, var2));
         });
      });
      Lifecycle var4 = var3.size() == VANILLA_DIMENSION_COUNT ? Lifecycle.stable() : Lifecycle.experimental();
      MappedRegistry var5 = new MappedRegistry(Registries.LEVEL_STEM, var4);
      var3.forEach((var1x) -> {
         var5.register(var1x.key, var1x.value, var1x.lifecycle());
      });
      Registry var6 = var5.freeze();
      PrimaryLevelData.SpecialWorldProperty var7 = specialWorldProperty(var6);
      return new WorldDimensions.Complete(var6.freeze(), var7);
   }

   public Registry<LevelStem> dimensions() {
      return this.dimensions;
   }

   static {
      BUILTIN_ORDER = ImmutableSet.of(LevelStem.OVERWORLD, LevelStem.NETHER, LevelStem.END);
      VANILLA_DIMENSION_COUNT = BUILTIN_ORDER.size();
   }

   public static record Complete(Registry<LevelStem> a, PrimaryLevelData.SpecialWorldProperty b) {
      private final Registry<LevelStem> dimensions;
      private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;

      public Complete(Registry<LevelStem> var1, PrimaryLevelData.SpecialWorldProperty var2) {
         this.dimensions = var1;
         this.specialWorldProperty = var2;
      }

      public Lifecycle lifecycle() {
         return this.dimensions.registryLifecycle();
      }

      public RegistryAccess.Frozen dimensionsRegistryAccess() {
         return (new RegistryAccess.ImmutableRegistryAccess(List.of(this.dimensions))).freeze();
      }

      public Registry<LevelStem> dimensions() {
         return this.dimensions;
      }

      public PrimaryLevelData.SpecialWorldProperty specialWorldProperty() {
         return this.specialWorldProperty;
      }
   }
}
