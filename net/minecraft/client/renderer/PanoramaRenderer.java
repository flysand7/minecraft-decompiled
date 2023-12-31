package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;

public class PanoramaRenderer {
   private final Minecraft minecraft;
   private final CubeMap cubeMap;
   private float spin;
   private float bob;

   public PanoramaRenderer(CubeMap var1) {
      this.cubeMap = var1;
      this.minecraft = Minecraft.getInstance();
   }

   public void render(float var1, float var2) {
      float var3 = (float)((double)var1 * (Double)this.minecraft.options.panoramaSpeed().get());
      this.spin = wrap(this.spin + var3 * 0.1F, 360.0F);
      this.bob = wrap(this.bob + var3 * 0.001F, 6.2831855F);
      this.cubeMap.render(this.minecraft, 10.0F, -this.spin, var2);
   }

   private static float wrap(float var0, float var1) {
      return var0 > var1 ? var0 - var1 : var0;
   }
}
