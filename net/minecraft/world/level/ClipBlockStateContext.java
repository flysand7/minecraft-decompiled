package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ClipBlockStateContext {
   private final Vec3 from;
   private final Vec3 to;
   private final Predicate<BlockState> block;

   public ClipBlockStateContext(Vec3 var1, Vec3 var2, Predicate<BlockState> var3) {
      this.from = var1;
      this.to = var2;
      this.block = var3;
   }

   public Vec3 getTo() {
      return this.to;
   }

   public Vec3 getFrom() {
      return this.from;
   }

   public Predicate<BlockState> isTargetBlock() {
      return this.block;
   }
}
