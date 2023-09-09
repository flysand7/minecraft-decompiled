package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.TropicalFish;

public class TropicalFishRenderer extends MobRenderer<TropicalFish, ColorableHierarchicalModel<TropicalFish>> {
   private final ColorableHierarchicalModel<TropicalFish> modelA = (ColorableHierarchicalModel)this.getModel();
   private final ColorableHierarchicalModel<TropicalFish> modelB;
   private static final ResourceLocation MODEL_A_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a.png");
   private static final ResourceLocation MODEL_B_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b.png");

   public TropicalFishRenderer(EntityRendererProvider.Context var1) {
      super(var1, new TropicalFishModelA(var1.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
      this.modelB = new TropicalFishModelB(var1.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
      this.addLayer(new TropicalFishPatternLayer(this, var1.getModelSet()));
   }

   public ResourceLocation getTextureLocation(TropicalFish var1) {
      ResourceLocation var10000;
      switch(var1.getVariant().base()) {
      case SMALL:
         var10000 = MODEL_A_TEXTURE;
         break;
      case LARGE:
         var10000 = MODEL_B_TEXTURE;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public void render(TropicalFish var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      ColorableHierarchicalModel var10000;
      switch(var1.getVariant().base()) {
      case SMALL:
         var10000 = this.modelA;
         break;
      case LARGE:
         var10000 = this.modelB;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      ColorableHierarchicalModel var7 = var10000;
      this.model = var7;
      float[] var8 = var1.getBaseColor().getTextureDiffuseColors();
      var7.setColor(var8[0], var8[1], var8[2]);
      super.render((Mob)var1, var2, var3, var4, var5, var6);
      var7.setColor(1.0F, 1.0F, 1.0F);
   }

   protected void setupRotations(TropicalFish var1, PoseStack var2, float var3, float var4, float var5) {
      super.setupRotations(var1, var2, var3, var4, var5);
      float var6 = 4.3F * Mth.sin(0.6F * var3);
      var2.mulPose(Axis.YP.rotationDegrees(var6));
      if (!var1.isInWater()) {
         var2.translate(0.2F, 0.1F, 0.0F);
         var2.mulPose(Axis.ZP.rotationDegrees(90.0F));
      }

   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Mob var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((TropicalFish)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((TropicalFish)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(LivingEntity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((TropicalFish)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((TropicalFish)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((TropicalFish)var1, var2, var3, var4, var5, var6);
   }
}
