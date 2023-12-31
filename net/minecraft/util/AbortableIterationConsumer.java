package net.minecraft.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface AbortableIterationConsumer<T> {
   AbortableIterationConsumer.Continuation accept(T var1);

   static <T> AbortableIterationConsumer<T> forConsumer(Consumer<T> var0) {
      return (var1) -> {
         var0.accept(var1);
         return AbortableIterationConsumer.Continuation.CONTINUE;
      };
   }

   public static enum Continuation {
      CONTINUE,
      ABORT;

      private Continuation() {
      }

      public boolean shouldAbort() {
         return this == ABORT;
      }

      // $FF: synthetic method
      private static AbortableIterationConsumer.Continuation[] $values() {
         return new AbortableIterationConsumer.Continuation[]{CONTINUE, ABORT};
      }
   }
}
