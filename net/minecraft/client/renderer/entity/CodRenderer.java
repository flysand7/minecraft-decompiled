package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cod;

public class CodRenderer extends MobRenderer<Cod, CodModel<Cod>> {
   private static final ResourceLocation COD_LOCATION = new ResourceLocation("textures/entity/fish/cod.png");

   public CodRenderer(EntityRendererProvider.Context var1) {
      super(var1, new CodModel(var1.bakeLayer(ModelLayers.COD)), 0.3F);
   }

   public ResourceLocation getTextureLocation(Cod var1) {
      return COD_LOCATION;
   }

   protected void setupRotations(Cod var1, PoseStack var2, float var3, float var4, float var5) {
      super.setupRotations(var1, var2, var3, var4, var5);
      float var6 = 4.3F * Mth.sin(0.6F * var3);
      var2.mulPose(Axis.YP.rotationDegrees(var6));
      if (!var1.isInWater()) {
         var2.translate(0.1F, 0.1F, -0.1F);
         var2.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }

   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Cod)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Cod)var1);
   }
}
