package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.slf4j.Logger;

public class FutureChain implements TaskChainer, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CompletableFuture<?> head = CompletableFuture.completedFuture((Object)null);
   private final Executor checkedExecutor;
   private volatile boolean closed;

   public FutureChain(Executor var1) {
      this.checkedExecutor = (var2) -> {
         if (!this.closed) {
            var1.execute(var2);
         }

      };
   }

   public void append(TaskChainer.DelayedTask var1) {
      this.head = this.head.thenComposeAsync((var2) -> {
         return var1.submit(this.checkedExecutor);
      }, this.checkedExecutor).exceptionally((var0) -> {
         if (var0 instanceof CompletionException) {
            CompletionException var1 = (CompletionException)var0;
            var0 = var1.getCause();
         }

         if (var0 instanceof CancellationException) {
            CancellationException var2 = (CancellationException)var0;
            throw var2;
         } else {
            LOGGER.error("Chain link failed, continuing to next one", var0);
            return null;
         }
      });
   }

   public void close() {
      this.closed = true;
   }
}
