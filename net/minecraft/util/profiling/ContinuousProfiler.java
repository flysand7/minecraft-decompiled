package net.minecraft.util.profiling;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public class ContinuousProfiler {
   private final LongSupplier realTime;
   private final IntSupplier tickCount;
   private ProfileCollector profiler;

   public ContinuousProfiler(LongSupplier var1, IntSupplier var2) {
      this.profiler = InactiveProfiler.INSTANCE;
      this.realTime = var1;
      this.tickCount = var2;
   }

   public boolean isEnabled() {
      return this.profiler != InactiveProfiler.INSTANCE;
   }

   public void disable() {
      this.profiler = InactiveProfiler.INSTANCE;
   }

   public void enable() {
      this.profiler = new ActiveProfiler(this.realTime, this.tickCount, true);
   }

   public ProfilerFiller getFiller() {
      return this.profiler;
   }

   public ProfileResults getResults() {
      return this.profiler.getResults();
   }
}
