package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.BatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;

public class BatRenderer extends MobRenderer<Bat, BatModel> {
   private static final ResourceLocation BAT_LOCATION = new ResourceLocation("textures/entity/bat.png");

   public BatRenderer(EntityRendererProvider.Context var1) {
      super(var1, new BatModel(var1.bakeLayer(ModelLayers.BAT)), 0.25F);
   }

   public ResourceLocation getTextureLocation(Bat var1) {
      return BAT_LOCATION;
   }

   protected void scale(Bat var1, PoseStack var2, float var3) {
      var2.scale(0.35F, 0.35F, 0.35F);
   }

   protected void setupRotations(Bat var1, PoseStack var2, float var3, float var4, float var5) {
      if (var1.isResting()) {
         var2.translate(0.0F, -0.1F, 0.0F);
      } else {
         var2.translate(0.0F, Mth.cos(var3 * 0.3F) * 0.1F, 0.0F);
      }

      super.setupRotations(var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void scale(LivingEntity var1, PoseStack var2, float var3) {
      this.scale((Bat)var1, var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Bat)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Bat)var1);
   }
}
