package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.DragonFireball;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DragonFireballRenderer extends EntityRenderer<DragonFireball> {
   private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/enderdragon/dragon_fireball.png");
   private static final RenderType RENDER_TYPE;

   public DragonFireballRenderer(EntityRendererProvider.Context var1) {
      super(var1);
   }

   protected int getBlockLightLevel(DragonFireball var1, BlockPos var2) {
      return 15;
   }

   public void render(DragonFireball var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      var4.pushPose();
      var4.scale(2.0F, 2.0F, 2.0F);
      var4.mulPose(this.entityRenderDispatcher.cameraOrientation());
      var4.mulPose(Axis.YP.rotationDegrees(180.0F));
      PoseStack.Pose var7 = var4.last();
      Matrix4f var8 = var7.pose();
      Matrix3f var9 = var7.normal();
      VertexConsumer var10 = var5.getBuffer(RENDER_TYPE);
      vertex(var10, var8, var9, var6, 0.0F, 0, 0, 1);
      vertex(var10, var8, var9, var6, 1.0F, 0, 1, 1);
      vertex(var10, var8, var9, var6, 1.0F, 1, 1, 0);
      vertex(var10, var8, var9, var6, 0.0F, 1, 0, 0);
      var4.popPose();
      super.render(var1, var2, var3, var4, var5, var6);
   }

   private static void vertex(VertexConsumer var0, Matrix4f var1, Matrix3f var2, int var3, float var4, int var5, int var6, int var7) {
      var0.vertex(var1, var4 - 0.5F, (float)var5 - 0.25F, 0.0F).color(255, 255, 255, 255).uv((float)var6, (float)var7).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(var3).normal(var2, 0.0F, 1.0F, 0.0F).endVertex();
   }

   public ResourceLocation getTextureLocation(DragonFireball var1) {
      return TEXTURE_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((DragonFireball)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((DragonFireball)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected int getBlockLightLevel(Entity var1, BlockPos var2) {
      return this.getBlockLightLevel((DragonFireball)var1, var2);
   }

   static {
      RENDER_TYPE = RenderType.entityCutoutNoCull(TEXTURE_LOCATION);
   }
}
