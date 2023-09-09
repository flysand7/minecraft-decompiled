package net.minecraft.client.renderer.entity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.SpectralArrow;

public class SpectralArrowRenderer extends ArrowRenderer<SpectralArrow> {
   public static final ResourceLocation SPECTRAL_ARROW_LOCATION = new ResourceLocation("textures/entity/projectiles/spectral_arrow.png");

   public SpectralArrowRenderer(EntityRendererProvider.Context var1) {
      super(var1);
   }

   public ResourceLocation getTextureLocation(SpectralArrow var1) {
      return SPECTRAL_ARROW_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((SpectralArrow)var1);
   }
}
