package net.minecraft.world.level.storage.loot.predicates;

public class AnyOfCondition extends CompositeLootItemCondition {
   AnyOfCondition(LootItemCondition[] var1) {
      super(var1, LootItemConditions.orConditions(var1));
   }

   public LootItemConditionType getType() {
      return LootItemConditions.ANY_OF;
   }

   public static AnyOfCondition.Builder anyOf(LootItemCondition.Builder... var0) {
      return new AnyOfCondition.Builder(var0);
   }

   public static class Builder extends CompositeLootItemCondition.Builder {
      public Builder(LootItemCondition.Builder... var1) {
         super(var1);
      }

      public AnyOfCondition.Builder or(LootItemCondition.Builder var1) {
         this.addTerm(var1);
         return this;
      }

      protected LootItemCondition create(LootItemCondition[] var1) {
         return new AnyOfCondition(var1);
      }
   }

   public static class Serializer extends CompositeLootItemCondition.Serializer<AnyOfCondition> {
      public Serializer() {
      }

      protected AnyOfCondition create(LootItemCondition[] var1) {
         return new AnyOfCondition(var1);
      }

      // $FF: synthetic method
      protected CompositeLootItemCondition create(LootItemCondition[] var1) {
         return this.create(var1);
      }
   }
}
