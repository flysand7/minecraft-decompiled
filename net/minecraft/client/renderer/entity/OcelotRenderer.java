package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Ocelot;

public class OcelotRenderer extends MobRenderer<Ocelot, OcelotModel<Ocelot>> {
   private static final ResourceLocation CAT_OCELOT_LOCATION = new ResourceLocation("textures/entity/cat/ocelot.png");

   public OcelotRenderer(EntityRendererProvider.Context var1) {
      super(var1, new OcelotModel(var1.bakeLayer(ModelLayers.OCELOT)), 0.4F);
   }

   public ResourceLocation getTextureLocation(Ocelot var1) {
      return CAT_OCELOT_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Ocelot)var1);
   }
}
