package net.minecraft.world.level.storage.loot.entries;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.GsonAdapterFactory;
import net.minecraft.world.level.storage.loot.Serializer;

public class LootPoolEntries {
   public static final LootPoolEntryType EMPTY = register("empty", new EmptyLootItem.Serializer());
   public static final LootPoolEntryType ITEM = register("item", new LootItem.Serializer());
   public static final LootPoolEntryType REFERENCE = register("loot_table", new LootTableReference.Serializer());
   public static final LootPoolEntryType DYNAMIC = register("dynamic", new DynamicLoot.Serializer());
   public static final LootPoolEntryType TAG = register("tag", new TagEntry.Serializer());
   public static final LootPoolEntryType ALTERNATIVES = register("alternatives", CompositeEntryBase.createSerializer(AlternativesEntry::new));
   public static final LootPoolEntryType SEQUENCE = register("sequence", CompositeEntryBase.createSerializer(SequentialEntry::new));
   public static final LootPoolEntryType GROUP = register("group", CompositeEntryBase.createSerializer(EntryGroup::new));

   public LootPoolEntries() {
   }

   private static LootPoolEntryType register(String var0, Serializer<? extends LootPoolEntryContainer> var1) {
      return (LootPoolEntryType)Registry.register(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, (ResourceLocation)(new ResourceLocation(var0)), new LootPoolEntryType(var1));
   }

   public static Object createGsonAdapter() {
      return GsonAdapterFactory.builder(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE, "entry", "type", LootPoolEntryContainer::getType).build();
   }
}
