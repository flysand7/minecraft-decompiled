package net.minecraft.world.entity.ai.behavior;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class Mount {
   private static final int CLOSE_ENOUGH_TO_START_RIDING_DIST = 1;

   public Mount() {
   }

   public static BehaviorControl<LivingEntity> create(float var0) {
      return BehaviorBuilder.create((var1) -> {
         return var1.group(var1.registered(MemoryModuleType.LOOK_TARGET), var1.absent(MemoryModuleType.WALK_TARGET), var1.present(MemoryModuleType.RIDE_TARGET)).apply(var1, (var2, var3, var4) -> {
            return (var5, var6, var7) -> {
               if (var6.isPassenger()) {
                  return false;
               } else {
                  Entity var9 = (Entity)var1.get(var4);
                  if (var9.closerThan(var6, 1.0D)) {
                     var6.startRiding(var9);
                  } else {
                     var2.set(new EntityTracker(var9, true));
                     var3.set(new WalkTarget(new EntityTracker(var9, false), var0, 1));
                  }

                  return true;
               }
            };
         });
      });
   }
}
