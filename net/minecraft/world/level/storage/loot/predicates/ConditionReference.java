package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import org.slf4j.Logger;

public class ConditionReference implements LootItemCondition {
   private static final Logger LOGGER = LogUtils.getLogger();
   final ResourceLocation name;

   ConditionReference(ResourceLocation var1) {
      this.name = var1;
   }

   public LootItemConditionType getType() {
      return LootItemConditions.REFERENCE;
   }

   public void validate(ValidationContext var1) {
      LootDataId var2 = new LootDataId(LootDataType.PREDICATE, this.name);
      if (var1.hasVisitedElement(var2)) {
         var1.reportProblem("Condition " + this.name + " is recursively called");
      } else {
         LootItemCondition.super.validate(var1);
         var1.resolver().getElementOptional(var2).ifPresentOrElse((var3) -> {
            var3.validate(var1.enterElement(".{" + this.name + "}", var2));
         }, () -> {
            var1.reportProblem("Unknown condition table called " + this.name);
         });
      }
   }

   public boolean test(LootContext var1) {
      LootItemCondition var2 = (LootItemCondition)var1.getResolver().getElement(LootDataType.PREDICATE, this.name);
      if (var2 == null) {
         LOGGER.warn("Tried using unknown condition table called {}", this.name);
         return false;
      } else {
         LootContext.VisitedEntry var3 = LootContext.createVisitedEntry(var2);
         if (var1.pushVisitedElement(var3)) {
            boolean var4;
            try {
               var4 = var2.test(var1);
            } finally {
               var1.popVisitedElement(var3);
            }

            return var4;
         } else {
            LOGGER.warn("Detected infinite loop in loot tables");
            return false;
         }
      }
   }

   public static LootItemCondition.Builder conditionReference(ResourceLocation var0) {
      return () -> {
         return new ConditionReference(var0);
      };
   }

   // $FF: synthetic method
   public boolean test(Object var1) {
      return this.test((LootContext)var1);
   }

   public static class Serializer implements net.minecraft.world.level.storage.loot.Serializer<ConditionReference> {
      public Serializer() {
      }

      public void serialize(JsonObject var1, ConditionReference var2, JsonSerializationContext var3) {
         var1.addProperty("name", var2.name.toString());
      }

      public ConditionReference deserialize(JsonObject var1, JsonDeserializationContext var2) {
         ResourceLocation var3 = new ResourceLocation(GsonHelper.getAsString(var1, "name"));
         return new ConditionReference(var3);
      }

      // $FF: synthetic method
      public Object deserialize(JsonObject var1, JsonDeserializationContext var2) {
         return this.deserialize(var1, var2);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void serialize(JsonObject var1, Object var2, JsonSerializationContext var3) {
         this.serialize(var1, (ConditionReference)var2, var3);
      }
   }
}
