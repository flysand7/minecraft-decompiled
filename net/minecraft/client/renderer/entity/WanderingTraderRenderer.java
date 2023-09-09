package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.WanderingTrader;

public class WanderingTraderRenderer extends MobRenderer<WanderingTrader, VillagerModel<WanderingTrader>> {
   private static final ResourceLocation VILLAGER_BASE_SKIN = new ResourceLocation("textures/entity/wandering_trader.png");

   public WanderingTraderRenderer(EntityRendererProvider.Context var1) {
      super(var1, new VillagerModel(var1.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5F);
      this.addLayer(new CustomHeadLayer(this, var1.getModelSet(), var1.getItemInHandRenderer()));
      this.addLayer(new CrossedArmsItemLayer(this, var1.getItemInHandRenderer()));
   }

   public ResourceLocation getTextureLocation(WanderingTrader var1) {
      return VILLAGER_BASE_SKIN;
   }

   protected void scale(WanderingTrader var1, PoseStack var2, float var3) {
      float var4 = 0.9375F;
      var2.scale(0.9375F, 0.9375F, 0.9375F);
   }

   // $FF: synthetic method
   // $FF: bridge method
   protected void scale(LivingEntity var1, PoseStack var2, float var3) {
      this.scale((WanderingTrader)var1, var2, var3);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((WanderingTrader)var1);
   }
}
