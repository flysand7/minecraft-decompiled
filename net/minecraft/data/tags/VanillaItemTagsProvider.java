package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class VanillaItemTagsProvider extends ItemTagsProvider {
   public VanillaItemTagsProvider(PackOutput var1, CompletableFuture<HolderLookup.Provider> var2, CompletableFuture<TagsProvider.TagLookup<Block>> var3) {
      super(var1, var2, var3);
   }

   protected void addTags(HolderLookup.Provider var1) {
      this.copy(BlockTags.WOOL, ItemTags.WOOL);
      this.copy(BlockTags.PLANKS, ItemTags.PLANKS);
      this.copy(BlockTags.STONE_BRICKS, ItemTags.STONE_BRICKS);
      this.copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
      this.copy(BlockTags.STONE_BUTTONS, ItemTags.STONE_BUTTONS);
      this.copy(BlockTags.BUTTONS, ItemTags.BUTTONS);
      this.copy(BlockTags.WOOL_CARPETS, ItemTags.WOOL_CARPETS);
      this.copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
      this.copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
      this.copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
      this.copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
      this.copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
      this.copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
      this.copy(BlockTags.DOORS, ItemTags.DOORS);
      this.copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
      this.copy(BlockTags.BAMBOO_BLOCKS, ItemTags.BAMBOO_BLOCKS);
      this.copy(BlockTags.OAK_LOGS, ItemTags.OAK_LOGS);
      this.copy(BlockTags.DARK_OAK_LOGS, ItemTags.DARK_OAK_LOGS);
      this.copy(BlockTags.BIRCH_LOGS, ItemTags.BIRCH_LOGS);
      this.copy(BlockTags.ACACIA_LOGS, ItemTags.ACACIA_LOGS);
      this.copy(BlockTags.SPRUCE_LOGS, ItemTags.SPRUCE_LOGS);
      this.copy(BlockTags.MANGROVE_LOGS, ItemTags.MANGROVE_LOGS);
      this.copy(BlockTags.JUNGLE_LOGS, ItemTags.JUNGLE_LOGS);
      this.copy(BlockTags.CHERRY_LOGS, ItemTags.CHERRY_LOGS);
      this.copy(BlockTags.CRIMSON_STEMS, ItemTags.CRIMSON_STEMS);
      this.copy(BlockTags.WARPED_STEMS, ItemTags.WARPED_STEMS);
      this.copy(BlockTags.WART_BLOCKS, ItemTags.WART_BLOCKS);
      this.copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
      this.copy(BlockTags.LOGS, ItemTags.LOGS);
      this.copy(BlockTags.SAND, ItemTags.SAND);
      this.copy(BlockTags.SMELTS_TO_GLASS, ItemTags.SMELTS_TO_GLASS);
      this.copy(BlockTags.SLABS, ItemTags.SLABS);
      this.copy(BlockTags.WALLS, ItemTags.WALLS);
      this.copy(BlockTags.STAIRS, ItemTags.STAIRS);
      this.copy(BlockTags.ANVIL, ItemTags.ANVIL);
      this.copy(BlockTags.RAILS, ItemTags.RAILS);
      this.copy(BlockTags.LEAVES, ItemTags.LEAVES);
      this.copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
      this.copy(BlockTags.TRAPDOORS, ItemTags.TRAPDOORS);
      this.copy(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS);
      this.copy(BlockTags.BEDS, ItemTags.BEDS);
      this.copy(BlockTags.FENCES, ItemTags.FENCES);
      this.copy(BlockTags.TALL_FLOWERS, ItemTags.TALL_FLOWERS);
      this.copy(BlockTags.FLOWERS, ItemTags.FLOWERS);
      this.copy(BlockTags.SOUL_FIRE_BASE_BLOCKS, ItemTags.SOUL_FIRE_BASE_BLOCKS);
      this.copy(BlockTags.CANDLES, ItemTags.CANDLES);
      this.copy(BlockTags.DAMPENS_VIBRATIONS, ItemTags.DAMPENS_VIBRATIONS);
      this.copy(BlockTags.GOLD_ORES, ItemTags.GOLD_ORES);
      this.copy(BlockTags.IRON_ORES, ItemTags.IRON_ORES);
      this.copy(BlockTags.DIAMOND_ORES, ItemTags.DIAMOND_ORES);
      this.copy(BlockTags.REDSTONE_ORES, ItemTags.REDSTONE_ORES);
      this.copy(BlockTags.LAPIS_ORES, ItemTags.LAPIS_ORES);
      this.copy(BlockTags.COAL_ORES, ItemTags.COAL_ORES);
      this.copy(BlockTags.EMERALD_ORES, ItemTags.EMERALD_ORES);
      this.copy(BlockTags.COPPER_ORES, ItemTags.COPPER_ORES);
      this.copy(BlockTags.DIRT, ItemTags.DIRT);
      this.copy(BlockTags.TERRACOTTA, ItemTags.TERRACOTTA);
      this.copy(BlockTags.COMPLETES_FIND_TREE_TUTORIAL, ItemTags.COMPLETES_FIND_TREE_TUTORIAL);
      this.tag(ItemTags.BANNERS).add((Object[])(Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER, Items.LIGHT_BLUE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER, Items.PINK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER, Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER, Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER, Items.BLACK_BANNER));
      this.tag(ItemTags.BOATS).add((Object[])(Items.OAK_BOAT, Items.SPRUCE_BOAT, Items.BIRCH_BOAT, Items.JUNGLE_BOAT, Items.ACACIA_BOAT, Items.DARK_OAK_BOAT, Items.MANGROVE_BOAT, Items.BAMBOO_RAFT, Items.CHERRY_BOAT)).addTag(ItemTags.CHEST_BOATS);
      this.tag(ItemTags.CHEST_BOATS).add((Object[])(Items.OAK_CHEST_BOAT, Items.SPRUCE_CHEST_BOAT, Items.BIRCH_CHEST_BOAT, Items.JUNGLE_CHEST_BOAT, Items.ACACIA_CHEST_BOAT, Items.DARK_OAK_CHEST_BOAT, Items.MANGROVE_CHEST_BOAT, Items.BAMBOO_CHEST_RAFT, Items.CHERRY_CHEST_BOAT));
      this.tag(ItemTags.FISHES).add((Object[])(Items.COD, Items.COOKED_COD, Items.SALMON, Items.COOKED_SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH));
      this.copy(BlockTags.STANDING_SIGNS, ItemTags.SIGNS);
      this.copy(BlockTags.CEILING_HANGING_SIGNS, ItemTags.HANGING_SIGNS);
      this.tag(ItemTags.CREEPER_DROP_MUSIC_DISCS).add((Object[])(Items.MUSIC_DISC_13, Items.MUSIC_DISC_CAT, Items.MUSIC_DISC_BLOCKS, Items.MUSIC_DISC_CHIRP, Items.MUSIC_DISC_FAR, Items.MUSIC_DISC_MALL, Items.MUSIC_DISC_MELLOHI, Items.MUSIC_DISC_STAL, Items.MUSIC_DISC_STRAD, Items.MUSIC_DISC_WARD, Items.MUSIC_DISC_11, Items.MUSIC_DISC_WAIT));
      this.tag(ItemTags.MUSIC_DISCS).addTag(ItemTags.CREEPER_DROP_MUSIC_DISCS).add((Object)Items.MUSIC_DISC_PIGSTEP).add((Object)Items.MUSIC_DISC_OTHERSIDE).add((Object)Items.MUSIC_DISC_5).add((Object)Items.MUSIC_DISC_RELIC);
      this.tag(ItemTags.COALS).add((Object[])(Items.COAL, Items.CHARCOAL));
      this.tag(ItemTags.ARROWS).add((Object[])(Items.ARROW, Items.TIPPED_ARROW, Items.SPECTRAL_ARROW));
      this.tag(ItemTags.LECTERN_BOOKS).add((Object[])(Items.WRITTEN_BOOK, Items.WRITABLE_BOOK));
      this.tag(ItemTags.BEACON_PAYMENT_ITEMS).add((Object[])(Items.NETHERITE_INGOT, Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.IRON_INGOT));
      this.tag(ItemTags.PIGLIN_REPELLENTS).add((Object)Items.SOUL_TORCH).add((Object)Items.SOUL_LANTERN).add((Object)Items.SOUL_CAMPFIRE);
      this.tag(ItemTags.PIGLIN_LOVED).addTag(ItemTags.GOLD_ORES).add((Object[])(Items.GOLD_BLOCK, Items.GILDED_BLACKSTONE, Items.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT, Items.BELL, Items.CLOCK, Items.GOLDEN_CARROT, Items.GLISTERING_MELON_SLICE, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR, Items.GOLDEN_SWORD, Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK));
      this.tag(ItemTags.IGNORED_BY_PIGLIN_BABIES).add((Object)Items.LEATHER);
      this.tag(ItemTags.PIGLIN_FOOD).add((Object[])(Items.PORKCHOP, Items.COOKED_PORKCHOP));
      this.tag(ItemTags.FOX_FOOD).add((Object[])(Items.SWEET_BERRIES, Items.GLOW_BERRIES));
      this.tag(ItemTags.NON_FLAMMABLE_WOOD).add((Object[])(Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM, Items.WARPED_HYPHAE, Items.STRIPPED_WARPED_HYPHAE, Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM, Items.CRIMSON_HYPHAE, Items.STRIPPED_CRIMSON_HYPHAE, Items.CRIMSON_PLANKS, Items.WARPED_PLANKS, Items.CRIMSON_SLAB, Items.WARPED_SLAB, Items.CRIMSON_PRESSURE_PLATE, Items.WARPED_PRESSURE_PLATE, Items.CRIMSON_FENCE, Items.WARPED_FENCE, Items.CRIMSON_TRAPDOOR, Items.WARPED_TRAPDOOR, Items.CRIMSON_FENCE_GATE, Items.WARPED_FENCE_GATE, Items.CRIMSON_STAIRS, Items.WARPED_STAIRS, Items.CRIMSON_BUTTON, Items.WARPED_BUTTON, Items.CRIMSON_DOOR, Items.WARPED_DOOR, Items.CRIMSON_SIGN, Items.WARPED_SIGN, Items.WARPED_HANGING_SIGN, Items.CRIMSON_HANGING_SIGN));
      this.tag(ItemTags.STONE_TOOL_MATERIALS).add((Object[])(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE));
      this.tag(ItemTags.STONE_CRAFTING_MATERIALS).add((Object[])(Items.COBBLESTONE, Items.BLACKSTONE, Items.COBBLED_DEEPSLATE));
      this.tag(ItemTags.FREEZE_IMMUNE_WEARABLES).add((Object[])(Items.LEATHER_BOOTS, Items.LEATHER_LEGGINGS, Items.LEATHER_CHESTPLATE, Items.LEATHER_HELMET, Items.LEATHER_HORSE_ARMOR));
      this.tag(ItemTags.AXOLOTL_TEMPT_ITEMS).add((Object)Items.TROPICAL_FISH_BUCKET);
      this.tag(ItemTags.CLUSTER_MAX_HARVESTABLES).add((Object[])(Items.DIAMOND_PICKAXE, Items.GOLDEN_PICKAXE, Items.IRON_PICKAXE, Items.NETHERITE_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_PICKAXE));
      this.tag(ItemTags.COMPASSES).add((Object)Items.COMPASS).add((Object)Items.RECOVERY_COMPASS);
      this.tag(ItemTags.CREEPER_IGNITERS).add((Object)Items.FLINT_AND_STEEL).add((Object)Items.FIRE_CHARGE);
      this.tag(ItemTags.SWORDS).add((Object)Items.DIAMOND_SWORD).add((Object)Items.STONE_SWORD).add((Object)Items.GOLDEN_SWORD).add((Object)Items.NETHERITE_SWORD).add((Object)Items.WOODEN_SWORD).add((Object)Items.IRON_SWORD);
      this.tag(ItemTags.AXES).add((Object)Items.DIAMOND_AXE).add((Object)Items.STONE_AXE).add((Object)Items.GOLDEN_AXE).add((Object)Items.NETHERITE_AXE).add((Object)Items.WOODEN_AXE).add((Object)Items.IRON_AXE);
      this.tag(ItemTags.PICKAXES).add((Object)Items.DIAMOND_PICKAXE).add((Object)Items.STONE_PICKAXE).add((Object)Items.GOLDEN_PICKAXE).add((Object)Items.NETHERITE_PICKAXE).add((Object)Items.WOODEN_PICKAXE).add((Object)Items.IRON_PICKAXE);
      this.tag(ItemTags.SHOVELS).add((Object)Items.DIAMOND_SHOVEL).add((Object)Items.STONE_SHOVEL).add((Object)Items.GOLDEN_SHOVEL).add((Object)Items.NETHERITE_SHOVEL).add((Object)Items.WOODEN_SHOVEL).add((Object)Items.IRON_SHOVEL);
      this.tag(ItemTags.HOES).add((Object)Items.DIAMOND_HOE).add((Object)Items.STONE_HOE).add((Object)Items.GOLDEN_HOE).add((Object)Items.NETHERITE_HOE).add((Object)Items.WOODEN_HOE).add((Object)Items.IRON_HOE);
      this.tag(ItemTags.TOOLS).addTag(ItemTags.SWORDS).addTag(ItemTags.AXES).addTag(ItemTags.PICKAXES).addTag(ItemTags.SHOVELS).addTag(ItemTags.HOES).add((Object)Items.TRIDENT);
      this.tag(ItemTags.BREAKS_DECORATED_POTS).addTag(ItemTags.TOOLS);
      this.tag(ItemTags.DECORATED_POT_SHERDS).add((Object[])(Items.ANGLER_POTTERY_SHERD, Items.ARCHER_POTTERY_SHERD, Items.ARMS_UP_POTTERY_SHERD, Items.BLADE_POTTERY_SHERD, Items.BREWER_POTTERY_SHERD, Items.BURN_POTTERY_SHERD, Items.DANGER_POTTERY_SHERD, Items.EXPLORER_POTTERY_SHERD, Items.FRIEND_POTTERY_SHERD, Items.HEART_POTTERY_SHERD, Items.HEARTBREAK_POTTERY_SHERD, Items.HOWL_POTTERY_SHERD, Items.MINER_POTTERY_SHERD, Items.MOURNER_POTTERY_SHERD, Items.PLENTY_POTTERY_SHERD, Items.PRIZE_POTTERY_SHERD, Items.SHEAF_POTTERY_SHERD, Items.SHELTER_POTTERY_SHERD, Items.SKULL_POTTERY_SHERD, Items.SNORT_POTTERY_SHERD));
      this.tag(ItemTags.DECORATED_POT_INGREDIENTS).add((Object)Items.BRICK).addTag(ItemTags.DECORATED_POT_SHERDS);
      this.tag(ItemTags.TRIMMABLE_ARMOR).add((Object)Items.NETHERITE_HELMET).add((Object)Items.NETHERITE_CHESTPLATE).add((Object)Items.NETHERITE_LEGGINGS).add((Object)Items.NETHERITE_BOOTS).add((Object)Items.DIAMOND_HELMET).add((Object)Items.DIAMOND_CHESTPLATE).add((Object)Items.DIAMOND_LEGGINGS).add((Object)Items.DIAMOND_BOOTS).add((Object)Items.GOLDEN_HELMET).add((Object)Items.GOLDEN_CHESTPLATE).add((Object)Items.GOLDEN_LEGGINGS).add((Object)Items.GOLDEN_BOOTS).add((Object)Items.IRON_HELMET).add((Object)Items.IRON_CHESTPLATE).add((Object)Items.IRON_LEGGINGS).add((Object)Items.IRON_BOOTS).add((Object)Items.CHAINMAIL_HELMET).add((Object)Items.CHAINMAIL_CHESTPLATE).add((Object)Items.CHAINMAIL_LEGGINGS).add((Object)Items.CHAINMAIL_BOOTS).add((Object)Items.LEATHER_HELMET).add((Object)Items.LEATHER_CHESTPLATE).add((Object)Items.LEATHER_LEGGINGS).add((Object)Items.LEATHER_BOOTS).add((Object)Items.TURTLE_HELMET);
      this.tag(ItemTags.TRIM_MATERIALS).add((Object)Items.IRON_INGOT).add((Object)Items.COPPER_INGOT).add((Object)Items.GOLD_INGOT).add((Object)Items.LAPIS_LAZULI).add((Object)Items.EMERALD).add((Object)Items.DIAMOND).add((Object)Items.NETHERITE_INGOT).add((Object)Items.REDSTONE).add((Object)Items.QUARTZ).add((Object)Items.AMETHYST_SHARD);
      this.tag(ItemTags.TRIM_TEMPLATES).add((Object)Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE).add((Object)Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
      this.tag(ItemTags.BOOKSHELF_BOOKS).add((Object[])(Items.BOOK, Items.WRITTEN_BOOK, Items.ENCHANTED_BOOK, Items.WRITABLE_BOOK, Items.KNOWLEDGE_BOOK));
      this.tag(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS).add((Object[])(Items.ZOMBIE_HEAD, Items.SKELETON_SKULL, Items.CREEPER_HEAD, Items.DRAGON_HEAD, Items.WITHER_SKELETON_SKULL, Items.PIGLIN_HEAD, Items.PLAYER_HEAD));
      this.tag(ItemTags.SNIFFER_FOOD).add((Object)Items.TORCHFLOWER_SEEDS);
      this.tag(ItemTags.VILLAGER_PLANTABLE_SEEDS).add((Object[])(Items.WHEAT_SEEDS, Items.POTATO, Items.CARROT, Items.BEETROOT_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD));
   }
}
