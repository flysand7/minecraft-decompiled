package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class IntegerArgumentInfo implements ArgumentTypeInfo<IntegerArgumentType, IntegerArgumentInfo.Template> {
   public IntegerArgumentInfo() {
   }

   public void serializeToNetwork(IntegerArgumentInfo.Template var1, FriendlyByteBuf var2) {
      boolean var3 = var1.min != Integer.MIN_VALUE;
      boolean var4 = var1.max != Integer.MAX_VALUE;
      var2.writeByte(ArgumentUtils.createNumberFlags(var3, var4));
      if (var3) {
         var2.writeInt(var1.min);
      }

      if (var4) {
         var2.writeInt(var1.max);
      }

   }

   public IntegerArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      byte var2 = var1.readByte();
      int var3 = ArgumentUtils.numberHasMin(var2) ? var1.readInt() : Integer.MIN_VALUE;
      int var4 = ArgumentUtils.numberHasMax(var2) ? var1.readInt() : Integer.MAX_VALUE;
      return new IntegerArgumentInfo.Template(var3, var4);
   }

   public void serializeToJson(IntegerArgumentInfo.Template var1, JsonObject var2) {
      if (var1.min != Integer.MIN_VALUE) {
         var2.addProperty("min", var1.min);
      }

      if (var1.max != Integer.MAX_VALUE) {
         var2.addProperty("max", var1.max);
      }

   }

   public IntegerArgumentInfo.Template unpack(IntegerArgumentType var1) {
      return new IntegerArgumentInfo.Template(var1.getMinimum(), var1.getMaximum());
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ArgumentTypeInfo.Template unpack(ArgumentType var1) {
      return this.unpack((IntegerArgumentType)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToJson(ArgumentTypeInfo.Template var1, JsonObject var2) {
      this.serializeToJson((IntegerArgumentInfo.Template)var1, var2);
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      return this.deserializeFromNetwork(var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToNetwork(ArgumentTypeInfo.Template var1, FriendlyByteBuf var2) {
      this.serializeToNetwork((IntegerArgumentInfo.Template)var1, var2);
   }

   public final class Template implements ArgumentTypeInfo.Template<IntegerArgumentType> {
      final int min;
      final int max;

      Template(int var2, int var3) {
         this.min = var2;
         this.max = var3;
      }

      public IntegerArgumentType instantiate(CommandBuildContext var1) {
         return IntegerArgumentType.integer(this.min, this.max);
      }

      public ArgumentTypeInfo<IntegerArgumentType, ?> type() {
         return IntegerArgumentInfo.this;
      }

      // $FF: synthetic method
      public ArgumentType instantiate(CommandBuildContext var1) {
         return this.instantiate(var1);
      }
   }
}
