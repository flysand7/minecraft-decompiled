package net.minecraft.data.advancements.packs;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.ChanneledLightningTrigger;
import net.minecraft.advancements.critereon.DamagePredicate;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.DistanceTrigger;
import net.minecraft.advancements.critereon.EntityEquipmentPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.KilledByCrossbowTrigger;
import net.minecraft.advancements.critereon.KilledTrigger;
import net.minecraft.advancements.critereon.LighthingBoltPredicate;
import net.minecraft.advancements.critereon.LightningStrikeTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.LootTableTrigger;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PlayerHurtEntityTrigger;
import net.minecraft.advancements.critereon.PlayerPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.RecipeCraftedTrigger;
import net.minecraft.advancements.critereon.ShotCrossbowTrigger;
import net.minecraft.advancements.critereon.SlideDownBlockTrigger;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.advancements.critereon.SummonedEntityTrigger;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.advancements.critereon.TargetBlockTrigger;
import net.minecraft.advancements.critereon.TradeTrigger;
import net.minecraft.advancements.critereon.UsedTotemTrigger;
import net.minecraft.advancements.critereon.UsingItemTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.DecoratedPotRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class VanillaAdventureAdvancements implements AdvancementSubProvider {
   private static final int DISTANCE_FROM_BOTTOM_TO_TOP = 384;
   private static final int Y_COORDINATE_AT_TOP = 320;
   private static final int Y_COORDINATE_AT_BOTTOM = -64;
   private static final int BEDROCK_THICKNESS = 5;
   private static final EntityType<?>[] MOBS_TO_KILL;

   public VanillaAdventureAdvancements() {
   }

   private static LightningStrikeTrigger.TriggerInstance fireCountAndBystander(MinMaxBounds.Ints var0, EntityPredicate var1) {
      return LightningStrikeTrigger.TriggerInstance.lighthingStrike(EntityPredicate.Builder.entity().distance(DistancePredicate.absolute(MinMaxBounds.Doubles.atMost(30.0D))).subPredicate(LighthingBoltPredicate.blockSetOnFire(var0)).build(), var1);
   }

   private static UsingItemTrigger.TriggerInstance lookAtThroughItem(EntityType<?> var0, Item var1) {
      return UsingItemTrigger.TriggerInstance.lookingAt(EntityPredicate.Builder.entity().subPredicate(PlayerPredicate.Builder.player().setLookingAt(EntityPredicate.Builder.entity().of(var0).build()).build()), ItemPredicate.Builder.item().of(var1));
   }

   public void generate(HolderLookup.Provider var1, Consumer<Advancement> var2) {
      Advancement var3 = Advancement.Builder.advancement().display((ItemLike)Items.MAP, Component.translatable("advancements.adventure.root.title"), Component.translatable("advancements.adventure.root.description"), new ResourceLocation("textures/gui/advancements/backgrounds/adventure.png"), FrameType.TASK, false, false, false).requirements(RequirementsStrategy.OR).addCriterion("killed_something", (CriterionTriggerInstance)KilledTrigger.TriggerInstance.playerKilledEntity()).addCriterion("killed_by_something", (CriterionTriggerInstance)KilledTrigger.TriggerInstance.entityKilledPlayer()).save(var2, "adventure/root");
      Advancement var4 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Blocks.RED_BED, Component.translatable("advancements.adventure.sleep_in_bed.title"), Component.translatable("advancements.adventure.sleep_in_bed.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("slept_in_bed", (CriterionTriggerInstance)PlayerTrigger.TriggerInstance.sleptInBed()).save(var2, "adventure/sleep_in_bed");
      createAdventuringTime(var2, var4, MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD);
      Advancement var5 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.EMERALD, Component.translatable("advancements.adventure.trade.title"), Component.translatable("advancements.adventure.trade.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("traded", (CriterionTriggerInstance)TradeTrigger.TriggerInstance.tradedWithVillager()).save(var2, "adventure/trade");
      Advancement.Builder.advancement().parent(var5).display((ItemLike)Items.EMERALD, Component.translatable("advancements.adventure.trade_at_world_height.title"), Component.translatable("advancements.adventure.trade_at_world_height.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("trade_at_world_height", (CriterionTriggerInstance)TradeTrigger.TriggerInstance.tradedWithVillager(EntityPredicate.Builder.entity().located(LocationPredicate.atYLocation(MinMaxBounds.Doubles.atLeast(319.0D))))).save(var2, "adventure/trade_at_world_height");
      Advancement var6 = addMobsToKill(Advancement.Builder.advancement()).parent(var3).display((ItemLike)Items.IRON_SWORD, Component.translatable("advancements.adventure.kill_a_mob.title"), Component.translatable("advancements.adventure.kill_a_mob.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).requirements(RequirementsStrategy.OR).save(var2, "adventure/kill_a_mob");
      addMobsToKill(Advancement.Builder.advancement()).parent(var6).display((ItemLike)Items.DIAMOND_SWORD, Component.translatable("advancements.adventure.kill_all_mobs.title"), Component.translatable("advancements.adventure.kill_all_mobs.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(var2, "adventure/kill_all_mobs");
      Advancement var7 = Advancement.Builder.advancement().parent(var6).display((ItemLike)Items.BOW, Component.translatable("advancements.adventure.shoot_arrow.title"), Component.translatable("advancements.adventure.shoot_arrow.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("shot_arrow", (CriterionTriggerInstance)PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntity(DamagePredicate.Builder.damageInstance().type(DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE)).direct(EntityPredicate.Builder.entity().of(EntityTypeTags.ARROWS))))).save(var2, "adventure/shoot_arrow");
      Advancement var8 = Advancement.Builder.advancement().parent(var6).display((ItemLike)Items.TRIDENT, Component.translatable("advancements.adventure.throw_trident.title"), Component.translatable("advancements.adventure.throw_trident.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("shot_trident", (CriterionTriggerInstance)PlayerHurtEntityTrigger.TriggerInstance.playerHurtEntity(DamagePredicate.Builder.damageInstance().type(DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE)).direct(EntityPredicate.Builder.entity().of(EntityType.TRIDENT))))).save(var2, "adventure/throw_trident");
      Advancement.Builder.advancement().parent(var8).display((ItemLike)Items.TRIDENT, Component.translatable("advancements.adventure.very_very_frightening.title"), Component.translatable("advancements.adventure.very_very_frightening.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("struck_villager", (CriterionTriggerInstance)ChanneledLightningTrigger.TriggerInstance.channeledLightning(EntityPredicate.Builder.entity().of(EntityType.VILLAGER).build())).save(var2, "adventure/very_very_frightening");
      Advancement.Builder.advancement().parent(var5).display((ItemLike)Blocks.CARVED_PUMPKIN, Component.translatable("advancements.adventure.summon_iron_golem.title"), Component.translatable("advancements.adventure.summon_iron_golem.description"), (ResourceLocation)null, FrameType.GOAL, true, true, false).addCriterion("summoned_golem", (CriterionTriggerInstance)SummonedEntityTrigger.TriggerInstance.summonedEntity(EntityPredicate.Builder.entity().of(EntityType.IRON_GOLEM))).save(var2, "adventure/summon_iron_golem");
      Advancement.Builder.advancement().parent(var7).display((ItemLike)Items.ARROW, Component.translatable("advancements.adventure.sniper_duel.title"), Component.translatable("advancements.adventure.sniper_duel.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).addCriterion("killed_skeleton", (CriterionTriggerInstance)KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityType.SKELETON).distance(DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(50.0D))), DamageSourcePredicate.Builder.damageType().tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE)))).save(var2, "adventure/sniper_duel");
      Advancement.Builder.advancement().parent(var6).display((ItemLike)Items.TOTEM_OF_UNDYING, Component.translatable("advancements.adventure.totem_of_undying.title"), Component.translatable("advancements.adventure.totem_of_undying.description"), (ResourceLocation)null, FrameType.GOAL, true, true, false).addCriterion("used_totem", (CriterionTriggerInstance)UsedTotemTrigger.TriggerInstance.usedTotem((ItemLike)Items.TOTEM_OF_UNDYING)).save(var2, "adventure/totem_of_undying");
      Advancement var9 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.CROSSBOW, Component.translatable("advancements.adventure.ol_betsy.title"), Component.translatable("advancements.adventure.ol_betsy.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("shot_crossbow", (CriterionTriggerInstance)ShotCrossbowTrigger.TriggerInstance.shotCrossbow((ItemLike)Items.CROSSBOW)).save(var2, "adventure/ol_betsy");
      Advancement.Builder.advancement().parent(var9).display((ItemLike)Items.CROSSBOW, Component.translatable("advancements.adventure.whos_the_pillager_now.title"), Component.translatable("advancements.adventure.whos_the_pillager_now.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("kill_pillager", (CriterionTriggerInstance)KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(EntityPredicate.Builder.entity().of(EntityType.PILLAGER))).save(var2, "adventure/whos_the_pillager_now");
      Advancement.Builder.advancement().parent(var9).display((ItemLike)Items.CROSSBOW, Component.translatable("advancements.adventure.two_birds_one_arrow.title"), Component.translatable("advancements.adventure.two_birds_one_arrow.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(65)).addCriterion("two_birds", (CriterionTriggerInstance)KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(EntityPredicate.Builder.entity().of(EntityType.PHANTOM), EntityPredicate.Builder.entity().of(EntityType.PHANTOM))).save(var2, "adventure/two_birds_one_arrow");
      Advancement.Builder.advancement().parent(var9).display((ItemLike)Items.CROSSBOW, Component.translatable("advancements.adventure.arbalistic.title"), Component.translatable("advancements.adventure.arbalistic.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, true).rewards(AdvancementRewards.Builder.experience(85)).addCriterion("arbalistic", (CriterionTriggerInstance)KilledByCrossbowTrigger.TriggerInstance.crossbowKilled(MinMaxBounds.Ints.exactly(5))).save(var2, "adventure/arbalistic");
      Advancement var10 = Advancement.Builder.advancement().parent(var3).display((ItemStack)Raid.getLeaderBannerInstance(), Component.translatable("advancements.adventure.voluntary_exile.title"), Component.translatable("advancements.adventure.voluntary_exile.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).addCriterion("voluntary_exile", (CriterionTriggerInstance)KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(EntityTypeTags.RAIDERS).equipment(EntityEquipmentPredicate.CAPTAIN))).save(var2, "adventure/voluntary_exile");
      Advancement.Builder.advancement().parent(var10).display((ItemStack)Raid.getLeaderBannerInstance(), Component.translatable("advancements.adventure.hero_of_the_village.title"), Component.translatable("advancements.adventure.hero_of_the_village.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, true).rewards(AdvancementRewards.Builder.experience(100)).addCriterion("hero_of_the_village", (CriterionTriggerInstance)PlayerTrigger.TriggerInstance.raidWon()).save(var2, "adventure/hero_of_the_village");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Blocks.HONEY_BLOCK.asItem(), Component.translatable("advancements.adventure.honey_block_slide.title"), Component.translatable("advancements.adventure.honey_block_slide.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("honey_block_slide", (CriterionTriggerInstance)SlideDownBlockTrigger.TriggerInstance.slidesDownBlock(Blocks.HONEY_BLOCK)).save(var2, "adventure/honey_block_slide");
      Advancement.Builder.advancement().parent(var7).display((ItemLike)Blocks.TARGET.asItem(), Component.translatable("advancements.adventure.bullseye.title"), Component.translatable("advancements.adventure.bullseye.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).addCriterion("bullseye", (CriterionTriggerInstance)TargetBlockTrigger.TriggerInstance.targetHit(MinMaxBounds.Ints.exactly(15), EntityPredicate.wrap(EntityPredicate.Builder.entity().distance(DistancePredicate.horizontal(MinMaxBounds.Doubles.atLeast(30.0D))).build()))).save(var2, "adventure/bullseye");
      Advancement.Builder.advancement().parent(var4).display((ItemLike)Items.LEATHER_BOOTS, Component.translatable("advancements.adventure.walk_on_powder_snow_with_leather_boots.title"), Component.translatable("advancements.adventure.walk_on_powder_snow_with_leather_boots.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("walk_on_powder_snow_with_leather_boots", (CriterionTriggerInstance)PlayerTrigger.TriggerInstance.walkOnBlockWithEquipment(Blocks.POWDER_SNOW, Items.LEATHER_BOOTS)).save(var2, "adventure/walk_on_powder_snow_with_leather_boots");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.LIGHTNING_ROD, Component.translatable("advancements.adventure.lightning_rod_with_villager_no_fire.title"), Component.translatable("advancements.adventure.lightning_rod_with_villager_no_fire.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("lightning_rod_with_villager_no_fire", (CriterionTriggerInstance)fireCountAndBystander(MinMaxBounds.Ints.exactly(0), EntityPredicate.Builder.entity().of(EntityType.VILLAGER).build())).save(var2, "adventure/lightning_rod_with_villager_no_fire");
      Advancement var11 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.SPYGLASS, Component.translatable("advancements.adventure.spyglass_at_parrot.title"), Component.translatable("advancements.adventure.spyglass_at_parrot.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("spyglass_at_parrot", (CriterionTriggerInstance)lookAtThroughItem(EntityType.PARROT, Items.SPYGLASS)).save(var2, "adventure/spyglass_at_parrot");
      Advancement var12 = Advancement.Builder.advancement().parent(var11).display((ItemLike)Items.SPYGLASS, Component.translatable("advancements.adventure.spyglass_at_ghast.title"), Component.translatable("advancements.adventure.spyglass_at_ghast.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("spyglass_at_ghast", (CriterionTriggerInstance)lookAtThroughItem(EntityType.GHAST, Items.SPYGLASS)).save(var2, "adventure/spyglass_at_ghast");
      Advancement.Builder.advancement().parent(var4).display((ItemLike)Items.JUKEBOX, Component.translatable("advancements.adventure.play_jukebox_in_meadows.title"), Component.translatable("advancements.adventure.play_jukebox_in_meadows.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("play_jukebox_in_meadows", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location().setBiome(Biomes.MEADOW).setBlock(BlockPredicate.Builder.block().of(Blocks.JUKEBOX).build()), ItemPredicate.Builder.item().of(ItemTags.MUSIC_DISCS))).save(var2, "adventure/play_jukebox_in_meadows");
      Advancement.Builder.advancement().parent(var12).display((ItemLike)Items.SPYGLASS, Component.translatable("advancements.adventure.spyglass_at_dragon.title"), Component.translatable("advancements.adventure.spyglass_at_dragon.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("spyglass_at_dragon", (CriterionTriggerInstance)lookAtThroughItem(EntityType.ENDER_DRAGON, Items.SPYGLASS)).save(var2, "adventure/spyglass_at_dragon");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.WATER_BUCKET, Component.translatable("advancements.adventure.fall_from_world_height.title"), Component.translatable("advancements.adventure.fall_from_world_height.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("fall_from_world_height", (CriterionTriggerInstance)DistanceTrigger.TriggerInstance.fallFromHeight(EntityPredicate.Builder.entity().located(LocationPredicate.atYLocation(MinMaxBounds.Doubles.atMost(-59.0D))), DistancePredicate.vertical(MinMaxBounds.Doubles.atLeast(379.0D)), LocationPredicate.atYLocation(MinMaxBounds.Doubles.atLeast(319.0D)))).save(var2, "adventure/fall_from_world_height");
      Advancement.Builder.advancement().parent(var6).display((ItemLike)Blocks.SCULK_CATALYST, Component.translatable("advancements.adventure.kill_mob_near_sculk_catalyst.title"), Component.translatable("advancements.adventure.kill_mob_near_sculk_catalyst.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).addCriterion("kill_mob_near_sculk_catalyst", (CriterionTriggerInstance)KilledTrigger.TriggerInstance.playerKilledEntityNearSculkCatalyst()).save(var2, "adventure/kill_mob_near_sculk_catalyst");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Blocks.SCULK_SENSOR, Component.translatable("advancements.adventure.avoid_vibration.title"), Component.translatable("advancements.adventure.avoid_vibration.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("avoid_vibration", (CriterionTriggerInstance)PlayerTrigger.TriggerInstance.avoidVibration()).save(var2, "adventure/avoid_vibration");
      Advancement var13 = respectingTheRemnantsCriterions(Advancement.Builder.advancement()).parent(var3).display((ItemLike)Items.BRUSH, Component.translatable("advancements.adventure.salvage_sherd.title"), Component.translatable("advancements.adventure.salvage_sherd.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "adventure/salvage_sherd");
      Advancement.Builder.advancement().parent(var13).display((ItemStack)DecoratedPotRecipe.createDecoratedPotItem(new DecoratedPotBlockEntity.Decorations(Items.BRICK, Items.HEART_POTTERY_SHERD, Items.BRICK, Items.EXPLORER_POTTERY_SHERD)), Component.translatable("advancements.adventure.craft_decorated_pot_using_only_sherds.title"), Component.translatable("advancements.adventure.craft_decorated_pot_using_only_sherds.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("pot_crafted_using_only_sherds", (CriterionTriggerInstance)RecipeCraftedTrigger.TriggerInstance.craftedItem(new ResourceLocation("minecraft:decorated_pot"), List.of(ItemPredicate.Builder.item().of(ItemTags.DECORATED_POT_SHERDS).build(), ItemPredicate.Builder.item().of(ItemTags.DECORATED_POT_SHERDS).build(), ItemPredicate.Builder.item().of(ItemTags.DECORATED_POT_SHERDS).build(), ItemPredicate.Builder.item().of(ItemTags.DECORATED_POT_SHERDS).build()))).save(var2, "adventure/craft_decorated_pot_using_only_sherds");
      Advancement var14 = craftingANewLook(Advancement.Builder.advancement()).parent(var3).display((ItemStack)(new ItemStack(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE)), Component.translatable("advancements.adventure.trim_with_any_armor_pattern.title"), Component.translatable("advancements.adventure.trim_with_any_armor_pattern.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "adventure/trim_with_any_armor_pattern");
      smithingWithStyle(Advancement.Builder.advancement()).parent(var14).display((ItemStack)(new ItemStack(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE)), Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.title"), Component.translatable("advancements.adventure.trim_with_all_exclusive_armor_patterns.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).save(var2, "adventure/trim_with_all_exclusive_armor_patterns");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.CHISELED_BOOKSHELF, Component.translatable("advancements.adventure.read_power_from_chiseled_bookshelf.title"), Component.translatable("advancements.adventure.read_power_from_chiseled_bookshelf.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).requirements(RequirementsStrategy.OR).addCriterion("chiseled_bookshelf", placedBlockReadByComparator(Blocks.CHISELED_BOOKSHELF)).addCriterion("comparator", placedComparatorReadingBlock(Blocks.CHISELED_BOOKSHELF)).save(var2, "adventure/read_power_of_chiseled_bookshelf");
   }

   private static CriterionTriggerInstance placedBlockReadByComparator(Block var0) {
      LootItemCondition.Builder[] var1 = (LootItemCondition.Builder[])ComparatorBlock.FACING.getPossibleValues().stream().map((var0x) -> {
         StatePropertiesPredicate var1 = StatePropertiesPredicate.Builder.properties().hasProperty(ComparatorBlock.FACING, (Comparable)var0x).build();
         BlockPredicate var2 = BlockPredicate.Builder.block().of(Blocks.COMPARATOR).setProperties(var1).build();
         return LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(var2), new BlockPos(var0x.getOpposite().getNormal()));
      }).toArray((var0x) -> {
         return new LootItemCondition.Builder[var0x];
      });
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(LootItemBlockStatePropertyCondition.hasBlockStateProperties(var0), AnyOfCondition.anyOf(var1));
   }

   private static CriterionTriggerInstance placedComparatorReadingBlock(Block var0) {
      LootItemCondition.Builder[] var1 = (LootItemCondition.Builder[])ComparatorBlock.FACING.getPossibleValues().stream().map((var1x) -> {
         StatePropertiesPredicate.Builder var2 = StatePropertiesPredicate.Builder.properties().hasProperty(ComparatorBlock.FACING, (Comparable)var1x);
         LootItemBlockStatePropertyCondition.Builder var3 = (new LootItemBlockStatePropertyCondition.Builder(Blocks.COMPARATOR)).setProperties(var2);
         LootItemCondition.Builder var4 = LocationCheck.checkLocation(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(var0).build()), new BlockPos(var1x.getNormal()));
         return AllOfCondition.allOf(var3, var4);
      }).toArray((var0x) -> {
         return new LootItemCondition.Builder[var0x];
      });
      return ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(AnyOfCondition.anyOf(var1));
   }

   private static Advancement.Builder smithingWithStyle(Advancement.Builder var0) {
      var0.requirements(RequirementsStrategy.AND);
      Map var1 = VanillaRecipeProvider.smithingTrims();
      Stream.of(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE).forEach((var2) -> {
         ResourceLocation var3 = (ResourceLocation)var1.get(var2);
         var0.addCriterion("armor_trimmed_" + var3, (CriterionTriggerInstance)RecipeCraftedTrigger.TriggerInstance.craftedItem(var3));
      });
      return var0;
   }

   private static Advancement.Builder craftingANewLook(Advancement.Builder var0) {
      var0.requirements(RequirementsStrategy.OR);
      Iterator var1 = VanillaRecipeProvider.smithingTrims().values().iterator();

      while(var1.hasNext()) {
         ResourceLocation var2 = (ResourceLocation)var1.next();
         var0.addCriterion("armor_trimmed_" + var2, (CriterionTriggerInstance)RecipeCraftedTrigger.TriggerInstance.craftedItem(var2));
      }

      return var0;
   }

   private static Advancement.Builder respectingTheRemnantsCriterions(Advancement.Builder var0) {
      var0.addCriterion("desert_pyramid", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY));
      var0.addCriterion("desert_well", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY));
      var0.addCriterion("ocean_ruin_cold", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_COLD_ARCHAEOLOGY));
      var0.addCriterion("ocean_ruin_warm", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.OCEAN_RUIN_WARM_ARCHAEOLOGY));
      var0.addCriterion("trail_ruins_rare", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE));
      var0.addCriterion("trail_ruins_common", (CriterionTriggerInstance)LootTableTrigger.TriggerInstance.lootTableUsed(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON));
      String[] var1 = (String[])var0.getCriteria().keySet().toArray((var0x) -> {
         return new String[var0x];
      });
      String var2 = "has_sherd";
      var0.addCriterion("has_sherd", (CriterionTriggerInstance)InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(ItemTags.DECORATED_POT_SHERDS).build()));
      var0.requirements(new String[][]{var1, {"has_sherd"}});
      return var0;
   }

   protected static void createAdventuringTime(Consumer<Advancement> var0, Advancement var1, MultiNoiseBiomeSourceParameterList.Preset var2) {
      addBiomes(Advancement.Builder.advancement(), var2.usedBiomes().toList()).parent(var1).display((ItemLike)Items.DIAMOND_BOOTS, Component.translatable("advancements.adventure.adventuring_time.title"), Component.translatable("advancements.adventure.adventuring_time.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(500)).save(var0, "adventure/adventuring_time");
   }

   private static Advancement.Builder addMobsToKill(Advancement.Builder var0) {
      EntityType[] var1 = MOBS_TO_KILL;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         EntityType var4 = var1[var3];
         var0.addCriterion(BuiltInRegistries.ENTITY_TYPE.getKey(var4).toString(), (CriterionTriggerInstance)KilledTrigger.TriggerInstance.playerKilledEntity(EntityPredicate.Builder.entity().of(var4)));
      }

      return var0;
   }

   protected static Advancement.Builder addBiomes(Advancement.Builder var0, List<ResourceKey<Biome>> var1) {
      Iterator var2 = var1.iterator();

      while(var2.hasNext()) {
         ResourceKey var3 = (ResourceKey)var2.next();
         var0.addCriterion(var3.location().toString(), (CriterionTriggerInstance)PlayerTrigger.TriggerInstance.located(LocationPredicate.inBiome(var3)));
      }

      return var0;
   }

   static {
      MOBS_TO_KILL = new EntityType[]{EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.CREEPER, EntityType.DROWNED, EntityType.ELDER_GUARDIAN, EntityType.ENDER_DRAGON, EntityType.ENDERMAN, EntityType.ENDERMITE, EntityType.EVOKER, EntityType.GHAST, EntityType.GUARDIAN, EntityType.HOGLIN, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE, EntityType.PILLAGER, EntityType.RAVAGER, EntityType.SHULKER, EntityType.SILVERFISH, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.WITHER, EntityType.ZOGLIN, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIE, EntityType.ZOMBIFIED_PIGLIN};
   }
}
