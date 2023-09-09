package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.SquidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;

public class SquidRenderer<T extends Squid> extends MobRenderer<T, SquidModel<T>> {
   private static final ResourceLocation SQUID_LOCATION = new ResourceLocation("textures/entity/squid/squid.png");

   public SquidRenderer(EntityRendererProvider.Context var1, SquidModel<T> var2) {
      super(var1, var2, 0.7F);
   }

   public ResourceLocation getTextureLocation(T var1) {
      return SQUID_LOCATION;
   }

   protected void setupRotations(T var1, PoseStack var2, float var3, float var4, float var5) {
      float var6 = Mth.lerp(var5, var1.xBodyRotO, var1.xBodyRot);
      float var7 = Mth.lerp(var5, var1.zBodyRotO, var1.zBodyRot);
      var2.translate(0.0F, 0.5F, 0.0F);
      var2.mulPose(Axis.YP.rotationDegrees(180.0F - var4));
      var2.mulPose(Axis.XP.rotationDegrees(var6));
      var2.mulPose(Axis.YP.rotationDegrees(var7));
      var2.translate(0.0F, -1.2F, 0.0F);
   }

   protected float getBob(T var1, float var2) {
      return Mth.lerp(var2, var1.oldTentacleAngle, var1.tentacleAngle);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected float getBob(LivingEntity var1, float var2) {
      return this.getBob((Squid)var1, var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void setupRotations(LivingEntity var1, PoseStack var2, float var3, float var4, float var5) {
      this.setupRotations((Squid)var1, var2, var3, var4, var5);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Squid)var1);
   }
}
