package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;

public class ZombieModel<T extends Zombie> extends AbstractZombieModel<T> {
   public ZombieModel(ModelPart var1) {
      super(var1);
   }

   public boolean isAggressive(T var1) {
      return var1.isAggressive();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public boolean isAggressive(Monster var1) {
      return this.isAggressive((Zombie)var1);
   }
}
