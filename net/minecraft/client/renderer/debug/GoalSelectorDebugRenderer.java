package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;

public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;
   private final Map<Integer, List<GoalSelectorDebugRenderer.DebugGoal>> goalSelectors = Maps.newHashMap();

   public void clear() {
      this.goalSelectors.clear();
   }

   public void addGoalSelector(int var1, List<GoalSelectorDebugRenderer.DebugGoal> var2) {
      this.goalSelectors.put(var1, var2);
   }

   public void removeGoalSelector(int var1) {
      this.goalSelectors.remove(var1);
   }

   public GoalSelectorDebugRenderer(Minecraft var1) {
      this.minecraft = var1;
   }

   public void render(PoseStack var1, MultiBufferSource var2, double var3, double var5, double var7) {
      Camera var9 = this.minecraft.gameRenderer.getMainCamera();
      BlockPos var10 = BlockPos.containing(var9.getPosition().x, 0.0D, var9.getPosition().z);
      this.goalSelectors.forEach((var3x, var4) -> {
         for(int var5 = 0; var5 < var4.size(); ++var5) {
            GoalSelectorDebugRenderer.DebugGoal var6 = (GoalSelectorDebugRenderer.DebugGoal)var4.get(var5);
            if (var10.closerThan(var6.pos, 160.0D)) {
               double var7 = (double)var6.pos.getX() + 0.5D;
               double var9 = (double)var6.pos.getY() + 2.0D + (double)var5 * 0.25D;
               double var11 = (double)var6.pos.getZ() + 0.5D;
               int var13 = var6.isRunning ? -16711936 : -3355444;
               DebugRenderer.renderFloatingText(var1, var2, var6.name, var7, var9, var11, var13);
            }
         }

      });
   }

   public static class DebugGoal {
      public final BlockPos pos;
      public final int priority;
      public final String name;
      public final boolean isRunning;

      public DebugGoal(BlockPos var1, int var2, String var3, boolean var4) {
         this.pos = var1;
         this.priority = var2;
         this.name = var3;
         this.isRunning = var4;
      }
   }
}
