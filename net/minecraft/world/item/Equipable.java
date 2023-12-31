package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface Equipable extends Vanishable {
   EquipmentSlot getEquipmentSlot();

   default SoundEvent getEquipSound() {
      return SoundEvents.ARMOR_EQUIP_GENERIC;
   }

   default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item var1, Level var2, Player var3, InteractionHand var4) {
      ItemStack var5 = var3.getItemInHand(var4);
      EquipmentSlot var6 = Mob.getEquipmentSlotForItem(var5);
      ItemStack var7 = var3.getItemBySlot(var6);
      if (!EnchantmentHelper.hasBindingCurse(var7) && !ItemStack.matches(var5, var7)) {
         if (!var2.isClientSide()) {
            var3.awardStat(Stats.ITEM_USED.get(var1));
         }

         ItemStack var8 = var7.isEmpty() ? var5 : var7.copyAndClear();
         ItemStack var9 = var5.copyAndClear();
         var3.setItemSlot(var6, var9);
         return InteractionResultHolder.sidedSuccess(var8, var2.isClientSide());
      } else {
         return InteractionResultHolder.fail(var5);
      }
   }

   @Nullable
   static Equipable get(ItemStack var0) {
      Item var2 = var0.getItem();
      if (var2 instanceof Equipable) {
         Equipable var4 = (Equipable)var2;
         return var4;
      } else {
         Item var3 = var0.getItem();
         if (var3 instanceof BlockItem) {
            BlockItem var1 = (BlockItem)var3;
            Block var6 = var1.getBlock();
            if (var6 instanceof Equipable) {
               Equipable var5 = (Equipable)var6;
               return var5;
            }
         }

         return null;
      }
   }
}
