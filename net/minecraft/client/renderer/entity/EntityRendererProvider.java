package net.minecraft.client.renderer.entity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface EntityRendererProvider<T extends Entity> {
   EntityRenderer<T> create(EntityRendererProvider.Context var1);

   public static class Context {
      private final EntityRenderDispatcher entityRenderDispatcher;
      private final ItemRenderer itemRenderer;
      private final BlockRenderDispatcher blockRenderDispatcher;
      private final ItemInHandRenderer itemInHandRenderer;
      private final ResourceManager resourceManager;
      private final EntityModelSet modelSet;
      private final Font font;

      public Context(EntityRenderDispatcher var1, ItemRenderer var2, BlockRenderDispatcher var3, ItemInHandRenderer var4, ResourceManager var5, EntityModelSet var6, Font var7) {
         this.entityRenderDispatcher = var1;
         this.itemRenderer = var2;
         this.blockRenderDispatcher = var3;
         this.itemInHandRenderer = var4;
         this.resourceManager = var5;
         this.modelSet = var6;
         this.font = var7;
      }

      public EntityRenderDispatcher getEntityRenderDispatcher() {
         return this.entityRenderDispatcher;
      }

      public ItemRenderer getItemRenderer() {
         return this.itemRenderer;
      }

      public BlockRenderDispatcher getBlockRenderDispatcher() {
         return this.blockRenderDispatcher;
      }

      public ItemInHandRenderer getItemInHandRenderer() {
         return this.itemInHandRenderer;
      }

      public ResourceManager getResourceManager() {
         return this.resourceManager;
      }

      public EntityModelSet getModelSet() {
         return this.modelSet;
      }

      public ModelManager getModelManager() {
         return this.blockRenderDispatcher.getBlockModelShaper().getModelManager();
      }

      public ModelPart bakeLayer(ModelLayerLocation var1) {
         return this.modelSet.bakeLayer(var1);
      }

      public Font getFont() {
         return this.font;
      }
   }
}
