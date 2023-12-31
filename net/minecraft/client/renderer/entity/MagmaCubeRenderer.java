package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.MagmaCube;

public class MagmaCubeRenderer extends MobRenderer<MagmaCube, LavaSlimeModel<MagmaCube>> {
   private static final ResourceLocation MAGMACUBE_LOCATION = new ResourceLocation("textures/entity/slime/magmacube.png");

   public MagmaCubeRenderer(EntityRendererProvider.Context var1) {
      super(var1, new LavaSlimeModel(var1.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
   }

   protected int getBlockLightLevel(MagmaCube var1, BlockPos var2) {
      return 15;
   }

   public ResourceLocation getTextureLocation(MagmaCube var1) {
      return MAGMACUBE_LOCATION;
   }

   public void render(MagmaCube var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.shadowRadius = 0.25F * (float)var1.getSize();
      super.render((Mob)var1, var2, var3, var4, var5, var6);
   }

   protected void scale(MagmaCube var1, PoseStack var2, float var3) {
      int var4 = var1.getSize();
      float var5 = Mth.lerp(var3, var1.oSquish, var1.squish) / ((float)var4 * 0.5F + 1.0F);
      float var6 = 1.0F / (var5 + 1.0F);
      var2.scale(var6 * (float)var4, 1.0F / var6 * (float)var4, var6 * (float)var4);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Mob var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((MagmaCube)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void scale(LivingEntity var1, PoseStack var2, float var3) {
      this.scale((MagmaCube)var1, var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(LivingEntity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((MagmaCube)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((MagmaCube)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((MagmaCube)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected int getBlockLightLevel(Entity var1, BlockPos var2) {
      return this.getBlockLightLevel((MagmaCube)var1, var2);
   }
}
