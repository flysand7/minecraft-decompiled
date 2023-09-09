package net.minecraft.advancements.critereon;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;

public class RecipeCraftedTrigger extends SimpleCriterionTrigger<RecipeCraftedTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("recipe_crafted");

   public RecipeCraftedTrigger() {
   }

   public ResourceLocation getId() {
      return ID;
   }

   protected RecipeCraftedTrigger.TriggerInstance createInstance(JsonObject var1, ContextAwarePredicate var2, DeserializationContext var3) {
      ResourceLocation var4 = new ResourceLocation(GsonHelper.getAsString(var1, "recipe_id"));
      ItemPredicate[] var5 = ItemPredicate.fromJsonArray(var1.get("ingredients"));
      return new RecipeCraftedTrigger.TriggerInstance(var2, var4, List.of(var5));
   }

   public void trigger(ServerPlayer var1, ResourceLocation var2, List<ItemStack> var3) {
      this.trigger(var1, (var2x) -> {
         return var2x.matches(var2, var3);
      });
   }

   // $FF: synthetic method
   protected AbstractCriterionTriggerInstance createInstance(JsonObject var1, ContextAwarePredicate var2, DeserializationContext var3) {
      return this.createInstance(var1, var2, var3);
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ResourceLocation recipeId;
      private final List<ItemPredicate> predicates;

      public TriggerInstance(ContextAwarePredicate var1, ResourceLocation var2, List<ItemPredicate> var3) {
         super(RecipeCraftedTrigger.ID, var1);
         this.recipeId = var2;
         this.predicates = var3;
      }

      public static RecipeCraftedTrigger.TriggerInstance craftedItem(ResourceLocation var0, List<ItemPredicate> var1) {
         return new RecipeCraftedTrigger.TriggerInstance(ContextAwarePredicate.ANY, var0, var1);
      }

      public static RecipeCraftedTrigger.TriggerInstance craftedItem(ResourceLocation var0) {
         return new RecipeCraftedTrigger.TriggerInstance(ContextAwarePredicate.ANY, var0, List.of());
      }

      boolean matches(ResourceLocation var1, List<ItemStack> var2) {
         if (!var1.equals(this.recipeId)) {
            return false;
         } else {
            ArrayList var3 = new ArrayList(var2);
            Iterator var4 = this.predicates.iterator();

            boolean var6;
            do {
               if (!var4.hasNext()) {
                  return true;
               }

               ItemPredicate var5 = (ItemPredicate)var4.next();
               var6 = false;
               Iterator var7 = var3.iterator();

               while(var7.hasNext()) {
                  if (var5.matches((ItemStack)var7.next())) {
                     var7.remove();
                     var6 = true;
                     break;
                  }
               }
            } while(var6);

            return false;
         }
      }

      public JsonObject serializeToJson(SerializationContext var1) {
         JsonObject var2 = super.serializeToJson(var1);
         var2.addProperty("recipe_id", this.recipeId.toString());
         if (this.predicates.size() > 0) {
            JsonArray var3 = new JsonArray();
            Iterator var4 = this.predicates.iterator();

            while(var4.hasNext()) {
               ItemPredicate var5 = (ItemPredicate)var4.next();
               var3.add(var5.serializeToJson());
            }

            var2.add("ingredients", var3);
         }

         return var2;
      }
   }
}
