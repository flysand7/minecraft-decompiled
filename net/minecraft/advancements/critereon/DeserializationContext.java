package net.minecraft.advancements.critereon;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class DeserializationContext {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation id;
   private final LootDataManager lootData;
   private final Gson predicateGson = Deserializers.createConditionSerializer().create();

   public DeserializationContext(ResourceLocation var1, LootDataManager var2) {
      this.id = var1;
      this.lootData = var2;
   }

   public final LootItemCondition[] deserializeConditions(JsonArray var1, String var2, LootContextParamSet var3) {
      LootItemCondition[] var4 = (LootItemCondition[])this.predicateGson.fromJson(var1, LootItemCondition[].class);
      ValidationContext var5 = new ValidationContext(var3, this.lootData);
      LootItemCondition[] var6 = var4;
      int var7 = var4.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         LootItemCondition var9 = var6[var8];
         var9.validate(var5);
         var5.getProblems().forEach((var1x, var2x) -> {
            LOGGER.warn("Found validation problem in advancement trigger {}/{}: {}", new Object[]{var2, var1x, var2x});
         });
      }

      return var4;
   }

   public ResourceLocation getAdvancementId() {
      return this.id;
   }
}
