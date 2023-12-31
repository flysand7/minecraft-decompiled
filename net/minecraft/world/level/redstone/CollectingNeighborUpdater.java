package net.minecraft.world.level.redstone;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class CollectingNeighborUpdater implements NeighborUpdater {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Level level;
   private final int maxChainedNeighborUpdates;
   private final ArrayDeque<CollectingNeighborUpdater.NeighborUpdates> stack = new ArrayDeque();
   private final List<CollectingNeighborUpdater.NeighborUpdates> addedThisLayer = new ArrayList();
   private int count = 0;

   public CollectingNeighborUpdater(Level var1, int var2) {
      this.level = var1;
      this.maxChainedNeighborUpdates = var2;
   }

   public void shapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, int var5, int var6) {
      this.addAndRun(var3, new CollectingNeighborUpdater.ShapeUpdate(var1, var2, var3.immutable(), var4.immutable(), var5, var6));
   }

   public void neighborChanged(BlockPos var1, Block var2, BlockPos var3) {
      this.addAndRun(var1, new CollectingNeighborUpdater.SimpleNeighborUpdate(var1, var2, var3.immutable()));
   }

   public void neighborChanged(BlockState var1, BlockPos var2, Block var3, BlockPos var4, boolean var5) {
      this.addAndRun(var2, new CollectingNeighborUpdater.FullNeighborUpdate(var1, var2.immutable(), var3, var4.immutable(), var5));
   }

   public void updateNeighborsAtExceptFromFacing(BlockPos var1, Block var2, @Nullable Direction var3) {
      this.addAndRun(var1, new CollectingNeighborUpdater.MultiNeighborUpdate(var1.immutable(), var2, var3));
   }

   private void addAndRun(BlockPos var1, CollectingNeighborUpdater.NeighborUpdates var2) {
      boolean var3 = this.count > 0;
      boolean var4 = this.maxChainedNeighborUpdates >= 0 && this.count >= this.maxChainedNeighborUpdates;
      ++this.count;
      if (!var4) {
         if (var3) {
            this.addedThisLayer.add(var2);
         } else {
            this.stack.push(var2);
         }
      } else if (this.count - 1 == this.maxChainedNeighborUpdates) {
         LOGGER.error("Too many chained neighbor updates. Skipping the rest. First skipped position: " + var1.toShortString());
      }

      if (!var3) {
         this.runUpdates();
      }

   }

   private void runUpdates() {
      try {
         while(!this.stack.isEmpty() || !this.addedThisLayer.isEmpty()) {
            for(int var1 = this.addedThisLayer.size() - 1; var1 >= 0; --var1) {
               this.stack.push((CollectingNeighborUpdater.NeighborUpdates)this.addedThisLayer.get(var1));
            }

            this.addedThisLayer.clear();
            CollectingNeighborUpdater.NeighborUpdates var5 = (CollectingNeighborUpdater.NeighborUpdates)this.stack.peek();

            while(this.addedThisLayer.isEmpty()) {
               if (!var5.runNext(this.level)) {
                  this.stack.pop();
                  break;
               }
            }
         }
      } finally {
         this.stack.clear();
         this.addedThisLayer.clear();
         this.count = 0;
      }

   }

   private static record ShapeUpdate(Direction a, BlockState b, BlockPos c, BlockPos d, int e, int f) implements CollectingNeighborUpdater.NeighborUpdates {
      private final Direction direction;
      private final BlockState state;
      private final BlockPos pos;
      private final BlockPos neighborPos;
      private final int updateFlags;
      private final int updateLimit;

      ShapeUpdate(Direction var1, BlockState var2, BlockPos var3, BlockPos var4, int var5, int var6) {
         this.direction = var1;
         this.state = var2;
         this.pos = var3;
         this.neighborPos = var4;
         this.updateFlags = var5;
         this.updateLimit = var6;
      }

      public boolean runNext(Level var1) {
         NeighborUpdater.executeShapeUpdate(var1, this.direction, this.state, this.pos, this.neighborPos, this.updateFlags, this.updateLimit);
         return false;
      }

      public Direction direction() {
         return this.direction;
      }

      public BlockState state() {
         return this.state;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public BlockPos neighborPos() {
         return this.neighborPos;
      }

      public int updateFlags() {
         return this.updateFlags;
      }

      public int updateLimit() {
         return this.updateLimit;
      }
   }

   private interface NeighborUpdates {
      boolean runNext(Level var1);
   }

   static record SimpleNeighborUpdate(BlockPos a, Block b, BlockPos c) implements CollectingNeighborUpdater.NeighborUpdates {
      private final BlockPos pos;
      private final Block block;
      private final BlockPos neighborPos;

      SimpleNeighborUpdate(BlockPos var1, Block var2, BlockPos var3) {
         this.pos = var1;
         this.block = var2;
         this.neighborPos = var3;
      }

      public boolean runNext(Level var1) {
         BlockState var2 = var1.getBlockState(this.pos);
         NeighborUpdater.executeUpdate(var1, var2, this.pos, this.block, this.neighborPos, false);
         return false;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public Block block() {
         return this.block;
      }

      public BlockPos neighborPos() {
         return this.neighborPos;
      }
   }

   static record FullNeighborUpdate(BlockState a, BlockPos b, Block c, BlockPos d, boolean e) implements CollectingNeighborUpdater.NeighborUpdates {
      private final BlockState state;
      private final BlockPos pos;
      private final Block block;
      private final BlockPos neighborPos;
      private final boolean movedByPiston;

      FullNeighborUpdate(BlockState var1, BlockPos var2, Block var3, BlockPos var4, boolean var5) {
         this.state = var1;
         this.pos = var2;
         this.block = var3;
         this.neighborPos = var4;
         this.movedByPiston = var5;
      }

      public boolean runNext(Level var1) {
         NeighborUpdater.executeUpdate(var1, this.state, this.pos, this.block, this.neighborPos, this.movedByPiston);
         return false;
      }

      public BlockState state() {
         return this.state;
      }

      public BlockPos pos() {
         return this.pos;
      }

      public Block block() {
         return this.block;
      }

      public BlockPos neighborPos() {
         return this.neighborPos;
      }

      public boolean movedByPiston() {
         return this.movedByPiston;
      }
   }

   static final class MultiNeighborUpdate implements CollectingNeighborUpdater.NeighborUpdates {
      private final BlockPos sourcePos;
      private final Block sourceBlock;
      @Nullable
      private final Direction skipDirection;
      private int idx = 0;

      MultiNeighborUpdate(BlockPos var1, Block var2, @Nullable Direction var3) {
         this.sourcePos = var1;
         this.sourceBlock = var2;
         this.skipDirection = var3;
         if (NeighborUpdater.UPDATE_ORDER[this.idx] == var3) {
            ++this.idx;
         }

      }

      public boolean runNext(Level var1) {
         BlockPos var2 = this.sourcePos.relative(NeighborUpdater.UPDATE_ORDER[this.idx++]);
         BlockState var3 = var1.getBlockState(var2);
         var3.neighborChanged(var1, var2, this.sourceBlock, this.sourcePos, false);
         if (this.idx < NeighborUpdater.UPDATE_ORDER.length && NeighborUpdater.UPDATE_ORDER[this.idx] == this.skipDirection) {
            ++this.idx;
         }

         return this.idx < NeighborUpdater.UPDATE_ORDER.length;
      }
   }
}
