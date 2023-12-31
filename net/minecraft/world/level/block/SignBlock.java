package net.minecraft.world.level.block;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED;
   protected static final float AABB_OFFSET = 4.0F;
   protected static final VoxelShape SHAPE;
   private final WoodType type;

   protected SignBlock(BlockBehaviour.Properties var1, WoodType var2) {
      super(var1);
      this.type = var2;
   }

   public BlockState updateShape(BlockState var1, Direction var2, BlockState var3, LevelAccessor var4, BlockPos var5, BlockPos var6) {
      if ((Boolean)var1.getValue(WATERLOGGED)) {
         var4.scheduleTick(var5, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay(var4));
      }

      return super.updateShape(var1, var2, var3, var4, var5, var6);
   }

   public VoxelShape getShape(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4) {
      return SHAPE;
   }

   public boolean isPossibleToRespawnInThis(BlockState var1) {
      return true;
   }

   public BlockEntity newBlockEntity(BlockPos var1, BlockState var2) {
      return new SignBlockEntity(var1, var2);
   }

   public InteractionResult use(BlockState var1, Level var2, BlockPos var3, Player var4, InteractionHand var5, BlockHitResult var6) {
      ItemStack var7 = var4.getItemInHand(var5);
      Item var8 = var7.getItem();
      Item var11 = var7.getItem();
      SignApplicator var10000;
      if (var11 instanceof SignApplicator) {
         SignApplicator var10 = (SignApplicator)var11;
         var10000 = var10;
      } else {
         var10000 = null;
      }

      SignApplicator var9 = var10000;
      boolean var15 = var9 != null && var4.mayBuild();
      BlockEntity var12 = var2.getBlockEntity(var3);
      if (var12 instanceof SignBlockEntity) {
         SignBlockEntity var16 = (SignBlockEntity)var12;
         if (!var2.isClientSide) {
            boolean var17 = var16.isFacingFrontText(var4);
            SignText var13 = var16.getText(var17);
            boolean var14 = var16.executeClickCommandsIfPresent(var4, var2, var3, var17);
            if (var16.isWaxed()) {
               var2.playSound((Player)null, var16.getBlockPos(), SoundEvents.WAXED_SIGN_INTERACT_FAIL, SoundSource.BLOCKS);
               return InteractionResult.PASS;
            } else if (var15 && !this.otherPlayerIsEditingSign(var4, var16) && var9.canApplyToSign(var13, var4) && var9.tryApplyToSign(var2, var16, var17, var4)) {
               if (!var4.isCreative()) {
                  var7.shrink(1);
               }

               var2.gameEvent(GameEvent.BLOCK_CHANGE, var16.getBlockPos(), GameEvent.Context.of(var4, var16.getBlockState()));
               var4.awardStat(Stats.ITEM_USED.get(var8));
               return InteractionResult.SUCCESS;
            } else if (var14) {
               return InteractionResult.SUCCESS;
            } else if (!this.otherPlayerIsEditingSign(var4, var16) && var4.mayBuild() && this.hasEditableText(var4, var16, var17)) {
               this.openTextEdit(var4, var16, var17);
               return InteractionResult.SUCCESS;
            } else {
               return InteractionResult.PASS;
            }
         } else {
            return !var15 && !var16.isWaxed() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private boolean hasEditableText(Player var1, SignBlockEntity var2, boolean var3) {
      SignText var4 = var2.getText(var3);
      return Arrays.stream(var4.getMessages(var1.isTextFilteringEnabled())).allMatch((var0) -> {
         return var0.equals(CommonComponents.EMPTY) || var0.getContents() instanceof LiteralContents;
      });
   }

   public abstract float getYRotationDegrees(BlockState var1);

   public Vec3 getSignHitboxCenterPosition(BlockState var1) {
      return new Vec3(0.5D, 0.5D, 0.5D);
   }

   public FluidState getFluidState(BlockState var1) {
      return (Boolean)var1.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(var1);
   }

   public WoodType type() {
      return this.type;
   }

   public static WoodType getWoodType(Block var0) {
      WoodType var1;
      if (var0 instanceof SignBlock) {
         var1 = ((SignBlock)var0).type();
      } else {
         var1 = WoodType.OAK;
      }

      return var1;
   }

   public void openTextEdit(Player var1, SignBlockEntity var2, boolean var3) {
      var2.setAllowedPlayerEditor(var1.getUUID());
      var1.openTextEdit(var2, var3);
   }

   private boolean otherPlayerIsEditingSign(Player var1, SignBlockEntity var2) {
      UUID var3 = var2.getPlayerWhoMayEdit();
      return var3 != null && !var3.equals(var1.getUUID());
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level var1, BlockState var2, BlockEntityType<T> var3) {
      return createTickerHelper(var3, BlockEntityType.SIGN, SignBlockEntity::tick);
   }

   static {
      WATERLOGGED = BlockStateProperties.WATERLOGGED;
      SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
   }
}
