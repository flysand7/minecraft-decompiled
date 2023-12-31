package net.minecraft.world.level.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public abstract class AbstractSkullBlock extends BaseEntityBlock implements Equipable {
   private final SkullBlock.Type type;

   public AbstractSkullBlock(SkullBlock.Type var1, BlockBehaviour.Properties var2) {
      super(var2);
      this.type = var1;
   }

   public BlockEntity newBlockEntity(BlockPos var1, BlockState var2) {
      return new SkullBlockEntity(var1, var2);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level var1, BlockState var2, BlockEntityType<T> var3) {
      if (var1.isClientSide) {
         boolean var4 = var2.is(Blocks.DRAGON_HEAD) || var2.is(Blocks.DRAGON_WALL_HEAD) || var2.is(Blocks.PIGLIN_HEAD) || var2.is(Blocks.PIGLIN_WALL_HEAD);
         if (var4) {
            return createTickerHelper(var3, BlockEntityType.SKULL, SkullBlockEntity::animation);
         }
      }

      return null;
   }

   public SkullBlock.Type getType() {
      return this.type;
   }

   public boolean isPathfindable(BlockState var1, BlockGetter var2, BlockPos var3, PathComputationType var4) {
      return false;
   }

   public EquipmentSlot getEquipmentSlot() {
      return EquipmentSlot.HEAD;
   }
}
