package net.minecraft.core.registries;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.DefaultedMappedRegistry;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.decoration.PaintingVariants;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Instruments;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSources;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGenerators;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSourceType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicateType;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSizeType;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacerType;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosRuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifierType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.nbt.LootNbtProviderType;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import net.minecraft.world.level.storage.loot.providers.number.LootNumberProviderType;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.score.LootScoreProviderType;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProviders;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class BuiltInRegistries {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Map<ResourceLocation, Supplier<?>> LOADERS = Maps.newLinkedHashMap();
   public static final ResourceLocation ROOT_REGISTRY_NAME = new ResourceLocation("root");
   private static final WritableRegistry<WritableRegistry<?>> WRITABLE_REGISTRY;
   public static final DefaultedRegistry<GameEvent> GAME_EVENT;
   public static final Registry<SoundEvent> SOUND_EVENT;
   public static final DefaultedRegistry<Fluid> FLUID;
   public static final Registry<MobEffect> MOB_EFFECT;
   public static final DefaultedRegistry<Block> BLOCK;
   public static final Registry<Enchantment> ENCHANTMENT;
   public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE;
   public static final DefaultedRegistry<Item> ITEM;
   public static final DefaultedRegistry<Potion> POTION;
   public static final Registry<ParticleType<?>> PARTICLE_TYPE;
   public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE;
   public static final DefaultedRegistry<PaintingVariant> PAINTING_VARIANT;
   public static final Registry<ResourceLocation> CUSTOM_STAT;
   public static final DefaultedRegistry<ChunkStatus> CHUNK_STATUS;
   public static final Registry<RuleTestType<?>> RULE_TEST;
   public static final Registry<RuleBlockEntityModifierType<?>> RULE_BLOCK_ENTITY_MODIFIER;
   public static final Registry<PosRuleTestType<?>> POS_RULE_TEST;
   public static final Registry<MenuType<?>> MENU;
   public static final Registry<RecipeType<?>> RECIPE_TYPE;
   public static final Registry<RecipeSerializer<?>> RECIPE_SERIALIZER;
   public static final Registry<Attribute> ATTRIBUTE;
   public static final Registry<PositionSourceType<?>> POSITION_SOURCE_TYPE;
   public static final Registry<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPE;
   public static final Registry<StatType<?>> STAT_TYPE;
   public static final DefaultedRegistry<VillagerType> VILLAGER_TYPE;
   public static final DefaultedRegistry<VillagerProfession> VILLAGER_PROFESSION;
   public static final Registry<PoiType> POINT_OF_INTEREST_TYPE;
   public static final DefaultedRegistry<MemoryModuleType<?>> MEMORY_MODULE_TYPE;
   public static final DefaultedRegistry<SensorType<?>> SENSOR_TYPE;
   public static final Registry<Schedule> SCHEDULE;
   public static final Registry<Activity> ACTIVITY;
   public static final Registry<LootPoolEntryType> LOOT_POOL_ENTRY_TYPE;
   public static final Registry<LootItemFunctionType> LOOT_FUNCTION_TYPE;
   public static final Registry<LootItemConditionType> LOOT_CONDITION_TYPE;
   public static final Registry<LootNumberProviderType> LOOT_NUMBER_PROVIDER_TYPE;
   public static final Registry<LootNbtProviderType> LOOT_NBT_PROVIDER_TYPE;
   public static final Registry<LootScoreProviderType> LOOT_SCORE_PROVIDER_TYPE;
   public static final Registry<FloatProviderType<?>> FLOAT_PROVIDER_TYPE;
   public static final Registry<IntProviderType<?>> INT_PROVIDER_TYPE;
   public static final Registry<HeightProviderType<?>> HEIGHT_PROVIDER_TYPE;
   public static final Registry<BlockPredicateType<?>> BLOCK_PREDICATE_TYPE;
   public static final Registry<WorldCarver<?>> CARVER;
   public static final Registry<Feature<?>> FEATURE;
   public static final Registry<StructurePlacementType<?>> STRUCTURE_PLACEMENT;
   public static final Registry<StructurePieceType> STRUCTURE_PIECE;
   public static final Registry<StructureType<?>> STRUCTURE_TYPE;
   public static final Registry<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPE;
   public static final Registry<BlockStateProviderType<?>> BLOCKSTATE_PROVIDER_TYPE;
   public static final Registry<FoliagePlacerType<?>> FOLIAGE_PLACER_TYPE;
   public static final Registry<TrunkPlacerType<?>> TRUNK_PLACER_TYPE;
   public static final Registry<RootPlacerType<?>> ROOT_PLACER_TYPE;
   public static final Registry<TreeDecoratorType<?>> TREE_DECORATOR_TYPE;
   public static final Registry<FeatureSizeType<?>> FEATURE_SIZE_TYPE;
   public static final Registry<Codec<? extends BiomeSource>> BIOME_SOURCE;
   public static final Registry<Codec<? extends ChunkGenerator>> CHUNK_GENERATOR;
   public static final Registry<Codec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITION;
   public static final Registry<Codec<? extends SurfaceRules.RuleSource>> MATERIAL_RULE;
   public static final Registry<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPE;
   public static final Registry<StructureProcessorType<?>> STRUCTURE_PROCESSOR;
   public static final Registry<StructurePoolElementType<?>> STRUCTURE_POOL_ELEMENT;
   public static final Registry<CatVariant> CAT_VARIANT;
   public static final Registry<FrogVariant> FROG_VARIANT;
   public static final Registry<BannerPattern> BANNER_PATTERN;
   public static final Registry<Instrument> INSTRUMENT;
   public static final Registry<String> DECORATED_POT_PATTERNS;
   public static final Registry<CreativeModeTab> CREATIVE_MODE_TAB;
   public static final Registry<? extends Registry<?>> REGISTRY;

   public BuiltInRegistries() {
   }

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> var0, BuiltInRegistries.RegistryBootstrap<T> var1) {
      return registerSimple(var0, Lifecycle.stable(), var1);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> var0, String var1, BuiltInRegistries.RegistryBootstrap<T> var2) {
      return registerDefaulted(var0, var1, Lifecycle.stable(), var2);
   }

   private static <T> DefaultedRegistry<T> registerDefaultedWithIntrusiveHolders(ResourceKey<? extends Registry<T>> var0, String var1, BuiltInRegistries.RegistryBootstrap<T> var2) {
      return registerDefaultedWithIntrusiveHolders(var0, var1, Lifecycle.stable(), var2);
   }

   private static <T> Registry<T> registerSimple(ResourceKey<? extends Registry<T>> var0, Lifecycle var1, BuiltInRegistries.RegistryBootstrap<T> var2) {
      return internalRegister(var0, new MappedRegistry(var0, var1, false), var2, var1);
   }

   private static <T> DefaultedRegistry<T> registerDefaulted(ResourceKey<? extends Registry<T>> var0, String var1, Lifecycle var2, BuiltInRegistries.RegistryBootstrap<T> var3) {
      return (DefaultedRegistry)internalRegister(var0, new DefaultedMappedRegistry(var1, var0, var2, false), var3, var2);
   }

   private static <T> DefaultedRegistry<T> registerDefaultedWithIntrusiveHolders(ResourceKey<? extends Registry<T>> var0, String var1, Lifecycle var2, BuiltInRegistries.RegistryBootstrap<T> var3) {
      return (DefaultedRegistry)internalRegister(var0, new DefaultedMappedRegistry(var1, var0, var2, true), var3, var2);
   }

   private static <T, R extends WritableRegistry<T>> R internalRegister(ResourceKey<? extends Registry<T>> var0, R var1, BuiltInRegistries.RegistryBootstrap<T> var2, Lifecycle var3) {
      ResourceLocation var4 = var0.location();
      LOADERS.put(var4, () -> {
         return var2.run(var1);
      });
      WRITABLE_REGISTRY.register(var0, var1, var3);
      return var1;
   }

   public static void bootStrap() {
      createContents();
      freeze();
      validate(REGISTRY);
   }

   private static void createContents() {
      LOADERS.forEach((var0, var1) -> {
         if (var1.get() == null) {
            LOGGER.error("Unable to bootstrap registry '{}'", var0);
         }

      });
   }

   private static void freeze() {
      REGISTRY.freeze();
      Iterator var0 = REGISTRY.iterator();

      while(var0.hasNext()) {
         Registry var1 = (Registry)var0.next();
         var1.freeze();
      }

   }

   private static <T extends Registry<?>> void validate(Registry<T> var0) {
      var0.forEach((var1) -> {
         if (var1.keySet().isEmpty()) {
            ResourceLocation var10000 = var0.getKey(var1);
            Util.logAndPauseIfInIde("Registry '" + var10000 + "' was empty after loading");
         }

         if (var1 instanceof DefaultedRegistry) {
            ResourceLocation var2 = ((DefaultedRegistry)var1).getDefaultKey();
            Validate.notNull(var1.get(var2), "Missing default of DefaultedMappedRegistry: " + var2, new Object[0]);
         }

      });
   }

   static {
      WRITABLE_REGISTRY = new MappedRegistry(ResourceKey.createRegistryKey(ROOT_REGISTRY_NAME), Lifecycle.stable());
      GAME_EVENT = registerDefaultedWithIntrusiveHolders(Registries.GAME_EVENT, "step", (var0) -> {
         return GameEvent.STEP;
      });
      SOUND_EVENT = registerSimple(Registries.SOUND_EVENT, (var0) -> {
         return SoundEvents.ITEM_PICKUP;
      });
      FLUID = registerDefaultedWithIntrusiveHolders(Registries.FLUID, "empty", (var0) -> {
         return Fluids.EMPTY;
      });
      MOB_EFFECT = registerSimple(Registries.MOB_EFFECT, (var0) -> {
         return MobEffects.LUCK;
      });
      BLOCK = registerDefaultedWithIntrusiveHolders(Registries.BLOCK, "air", (var0) -> {
         return Blocks.AIR;
      });
      ENCHANTMENT = registerSimple(Registries.ENCHANTMENT, (var0) -> {
         return Enchantments.BLOCK_FORTUNE;
      });
      ENTITY_TYPE = registerDefaultedWithIntrusiveHolders(Registries.ENTITY_TYPE, "pig", (var0) -> {
         return EntityType.PIG;
      });
      ITEM = registerDefaultedWithIntrusiveHolders(Registries.ITEM, "air", (var0) -> {
         return Items.AIR;
      });
      POTION = registerDefaulted(Registries.POTION, "empty", (var0) -> {
         return Potions.EMPTY;
      });
      PARTICLE_TYPE = registerSimple(Registries.PARTICLE_TYPE, (var0) -> {
         return ParticleTypes.BLOCK;
      });
      BLOCK_ENTITY_TYPE = registerSimple(Registries.BLOCK_ENTITY_TYPE, (var0) -> {
         return BlockEntityType.FURNACE;
      });
      PAINTING_VARIANT = registerDefaulted(Registries.PAINTING_VARIANT, "kebab", PaintingVariants::bootstrap);
      CUSTOM_STAT = registerSimple(Registries.CUSTOM_STAT, (var0) -> {
         return Stats.JUMP;
      });
      CHUNK_STATUS = registerDefaulted(Registries.CHUNK_STATUS, "empty", (var0) -> {
         return ChunkStatus.EMPTY;
      });
      RULE_TEST = registerSimple(Registries.RULE_TEST, (var0) -> {
         return RuleTestType.ALWAYS_TRUE_TEST;
      });
      RULE_BLOCK_ENTITY_MODIFIER = registerSimple(Registries.RULE_BLOCK_ENTITY_MODIFIER, (var0) -> {
         return RuleBlockEntityModifierType.PASSTHROUGH;
      });
      POS_RULE_TEST = registerSimple(Registries.POS_RULE_TEST, (var0) -> {
         return PosRuleTestType.ALWAYS_TRUE_TEST;
      });
      MENU = registerSimple(Registries.MENU, (var0) -> {
         return MenuType.ANVIL;
      });
      RECIPE_TYPE = registerSimple(Registries.RECIPE_TYPE, (var0) -> {
         return RecipeType.CRAFTING;
      });
      RECIPE_SERIALIZER = registerSimple(Registries.RECIPE_SERIALIZER, (var0) -> {
         return RecipeSerializer.SHAPELESS_RECIPE;
      });
      ATTRIBUTE = registerSimple(Registries.ATTRIBUTE, (var0) -> {
         return Attributes.LUCK;
      });
      POSITION_SOURCE_TYPE = registerSimple(Registries.POSITION_SOURCE_TYPE, (var0) -> {
         return PositionSourceType.BLOCK;
      });
      COMMAND_ARGUMENT_TYPE = registerSimple(Registries.COMMAND_ARGUMENT_TYPE, ArgumentTypeInfos::bootstrap);
      STAT_TYPE = registerSimple(Registries.STAT_TYPE, (var0) -> {
         return Stats.ITEM_USED;
      });
      VILLAGER_TYPE = registerDefaulted(Registries.VILLAGER_TYPE, "plains", (var0) -> {
         return VillagerType.PLAINS;
      });
      VILLAGER_PROFESSION = registerDefaulted(Registries.VILLAGER_PROFESSION, "none", (var0) -> {
         return VillagerProfession.NONE;
      });
      POINT_OF_INTEREST_TYPE = registerSimple(Registries.POINT_OF_INTEREST_TYPE, PoiTypes::bootstrap);
      MEMORY_MODULE_TYPE = registerDefaulted(Registries.MEMORY_MODULE_TYPE, "dummy", (var0) -> {
         return MemoryModuleType.DUMMY;
      });
      SENSOR_TYPE = registerDefaulted(Registries.SENSOR_TYPE, "dummy", (var0) -> {
         return SensorType.DUMMY;
      });
      SCHEDULE = registerSimple(Registries.SCHEDULE, (var0) -> {
         return Schedule.EMPTY;
      });
      ACTIVITY = registerSimple(Registries.ACTIVITY, (var0) -> {
         return Activity.IDLE;
      });
      LOOT_POOL_ENTRY_TYPE = registerSimple(Registries.LOOT_POOL_ENTRY_TYPE, (var0) -> {
         return LootPoolEntries.EMPTY;
      });
      LOOT_FUNCTION_TYPE = registerSimple(Registries.LOOT_FUNCTION_TYPE, (var0) -> {
         return LootItemFunctions.SET_COUNT;
      });
      LOOT_CONDITION_TYPE = registerSimple(Registries.LOOT_CONDITION_TYPE, (var0) -> {
         return LootItemConditions.INVERTED;
      });
      LOOT_NUMBER_PROVIDER_TYPE = registerSimple(Registries.LOOT_NUMBER_PROVIDER_TYPE, (var0) -> {
         return NumberProviders.CONSTANT;
      });
      LOOT_NBT_PROVIDER_TYPE = registerSimple(Registries.LOOT_NBT_PROVIDER_TYPE, (var0) -> {
         return NbtProviders.CONTEXT;
      });
      LOOT_SCORE_PROVIDER_TYPE = registerSimple(Registries.LOOT_SCORE_PROVIDER_TYPE, (var0) -> {
         return ScoreboardNameProviders.CONTEXT;
      });
      FLOAT_PROVIDER_TYPE = registerSimple(Registries.FLOAT_PROVIDER_TYPE, (var0) -> {
         return FloatProviderType.CONSTANT;
      });
      INT_PROVIDER_TYPE = registerSimple(Registries.INT_PROVIDER_TYPE, (var0) -> {
         return IntProviderType.CONSTANT;
      });
      HEIGHT_PROVIDER_TYPE = registerSimple(Registries.HEIGHT_PROVIDER_TYPE, (var0) -> {
         return HeightProviderType.CONSTANT;
      });
      BLOCK_PREDICATE_TYPE = registerSimple(Registries.BLOCK_PREDICATE_TYPE, (var0) -> {
         return BlockPredicateType.NOT;
      });
      CARVER = registerSimple(Registries.CARVER, (var0) -> {
         return WorldCarver.CAVE;
      });
      FEATURE = registerSimple(Registries.FEATURE, (var0) -> {
         return Feature.ORE;
      });
      STRUCTURE_PLACEMENT = registerSimple(Registries.STRUCTURE_PLACEMENT, (var0) -> {
         return StructurePlacementType.RANDOM_SPREAD;
      });
      STRUCTURE_PIECE = registerSimple(Registries.STRUCTURE_PIECE, (var0) -> {
         return StructurePieceType.MINE_SHAFT_ROOM;
      });
      STRUCTURE_TYPE = registerSimple(Registries.STRUCTURE_TYPE, (var0) -> {
         return StructureType.JIGSAW;
      });
      PLACEMENT_MODIFIER_TYPE = registerSimple(Registries.PLACEMENT_MODIFIER_TYPE, (var0) -> {
         return PlacementModifierType.COUNT;
      });
      BLOCKSTATE_PROVIDER_TYPE = registerSimple(Registries.BLOCK_STATE_PROVIDER_TYPE, (var0) -> {
         return BlockStateProviderType.SIMPLE_STATE_PROVIDER;
      });
      FOLIAGE_PLACER_TYPE = registerSimple(Registries.FOLIAGE_PLACER_TYPE, (var0) -> {
         return FoliagePlacerType.BLOB_FOLIAGE_PLACER;
      });
      TRUNK_PLACER_TYPE = registerSimple(Registries.TRUNK_PLACER_TYPE, (var0) -> {
         return TrunkPlacerType.STRAIGHT_TRUNK_PLACER;
      });
      ROOT_PLACER_TYPE = registerSimple(Registries.ROOT_PLACER_TYPE, (var0) -> {
         return RootPlacerType.MANGROVE_ROOT_PLACER;
      });
      TREE_DECORATOR_TYPE = registerSimple(Registries.TREE_DECORATOR_TYPE, (var0) -> {
         return TreeDecoratorType.LEAVE_VINE;
      });
      FEATURE_SIZE_TYPE = registerSimple(Registries.FEATURE_SIZE_TYPE, (var0) -> {
         return FeatureSizeType.TWO_LAYERS_FEATURE_SIZE;
      });
      BIOME_SOURCE = registerSimple(Registries.BIOME_SOURCE, Lifecycle.stable(), BiomeSources::bootstrap);
      CHUNK_GENERATOR = registerSimple(Registries.CHUNK_GENERATOR, Lifecycle.stable(), ChunkGenerators::bootstrap);
      MATERIAL_CONDITION = registerSimple(Registries.MATERIAL_CONDITION, SurfaceRules.ConditionSource::bootstrap);
      MATERIAL_RULE = registerSimple(Registries.MATERIAL_RULE, SurfaceRules.RuleSource::bootstrap);
      DENSITY_FUNCTION_TYPE = registerSimple(Registries.DENSITY_FUNCTION_TYPE, DensityFunctions::bootstrap);
      STRUCTURE_PROCESSOR = registerSimple(Registries.STRUCTURE_PROCESSOR, (var0) -> {
         return StructureProcessorType.BLOCK_IGNORE;
      });
      STRUCTURE_POOL_ELEMENT = registerSimple(Registries.STRUCTURE_POOL_ELEMENT, (var0) -> {
         return StructurePoolElementType.EMPTY;
      });
      CAT_VARIANT = registerSimple(Registries.CAT_VARIANT, CatVariant::bootstrap);
      FROG_VARIANT = registerSimple(Registries.FROG_VARIANT, (var0) -> {
         return FrogVariant.TEMPERATE;
      });
      BANNER_PATTERN = registerSimple(Registries.BANNER_PATTERN, BannerPatterns::bootstrap);
      INSTRUMENT = registerSimple(Registries.INSTRUMENT, Instruments::bootstrap);
      DECORATED_POT_PATTERNS = registerSimple(Registries.DECORATED_POT_PATTERNS, DecoratedPotPatterns::bootstrap);
      CREATIVE_MODE_TAB = registerSimple(Registries.CREATIVE_MODE_TAB, CreativeModeTabs::bootstrap);
      REGISTRY = WRITABLE_REGISTRY;
   }

   @FunctionalInterface
   interface RegistryBootstrap<T> {
      T run(Registry<T> var1);
   }
}
