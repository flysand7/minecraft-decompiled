package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CalibratedSculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public class CalibratedSculkSensorBlockEntity extends SculkSensorBlockEntity {
   public CalibratedSculkSensorBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.CALIBRATED_SCULK_SENSOR, var1, var2);
   }

   public VibrationSystem.User createVibrationUser() {
      return new CalibratedSculkSensorBlockEntity.VibrationUser(this.getBlockPos());
   }

   protected class VibrationUser extends SculkSensorBlockEntity.VibrationUser {
      public VibrationUser(BlockPos var2) {
         super(var2);
      }

      public int getListenerRadius() {
         return 16;
      }

      public boolean canReceiveVibration(ServerLevel var1, BlockPos var2, GameEvent var3, @Nullable GameEvent.Context var4) {
         int var5 = this.getBackSignal(var1, this.blockPos, CalibratedSculkSensorBlockEntity.this.getBlockState());
         return var5 != 0 && VibrationSystem.getGameEventFrequency(var3) != var5 ? false : super.canReceiveVibration(var1, var2, var3, var4);
      }

      private int getBackSignal(Level var1, BlockPos var2, BlockState var3) {
         Direction var4 = ((Direction)var3.getValue(CalibratedSculkSensorBlock.FACING)).getOpposite();
         return var1.getSignal(var2.relative(var4), var4);
      }
   }
}
