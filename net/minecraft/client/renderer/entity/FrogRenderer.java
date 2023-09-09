package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.FrogModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.frog.Frog;

public class FrogRenderer extends MobRenderer<Frog, FrogModel<Frog>> {
   public FrogRenderer(EntityRendererProvider.Context var1) {
      super(var1, new FrogModel(var1.bakeLayer(ModelLayers.FROG)), 0.3F);
   }

   public ResourceLocation getTextureLocation(Frog var1) {
      return var1.getVariant().texture();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Frog)var1);
   }
}
