package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ColorableHierarchicalModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.TropicalFish;

public class TropicalFishPatternLayer extends RenderLayer<TropicalFish, ColorableHierarchicalModel<TropicalFish>> {
   private static final ResourceLocation KOB_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_1.png");
   private static final ResourceLocation SUNSTREAK_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_2.png");
   private static final ResourceLocation SNOOPER_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_3.png");
   private static final ResourceLocation DASHER_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_4.png");
   private static final ResourceLocation BRINELY_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_5.png");
   private static final ResourceLocation SPOTTY_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_a_pattern_6.png");
   private static final ResourceLocation FLOPPER_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_1.png");
   private static final ResourceLocation STRIPEY_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_2.png");
   private static final ResourceLocation GLITTER_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_3.png");
   private static final ResourceLocation BLOCKFISH_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_4.png");
   private static final ResourceLocation BETTY_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_5.png");
   private static final ResourceLocation CLAYFISH_TEXTURE = new ResourceLocation("textures/entity/fish/tropical_b_pattern_6.png");
   private final TropicalFishModelA<TropicalFish> modelA;
   private final TropicalFishModelB<TropicalFish> modelB;

   public TropicalFishPatternLayer(RenderLayerParent<TropicalFish, ColorableHierarchicalModel<TropicalFish>> var1, EntityModelSet var2) {
      super(var1);
      this.modelA = new TropicalFishModelA(var2.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL_PATTERN));
      this.modelB = new TropicalFishModelB(var2.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE_PATTERN));
   }

   public void render(PoseStack var1, MultiBufferSource var2, int var3, TropicalFish var4, float var5, float var6, float var7, float var8, float var9, float var10) {
      TropicalFish.Pattern var11 = var4.getVariant();
      Object var10000;
      switch(var11.base()) {
      case SMALL:
         var10000 = this.modelA;
         break;
      case LARGE:
         var10000 = this.modelB;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      Object var12 = var10000;
      ResourceLocation var15;
      switch(var11) {
      case KOB:
         var15 = KOB_TEXTURE;
         break;
      case SUNSTREAK:
         var15 = SUNSTREAK_TEXTURE;
         break;
      case SNOOPER:
         var15 = SNOOPER_TEXTURE;
         break;
      case DASHER:
         var15 = DASHER_TEXTURE;
         break;
      case BRINELY:
         var15 = BRINELY_TEXTURE;
         break;
      case SPOTTY:
         var15 = SPOTTY_TEXTURE;
         break;
      case FLOPPER:
         var15 = FLOPPER_TEXTURE;
         break;
      case STRIPEY:
         var15 = STRIPEY_TEXTURE;
         break;
      case GLITTER:
         var15 = GLITTER_TEXTURE;
         break;
      case BLOCKFISH:
         var15 = BLOCKFISH_TEXTURE;
         break;
      case BETTY:
         var15 = BETTY_TEXTURE;
         break;
      case CLAYFISH:
         var15 = CLAYFISH_TEXTURE;
         break;
      default:
         throw new IncompatibleClassChangeError();
      }

      ResourceLocation var13 = var15;
      float[] var14 = var4.getPatternColor().getTextureDiffuseColors();
      coloredCutoutModelCopyLayerRender(this.getParentModel(), (EntityModel)var12, var13, var1, var2, var3, var4, var5, var6, var8, var9, var10, var7, var14[0], var14[1], var14[2]);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(PoseStack var1, MultiBufferSource var2, int var3, Entity var4, float var5, float var6, float var7, float var8, float var9, float var10) {
      this.render(var1, var2, var3, (TropicalFish)var4, var5, var6, var7, var8, var9, var10);
   }
}
