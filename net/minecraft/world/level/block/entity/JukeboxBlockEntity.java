package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ContainerSingleItem;

public class JukeboxBlockEntity extends BlockEntity implements Clearable, ContainerSingleItem {
   private static final int SONG_END_PADDING = 20;
   private final NonNullList<ItemStack> items;
   private int ticksSinceLastEvent;
   private long tickCount;
   private long recordStartedTick;
   private boolean isPlaying;

   public JukeboxBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.JUKEBOX, var1, var2);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
   }

   public void load(CompoundTag var1) {
      super.load(var1);
      if (var1.contains("RecordItem", 10)) {
         this.items.set(0, ItemStack.of(var1.getCompound("RecordItem")));
      }

      this.isPlaying = var1.getBoolean("IsPlaying");
      this.recordStartedTick = var1.getLong("RecordStartTick");
      this.tickCount = var1.getLong("TickCount");
   }

   protected void saveAdditional(CompoundTag var1) {
      super.saveAdditional(var1);
      if (!this.getFirstItem().isEmpty()) {
         var1.put("RecordItem", this.getFirstItem().save(new CompoundTag()));
      }

      var1.putBoolean("IsPlaying", this.isPlaying);
      var1.putLong("RecordStartTick", this.recordStartedTick);
      var1.putLong("TickCount", this.tickCount);
   }

   public boolean isRecordPlaying() {
      return !this.getFirstItem().isEmpty() && this.isPlaying;
   }

   private void setHasRecordBlockState(@Nullable Entity var1, boolean var2) {
      if (this.level.getBlockState(this.getBlockPos()) == this.getBlockState()) {
         this.level.setBlock(this.getBlockPos(), (BlockState)this.getBlockState().setValue(JukeboxBlock.HAS_RECORD, var2), 2);
         this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(var1, this.getBlockState()));
      }

   }

   @VisibleForTesting
   public void startPlaying() {
      this.recordStartedTick = this.tickCount;
      this.isPlaying = true;
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.level.levelEvent((Player)null, 1010, this.getBlockPos(), Item.getId(this.getFirstItem().getItem()));
      this.setChanged();
   }

   private void stopPlaying() {
      this.isPlaying = false;
      this.level.gameEvent(GameEvent.JUKEBOX_STOP_PLAY, this.getBlockPos(), GameEvent.Context.of(this.getBlockState()));
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.level.levelEvent(1011, this.getBlockPos(), 0);
      this.setChanged();
   }

   private void tick(Level var1, BlockPos var2, BlockState var3) {
      ++this.ticksSinceLastEvent;
      if (this.isRecordPlaying()) {
         Item var5 = this.getFirstItem().getItem();
         if (var5 instanceof RecordItem) {
            RecordItem var4 = (RecordItem)var5;
            if (this.shouldRecordStopPlaying(var4)) {
               this.stopPlaying();
            } else if (this.shouldSendJukeboxPlayingEvent()) {
               this.ticksSinceLastEvent = 0;
               var1.gameEvent(GameEvent.JUKEBOX_PLAY, var2, GameEvent.Context.of(var3));
               this.spawnMusicParticles(var1, var2);
            }
         }
      }

      ++this.tickCount;
   }

   private boolean shouldRecordStopPlaying(RecordItem var1) {
      return this.tickCount >= this.recordStartedTick + (long)var1.getLengthInTicks() + 20L;
   }

   private boolean shouldSendJukeboxPlayingEvent() {
      return this.ticksSinceLastEvent >= 20;
   }

   public ItemStack getItem(int var1) {
      return (ItemStack)this.items.get(var1);
   }

   public ItemStack removeItem(int var1, int var2) {
      ItemStack var3 = (ItemStack)Objects.requireNonNullElse((ItemStack)this.items.get(var1), ItemStack.EMPTY);
      this.items.set(var1, ItemStack.EMPTY);
      if (!var3.isEmpty()) {
         this.setHasRecordBlockState((Entity)null, false);
         this.stopPlaying();
      }

      return var3;
   }

   public void setItem(int var1, ItemStack var2) {
      if (var2.is(ItemTags.MUSIC_DISCS) && this.level != null) {
         this.items.set(var1, var2);
         this.setHasRecordBlockState((Entity)null, true);
         this.startPlaying();
      }

   }

   public int getMaxStackSize() {
      return 1;
   }

   public boolean stillValid(Player var1) {
      return Container.stillValidBlockEntity(this, var1);
   }

   public boolean canPlaceItem(int var1, ItemStack var2) {
      return var2.is(ItemTags.MUSIC_DISCS) && this.getItem(var1).isEmpty();
   }

   public boolean canTakeItem(Container var1, int var2, ItemStack var3) {
      return var1.hasAnyMatching(ItemStack::isEmpty);
   }

   private void spawnMusicParticles(Level var1, BlockPos var2) {
      if (var1 instanceof ServerLevel) {
         ServerLevel var3 = (ServerLevel)var1;
         Vec3 var4 = Vec3.atBottomCenterOf(var2).add(0.0D, 1.2000000476837158D, 0.0D);
         float var5 = (float)var1.getRandom().nextInt(4) / 24.0F;
         var3.sendParticles(ParticleTypes.NOTE, var4.x(), var4.y(), var4.z(), 0, (double)var5, 0.0D, 0.0D, 1.0D);
      }

   }

   public void popOutRecord() {
      if (this.level != null && !this.level.isClientSide) {
         BlockPos var1 = this.getBlockPos();
         ItemStack var2 = this.getFirstItem();
         if (!var2.isEmpty()) {
            this.removeFirstItem();
            Vec3 var3 = Vec3.atLowerCornerWithOffset(var1, 0.5D, 1.01D, 0.5D).offsetRandom(this.level.random, 0.7F);
            ItemStack var4 = var2.copy();
            ItemEntity var5 = new ItemEntity(this.level, var3.x(), var3.y(), var3.z(), var4);
            var5.setDefaultPickUpDelay();
            this.level.addFreshEntity(var5);
         }
      }
   }

   public static void playRecordTick(Level var0, BlockPos var1, BlockState var2, JukeboxBlockEntity var3) {
      var3.tick(var0, var1, var2);
   }

   @VisibleForTesting
   public void setRecordWithoutPlaying(ItemStack var1) {
      this.items.set(0, var1);
      this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.setChanged();
   }
}
