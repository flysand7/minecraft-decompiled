package net.minecraft.world.level.block;

import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CaveVines {
   VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
   BooleanProperty BERRIES = BlockStateProperties.BERRIES;

   static InteractionResult use(@Nullable Entity var0, BlockState var1, Level var2, BlockPos var3) {
      if ((Boolean)var1.getValue(BERRIES)) {
         Block.popResource(var2, var3, new ItemStack(Items.GLOW_BERRIES, 1));
         float var4 = Mth.randomBetween(var2.random, 0.8F, 1.2F);
         var2.playSound((Player)null, (BlockPos)var3, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, var4);
         BlockState var5 = (BlockState)var1.setValue(BERRIES, false);
         var2.setBlock(var3, var5, 2);
         var2.gameEvent(GameEvent.BLOCK_CHANGE, var3, GameEvent.Context.of(var0, var5));
         return InteractionResult.sidedSuccess(var2.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   static boolean hasGlowBerries(BlockState var0) {
      return var0.hasProperty(BERRIES) && (Boolean)var0.getValue(BERRIES);
   }

   static ToIntFunction<BlockState> emission(int var0) {
      return (var1) -> {
         return (Boolean)var1.getValue(BlockStateProperties.BERRIES) ? var0 : 0;
      };
   }
}
