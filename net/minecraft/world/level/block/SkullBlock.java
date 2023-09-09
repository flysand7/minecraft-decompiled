package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SkullBlock extends AbstractSkullBlock {
   public static final int MAX = RotationSegment.getMaxSegmentIndex();
   private static final int ROTATIONS;
   public static final IntegerProperty ROTATION;
   protected static final VoxelShape SHAPE;
   protected static final VoxelShape PIGLIN_SHAPE;

   protected SkullBlock(SkullBlock.Type var1, BlockBehaviour.Properties var2) {
      super(var1, var2);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(ROTATION, 0));
   }

   public VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return this.getType() == SkullBlock.Types.PIGLIN ? PIGLIN_SHAPE : SHAPE;
   }

   public VoxelShape getOcclusionShape(BlockState var1, BlockGetter var2, BlockPos var3) {
      return Shapes.empty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext var1) {
      return (BlockState)this.defaultBlockState().setValue(ROTATION, RotationSegment.convertToSegment(var1.getRotation()));
   }

   public BlockState rotate(BlockState var1, Rotation var2) {
      return (BlockState)var1.setValue(ROTATION, var2.rotate((Integer)var1.getValue(ROTATION), ROTATIONS));
   }

   public BlockState mirror(BlockState var1, Mirror var2) {
      return (BlockState)var1.setValue(ROTATION, var2.mirror((Integer)var1.getValue(ROTATION), ROTATIONS));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(ROTATION);
   }

   static {
      ROTATIONS = MAX + 1;
      ROTATION = BlockStateProperties.ROTATION_16;
      SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);
      PIGLIN_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);
   }

   public interface Type {
   }

   public static enum Types implements SkullBlock.Type {
      SKELETON,
      WITHER_SKELETON,
      PLAYER,
      ZOMBIE,
      CREEPER,
      PIGLIN,
      DRAGON;

      private Types() {
      }

      // $FF: synthetic method
      private static SkullBlock.Types[] $values() {
         return new SkullBlock.Types[]{SKELETON, WITHER_SKELETON, PLAYER, ZOMBIE, CREEPER, PIGLIN, DRAGON};
      }
   }
}
