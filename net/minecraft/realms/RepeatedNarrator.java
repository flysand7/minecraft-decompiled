package net.minecraft.realms;

import com.google.common.util.concurrent.RateLimiter;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.GameNarrator;
import net.minecraft.network.chat.Component;

public class RepeatedNarrator {
   private final float permitsPerSecond;
   private final AtomicReference<RepeatedNarrator.Params> params = new AtomicReference();

   public RepeatedNarrator(Duration var1) {
      this.permitsPerSecond = 1000.0F / (float)var1.toMillis();
   }

   public void narrate(GameNarrator var1, Component var2) {
      RepeatedNarrator.Params var3 = (RepeatedNarrator.Params)this.params.updateAndGet((var2x) -> {
         return var2x != null && var2.equals(var2x.narration) ? var2x : new RepeatedNarrator.Params(var2, RateLimiter.create((double)this.permitsPerSecond));
      });
      if (var3.rateLimiter.tryAcquire(1)) {
         var1.sayNow(var2);
      }

   }

   private static class Params {
      final Component narration;
      final RateLimiter rateLimiter;

      Params(Component var1, RateLimiter var2) {
         this.narration = var1;
         this.rateLimiter = var2;
      }
   }
}
