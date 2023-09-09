package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Turtle;

public class TurtleRenderer extends MobRenderer<Turtle, TurtleModel<Turtle>> {
   private static final ResourceLocation TURTLE_LOCATION = new ResourceLocation("textures/entity/turtle/big_sea_turtle.png");

   public TurtleRenderer(EntityRendererProvider.Context var1) {
      super(var1, new TurtleModel(var1.bakeLayer(ModelLayers.TURTLE)), 0.7F);
   }

   public void render(Turtle var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      if (var1.isBaby()) {
         this.shadowRadius *= 0.5F;
      }

      super.render((Mob)var1, var2, var3, var4, var5, var6);
   }

   public ResourceLocation getTextureLocation(Turtle var1) {
      return TURTLE_LOCATION;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Mob var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((Turtle)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(LivingEntity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((Turtle)var1, var2, var3, var4, var5, var6);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Turtle)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((Turtle)var1, var2, var3, var4, var5, var6);
   }
}
