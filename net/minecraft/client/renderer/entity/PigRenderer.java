package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.SaddleLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Pig;

public class PigRenderer extends MobRenderer<Pig, PigModel<Pig>> {
   private static final ResourceLocation PIG_LOCATION = new ResourceLocation("textures/entity/pig/pig.png");

   public PigRenderer(EntityRendererProvider.Context var1) {
      super(var1, new PigModel(var1.bakeLayer(ModelLayers.PIG)), 0.7F);
      this.addLayer(new SaddleLayer(this, new PigModel(var1.bakeLayer(ModelLayers.PIG_SADDLE)), new ResourceLocation("textures/entity/pig/pig_saddle.png")));
   }

   public ResourceLocation getTextureLocation(Pig var1) {
      return PIG_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Pig)var1);
   }
}
