package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.resources.ResourceLocation;

public abstract class AbstractCriterionTriggerInstance implements CriterionTriggerInstance {
   private final ResourceLocation criterion;
   private final ContextAwarePredicate player;

   public AbstractCriterionTriggerInstance(ResourceLocation var1, ContextAwarePredicate var2) {
      this.criterion = var1;
      this.player = var2;
   }

   public ResourceLocation getCriterion() {
      return this.criterion;
   }

   protected ContextAwarePredicate getPlayerPredicate() {
      return this.player;
   }

   public JsonObject serializeToJson(SerializationContext var1) {
      JsonObject var2 = new JsonObject();
      var2.add("player", this.player.toJson(var1));
      return var2;
   }

   public String toString() {
      return "AbstractCriterionInstance{criterion=" + this.criterion + "}";
   }
}
