package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class WeightedPressurePlateBlock extends BasePressurePlateBlock {
   public static final IntegerProperty POWER;
   private final int maxWeight;

   protected WeightedPressurePlateBlock(int var1, BlockBehaviour.Properties var2, BlockSetType var3) {
      super(var2, var3);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(POWER, 0));
      this.maxWeight = var1;
   }

   protected int getSignalStrength(Level var1, BlockPos var2) {
      int var3 = Math.min(getEntityCount(var1, TOUCH_AABB.move(var2), Entity.class), this.maxWeight);
      if (var3 > 0) {
         float var4 = (float)Math.min(this.maxWeight, var3) / (float)this.maxWeight;
         return Mth.ceil(var4 * 15.0F);
      } else {
         return 0;
      }
   }

   protected int getSignalForState(BlockState var1) {
      return (Integer)var1.getValue(POWER);
   }

   protected BlockState setSignalForState(BlockState var1, int var2) {
      return (BlockState)var1.setValue(POWER, var2);
   }

   protected int getPressedTime() {
      return 10;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(POWER);
   }

   static {
      POWER = BlockStateProperties.POWER;
   }
}
