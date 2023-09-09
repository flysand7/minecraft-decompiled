package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentUtils;
import net.minecraft.network.FriendlyByteBuf;

public class FloatArgumentInfo implements ArgumentTypeInfo<FloatArgumentType, FloatArgumentInfo.Template> {
   public FloatArgumentInfo() {
   }

   public void serializeToNetwork(FloatArgumentInfo.Template var1, FriendlyByteBuf var2) {
      boolean var3 = var1.min != -3.4028235E38F;
      boolean var4 = var1.max != Float.MAX_VALUE;
      var2.writeByte(ArgumentUtils.createNumberFlags(var3, var4));
      if (var3) {
         var2.writeFloat(var1.min);
      }

      if (var4) {
         var2.writeFloat(var1.max);
      }

   }

   public FloatArgumentInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      byte var2 = var1.readByte();
      float var3 = ArgumentUtils.numberHasMin(var2) ? var1.readFloat() : -3.4028235E38F;
      float var4 = ArgumentUtils.numberHasMax(var2) ? var1.readFloat() : Float.MAX_VALUE;
      return new FloatArgumentInfo.Template(var3, var4);
   }

   public void serializeToJson(FloatArgumentInfo.Template var1, JsonObject var2) {
      if (var1.min != -3.4028235E38F) {
         var2.addProperty("min", var1.min);
      }

      if (var1.max != Float.MAX_VALUE) {
         var2.addProperty("max", var1.max);
      }

   }

   public FloatArgumentInfo.Template unpack(FloatArgumentType var1) {
      return new FloatArgumentInfo.Template(var1.getMinimum(), var1.getMaximum());
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ArgumentTypeInfo.Template unpack(ArgumentType var1) {
      return this.unpack((FloatArgumentType)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToJson(ArgumentTypeInfo.Template var1, JsonObject var2) {
      this.serializeToJson((FloatArgumentInfo.Template)var1, var2);
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      return this.deserializeFromNetwork(var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToNetwork(ArgumentTypeInfo.Template var1, FriendlyByteBuf var2) {
      this.serializeToNetwork((FloatArgumentInfo.Template)var1, var2);
   }

   public final class Template implements ArgumentTypeInfo.Template<FloatArgumentType> {
      final float min;
      final float max;

      Template(float var2, float var3) {
         this.min = var2;
         this.max = var3;
      }

      public FloatArgumentType instantiate(CommandBuildContext var1) {
         return FloatArgumentType.floatArg(this.min, this.max);
      }

      public ArgumentTypeInfo<FloatArgumentType, ?> type() {
         return FloatArgumentInfo.this;
      }

      // $FF: synthetic method
      public ArgumentType instantiate(CommandBuildContext var1) {
         return this.instantiate(var1);
      }
   }
}
