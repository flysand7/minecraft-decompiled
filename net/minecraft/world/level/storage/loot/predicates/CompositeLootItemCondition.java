package net.minecraft.world.level.storage.loot.predicates;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeLootItemCondition implements LootItemCondition {
   final LootItemCondition[] terms;
   private final Predicate<LootContext> composedPredicate;

   protected CompositeLootItemCondition(LootItemCondition[] var1, Predicate<LootContext> var2) {
      this.terms = var1;
      this.composedPredicate = var2;
   }

   public final boolean test(LootContext var1) {
      return this.composedPredicate.test(var1);
   }

   public void validate(ValidationContext var1) {
      LootItemCondition.super.validate(var1);

      for(int var2 = 0; var2 < this.terms.length; ++var2) {
         this.terms[var2].validate(var1.forChild(".term[" + var2 + "]"));
      }

   }

   // $FF: synthetic method
   public boolean test(Object var1) {
      return this.test((LootContext)var1);
   }

   public abstract static class Serializer<T extends CompositeLootItemCondition> implements net.minecraft.world.level.storage.loot.Serializer<T> {
      public Serializer() {
      }

      public void serialize(JsonObject var1, CompositeLootItemCondition var2, JsonSerializationContext var3) {
         var1.add("terms", var3.serialize(var2.terms));
      }

      public T deserialize(JsonObject var1, JsonDeserializationContext var2) {
         LootItemCondition[] var3 = (LootItemCondition[])GsonHelper.getAsObject(var1, "terms", var2, LootItemCondition[].class);
         return this.create(var3);
      }

      protected abstract T create(LootItemCondition[] var1);

      // $FF: synthetic method
      public Object deserialize(JsonObject var1, JsonDeserializationContext var2) {
         return this.deserialize(var1, var2);
      }

      // $FF: synthetic method
      // $FF: bridge method
      public void serialize(JsonObject var1, Object var2, JsonSerializationContext var3) {
         this.serialize(var1, (CompositeLootItemCondition)var2, var3);
      }
   }

   public abstract static class Builder implements LootItemCondition.Builder {
      private final List<LootItemCondition> terms = new ArrayList();

      public Builder(LootItemCondition.Builder... var1) {
         LootItemCondition.Builder[] var2 = var1;
         int var3 = var1.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            LootItemCondition.Builder var5 = var2[var4];
            this.terms.add(var5.build());
         }

      }

      public void addTerm(LootItemCondition.Builder var1) {
         this.terms.add(var1.build());
      }

      public LootItemCondition build() {
         LootItemCondition[] var1 = (LootItemCondition[])this.terms.toArray((var0) -> {
            return new LootItemCondition[var0];
         });
         return this.create(var1);
      }

      protected abstract LootItemCondition create(LootItemCondition[] var1);
   }
}
