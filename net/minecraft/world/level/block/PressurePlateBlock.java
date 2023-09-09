package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class PressurePlateBlock extends BasePressurePlateBlock {
   public static final BooleanProperty POWERED;
   private final PressurePlateBlock.Sensitivity sensitivity;

   protected PressurePlateBlock(PressurePlateBlock.Sensitivity var1, BlockBehaviour.Properties var2, BlockSetType var3) {
      super(var2, var3);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWERED, false));
      this.sensitivity = var1;
   }

   protected int getSignalForState(BlockState var1) {
      return (Boolean)var1.getValue(POWERED) ? 15 : 0;
   }

   protected BlockState setSignalForState(BlockState var1, int var2) {
      return (BlockState)var1.setValue(POWERED, var2 > 0);
   }

   protected int getSignalStrength(Level var1, BlockPos var2) {
      Class var10000;
      switch(this.sensitivity) {
      case EVERYTHING:
         var10000 = Entity.class;
         break;
      case MOBS:
         var10000 = LivingEntity.class;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      Class var3 = var10000;
      return getEntityCount(var1, TOUCH_AABB.move(var2), var3) > 0 ? 15 : 0;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(POWERED);
   }

   static {
      POWERED = BlockStateProperties.POWERED;
   }

   public static enum Sensitivity {
      EVERYTHING,
      MOBS;

      private Sensitivity() {
      }

      // $FF: synthetic method
      private static PressurePlateBlock.Sensitivity[] $values() {
         return new PressurePlateBlock.Sensitivity[]{EVERYTHING, MOBS};
      }
   }
}
