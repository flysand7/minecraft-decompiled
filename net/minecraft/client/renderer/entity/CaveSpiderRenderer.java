package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Spider;

public class CaveSpiderRenderer extends SpiderRenderer<CaveSpider> {
   private static final ResourceLocation CAVE_SPIDER_LOCATION = new ResourceLocation("textures/entity/spider/cave_spider.png");
   private static final float SCALE = 0.7F;

   public CaveSpiderRenderer(EntityRendererProvider.Context var1) {
      super(var1, ModelLayers.CAVE_SPIDER);
      this.shadowRadius *= 0.7F;
   }

   protected void scale(CaveSpider var1, PoseStack var2, float var3) {
      var2.scale(0.7F, 0.7F, 0.7F);
   }

   public ResourceLocation getTextureLocation(CaveSpider var1) {
      return CAVE_SPIDER_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Spider var1) {
      return this.getTextureLocation((CaveSpider)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void scale(LivingEntity var1, PoseStack var2, float var3) {
      this.scale((CaveSpider)var1, var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((CaveSpider)var1);
   }
}
