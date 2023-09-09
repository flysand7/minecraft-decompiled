package net.minecraft.client.renderer.blockentity;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BrightnessCombiner<S extends BlockEntity> implements DoubleBlockCombiner.Combiner<S, Int2IntFunction> {
   public BrightnessCombiner() {
   }

   public Int2IntFunction acceptDouble(S var1, S var2) {
      return (var2x) -> {
         int var3 = LevelRenderer.getLightColor(var1.getLevel(), var1.getBlockPos());
         int var4 = LevelRenderer.getLightColor(var2.getLevel(), var2.getBlockPos());
         int var5 = LightTexture.block(var3);
         int var6 = LightTexture.block(var4);
         int var7 = LightTexture.sky(var3);
         int var8 = LightTexture.sky(var4);
         return LightTexture.pack(Math.max(var5, var6), Math.max(var7, var8));
      };
   }

   public Int2IntFunction acceptSingle(S var1) {
      return (var0) -> {
         return var0;
      };
   }

   public Int2IntFunction acceptNone() {
      return (var0) -> {
         return var0;
      };
   }

   // $FF: synthetic method
   public Object acceptNone() {
      return this.acceptNone();
   }

   // $FF: synthetic method
   // $FF: bridge method
   public Object acceptSingle(Object var1) {
      return this.acceptSingle((BlockEntity)var1);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public Object acceptDouble(Object var1, Object var2) {
      return this.acceptDouble((BlockEntity)var1, (BlockEntity)var2);
   }
}
