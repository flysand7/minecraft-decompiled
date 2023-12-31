package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Map<Integer, Path> pathMap = Maps.newHashMap();
   private final Map<Integer, Float> pathMaxDist = Maps.newHashMap();
   private final Map<Integer, Long> creationMap = Maps.newHashMap();
   private static final long TIMEOUT = 5000L;
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.02F;

   public PathfindingRenderer() {
   }

   public void addPath(int var1, Path var2, float var3) {
      this.pathMap.put(var1, var2);
      this.creationMap.put(var1, Util.getMillis());
      this.pathMaxDist.put(var1, var3);
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7) {
      if (!this.pathMap.isEmpty()) {
         long var9 = Util.getMillis();
         Iterator var11 = this.pathMap.keySet().iterator();

         while(var11.hasNext()) {
            Integer var12 = (Integer)var11.next();
            Path var13 = (Path)this.pathMap.get(var12);
            float var14 = (Float)this.pathMaxDist.get(var12);
            renderPath(var1, var2, var13, var14, true, true, var3, var5, var7);
         }

         Integer[] var15 = (Integer[])this.creationMap.keySet().toArray(new Integer[0]);
         int var16 = var15.length;

         for(int var17 = 0; var17 < var16; ++var17) {
            Integer var18 = var15[var17];
            if (var9 - (Long)this.creationMap.get(var18) > 5000L) {
               this.pathMap.remove(var18);
               this.creationMap.remove(var18);
            }
         }

      }
   }

   public static void renderPath(PoseStack var0, MultiBufferSource var1, Path var2, float var3, boolean var4, boolean var5, double var6, double var8, double var10) {
      renderPathLine(var0, var1.getBuffer(RenderType.debugLineStrip(6.0D)), var2, var6, var8, var10);
      BlockPos var12 = var2.getTarget();
      int var13;
      Node var14;
      if (distanceToCamera(var12, var6, var8, var10) <= 80.0F) {
         DebugRenderer.renderFilledBox(var0, var1, (new AABB((double)((float)var12.getX() + 0.25F), (double)((float)var12.getY() + 0.25F), (double)var12.getZ() + 0.25D, (double)((float)var12.getX() + 0.75F), (double)((float)var12.getY() + 0.75F), (double)((float)var12.getZ() + 0.75F))).move(-var6, -var8, -var10), 0.0F, 1.0F, 0.0F, 0.5F);

         for(var13 = 0; var13 < var2.getNodeCount(); ++var13) {
            var14 = var2.getNode(var13);
            if (distanceToCamera(var14.asBlockPos(), var6, var8, var10) <= 80.0F) {
               float var15 = var13 == var2.getNextNodeIndex() ? 1.0F : 0.0F;
               float var16 = var13 == var2.getNextNodeIndex() ? 0.0F : 1.0F;
               DebugRenderer.renderFilledBox(var0, var1, (new AABB((double)((float)var14.x + 0.5F - var3), (double)((float)var14.y + 0.01F * (float)var13), (double)((float)var14.z + 0.5F - var3), (double)((float)var14.x + 0.5F + var3), (double)((float)var14.y + 0.25F + 0.01F * (float)var13), (double)((float)var14.z + 0.5F + var3))).move(-var6, -var8, -var10), var15, 0.0F, var16, 0.5F);
            }
         }
      }

      if (var4) {
         Node[] var17 = var2.getClosedSet();
         int var18 = var17.length;

         int var19;
         Node var20;
         for(var19 = 0; var19 < var18; ++var19) {
            var20 = var17[var19];
            if (distanceToCamera(var20.asBlockPos(), var6, var8, var10) <= 80.0F) {
               DebugRenderer.renderFilledBox(var0, var1, (new AABB((double)((float)var20.x + 0.5F - var3 / 2.0F), (double)((float)var20.y + 0.01F), (double)((float)var20.z + 0.5F - var3 / 2.0F), (double)((float)var20.x + 0.5F + var3 / 2.0F), (double)var20.y + 0.1D, (double)((float)var20.z + 0.5F + var3 / 2.0F))).move(-var6, -var8, -var10), 1.0F, 0.8F, 0.8F, 0.5F);
            }
         }

         var17 = var2.getOpenSet();
         var18 = var17.length;

         for(var19 = 0; var19 < var18; ++var19) {
            var20 = var17[var19];
            if (distanceToCamera(var20.asBlockPos(), var6, var8, var10) <= 80.0F) {
               DebugRenderer.renderFilledBox(var0, var1, (new AABB((double)((float)var20.x + 0.5F - var3 / 2.0F), (double)((float)var20.y + 0.01F), (double)((float)var20.z + 0.5F - var3 / 2.0F), (double)((float)var20.x + 0.5F + var3 / 2.0F), (double)var20.y + 0.1D, (double)((float)var20.z + 0.5F + var3 / 2.0F))).move(-var6, -var8, -var10), 0.8F, 1.0F, 1.0F, 0.5F);
            }
         }
      }

      if (var5) {
         for(var13 = 0; var13 < var2.getNodeCount(); ++var13) {
            var14 = var2.getNode(var13);
            if (distanceToCamera(var14.asBlockPos(), var6, var8, var10) <= 80.0F) {
               DebugRenderer.renderFloatingText(var0, var1, String.valueOf(var14.type), (double)var14.x + 0.5D, (double)var14.y + 0.75D, (double)var14.z + 0.5D, -1, 0.02F, true, 0.0F, true);
               DebugRenderer.renderFloatingText(var0, var1, String.format(Locale.ROOT, "%.2f", var14.costMalus), (double)var14.x + 0.5D, (double)var14.y + 0.25D, (double)var14.z + 0.5D, -1, 0.02F, true, 0.0F, true);
            }
         }
      }

   }

   public static void renderPathLine(PoseStack var0, VertexConsumer var1, Path var2, double var3, double var5, double var7) {
      for(int var9 = 0; var9 < var2.getNodeCount(); ++var9) {
         Node var10 = var2.getNode(var9);
         if (!(distanceToCamera(var10.asBlockPos(), var3, var5, var7) > 80.0F)) {
            float var11 = (float)var9 / (float)var2.getNodeCount() * 0.33F;
            int var12 = var9 == 0 ? 0 : Mth.hsvToRgb(var11, 0.9F, 0.9F);
            int var13 = var12 >> 16 & 255;
            int var14 = var12 >> 8 & 255;
            int var15 = var12 & 255;
            var1.vertex(var0.last().pose(), (float)((double)var10.x - var3 + 0.5D), (float)((double)var10.y - var5 + 0.5D), (float)((double)var10.z - var7 + 0.5D)).color(var13, var14, var15, 255).endVertex();
         }
      }

   }

   private static float distanceToCamera(BlockPos var0, double var1, double var3, double var5) {
      return (float)(Math.abs((double)var0.getX() - var1) + Math.abs((double)var0.getY() - var3) + Math.abs((double)var0.getZ() - var5));
   }
}
