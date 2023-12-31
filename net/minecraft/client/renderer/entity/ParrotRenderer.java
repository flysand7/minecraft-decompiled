package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;

public class ParrotRenderer extends MobRenderer<Parrot, ParrotModel> {
   private static final ResourceLocation RED_BLUE = new ResourceLocation("textures/entity/parrot/parrot_red_blue.png");
   private static final ResourceLocation BLUE = new ResourceLocation("textures/entity/parrot/parrot_blue.png");
   private static final ResourceLocation GREEN = new ResourceLocation("textures/entity/parrot/parrot_green.png");
   private static final ResourceLocation YELLOW_BLUE = new ResourceLocation("textures/entity/parrot/parrot_yellow_blue.png");
   private static final ResourceLocation GREY = new ResourceLocation("textures/entity/parrot/parrot_grey.png");

   public ParrotRenderer(EntityRendererProvider.Context var1) {
      super(var1, new ParrotModel(var1.bakeLayer(ModelLayers.PARROT)), 0.3F);
   }

   public ResourceLocation getTextureLocation(Parrot var1) {
      return getVariantTexture(var1.getVariant());
   }

   public static ResourceLocation getVariantTexture(Parrot.Variant var0) {
      ResourceLocation var10000;
      switch(var0) {
      case RED_BLUE:
         var10000 = RED_BLUE;
         break;
      case BLUE:
         var10000 = BLUE;
         break;
      case GREEN:
         var10000 = GREEN;
         break;
      case YELLOW_BLUE:
         var10000 = YELLOW_BLUE;
         break;
      case GRAY:
         var10000 = GREY;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      return var10000;
   }

   public float getBob(Parrot var1, float var2) {
      float var3 = Mth.lerp(var2, var1.oFlap, var1.flap);
      float var4 = Mth.lerp(var2, var1.oFlapSpeed, var1.flapSpeed);
      return (Mth.sin(var3) + 1.0F) * var4;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public float getBob(LivingEntity var1, float var2) {
      return this.getBob((Parrot)var1, var2);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((Parrot)var1);
   }
}
