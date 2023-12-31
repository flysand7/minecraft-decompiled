package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ItemEntityRenderer extends EntityRenderer<ItemEntity> {
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final int ITEM_COUNT_FOR_5_BUNDLE = 48;
   private static final int ITEM_COUNT_FOR_4_BUNDLE = 32;
   private static final int ITEM_COUNT_FOR_3_BUNDLE = 16;
   private static final int ITEM_COUNT_FOR_2_BUNDLE = 1;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_X = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Y = 0.0F;
   private static final float FLAT_ITEM_BUNDLE_OFFSET_Z = 0.09375F;
   private final ItemRenderer itemRenderer;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(EntityRendererProvider.Context var1) {
      super(var1);
      this.itemRenderer = var1.getItemRenderer();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   private int getRenderAmount(ItemStack var1) {
      byte var2 = 1;
      if (var1.getCount() > 48) {
         var2 = 5;
      } else if (var1.getCount() > 32) {
         var2 = 4;
      } else if (var1.getCount() > 16) {
         var2 = 3;
      } else if (var1.getCount() > 1) {
         var2 = 2;
      }

      return var2;
   }

   public void render(ItemEntity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      var4.pushPose();
      ItemStack var7 = var1.getItem();
      int var8 = var7.isEmpty() ? 187 : Item.getId(var7.getItem()) + var7.getDamageValue();
      this.random.setSeed((long)var8);
      BakedModel var9 = this.itemRenderer.getModel(var7, var1.level(), (LivingEntity)null, var1.getId());
      boolean var10 = var9.isGui3d();
      int var11 = this.getRenderAmount(var7);
      float var12 = 0.25F;
      float var13 = Mth.sin(((float)var1.getAge() + var3) / 10.0F + var1.bobOffs) * 0.1F + 0.1F;
      float var14 = var9.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
      var4.translate(0.0F, var13 + 0.25F * var14, 0.0F);
      float var15 = var1.getSpin(var3);
      var4.mulPose(Axis.YP.rotation(var15));
      float var16 = var9.getTransforms().ground.scale.x();
      float var17 = var9.getTransforms().ground.scale.y();
      float var18 = var9.getTransforms().ground.scale.z();
      float var20;
      float var21;
      if (!var10) {
         float var19 = -0.0F * (float)(var11 - 1) * 0.5F * var16;
         var20 = -0.0F * (float)(var11 - 1) * 0.5F * var17;
         var21 = -0.09375F * (float)(var11 - 1) * 0.5F * var18;
         var4.translate(var19, var20, var21);
      }

      for(int var23 = 0; var23 < var11; ++var23) {
         var4.pushPose();
         if (var23 > 0) {
            if (var10) {
               var20 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               var21 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float var22 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               var4.translate(var20, var21, var22);
            } else {
               var20 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               var21 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               var4.translate(var20, var21, 0.0F);
            }
         }

         this.itemRenderer.render(var7, ItemDisplayContext.GROUND, false, var4, var5, var6, OverlayTexture.NO_OVERLAY, var9);
         var4.popPose();
         if (!var10) {
            var4.translate(0.0F * var16, 0.0F * var17, 0.09375F * var18);
         }
      }

      var4.popPose();
      super.render(var1, var2, var3, var4, var5, var6);
   }

   public ResourceLocation getTextureLocation(ItemEntity var1) {
      return TextureAtlas.LOCATION_BLOCKS;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public ResourceLocation getTextureLocation(Entity var1) {
      return this.getTextureLocation((ItemEntity)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void render(Entity var1, float var2, float var3, PoseStack var4, MultiBufferSource var5, int var6) {
      this.render((ItemEntity)var1, var2, var3, var4, var5, var6);
   }
}
