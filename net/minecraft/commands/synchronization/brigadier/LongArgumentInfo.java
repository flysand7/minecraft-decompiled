package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class LongArgumentInfo implements ArgumentTypeInfo<LongArgumentType, LongArgumentInfo.Template> {
   public LongArgumentInfo() {
   }

   public void serializeToNetwork(LongArgumentInfo.Template var1, FriendlyByteBuf var2) {
      boolean var3 = var1.min != Long.MIN_VALUE;
      boolean var4 = var1.max != Long.MAX_VALUE;
      var2.writeByte(ArgumentUtils.createNumberFlags(var3, var4));
      if (var3) {
         var2.writeLong(var1.min);
      }

      if (var4) {
         var2.writeLong(var1.max);
      }

   }

   public LongArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      byte var2 = var1.readByte();
      long var3 = ArgumentUtils.numberHasMin(var2) ? var1.readLong() : Long.MIN_VALUE;
      long var5 = ArgumentUtils.numberHasMax(var2) ? var1.readLong() : Long.MAX_VALUE;
      return new LongArgumentInfo.Template(var3, var5);
   }

   public void serializeToJson(LongArgumentInfo.Template var1, JsonObject var2) {
      if (var1.min != Long.MIN_VALUE) {
         var2.addProperty("min", var1.min);
      }

      if (var1.max != Long.MAX_VALUE) {
         var2.addProperty("max", var1.max);
      }

   }

   public LongArgumentInfo.Template unpack(LongArgumentType var1) {
      return new LongArgumentInfo.Template(var1.getMinimum(), var1.getMaximum());
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ArgumentTypeInfo.Template unpack(ArgumentType var1) {
      return this.unpack((LongArgumentType)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToJson(ArgumentTypeInfo.Template var1, JsonObject var2) {
      this.serializeToJson((LongArgumentInfo.Template)var1, var2);
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      return this.deserializeFromNetwork(var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToNetwork(ArgumentTypeInfo.Template var1, FriendlyByteBuf var2) {
      this.serializeToNetwork((LongArgumentInfo.Template)var1, var2);
   }

   public final class Template implements ArgumentTypeInfo.Template<LongArgumentType> {
      final long min;
      final long max;

      Template(long var2, long var4) {
         this.min = var2;
         this.max = var4;
      }

      public LongArgumentType instantiate(CommandBuildContext var1) {
         return LongArgumentType.longArg(this.min, this.max);
      }

      public ArgumentTypeInfo<LongArgumentType, ?> type() {
         return LongArgumentInfo.this;
      }

      // $FF: synthetic method
      public ArgumentType instantiate(CommandBuildContext var1) {
         return this.instantiate(var1);
      }
   }
}
