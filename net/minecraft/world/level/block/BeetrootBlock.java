package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BeetrootBlock extends CropBlock {
   public static final int MAX_AGE = 3;
   public static final IntegerProperty AGE;
   private static final VoxelShape[] SHAPE_BY_AGE;

   public BeetrootBlock(BlockBehaviour.Properties var1) {
      super(var1);
   }

   protected IntegerProperty getAgeProperty() {
      return AGE;
   }

   public int getMaxAge() {
      return 3;
   }

   protected ItemLike getBaseSeedId() {
      return Items.BEETROOT_SEEDS;
   }

   public void randomTick(BlockState var1, ServerLevel var2, BlockPos var3, RandomSource var4) {
      if (var4.nextInt(3) != 0) {
         super.randomTick(var1, var2, var3, var4);
      }

   }

   protected int getBonemealAgeIncrease(Level var1) {
      return super.getBonemealAgeIncrease(var1) / 3;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(AGE);
   }

   public VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return SHAPE_BY_AGE[this.getAge(var1)];
   }

   static {
      AGE = BlockStateProperties.AGE_3;
      SHAPE_BY_AGE = new VoxelShape[]{Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D), Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D)};
   }
}
