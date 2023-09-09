package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.level.block.Block;

public enum EnchantmentCategory {
   ARMOR {
      public boolean canEnchant(Item var1) {
         return var1 instanceof ArmorItem;
      }
   },
   ARMOR_FEET {
      public boolean canEnchant(Item var1) {
         boolean var10000;
         if (var1 instanceof ArmorItem) {
            ArmorItem var2 = (ArmorItem)var1;
            if (var2.getEquipmentSlot() == EquipmentSlot.FEET) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   },
   ARMOR_LEGS {
      public boolean canEnchant(Item var1) {
         boolean var10000;
         if (var1 instanceof ArmorItem) {
            ArmorItem var2 = (ArmorItem)var1;
            if (var2.getEquipmentSlot() == EquipmentSlot.LEGS) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   },
   ARMOR_CHEST {
      public boolean canEnchant(Item var1) {
         boolean var10000;
         if (var1 instanceof ArmorItem) {
            ArmorItem var2 = (ArmorItem)var1;
            if (var2.getEquipmentSlot() == EquipmentSlot.CHEST) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   },
   ARMOR_HEAD {
      public boolean canEnchant(Item var1) {
         boolean var10000;
         if (var1 instanceof ArmorItem) {
            ArmorItem var2 = (ArmorItem)var1;
            if (var2.getEquipmentSlot() == EquipmentSlot.HEAD) {
               var10000 = true;
               return var10000;
            }
         }

         var10000 = false;
         return var10000;
      }
   },
   WEAPON {
      public boolean canEnchant(Item var1) {
         return var1 instanceof SwordItem;
      }
   },
   DIGGER {
      public boolean canEnchant(Item var1) {
         return var1 instanceof DiggerItem;
      }
   },
   FISHING_ROD {
      public boolean canEnchant(Item var1) {
         return var1 instanceof FishingRodItem;
      }
   },
   TRIDENT {
      public boolean canEnchant(Item var1) {
         return var1 instanceof TridentItem;
      }
   },
   BREAKABLE {
      public boolean canEnchant(Item var1) {
         return var1.canBeDepleted();
      }
   },
   BOW {
      public boolean canEnchant(Item var1) {
         return var1 instanceof BowItem;
      }
   },
   WEARABLE {
      public boolean canEnchant(Item var1) {
         return var1 instanceof Equipable || Block.byItem(var1) instanceof Equipable;
      }
   },
   CROSSBOW {
      public boolean canEnchant(Item var1) {
         return var1 instanceof CrossbowItem;
      }
   },
   VANISHABLE {
      public boolean canEnchant(Item var1) {
         return var1 instanceof Vanishable || Block.byItem(var1) instanceof Vanishable || BREAKABLE.canEnchant(var1);
      }
   };

   EnchantmentCategory() {
   }

   public abstract boolean canEnchant(Item var1);

   // $FF: synthetic method
   private static EnchantmentCategory[] $values() {
      return new EnchantmentCategory[]{ARMOR, ARMOR_FEET, ARMOR_LEGS, ARMOR_CHEST, ARMOR_HEAD, WEAPON, DIGGER, FISHING_ROD, TRIDENT, BREAKABLE, BOW, WEARABLE, CROSSBOW, VANISHABLE};
   }
}
