package net.minecraft.data.advancements.packs;

import com.google.common.collect.BiMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.BeeNestDestroyedTrigger;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.advancements.critereon.BredAnimalsTrigger;
import net.minecraft.advancements.critereon.ConsumeItemTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EffectsChangedTrigger;
import net.minecraft.advancements.critereon.EnchantmentPredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.EntitySubPredicate;
import net.minecraft.advancements.critereon.FilledBucketTrigger;
import net.minecraft.advancements.critereon.FishingRodHookedTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PickedUpItemTrigger;
import net.minecraft.advancements.critereon.PlayerInteractTrigger;
import net.minecraft.advancements.critereon.StartRidingTrigger;
import net.minecraft.advancements.critereon.TameAnimalTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class VanillaHusbandryAdvancements implements AdvancementSubProvider {
   public static final List<EntityType<?>> BREEDABLE_ANIMALS;
   public static final List<EntityType<?>> INDIRECTLY_BREEDABLE_ANIMALS;
   private static final Item[] FISH;
   private static final Item[] FISH_BUCKETS;
   private static final Item[] EDIBLE_ITEMS;
   private static final Item[] WAX_SCRAPING_TOOLS;

   public VanillaHusbandryAdvancements() {
   }

   public void generate(HolderLookup.Provider var1, Consumer<Advancement> var2) {
      Advancement var3 = Advancement.Builder.advancement().display((ItemLike)Blocks.HAY_BLOCK, Component.translatable("advancements.husbandry.root.title"), Component.translatable("advancements.husbandry.root.description"), new ResourceLocation("textures/gui/advancements/backgrounds/husbandry.png"), FrameType.TASK, false, false, false).addCriterion("consumed_item", (CriterionTriggerInstance)ConsumeItemTrigger.TriggerInstance.usedItem()).save(var2, "husbandry/root");
      Advancement var4 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.WHEAT, Component.translatable("advancements.husbandry.plant_seed.title"), Component.translatable("advancements.husbandry.plant_seed.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).requirements(RequirementsStrategy.OR).addCriterion("wheat", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.WHEAT)).addCriterion("pumpkin_stem", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PUMPKIN_STEM)).addCriterion("melon_stem", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.MELON_STEM)).addCriterion("beetroots", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.BEETROOTS)).addCriterion("nether_wart", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.NETHER_WART)).addCriterion("torchflower", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.TORCHFLOWER_CROP)).addCriterion("pitcher_pod", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PITCHER_CROP)).save(var2, "husbandry/plant_seed");
      Advancement var5 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.WHEAT, Component.translatable("advancements.husbandry.breed_an_animal.title"), Component.translatable("advancements.husbandry.breed_an_animal.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).requirements(RequirementsStrategy.OR).addCriterion("bred", (CriterionTriggerInstance)BredAnimalsTrigger.TriggerInstance.bredAnimals()).save(var2, "husbandry/breed_an_animal");
      createBreedAllAnimalsAdvancement(var5, var2, BREEDABLE_ANIMALS.stream(), INDIRECTLY_BREEDABLE_ANIMALS.stream());
      addFood(Advancement.Builder.advancement()).parent(var4).display((ItemLike)Items.APPLE, Component.translatable("advancements.husbandry.balanced_diet.title"), Component.translatable("advancements.husbandry.balanced_diet.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(var2, "husbandry/balanced_diet");
      Advancement.Builder.advancement().parent(var4).display((ItemLike)Items.NETHERITE_HOE, Component.translatable("advancements.husbandry.netherite_hoe.title"), Component.translatable("advancements.husbandry.netherite_hoe.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).addCriterion("netherite_hoe", (CriterionTriggerInstance)InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HOE)).save(var2, "husbandry/obtain_netherite_hoe");
      Advancement var6 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.LEAD, Component.translatable("advancements.husbandry.tame_an_animal.title"), Component.translatable("advancements.husbandry.tame_an_animal.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("tamed_animal", (CriterionTriggerInstance)TameAnimalTrigger.TriggerInstance.tamedAnimal()).save(var2, "husbandry/tame_an_animal");
      Advancement var7 = addFish(Advancement.Builder.advancement()).parent(var3).requirements(RequirementsStrategy.OR).display((ItemLike)Items.FISHING_ROD, Component.translatable("advancements.husbandry.fishy_business.title"), Component.translatable("advancements.husbandry.fishy_business.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/fishy_business");
      Advancement var8 = addFishBuckets(Advancement.Builder.advancement()).parent(var7).requirements(RequirementsStrategy.OR).display((ItemLike)Items.PUFFERFISH_BUCKET, Component.translatable("advancements.husbandry.tactical_fishing.title"), Component.translatable("advancements.husbandry.tactical_fishing.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/tactical_fishing");
      Advancement var9 = Advancement.Builder.advancement().parent(var8).requirements(RequirementsStrategy.OR).addCriterion(BuiltInRegistries.ITEM.getKey(Items.AXOLOTL_BUCKET).getPath(), (CriterionTriggerInstance)FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(Items.AXOLOTL_BUCKET).build())).display((ItemLike)Items.AXOLOTL_BUCKET, Component.translatable("advancements.husbandry.axolotl_in_a_bucket.title"), Component.translatable("advancements.husbandry.axolotl_in_a_bucket.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/axolotl_in_a_bucket");
      Advancement.Builder.advancement().parent(var9).addCriterion("kill_axolotl_target", (CriterionTriggerInstance)EffectsChangedTrigger.TriggerInstance.gotEffectsFrom(EntityPredicate.Builder.entity().of(EntityType.AXOLOTL).build())).display((ItemLike)Items.TROPICAL_FISH_BUCKET, Component.translatable("advancements.husbandry.kill_axolotl_target.title"), Component.translatable("advancements.husbandry.kill_axolotl_target.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/kill_axolotl_target");
      addCatVariants(Advancement.Builder.advancement()).parent(var6).display((ItemLike)Items.COD, Component.translatable("advancements.husbandry.complete_catalogue.title"), Component.translatable("advancements.husbandry.complete_catalogue.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(50)).save(var2, "husbandry/complete_catalogue");
      Advancement var10 = Advancement.Builder.advancement().parent(var3).addCriterion("safely_harvest_honey", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(BlockTags.BEEHIVES).build()).setSmokey(true), ItemPredicate.Builder.item().of(Items.GLASS_BOTTLE))).display((ItemLike)Items.HONEY_BOTTLE, Component.translatable("advancements.husbandry.safely_harvest_honey.title"), Component.translatable("advancements.husbandry.safely_harvest_honey.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/safely_harvest_honey");
      Advancement var11 = Advancement.Builder.advancement().parent(var10).display((ItemLike)Items.HONEYCOMB, Component.translatable("advancements.husbandry.wax_on.title"), Component.translatable("advancements.husbandry.wax_on.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("wax_on", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of((Iterable)((BiMap)HoneycombItem.WAXABLES.get()).keySet()).build()), ItemPredicate.Builder.item().of(Items.HONEYCOMB))).save(var2, "husbandry/wax_on");
      Advancement.Builder.advancement().parent(var11).display((ItemLike)Items.STONE_AXE, Component.translatable("advancements.husbandry.wax_off.title"), Component.translatable("advancements.husbandry.wax_off.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("wax_off", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of((Iterable)((BiMap)HoneycombItem.WAX_OFF_BY_BLOCK.get()).keySet()).build()), ItemPredicate.Builder.item().of((ItemLike[])WAX_SCRAPING_TOOLS))).save(var2, "husbandry/wax_off");
      Advancement var12 = Advancement.Builder.advancement().parent(var3).addCriterion(BuiltInRegistries.ITEM.getKey(Items.TADPOLE_BUCKET).getPath(), (CriterionTriggerInstance)FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(Items.TADPOLE_BUCKET).build())).display((ItemLike)Items.TADPOLE_BUCKET, Component.translatable("advancements.husbandry.tadpole_in_a_bucket.title"), Component.translatable("advancements.husbandry.tadpole_in_a_bucket.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/tadpole_in_a_bucket");
      Advancement var13 = addLeashedFrogVariants(Advancement.Builder.advancement()).parent(var12).display((ItemLike)Items.LEAD, Component.translatable("advancements.husbandry.leash_all_frog_variants.title"), Component.translatable("advancements.husbandry.leash_all_frog_variants.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/leash_all_frog_variants");
      Advancement.Builder.advancement().parent(var13).display((ItemLike)Items.VERDANT_FROGLIGHT, Component.translatable("advancements.husbandry.froglights.title"), Component.translatable("advancements.husbandry.froglights.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).addCriterion("froglights", (CriterionTriggerInstance)InventoryChangeTrigger.TriggerInstance.hasItems(Items.OCHRE_FROGLIGHT, Items.PEARLESCENT_FROGLIGHT, Items.VERDANT_FROGLIGHT)).save(var2, "husbandry/froglights");
      Advancement.Builder.advancement().parent(var3).addCriterion("silk_touch_nest", (CriterionTriggerInstance)BeeNestDestroyedTrigger.TriggerInstance.destroyedBeeNest(Blocks.BEE_NEST, ItemPredicate.Builder.item().hasEnchantment(new EnchantmentPredicate(Enchantments.SILK_TOUCH, MinMaxBounds.Ints.atLeast(1))), MinMaxBounds.Ints.exactly(3))).display((ItemLike)Blocks.BEE_NEST, Component.translatable("advancements.husbandry.silk_touch_nest.title"), Component.translatable("advancements.husbandry.silk_touch_nest.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).save(var2, "husbandry/silk_touch_nest");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.OAK_BOAT, Component.translatable("advancements.husbandry.ride_a_boat_with_a_goat.title"), Component.translatable("advancements.husbandry.ride_a_boat_with_a_goat.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("ride_a_boat_with_a_goat", (CriterionTriggerInstance)StartRidingTrigger.TriggerInstance.playerStartsRiding(EntityPredicate.Builder.entity().vehicle(EntityPredicate.Builder.entity().of(EntityType.BOAT).passenger(EntityPredicate.Builder.entity().of(EntityType.GOAT).build()).build()))).save(var2, "husbandry/ride_a_boat_with_a_goat");
      Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.GLOW_INK_SAC, Component.translatable("advancements.husbandry.make_a_sign_glow.title"), Component.translatable("advancements.husbandry.make_a_sign_glow.description"), (ResourceLocation)null, FrameType.TASK, true, true, false).addCriterion("make_a_sign_glow", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.itemUsedOnBlock(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(BlockTags.ALL_SIGNS).build()), ItemPredicate.Builder.item().of(Items.GLOW_INK_SAC))).save(var2, "husbandry/make_a_sign_glow");
      Advancement var14 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.COOKIE, Component.translatable("advancements.husbandry.allay_deliver_item_to_player.title"), Component.translatable("advancements.husbandry.allay_deliver_item_to_player.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).addCriterion("allay_deliver_item_to_player", (CriterionTriggerInstance)PickedUpItemTrigger.TriggerInstance.thrownItemPickedUpByPlayer(ContextAwarePredicate.ANY, ItemPredicate.ANY, EntityPredicate.wrap(EntityPredicate.Builder.entity().of(EntityType.ALLAY).build()))).save(var2, "husbandry/allay_deliver_item_to_player");
      Advancement.Builder.advancement().parent(var14).display((ItemLike)Items.NOTE_BLOCK, Component.translatable("advancements.husbandry.allay_deliver_cake_to_note_block.title"), Component.translatable("advancements.husbandry.allay_deliver_cake_to_note_block.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).addCriterion("allay_deliver_cake_to_note_block", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.allayDropItemOnBlock(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(Blocks.NOTE_BLOCK).build()), ItemPredicate.Builder.item().of(Items.CAKE))).save(var2, "husbandry/allay_deliver_cake_to_note_block");
      Advancement var15 = Advancement.Builder.advancement().parent(var3).display((ItemLike)Items.SNIFFER_EGG, Component.translatable("advancements.husbandry.obtain_sniffer_egg.title"), Component.translatable("advancements.husbandry.obtain_sniffer_egg.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).addCriterion("obtain_sniffer_egg", (CriterionTriggerInstance)InventoryChangeTrigger.TriggerInstance.hasItems(Items.SNIFFER_EGG)).save(var2, "husbandry/obtain_sniffer_egg");
      Advancement var16 = Advancement.Builder.advancement().parent(var15).display((ItemLike)Items.TORCHFLOWER_SEEDS, Component.translatable("advancements.husbandry.feed_snifflet.title"), Component.translatable("advancements.husbandry.feed_snifflet.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).addCriterion("feed_snifflet", (CriterionTriggerInstance)PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(ItemPredicate.Builder.item().of(ItemTags.SNIFFER_FOOD), EntityPredicate.wrap(EntityPredicate.Builder.entity().of(EntityType.SNIFFER).flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true).build()).build()))).save(var2, "husbandry/feed_snifflet");
      Advancement.Builder.advancement().parent(var16).display((ItemLike)Items.PITCHER_POD, Component.translatable("advancements.husbandry.plant_any_sniffer_seed.title"), Component.translatable("advancements.husbandry.plant_any_sniffer_seed.description"), (ResourceLocation)null, FrameType.TASK, true, true, true).requirements(RequirementsStrategy.OR).addCriterion("torchflower", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.TORCHFLOWER_CROP)).addCriterion("pitcher_pod", (CriterionTriggerInstance)ItemUsedOnLocationTrigger.TriggerInstance.placedBlock(Blocks.PITCHER_CROP)).save(var2, "husbandry/plant_any_sniffer_seed");
   }

   public static Advancement createBreedAllAnimalsAdvancement(Advancement var0, Consumer<Advancement> var1, Stream<EntityType<?>> var2, Stream<EntityType<?>> var3) {
      return addBreedable(Advancement.Builder.advancement(), var2, var3).parent(var0).display((ItemLike)Items.GOLDEN_CARROT, Component.translatable("advancements.husbandry.breed_all_animals.title"), Component.translatable("advancements.husbandry.breed_all_animals.description"), (ResourceLocation)null, FrameType.CHALLENGE, true, true, false).rewards(AdvancementRewards.Builder.experience(100)).save(var1, "husbandry/bred_all_animals");
   }

   private static Advancement.Builder addLeashedFrogVariants(Advancement.Builder var0) {
      BuiltInRegistries.FROG_VARIANT.holders().forEach((var1) -> {
         var0.addCriterion(var1.key().location().toString(), (CriterionTriggerInstance)PlayerInteractTrigger.TriggerInstance.itemUsedOnEntity(ItemPredicate.Builder.item().of(Items.LEAD), EntityPredicate.wrap(EntityPredicate.Builder.entity().of(EntityType.FROG).subPredicate(EntitySubPredicate.variant((FrogVariant)var1.value())).build())));
      });
      return var0;
   }

   private static Advancement.Builder addFood(Advancement.Builder var0) {
      Item[] var1 = EDIBLE_ITEMS;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Item var4 = var1[var3];
         var0.addCriterion(BuiltInRegistries.ITEM.getKey(var4).getPath(), (CriterionTriggerInstance)ConsumeItemTrigger.TriggerInstance.usedItem((ItemLike)var4));
      }

      return var0;
   }

   private static Advancement.Builder addBreedable(Advancement.Builder var0, Stream<EntityType<?>> var1, Stream<EntityType<?>> var2) {
      var1.forEach((var1x) -> {
         var0.addCriterion(EntityType.getKey(var1x).toString(), (CriterionTriggerInstance)BredAnimalsTrigger.TriggerInstance.bredAnimals(EntityPredicate.Builder.entity().of(var1x)));
      });
      var2.forEach((var1x) -> {
         var0.addCriterion(EntityType.getKey(var1x).toString(), (CriterionTriggerInstance)BredAnimalsTrigger.TriggerInstance.bredAnimals(EntityPredicate.Builder.entity().of(var1x).build(), EntityPredicate.Builder.entity().of(var1x).build(), EntityPredicate.ANY));
      });
      return var0;
   }

   private static Advancement.Builder addFishBuckets(Advancement.Builder var0) {
      Item[] var1 = FISH_BUCKETS;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Item var4 = var1[var3];
         var0.addCriterion(BuiltInRegistries.ITEM.getKey(var4).getPath(), (CriterionTriggerInstance)FilledBucketTrigger.TriggerInstance.filledBucket(ItemPredicate.Builder.item().of(var4).build()));
      }

      return var0;
   }

   private static Advancement.Builder addFish(Advancement.Builder var0) {
      Item[] var1 = FISH;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         Item var4 = var1[var3];
         var0.addCriterion(BuiltInRegistries.ITEM.getKey(var4).getPath(), (CriterionTriggerInstance)FishingRodHookedTrigger.TriggerInstance.fishedItem(ItemPredicate.ANY, EntityPredicate.ANY, ItemPredicate.Builder.item().of(var4).build()));
      }

      return var0;
   }

   private static Advancement.Builder addCatVariants(Advancement.Builder var0) {
      BuiltInRegistries.CAT_VARIANT.entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(ResourceKey::location))).forEach((var1) -> {
         var0.addCriterion(((ResourceKey)var1.getKey()).location().toString(), (CriterionTriggerInstance)TameAnimalTrigger.TriggerInstance.tamedAnimal(EntityPredicate.Builder.entity().subPredicate(EntitySubPredicate.variant((CatVariant)var1.getValue())).build()));
      });
      return var0;
   }

   static {
      BREEDABLE_ANIMALS = List.of(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.SHEEP, EntityType.COW, EntityType.MOOSHROOM, EntityType.PIG, EntityType.CHICKEN, EntityType.WOLF, EntityType.OCELOT, EntityType.RABBIT, EntityType.LLAMA, EntityType.CAT, EntityType.PANDA, EntityType.FOX, EntityType.BEE, EntityType.HOGLIN, EntityType.STRIDER, EntityType.GOAT, EntityType.AXOLOTL, EntityType.CAMEL);
      INDIRECTLY_BREEDABLE_ANIMALS = List.of(EntityType.TURTLE, EntityType.FROG, EntityType.SNIFFER);
      FISH = new Item[]{Items.COD, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.SALMON};
      FISH_BUCKETS = new Item[]{Items.COD_BUCKET, Items.TROPICAL_FISH_BUCKET, Items.PUFFERFISH_BUCKET, Items.SALMON_BUCKET};
      EDIBLE_ITEMS = new Item[]{Items.APPLE, Items.MUSHROOM_STEW, Items.BREAD, Items.PORKCHOP, Items.COOKED_PORKCHOP, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE, Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH, Items.COOKED_COD, Items.COOKED_SALMON, Items.COOKIE, Items.MELON_SLICE, Items.BEEF, Items.COOKED_BEEF, Items.CHICKEN, Items.COOKED_CHICKEN, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.CARROT, Items.POTATO, Items.BAKED_POTATO, Items.POISONOUS_POTATO, Items.GOLDEN_CARROT, Items.PUMPKIN_PIE, Items.RABBIT, Items.COOKED_RABBIT, Items.RABBIT_STEW, Items.MUTTON, Items.COOKED_MUTTON, Items.CHORUS_FRUIT, Items.BEETROOT, Items.BEETROOT_SOUP, Items.DRIED_KELP, Items.SUSPICIOUS_STEW, Items.SWEET_BERRIES, Items.HONEY_BOTTLE, Items.GLOW_BERRIES};
      WAX_SCRAPING_TOOLS = new Item[]{Items.WOODEN_AXE, Items.GOLDEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE};
   }
}
