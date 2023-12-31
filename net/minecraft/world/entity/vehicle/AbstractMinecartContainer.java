package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class AbstractMinecartContainer extends AbstractMinecart implements ContainerEntity {
   private NonNullList<ItemStack> itemStacks;
   @Nullable
   private ResourceLocation lootTable;
   private long lootTableSeed;

   protected AbstractMinecartContainer(EntityType<?> var1, Level var2) {
      super(var1, var2);
      this.itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
   }

   protected AbstractMinecartContainer(EntityType<?> var1, double var2, double var4, double var6, Level var8) {
      super(var1, var8, var2, var4, var6);
      this.itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
   }

   public void destroy(DamageSource var1) {
      super.destroy(var1);
      this.chestVehicleDestroyed(var1, this.level(), this);
   }

   public ItemStack getItem(int var1) {
      return this.getChestVehicleItem(var1);
   }

   public ItemStack removeItem(int var1, int var2) {
      return this.removeChestVehicleItem(var1, var2);
   }

   public ItemStack removeItemNoUpdate(int var1) {
      return this.removeChestVehicleItemNoUpdate(var1);
   }

   public void setItem(int var1, ItemStack var2) {
      this.setChestVehicleItem(var1, var2);
   }

   public SlotAccess getSlot(int var1) {
      return this.getChestVehicleSlot(var1);
   }

   public void setChanged() {
   }

   public boolean stillValid(Player var1) {
      return this.isChestVehicleStillValid(var1);
   }

   public void remove(Entity.RemovalReason var1) {
      if (!this.level().isClientSide && var1.shouldDestroy()) {
         Containers.dropContents(this.level(), (Entity)this, (Container)this);
      }

      super.remove(var1);
   }

   protected void addAdditionalSaveData(CompoundTag var1) {
      super.addAdditionalSaveData(var1);
      this.addChestVehicleSaveData(var1);
   }

   protected void readAdditionalSaveData(CompoundTag var1) {
      super.readAdditionalSaveData(var1);
      this.readChestVehicleSaveData(var1);
   }

   public InteractionResult interact(Player var1, InteractionHand var2) {
      return this.interactWithContainerVehicle(var1);
   }

   protected void applyNaturalSlowdown() {
      float var1 = 0.98F;
      if (this.lootTable == null) {
         int var2 = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);
         var1 += (float)var2 * 0.001F;
      }

      if (this.isInWater()) {
         var1 *= 0.95F;
      }

      this.setDeltaMovement(this.getDeltaMovement().multiply((double)var1, 0.0D, (double)var1));
   }

   public void clearContent() {
      this.clearChestVehicleContent();
   }

   public void setLootTable(ResourceLocation var1, long var2) {
      this.lootTable = var1;
      this.lootTableSeed = var2;
   }

   @Nullable
   public AbstractContainerMenu createMenu(int var1, Inventory var2, Player var3) {
      if (this.lootTable != null && var3.isSpectator()) {
         return null;
      } else {
         this.unpackChestVehicleLootTable(var2.player);
         return this.createMenu(var1, var2);
      }
   }

   protected abstract AbstractContainerMenu createMenu(int var1, Inventory var2);

   @Nullable
   public ResourceLocation getLootTable() {
      return this.lootTable;
   }

   public void setLootTable(@Nullable ResourceLocation var1) {
      this.lootTable = var1;
   }

   public long getLootTableSeed() {
      return this.lootTableSeed;
   }

   public void setLootTableSeed(long var1) {
      this.lootTableSeed = var1;
   }

   public NonNullList<ItemStack> getItemStacks() {
      return this.itemStacks;
   }

   public void clearItemStacks() {
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
   }
}
