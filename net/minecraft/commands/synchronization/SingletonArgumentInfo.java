package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.FriendlyByteBuf;

public class SingletonArgumentInfo<A extends ArgumentType<?>> implements ArgumentTypeInfo<A, SingletonArgumentInfo<A>.Template> {
   private final SingletonArgumentInfo<A>.Template template;

   private SingletonArgumentInfo(Function<CommandBuildContext, A> var1) {
      this.template = new SingletonArgumentInfo.Template(var1);
   }

   public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextFree(Supplier<T> var0) {
      return new SingletonArgumentInfo((var1) -> {
         return (ArgumentType)var0.get();
      });
   }

   public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextAware(Function<CommandBuildContext, T> var0) {
      return new SingletonArgumentInfo(var0);
   }

   public void serializeToNetwork(SingletonArgumentInfo<A>.Template var1, FriendlyByteBuf var2) {
   }

   public void serializeToJson(SingletonArgumentInfo<A>.Template var1, JsonObject var2) {
   }

   public SingletonArgumentInfo<A>.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      return this.template;
   }

   public SingletonArgumentInfo<A>.Template unpack(A var1) {
      return this.template;
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template unpack(ArgumentType var1) {
      return this.unpack(var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToJson(ArgumentTypeInfo.Template var1, JsonObject var2) {
      this.serializeToJson((SingletonArgumentInfo.Template)var1, var2);
   }

   // $FF: synthetic method
   public ArgumentTypeInfo.Template deserializeFromNetwork(FriendlyByteBuf var1) {
      return this.deserializeFromNetwork(var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void serializeToNetwork(ArgumentTypeInfo.Template var1, FriendlyByteBuf var2) {
      this.serializeToNetwork((SingletonArgumentInfo.Template)var1, var2);
   }

   public final class Template implements ArgumentTypeInfo.Template<A> {
      private final Function<CommandBuildContext, A> constructor;

      public Template(Function<CommandBuildContext, A> var2) {
         this.constructor = var2;
      }

      public A instantiate(CommandBuildContext var1) {
         return (ArgumentType)this.constructor.apply(var1);
      }

      public ArgumentTypeInfo<A, ?> type() {
         return SingletonArgumentInfo.this;
      }
   }
}
