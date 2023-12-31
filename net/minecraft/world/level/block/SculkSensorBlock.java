package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SculkSensorBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final int ACTIVE_TICKS = 30;
   public static final int COOLDOWN_TICKS = 10;
   public static final EnumProperty<SculkSensorPhase> PHASE;
   public static final IntegerProperty POWER;
   public static final BooleanProperty WATERLOGGED;
   protected static final VoxelShape SHAPE;
   private static final float[] RESONANCE_PITCH_BEND;

   public SculkSensorBlock(BlockBehaviour.Properties var1) {
      super(var1);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(PHASE, SculkSensorPhase.INACTIVE)).setValue(POWER, 0)).setValue(WATERLOGGED, false));
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext var1) {
      BlockPos var2 = var1.getClickedPos();
      FluidState var3 = var1.getLevel().getFluidState(var2);
      return (BlockState)this.defaultBlockState().setValue(WATERLOGGED, var3.getType() == Fluids.WATER);
   }

   public FluidState getFluidState(BlockState var1) {
      return (Boolean)var1.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(var1);
   }

   public void tick(BlockState var1, ServerLevel var2, BlockPos var3, RandomSource var4) {
      if (getPhase(var1) != SculkSensorPhase.ACTIVE) {
         if (getPhase(var1) == SculkSensorPhase.COOLDOWN) {
            var2.setBlock(var3, (BlockState)var1.setValue(PHASE, SculkSensorPhase.INACTIVE), 3);
            if (!(Boolean)var1.getValue(WATERLOGGED)) {
               var2.playSound((Player)null, var3, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, var2.random.nextFloat() * 0.2F + 0.8F);
            }
         }

      } else {
         deactivate(var2, var3, var1);
      }
   }

   public void stepOn(Level var1, BlockPos var2, BlockState var3, Entity var4) {
      if (!var1.isClientSide() && canActivate(var3) && var4.getType() != EntityType.WARDEN) {
         BlockEntity var5 = var1.getBlockEntity(var2);
         if (var5 instanceof SculkSensorBlockEntity) {
            SculkSensorBlockEntity var6 = (SculkSensorBlockEntity)var5;
            if (var1 instanceof ServerLevel) {
               ServerLevel var7 = (ServerLevel)var1;
               if (var6.getVibrationUser().canReceiveVibration(var7, var2, GameEvent.STEP, GameEvent.Context.of(var3))) {
                  var6.getListener().forceScheduleVibration(var7, GameEvent.STEP, GameEvent.Context.of(var4), var4.position());
               }
            }
         }
      }

      super.stepOn(var1, var2, var3, var4);
   }

   public void onPlace(BlockState var1, Level var2, BlockPos var3, BlockState var4, boolean var5) {
      if (!var2.isClientSide() && !var1.is(var4.getBlock())) {
         if ((Integer)var1.getValue(POWER) > 0 && !var2.getBlockTicks().hasScheduledTick(var3, this)) {
            var2.setBlock(var3, (BlockState)var1.setValue(POWER, 0), 18);
         }

      }
   }

   public void onRemove(BlockState var1, Level var2, BlockPos var3, BlockState var4, boolean var5) {
      if (!var1.is(var4.getBlock())) {
         if (getPhase(var1) == SculkSensorPhase.ACTIVE) {
            updateNeighbours(var2, var3, var1);
         }

         super.onRemove(var1, var2, var3, var4, var5);
      }
   }

   public BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6) {
      if ((Boolean)var1.getValue(WATERLOGGED)) {
         var4.scheduleTick(var5, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay(var4));
      }

      return super.updateShape(var1, var2, var3, var4, var5, var6);
   }

   private static void updateNeighbours(Level var0, BlockPos var1, BlockState var2) {
      Block var3 = var2.getBlock();
      var0.updateNeighborsAt(var1, var3);
      var0.updateNeighborsAt(var1.below(), var3);
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos var1, BlockState var2) {
      return new SculkSensorBlockEntity(var1, var2);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level var1, BlockState var2, BlockEntityType<T> var3) {
      return !var1.isClientSide ? createTickerHelper(var3, BlockEntityType.SCULK_SENSOR, (var0, var1x, var2x, var3x) -> {
         VibrationSystem.Ticker.tick(var0, var3x.getVibrationData(), var3x.getVibrationUser());
      }) : null;
   }

   public RenderShape getRenderShape(BlockState var1) {
      return RenderShape.MODEL;
   }

   public VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return SHAPE;
   }

   public boolean isSignalSource(BlockState var1) {
      return true;
   }

   public int getSignal(BlockState var1, BlockGetter var2, BlockPos var3, Direction var4) {
      return (Integer)var1.getValue(POWER);
   }

   public int getDirectSignal(BlockState var1, BlockGetter var2, BlockPos var3, Direction var4) {
      return var4 == Direction.UP ? var1.getSignal(var2, var3, var4) : 0;
   }

   public static SculkSensorPhase getPhase(BlockState var0) {
      return (SculkSensorPhase)var0.getValue(PHASE);
   }

   public static boolean canActivate(BlockState var0) {
      return getPhase(var0) == SculkSensorPhase.INACTIVE;
   }

   public static void deactivate(Level var0, BlockPos var1, BlockState var2) {
      var0.setBlock(var1, (BlockState)((BlockState)var2.setValue(PHASE, SculkSensorPhase.COOLDOWN)).setValue(POWER, 0), 3);
      var0.scheduleTick(var1, var2.getBlock(), 10);
      updateNeighbours(var0, var1, var2);
   }

   @VisibleForTesting
   public int getActiveTicks() {
      return 30;
   }

   public void activate(@Nullable Entity var1, Level var2, BlockPos var3, BlockState var4, int var5, int var6) {
      var2.setBlock(var3, (BlockState)((BlockState)var4.setValue(PHASE, SculkSensorPhase.ACTIVE)).setValue(POWER, var5), 3);
      var2.scheduleTick(var3, var4.getBlock(), this.getActiveTicks());
      updateNeighbours(var2, var3, var4);
      tryResonateVibration(var1, var2, var3, var6);
      var2.gameEvent(var1, GameEvent.SCULK_SENSOR_TENDRILS_CLICKING, var3);
      if (!(Boolean)var4.getValue(WATERLOGGED)) {
         var2.playSound((Player)null, (double)var3.getX() + 0.5D, (double)var3.getY() + 0.5D, (double)var3.getZ() + 0.5D, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, var2.random.nextFloat() * 0.2F + 0.8F);
      }

   }

   public static void tryResonateVibration(@Nullable Entity var0, Level var1, BlockPos var2, int var3) {
      Direction[] var4 = Direction.values();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Direction var7 = var4[var6];
         BlockPos var8 = var2.relative(var7);
         BlockState var9 = var1.getBlockState(var8);
         if (var9.is(BlockTags.VIBRATION_RESONATORS)) {
            var1.gameEvent(VibrationSystem.getResonanceEventByFrequency(var3), var8, GameEvent.Context.of(var0, var9));
            float var10 = RESONANCE_PITCH_BEND[var3];
            var1.playSound((Player)null, (BlockPos)var8, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, var10);
         }
      }

   }

   public void animateTick(BlockState var1, Level var2, BlockPos var3, RandomSource var4) {
      if (getPhase(var1) == SculkSensorPhase.ACTIVE) {
         Direction var5 = Direction.getRandom(var4);
         if (var5 != Direction.UP && var5 != Direction.DOWN) {
            double var6 = (double)var3.getX() + 0.5D + (var5.getStepX() == 0 ? 0.5D - var4.nextDouble() : (double)var5.getStepX() * 0.6D);
            double var8 = (double)var3.getY() + 0.25D;
            double var10 = (double)var3.getZ() + 0.5D + (var5.getStepZ() == 0 ? 0.5D - var4.nextDouble() : (double)var5.getStepZ() * 0.6D);
            double var12 = (double)var4.nextFloat() * 0.04D;
            var2.addParticle(DustColorTransitionOptions.SCULK_TO_REDSTONE, var6, var8, var10, 0.0D, var12, 0.0D);
         }
      }
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> var1) {
      var1.add(PHASE, POWER, WATERLOGGED);
   }

   public boolean hasAnalogOutputSignal(BlockState var1) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState var1, Level var2, BlockPos var3) {
      BlockEntity var4 = var2.getBlockEntity(var3);
      if (var4 instanceof SculkSensorBlockEntity) {
         SculkSensorBlockEntity var5 = (SculkSensorBlockEntity)var4;
         return getPhase(var1) == SculkSensorPhase.ACTIVE ? var5.getLastVibrationFrequency() : 0;
      } else {
         return 0;
      }
   }

   public boolean isPathfindable(BlockState var1, BlockGetter var2, BlockPos var3, PathComputationType var4) {
      return false;
   }

   public boolean useShapeForLightOcclusion(BlockState var1) {
      return true;
   }

   public void spawnAfterBreak(BlockState var1, ServerLevel var2, BlockPos var3, ItemStack var4, boolean var5) {
      super.spawnAfterBreak(var1, var2, var3, var4, var5);
      if (var5) {
         this.tryDropExperience(var2, var3, var4, ConstantInt.of(5));
      }

   }

   static {
      PHASE = BlockStateProperties.SCULK_SENSOR_PHASE;
      POWER = BlockStateProperties.POWER;
      WATERLOGGED = BlockStateProperties.WATERLOGGED;
      SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
      RESONANCE_PITCH_BEND = (float[])Util.make(new float[16], (var0) -> {
         int[] var1 = new int[]{0, 0, 2, 4, 6, 7, 9, 10, 12, 14, 15, 18, 19, 21, 22, 24};

         for(int var2 = 0; var2 < 16; ++var2) {
            var0[var2] = NoteBlock.getPitchFromNote(var1[var2]);
         }

      });
   }
}
