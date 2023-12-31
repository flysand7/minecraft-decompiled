package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.layers.ShulkerHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShulkerRenderer extends MobRenderer<Shulker, ShulkerModel<Shulker>> {
   private static final ResourceLocation DEFAULT_TEXTURE_LOCATION;
   private static final ResourceLocation[] TEXTURE_LOCATION;

   public ShulkerRenderer(EntityRendererProvider.Context var1) {
      super(var1, new ShulkerModel(var1.bakeLayer(ModelLayers.SHULKER)), 0.0F);
      this.addLayer(new ShulkerHeadLayer(this));
   }

   public Vec3 getRenderOffset(Shulker var1, float var2) {
      return (Vec3)var1.getRenderPosition(var2).orElse(super.getRenderOffset(var1, var2));
   }

   public boolean shouldRender(Shulker var1, Frustum var2, double var3, double var5, double var7) {
      return super.shouldRender((Mob)var1, var2, var3, var5, var7) ? true : var1.getRenderPosition(0.0F).filter((var2x) -> {
         EntityType var3 = var1.getType();
         float var4 = var3.getHeight() / 2.0F;
         float var5 = var3.getWidth() / 2.0F;
         Vec3 var6 = Vec3.atBottomCenterOf(var1.blockPosition());
         return var2.isVisible((new AABB(var2x.x, var2x.y + (double)var4, var2x.z, var6.x, var6.y + (double)var4, var6.z)).inflate((double)var5, (double)var4, (double)var5));
      }).isPresent();
   }

   public ResourceLocation getTextureLocation(Shulker var1) {
      return getTextureLocation(var1.getColor());
   }

   public static ResourceLocation getTextureLocation(@Nullable DyeColor var0) {
      return var0 == null ? DEFAULT_TEXTURE_LOCATION : TEXTURE_LOCATION[var0.getId()];
   }

   protected void setupRotations(Shulker var1, PoseStack var2, float var3, float var4, float var5) {
      super.setupRotations(var1, var2, var3, var4 + 180.0F, var5);
      var2.translate(0.0D, 0.5D, 0.0D);
      var2.mulPose(var1.getAttachFace().getOpposite().getRotation());
      var2.translate(0.0D, -0.5D, 0.0D);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public boolean shouldRender(Mob var1, Frustum var2, double var3, double var5, double var7) {
      return this.shouldRender((Shulker)var1, var2, var3, var5, var7);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Shulker)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Shulker)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public Vec3 getRenderOffset(Entity var1, float var2) {
      return this.getRenderOffset((Shulker)var1, var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public boolean shouldRender(Entity var1, Frustum var2, double var3, double var5, double var7) {
      return this.shouldRender((Shulker)var1, var2, var3, var5, var7);
   }

   static {
      DEFAULT_TEXTURE_LOCATION = new ResourceLocation("textures/" + Sheets.DEFAULT_SHULKER_TEXTURE_LOCATION.texture().getPath() + ".png");
      TEXTURE_LOCATION = (ResourceLocation[])Sheets.SHULKER_TEXTURE_LOCATION.stream().map((var0) -> {
         return new ResourceLocation("textures/" + var0.texture().getPath() + ".png");
      }).toArray((var0) -> {
         return new ResourceLocation[var0];
      });
   }
}
