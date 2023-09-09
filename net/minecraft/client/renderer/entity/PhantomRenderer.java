package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Phantom;

public class PhantomRenderer extends MobRenderer<Phantom, PhantomModel<Phantom>> {
   private static final ResourceLocation PHANTOM_LOCATION = new ResourceLocation("textures/entity/phantom.png");

   public PhantomRenderer(EntityRendererProvider.Context var1) {
      super(var1, new PhantomModel(var1.bakeLayer(ModelLayers.PHANTOM)), 0.75F);
      this.addLayer(new PhantomEyesLayer(this));
   }

   public ResourceLocation getTextureLocation(Phantom var1) {
      return PHANTOM_LOCATION;
   }

   protected void scale(Phantom var1, PoseStack var2, float var3) {
      int var4 = var1.getPhantomSize();
      float var5 = 1.0F + 0.15F * (float)var4;
      var2.scale(var5, var5, var5);
      var2.translate(0.0F, 1.3125F, 0.1875F);
   }

   protected void setupRotations(Phantom var1, PoseStack var2, float var3, float var4, float var5) {
      super.setupRotations(var1, var2, var3, var4, var5);
      var2.mulPose(Axis.XP.rotationDegrees(var1.getXRot()));
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void scale(LivingEntity var1, PoseStack var2, float var3) {
      this.scale((Phantom)var1, var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Phantom)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Phantom)var1);
   }
}
