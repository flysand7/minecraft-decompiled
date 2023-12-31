package net.minecraft.data.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ItemModelGenerators {
   public static final ResourceLocation TRIM_TYPE_PREDICATE_ID = new ResourceLocation("trim_type");
   private static final List<ItemModelGenerators.TrimModelData> GENERATED_TRIM_MODELS;
   private final BiConsumer<ResourceLocation, Supplier<JsonElement>> output;

   public ItemModelGenerators(BiConsumer<ResourceLocation, Supplier<JsonElement>> var1) {
      this.output = var1;
   }

   private void generateFlatItem(Item var1, ModelTemplate var2) {
      var2.create(ModelLocationUtils.getModelLocation(var1), TextureMapping.layer0(var1), this.output);
   }

   private void generateFlatItem(Item var1, String var2, ModelTemplate var3) {
      var3.create(ModelLocationUtils.getModelLocation(var1, var2), TextureMapping.layer0(TextureMapping.getItemTexture(var1, var2)), this.output);
   }

   private void generateFlatItem(Item var1, Item var2, ModelTemplate var3) {
      var3.create(ModelLocationUtils.getModelLocation(var1), TextureMapping.layer0(var2), this.output);
   }

   private void generateCompassItem(Item var1) {
      for(int var2 = 0; var2 < 32; ++var2) {
         if (var2 != 16) {
            this.generateFlatItem(var1, String.format(Locale.ROOT, "_%02d", var2), ModelTemplates.FLAT_ITEM);
         }
      }

   }

   private void generateClockItem(Item var1) {
      for(int var2 = 1; var2 < 64; ++var2) {
         this.generateFlatItem(var1, String.format(Locale.ROOT, "_%02d", var2), ModelTemplates.FLAT_ITEM);
      }

   }

   private void generateLayeredItem(ResourceLocation var1, ResourceLocation var2, ResourceLocation var3) {
      ModelTemplates.TWO_LAYERED_ITEM.create(var1, TextureMapping.layered(var2, var3), this.output);
   }

   private void generateLayeredItem(ResourceLocation var1, ResourceLocation var2, ResourceLocation var3, ResourceLocation var4) {
      ModelTemplates.THREE_LAYERED_ITEM.create(var1, TextureMapping.layered(var2, var3, var4), this.output);
   }

   private ResourceLocation getItemModelForTrimMaterial(ResourceLocation var1, String var2) {
      return var1.withSuffix("_" + var2 + "_trim");
   }

   private JsonObject generateBaseArmorTrimTemplate(ResourceLocation var1, Map<TextureSlot, ResourceLocation> var2, ArmorMaterial var3) {
      JsonObject var4 = ModelTemplates.TWO_LAYERED_ITEM.createBaseTemplate(var1, var2);
      JsonArray var5 = new JsonArray();
      Iterator var6 = GENERATED_TRIM_MODELS.iterator();

      while(var6.hasNext()) {
         ItemModelGenerators.TrimModelData var7 = (ItemModelGenerators.TrimModelData)var6.next();
         JsonObject var8 = new JsonObject();
         JsonObject var9 = new JsonObject();
         var9.addProperty(TRIM_TYPE_PREDICATE_ID.getPath(), var7.itemModelIndex());
         var8.add("predicate", var9);
         var8.addProperty("model", this.getItemModelForTrimMaterial(var1, var7.name(var3)).toString());
         var5.add(var8);
      }

      var4.add("overrides", var5);
      return var4;
   }

   private void generateArmorTrims(ArmorItem var1) {
      ResourceLocation var2 = ModelLocationUtils.getModelLocation((Item)var1);
      ResourceLocation var3 = TextureMapping.getItemTexture(var1);
      ResourceLocation var4 = TextureMapping.getItemTexture(var1, "_overlay");
      if (var1.getMaterial() == ArmorMaterials.LEATHER) {
         ModelTemplates.TWO_LAYERED_ITEM.create(var2, TextureMapping.layered(var3, var4), this.output, (var2x, var3x) -> {
            return this.generateBaseArmorTrimTemplate(var2x, var3x, var1.getMaterial());
         });
      } else {
         ModelTemplates.FLAT_ITEM.create(var2, TextureMapping.layer0(var3), this.output, (var2x, var3x) -> {
            return this.generateBaseArmorTrimTemplate(var2x, var3x, var1.getMaterial());
         });
      }

      Iterator var5 = GENERATED_TRIM_MODELS.iterator();

      while(var5.hasNext()) {
         ItemModelGenerators.TrimModelData var6 = (ItemModelGenerators.TrimModelData)var5.next();
         String var7 = var6.name(var1.getMaterial());
         ResourceLocation var8 = this.getItemModelForTrimMaterial(var2, var7);
         String var10000 = var1.getType().getName();
         String var9 = var10000 + "_trim_" + var7;
         ResourceLocation var10 = (new ResourceLocation(var9)).withPrefix("trims/items/");
         if (var1.getMaterial() == ArmorMaterials.LEATHER) {
            this.generateLayeredItem(var8, var3, var4, var10);
         } else {
            this.generateLayeredItem(var8, var3, var10);
         }
      }

   }

   public void run() {
      this.generateFlatItem(Items.ACACIA_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHERRY_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ACACIA_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHERRY_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.AMETHYST_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.APPLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMOR_STAND, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARROW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAKED_POTATO, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.BEEF, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BEETROOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BEETROOT_SOUP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BIRCH_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BIRCH_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLACK_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLAZE_POWDER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLAZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.BLUE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BONE_MEAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOWL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREAD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BRICK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BROWN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CARROT_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
      this.generateFlatItem(Items.WARPED_FUNGUS_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
      this.generateFlatItem(Items.CHARCOAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHEST_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHICKEN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CLAY_BALL, ModelTemplates.FLAT_ITEM);
      this.generateClockItem(Items.CLOCK);
      this.generateFlatItem(Items.COAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COD_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COMMAND_BLOCK_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateCompassItem(Items.COMPASS);
      this.generateCompassItem(Items.RECOVERY_COMPASS);
      this.generateFlatItem(Items.COOKED_BEEF, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_CHICKEN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_COD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_MUTTON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_PORKCHOP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_RABBIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_SALMON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKIE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_COPPER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CREEPER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CYAN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DARK_OAK_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DARK_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DRAGON_BREATH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DRIED_KELP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EMERALD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENCHANTED_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDER_PEARL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.END_CRYSTAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EXPERIENCE_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FERMENTED_SPIDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FIREWORK_ROCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FIRE_CHARGE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLINT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLINT_AND_STEEL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLOWER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FURNACE_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GHAST_TEAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLASS_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLISTERING_MELON_SLICE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOBE_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_BERRIES, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOWSTONE_DUST, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_INK_SAC, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_ITEM_FRAME, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_GOLD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_CARROT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLD_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLD_NUGGET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GRAY_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GREEN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GUNPOWDER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEART_OF_THE_SEA, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HONEYCOMB, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HONEY_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOPPER_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.INK_SAC, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_IRON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_NUGGET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.ITEM_FRAME, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.JUNGLE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.JUNGLE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.KNOWLEDGE_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LAPIS_LAZULI, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LAVA_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LEATHER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LEATHER_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_BLUE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_GRAY_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIME_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGENTA_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGMA_CREAM, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MANGROVE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MANGROVE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO_RAFT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO_CHEST_RAFT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MELON_SLICE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MILK_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MOJANG_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MUSHROOM_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DISC_FRAGMENT_5, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MUSIC_DISC_11, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_13, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_BLOCKS, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CAT, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CHIRP, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_FAR, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_MALL, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_MELLOHI, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_PIGSTEP, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_STAL, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_STRAD, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_WAIT, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_WARD, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_OTHERSIDE, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_RELIC, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_5, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUTTON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NAME_TAG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NAUTILUS_SHELL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_SCRAP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHER_BRICK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHER_STAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OAK_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ORANGE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PAINTING, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PAPER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PHANTOM_MEMBRANE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PIGLIN_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PINK_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POISONOUS_POTATO, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POPPED_CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PORKCHOP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POWDER_SNOW_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRISMARINE_CRYSTALS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRISMARINE_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUFFERFISH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUFFERFISH_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUMPKIN_PIE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PURPLE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.QUARTZ, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_FOOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_HIDE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RED_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ROTTEN_FLESH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SADDLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SALMON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SALMON_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SCUTE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHEARS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKULL_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SLIME_BALL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNOWBALL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ECHO_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPECTRAL_ARROW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPIDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPRUCE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPRUCE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPYGLASS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.SUGAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SUSPICIOUS_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TNT_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TOTEM_OF_UNDYING, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TRIDENT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TROPICAL_FISH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TROPICAL_FISH_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.AXOLOTL_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TADPOLE_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WATER_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WHEAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WHITE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WOODEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WRITABLE_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WRITTEN_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.YELLOW_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DEBUG_STICK, Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
      Iterator var1 = BuiltInRegistries.ITEM.iterator();

      while(var1.hasNext()) {
         Item var2 = (Item)var1.next();
         if (var2 instanceof ArmorItem) {
            ArmorItem var3 = (ArmorItem)var2;
            this.generateArmorTrims(var3);
         }
      }

      this.generateFlatItem(Items.ANGLER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARCHER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMS_UP_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLADE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREWER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BURN_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DANGER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EXPLORER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FRIEND_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEART_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEARTBREAK_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOWL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MINER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MOURNER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PLENTY_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRIZE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHEAF_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHELTER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKULL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNORT_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
   }

   static {
      GENERATED_TRIM_MODELS = List.of(new ItemModelGenerators.TrimModelData("quartz", 0.1F, Map.of()), new ItemModelGenerators.TrimModelData("iron", 0.2F, Map.of(ArmorMaterials.IRON, "iron_darker")), new ItemModelGenerators.TrimModelData("netherite", 0.3F, Map.of(ArmorMaterials.NETHERITE, "netherite_darker")), new ItemModelGenerators.TrimModelData("redstone", 0.4F, Map.of()), new ItemModelGenerators.TrimModelData("copper", 0.5F, Map.of()), new ItemModelGenerators.TrimModelData("gold", 0.6F, Map.of(ArmorMaterials.GOLD, "gold_darker")), new ItemModelGenerators.TrimModelData("emerald", 0.7F, Map.of()), new ItemModelGenerators.TrimModelData("diamond", 0.8F, Map.of(ArmorMaterials.DIAMOND, "diamond_darker")), new ItemModelGenerators.TrimModelData("lapis", 0.9F, Map.of()), new ItemModelGenerators.TrimModelData("amethyst", 1.0F, Map.of()));
   }

   private static record TrimModelData(String a, float b, Map<ArmorMaterial, String> c) {
      private final String name;
      private final float itemModelIndex;
      private final Map<ArmorMaterial, String> overrideArmorMaterials;

      TrimModelData(String var1, float var2, Map<ArmorMaterial, String> var3) {
         this.name = var1;
         this.itemModelIndex = var2;
         this.overrideArmorMaterials = var3;
      }

      public String name(ArmorMaterial var1) {
         return (String)this.overrideArmorMaterials.getOrDefault(var1, this.name);
      }

      public String name() {
         return this.name;
      }

      public float itemModelIndex() {
         return this.itemModelIndex;
      }

      public Map<ArmorMaterial, String> overrideArmorMaterials() {
         return this.overrideArmorMaterials;
      }
   }
}
