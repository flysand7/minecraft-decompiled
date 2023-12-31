package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.SquidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.animal.Squid;

public class GlowSquidRenderer extends SquidRenderer<GlowSquid> {
   private static final ResourceLocation GLOW_SQUID_LOCATION = new ResourceLocation("textures/entity/squid/glow_squid.png");

   public GlowSquidRenderer(EntityRendererProvider.Context var1, SquidModel<GlowSquid> var2) {
      super(var1, var2);
   }

   public ResourceLocation getTextureLocation(GlowSquid var1) {
      return GLOW_SQUID_LOCATION;
   }

   protected int getBlockLightLevel(GlowSquid var1, BlockPos var2) {
      int var3 = (int)Mth.clampedLerp(0.0F, 15.0F, 1.0F - (float)var1.getDarkTicksRemaining() / 10.0F);
      return var3 == 15 ? 15 : Math.max(var3, super.getBlockLightLevel(var1, var2));
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Squid var1) {
      return this.getTextureLocation((GlowSquid)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((GlowSquid)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected int getBlockLightLevel(Entity var1, BlockPos var2) {
      return this.getBlockLightLevel((GlowSquid)var1, var2);
   }
}
