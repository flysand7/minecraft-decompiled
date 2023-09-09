package net.minecraft.world.entity.npc;

import net.minecraft.world.entity.VariantHolder;

public interface VillagerDataHolder extends VariantHolder<VillagerType> {
   VillagerData getVillagerData();

   void setVillagerData(VillagerData var1);

   default VillagerType getVariant() {
      return this.getVillagerData().getType();
   }

   default void setVariant(VillagerType var1) {
      this.setVillagerData(this.getVillagerData().setType(var1));
   }

   // $FF: synthetic method
   default Object getVariant() {
      return this.getVariant();
   }

   // $FF: synthetic method
   // $FF: bridge method
   default void setVariant(Object var1) {
      this.setVariant((VillagerType)var1);
   }
}
