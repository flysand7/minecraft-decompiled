package net.minecraft.world.level.block;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DecoratedPotBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final ResourceLocation SHERDS_DYNAMIC_DROP_ID = new ResourceLocation("sherds");
   private static final VoxelShape BOUNDING_BOX = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
   private static final DirectionProperty HORIZONTAL_FACING;
   private static final BooleanProperty CRACKED;
   private static final BooleanProperty WATERLOGGED;

   protected DecoratedPotBlock(BlockBehaviour.Properties var1) {
      super(var1);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(HORIZONTAL_FACING, Direction.NORTH)).setValue(WATERLOGGED, false)).setValue(CRACKED, false));
   }

   public BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6) {
      if ((Boolean)var1.getValue(WATERLOGGED)) {
         var4.scheduleTick(var5, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay(var4));
      }

      return super.updateShape(var1, var2, var3, var4, var5, var6);
   }

   public BlockState getStateForPlacement(BlockPlaceContext var1) {
      FluidState var2 = var1.getLevel().getFluidState(var1.getClickedPos());
      return (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, var1.getHorizontalDirection())).setValue(WATERLOGGED, var2.getType() == Fluids.WATER)).setValue(CRACKED, false);
   }

   public boolean isPathfindable(BlockState var1, BlockGetter var2, BlockPos var3, PathComputationType var4) {
      return false;
   }

   public VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return BOUNDING_BOX;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(HORIZONTAL_FACING, WATERLOGGED, CRACKED);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos var1, BlockState var2) {
      return new DecoratedPotBlockEntity(var1, var2);
   }

   public List<ItemStack> getDrops(BlockState var1, LootParams.Builder var2) {
      BlockEntity var3 = (BlockEntity)var2.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (var3 instanceof DecoratedPotBlockEntity) {
         DecoratedPotBlockEntity var4 = (DecoratedPotBlockEntity)var3;
         var2.withDynamicDrop(SHERDS_DYNAMIC_DROP_ID, (var1x) -> {
            var4.getDecorations().sorted().map(Item::getDefaultInstance).forEach(var1x);
         });
      }

      return super.getDrops(var1, var2);
   }

   public void playerWillDestroy(Level var1, BlockPos var2, BlockState var3, Player var4) {
      ItemStack var5 = var4.getMainHandItem();
      BlockState var6 = var3;
      if (var5.is(ItemTags.BREAKS_DECORATED_POTS) && !EnchantmentHelper.hasSilkTouch(var5)) {
         var6 = (BlockState)var3.setValue(CRACKED, true);
         var1.setBlock(var2, var6, 4);
      }

      super.playerWillDestroy(var1, var2, var6, var4);
   }

   public FluidState getFluidState(BlockState var1) {
      return (Boolean)var1.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(var1);
   }

   public SoundType getSoundType(BlockState var1) {
      return (Boolean)var1.getValue(CRACKED) ? SoundType.DECORATED_POT_CRACKED : SoundType.DECORATED_POT;
   }

   public void appendHoverText(ItemStack var1, @Nullable BlockGetter var2, List<Component> var3, TooltipFlag var4) {
      super.appendHoverText(var1, var2, var3, var4);
      DecoratedPotBlockEntity.Decorations var5 = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(var1));
      if (!var5.equals(DecoratedPotBlockEntity.Decorations.EMPTY)) {
         var3.add(CommonComponents.EMPTY);
         Stream.of(var5.front(), var5.left(), var5.right(), var5.back()).forEach((var1x) -> {
            var3.add((new ItemStack(var1x, 1)).getHoverName().plainCopy().withStyle(ChatFormatting.GRAY));
         });
      }
   }

   static {
      HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
      CRACKED = BlockStateProperties.CRACKED;
      WATERLOGGED = BlockStateProperties.WATERLOGGED;
   }
}
