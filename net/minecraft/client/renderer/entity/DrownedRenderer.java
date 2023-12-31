package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.DrownedOuterLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;

public class DrownedRenderer extends AbstractZombieRenderer<Drowned, DrownedModel<Drowned>> {
   private static final ResourceLocation DROWNED_LOCATION = new ResourceLocation("textures/entity/zombie/drowned.png");

   public DrownedRenderer(EntityRendererProvider.Context var1) {
      super(var1, new DrownedModel(var1.bakeLayer(ModelLayers.DROWNED)), new DrownedModel(var1.bakeLayer(ModelLayers.DROWNED_INNER_ARMOR)), new DrownedModel(var1.bakeLayer(ModelLayers.DROWNED_OUTER_ARMOR)));
      this.addLayer(new DrownedOuterLayer(this, var1.getModelSet()));
   }

   public ResourceLocation getTextureLocation(Zombie var1) {
      return DROWNED_LOCATION;
   }

   protected void setupRotations(Drowned var1, PoseStack var2, float var3, float var4, float var5) {
      super.setupRotations(var1, var2, var3, var4, var5);
      float var6 = var1.getSwimAmount(var5);
      if (var6 > 0.0F) {
         float var7 = -10.0F - var1.getXRot();
         float var8 = Mth.lerp(var6, 0.0F, var7);
         var2.rotateAround(Axis.XP.rotationDegrees(var8), 0.0F, var1.getBbHeight() / 2.0F, 0.0F);
      }

   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Drowned)var1, var2, var3, var4, var5);
   }
}
