package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootPool {
   final LootPoolEntryContainer[] entries;
   final LootItemCondition[] conditions;
   private final Predicate<LootContext> compositeCondition;
   final LootItemFunction[] functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
   final NumberProvider rolls;
   final NumberProvider bonusRolls;

   LootPool(LootPoolEntryContainer[] var1, LootItemCondition[] var2, LootItemFunction[] var3, NumberProvider var4, NumberProvider var5) {
      this.entries = var1;
      this.conditions = var2;
      this.compositeCondition = LootItemConditions.andConditions(var2);
      this.functions = var3;
      this.compositeFunction = LootItemFunctions.compose(var3);
      this.rolls = var4;
      this.bonusRolls = var5;
   }

   private void addRandomItem(Consumer<ItemStack> var1, LootContext var2) {
      RandomSource var3 = var2.getRandom();
      ArrayList var4 = Lists.newArrayList();
      MutableInt var5 = new MutableInt();
      LootPoolEntryContainer[] var6 = this.entries;
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         LootPoolEntryContainer var9 = var6[var8];
         var9.expand(var2, (var3x) -> {
            int var4x = var3x.getWeight(var2.getLuck());
            if (var4x > 0) {
               var4.add(var3x);
               var5.add(var4x);
            }

         });
      }

      int var10 = var4.size();
      if (var5.intValue() != 0 && var10 != 0) {
         if (var10 == 1) {
            ((LootPoolEntry)var4.get(0)).createItemStack(var1, var2);
         } else {
            var7 = var3.nextInt(var5.intValue());
            Iterator var11 = var4.iterator();

            LootPoolEntry var12;
            do {
               if (!var11.hasNext()) {
                  return;
               }

               var12 = (LootPoolEntry)var11.next();
               var7 -= var12.getWeight(var2.getLuck());
            } while(var7 >= 0);

            var12.createItemStack(var1, var2);
         }
      }
   }

   public void addRandomItems(Consumer<ItemStack> var1, LootContext var2) {
      if (this.compositeCondition.test(var2)) {
         Consumer var3 = LootItemFunction.decorate(this.compositeFunction, var1, var2);
         int var4 = this.rolls.getInt(var2) + Mth.floor(this.bonusRolls.getFloat(var2) * var2.getLuck());

         for(int var5 = 0; var5 < var4; ++var5) {
            this.addRandomItem(var3, var2);
         }

      }
   }

   public void validate(ValidationContext var1) {
      int var2;
      for(var2 = 0; var2 < this.conditions.length; ++var2) {
         this.conditions[var2].validate(var1.forChild(".condition[" + var2 + "]"));
      }

      for(var2 = 0; var2 < this.functions.length; ++var2) {
         this.functions[var2].validate(var1.forChild(".functions[" + var2 + "]"));
      }

      for(var2 = 0; var2 < this.entries.length; ++var2) {
         this.entries[var2].validate(var1.forChild(".entries[" + var2 + "]"));
      }

      this.rolls.validate(var1.forChild(".rolls"));
      this.bonusRolls.validate(var1.forChild(".bonusRolls"));
   }

   public static LootPool.Builder lootPool() {
      return new LootPool.Builder();
   }

   public static class Builder implements FunctionUserBuilder<LootPool.Builder>, ConditionUserBuilder<LootPool.Builder> {
      private final List<LootPoolEntryContainer> entries = Lists.newArrayList();
      private final List<LootItemCondition> conditions = Lists.newArrayList();
      private final List<LootItemFunction> functions = Lists.newArrayList();
      private NumberProvider rolls = ConstantValue.exactly(1.0F);
      private NumberProvider bonusRolls = ConstantValue.exactly(0.0F);

      public Builder() {
      }

      public LootPool.Builder setRolls(NumberProvider var1) {
         this.rolls = var1;
         return this;
      }

      public LootPool.Builder unwrap() {
         return this;
      }

      public LootPool.Builder setBonusRolls(NumberProvider var1) {
         this.bonusRolls = var1;
         return this;
      }

      public LootPool.Builder add(LootPoolEntryContainer.Builder<?> var1) {
         this.entries.add(var1.build());
         return this;
      }

      public LootPool.Builder when(LootItemCondition.Builder var1) {
         this.conditions.add(var1.build());
         return this;
      }

      public LootPool.Builder apply(LootItemFunction.Builder var1) {
         this.functions.add(var1.build());
         return this;
      }

      public LootPool build() {
         if (this.rolls == null) {
            throw new IllegalArgumentException("Rolls not set");
         } else {
            return new LootPool((LootPoolEntryContainer[])this.entries.toArray(new LootPoolEntryContainer[0]), (LootItemCondition[])this.conditions.toArray(new LootItemCondition[0]), (LootItemFunction[])this.functions.toArray(new LootItemFunction[0]), this.rolls, this.bonusRolls);
         }
      }

      // $FF: synthetic method
      public FunctionUserBuilder unwrap() {
         return this.unwrap();
      }

      // $FF: synthetic method
      public FunctionUserBuilder apply(LootItemFunction.Builder var1) {
         return this.apply(var1);
      }

      // $FF: synthetic method
      public ConditionUserBuilder unwrap() {
         return this.unwrap();
      }

      // $FF: synthetic method
      public ConditionUserBuilder when(LootItemCondition.Builder var1) {
         return this.when(var1);
      }
   }

   public static class Serializer implements JsonDeserializer<LootPool>, JsonSerializer<LootPool> {
      public Serializer() {
      }

      public LootPool deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         JsonObject var4 = GsonHelper.convertToJsonObject(var1, "loot pool");
         LootPoolEntryContainer[] var5 = (LootPoolEntryContainer[])GsonHelper.getAsObject(var4, "entries", var3, LootPoolEntryContainer[].class);
         LootItemCondition[] var6 = (LootItemCondition[])GsonHelper.getAsObject(var4, "conditions", new LootItemCondition[0], var3, LootItemCondition[].class);
         LootItemFunction[] var7 = (LootItemFunction[])GsonHelper.getAsObject(var4, "functions", new LootItemFunction[0], var3, LootItemFunction[].class);
         NumberProvider var8 = (NumberProvider)GsonHelper.getAsObject(var4, "rolls", var3, NumberProvider.class);
         NumberProvider var9 = (NumberProvider)GsonHelper.getAsObject(var4, "bonus_rolls", ConstantValue.exactly(0.0F), var3, NumberProvider.class);
         return new LootPool(var5, var6, var7, var8, var9);
      }

      public JsonElement serialize(LootPool var1, Type var2, JsonSerializationContext var3) {
         JsonObject var4 = new JsonObject();
         var4.add("rolls", var3.serialize(var1.rolls));
         var4.add("bonus_rolls", var3.serialize(var1.bonusRolls));
         var4.add("entries", var3.serialize(var1.entries));
         if (!ArrayUtils.isEmpty(var1.conditions)) {
            var4.add("conditions", var3.serialize(var1.conditions));
         }

         if (!ArrayUtils.isEmpty(var1.functions)) {
            var4.add("functions", var3.serialize(var1.functions));
         }

         return var4;
      }

      // $FF: synthetic method
      public JsonElement serialize(Object var1, Type var2, JsonSerializationContext var3) {
         return this.serialize((LootPool)var1, var2, var3);
      }

      // $FF: synthetic method
      public Object deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         return this.deserialize(var1, var2, var3);
      }
   }
}
