package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;

public class RavagerRenderer extends MobRenderer<Ravager, RavagerModel> {
   private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/illager/ravager.png");

   public RavagerRenderer(EntityRendererProvider.Context var1) {
      super(var1, new RavagerModel(var1.bakeLayer(ModelLayers.RAVAGER)), 1.1F);
   }

   public ResourceLocation getTextureLocation(Ravager var1) {
      return TEXTURE_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Ravager)var1);
   }
}
