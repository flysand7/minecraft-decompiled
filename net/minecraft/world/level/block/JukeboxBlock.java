package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class JukeboxBlock extends BaseEntityBlock {
   public static final BooleanProperty HAS_RECORD;

   protected JukeboxBlock(BlockBehaviour.Properties var1) {
      super(var1);
      this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(HAS_RECORD, false));
   }

   public void setPlacedBy(Level var1, BlockPos var2, BlockState var3, @Nullable LivingEntity var4, ItemStack var5) {
      super.setPlacedBy(var1, var2, var3, var4, var5);
      CompoundTag var6 = BlockItem.getBlockEntityData(var5);
      if (var6 != null && var6.contains("RecordItem")) {
         var1.setBlock(var2, (BlockState)var3.setValue(HAS_RECORD, true), 2);
      }

   }

   public InteractionResult use(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, BlockHitResult var6) {
      if ((Boolean)var1.getValue(HAS_RECORD)) {
         BlockEntity var8 = var2.getBlockEntity(var3);
         if (var8 instanceof JukeboxBlockEntity) {
            JukeboxBlockEntity var7 = (JukeboxBlockEntity)var8;
            var7.popOutRecord();
            return InteractionResult.sidedSuccess(var2.isClientSide);
         }
      }

      return InteractionResult.PASS;
   }

   public void onRemove(BlockState var1, Level var2, BlockPos var3, BlockState var4, boolean var5) {
      if (!var1.is(var4.getBlock())) {
         BlockEntity var7 = var2.getBlockEntity(var3);
         if (var7 instanceof JukeboxBlockEntity) {
            JukeboxBlockEntity var6 = (JukeboxBlockEntity)var7;
            var6.popOutRecord();
         }

         super.onRemove(var1, var2, var3, var4, var5);
      }
   }

   public BlockEntity newBlockEntity(BlockPos var1, BlockState var2) {
      return new JukeboxBlockEntity(var1, var2);
   }

   public boolean isSignalSource(BlockState var1) {
      return true;
   }

   public int getSignal(BlockState var1, BlockGetter var2, BlockPos var3, Direction var4) {
      BlockEntity var6 = var2.getBlockEntity(var3);
      if (var6 instanceof JukeboxBlockEntity) {
         JukeboxBlockEntity var5 = (JukeboxBlockEntity)var6;
         if (var5.isRecordPlaying()) {
            return 15;
         }
      }

      return 0;
   }

   public boolean hasAnalogOutputSignal(BlockState var1) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState var1, Level var2, BlockPos var3) {
      BlockEntity var6 = var2.getBlockEntity(var3);
      if (var6 instanceof JukeboxBlockEntity) {
         JukeboxBlockEntity var4 = (JukeboxBlockEntity)var6;
         Item var7 = var4.getFirstItem().getItem();
         if (var7 instanceof RecordItem) {
            RecordItem var5 = (RecordItem)var7;
            return var5.getAnalogOutput();
         }
      }

      return 0;
   }

   public RenderShape getRenderShape(BlockState var1) {
      return RenderShape.MODEL;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(HAS_RECORD);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level var1, BlockState var2, BlockEntityType<T> var3) {
      return (Boolean)var2.getValue(HAS_RECORD) ? createTickerHelper(var3, BlockEntityType.JUKEBOX, JukeboxBlockEntity::playRecordTick) : null;
   }

   static {
      HAS_RECORD = BlockStateProperties.HAS_RECORD;
   }
}
