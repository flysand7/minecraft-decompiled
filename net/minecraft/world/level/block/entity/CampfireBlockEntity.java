package net.minecraft.world.level.block.entity;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
   private static final int BURN_COOL_SPEED = 2;
   private static final int NUM_SLOTS = 4;
   private final NonNullList<ItemStack> items;
   private final int[] cookingProgress;
   private final int[] cookingTime;
   private final RecipeManager.CachedCheck<Container, CampfireCookingRecipe> quickCheck;

   public CampfireBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.CAMPFIRE, var1, var2);
      this.items = NonNullList.withSize(4, ItemStack.EMPTY);
      this.cookingProgress = new int[4];
      this.cookingTime = new int[4];
      this.quickCheck = RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
   }

   public static void cookTick(Level var0, BlockPos var1, BlockState var2, CampfireBlockEntity var3) {
      boolean var4 = false;

      for(int var5 = 0; var5 < var3.items.size(); ++var5) {
         ItemStack var6 = (ItemStack)var3.items.get(var5);
         if (!var6.isEmpty()) {
            var4 = true;
            int var10002 = var3.cookingProgress[var5]++;
            if (var3.cookingProgress[var5] >= var3.cookingTime[var5]) {
               SimpleContainer var7 = new SimpleContainer(new ItemStack[]{var6});
               ItemStack var8 = (ItemStack)var3.quickCheck.getRecipeFor(var7, var0).map((var2x) -> {
                  return var2x.assemble(var7, var0.registryAccess());
               }).orElse(var6);
               if (var8.isItemEnabled(var0.enabledFeatures())) {
                  Containers.dropItemStack(var0, (double)var1.getX(), (double)var1.getY(), (double)var1.getZ(), var8);
                  var3.items.set(var5, ItemStack.EMPTY);
                  var0.sendBlockUpdated(var1, var2, var2, 3);
                  var0.gameEvent(GameEvent.BLOCK_CHANGE, var1, GameEvent.Context.of(var2));
               }
            }
         }
      }

      if (var4) {
         setChanged(var0, var1, var2);
      }

   }

   public static void cooldownTick(Level var0, BlockPos var1, BlockState var2, CampfireBlockEntity var3) {
      boolean var4 = false;

      for(int var5 = 0; var5 < var3.items.size(); ++var5) {
         if (var3.cookingProgress[var5] > 0) {
            var4 = true;
            var3.cookingProgress[var5] = Mth.clamp(var3.cookingProgress[var5] - 2, 0, var3.cookingTime[var5]);
         }
      }

      if (var4) {
         setChanged(var0, var1, var2);
      }

   }

   public static void particleTick(Level var0, BlockPos var1, BlockState var2, CampfireBlockEntity var3) {
      RandomSource var4 = var0.random;
      int var5;
      if (var4.nextFloat() < 0.11F) {
         for(var5 = 0; var5 < var4.nextInt(2) + 2; ++var5) {
            CampfireBlock.makeParticles(var0, var1, (Boolean)var2.getValue(CampfireBlock.SIGNAL_FIRE), false);
         }
      }

      var5 = ((Direction)var2.getValue(CampfireBlock.FACING)).get2DDataValue();

      for(int var6 = 0; var6 < var3.items.size(); ++var6) {
         if (!((ItemStack)var3.items.get(var6)).isEmpty() && var4.nextFloat() < 0.2F) {
            Direction var7 = Direction.from2DDataValue(Math.floorMod(var6 + var5, 4));
            float var8 = 0.3125F;
            double var9 = (double)var1.getX() + 0.5D - (double)((float)var7.getStepX() * 0.3125F) + (double)((float)var7.getClockWise().getStepX() * 0.3125F);
            double var11 = (double)var1.getY() + 0.5D;
            double var13 = (double)var1.getZ() + 0.5D - (double)((float)var7.getStepZ() * 0.3125F) + (double)((float)var7.getClockWise().getStepZ() * 0.3125F);

            for(int var15 = 0; var15 < 4; ++var15) {
               var0.addParticle(ParticleTypes.SMOKE, var9, var11, var13, 0.0D, 5.0E-4D, 0.0D);
            }
         }
      }

   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   public void load(CompoundTag var1) {
      super.load(var1);
      this.items.clear();
      ContainerHelper.loadAllItems(var1, this.items);
      int[] var2;
      if (var1.contains("CookingTimes", 11)) {
         var2 = var1.getIntArray("CookingTimes");
         System.arraycopy(var2, 0, this.cookingProgress, 0, Math.min(this.cookingTime.length, var2.length));
      }

      if (var1.contains("CookingTotalTimes", 11)) {
         var2 = var1.getIntArray("CookingTotalTimes");
         System.arraycopy(var2, 0, this.cookingTime, 0, Math.min(this.cookingTime.length, var2.length));
      }

   }

   protected void saveAdditional(CompoundTag var1) {
      super.saveAdditional(var1);
      ContainerHelper.saveAllItems(var1, this.items, true);
      var1.putIntArray("CookingTimes", this.cookingProgress);
      var1.putIntArray("CookingTotalTimes", this.cookingTime);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag var1 = new CompoundTag();
      ContainerHelper.saveAllItems(var1, this.items, true);
      return var1;
   }

   public Optional<CampfireCookingRecipe> getCookableRecipe(ItemStack var1) {
      return this.items.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.quickCheck.getRecipeFor(new SimpleContainer(new ItemStack[]{var1}), this.level);
   }

   public boolean placeFood(@Nullable Entity var1, ItemStack var2, int var3) {
      for(int var4 = 0; var4 < this.items.size(); ++var4) {
         ItemStack var5 = (ItemStack)this.items.get(var4);
         if (var5.isEmpty()) {
            this.cookingTime[var4] = var3;
            this.cookingProgress[var4] = 0;
            this.items.set(var4, var2.split(1));
            this.level.gameEvent(GameEvent.BLOCK_CHANGE, this.getBlockPos(), GameEvent.Context.of(var1, this.getBlockState()));
            this.markUpdated();
            return true;
         }
      }

      return false;
   }

   private void markUpdated() {
      this.setChanged();
      this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
   }

   public void clearContent() {
      this.items.clear();
   }

   public void dowse() {
      if (this.level != null) {
         this.markUpdated();
      }

   }

   // $FF: synthetic method
   public Packet getUpdatePacket() {
      return this.getUpdatePacket();
   }
}
