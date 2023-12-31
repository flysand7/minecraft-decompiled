package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemOverrides {
   public static final ItemOverrides EMPTY = new ItemOverrides();
   public static final float NO_OVERRIDE = Float.NEGATIVE_INFINITY;
   private final ItemOverrides.BakedOverride[] overrides;
   private final ResourceLocation[] properties;

   private ItemOverrides() {
      this.overrides = new ItemOverrides.BakedOverride[0];
      this.properties = new ResourceLocation[0];
   }

   public ItemOverrides(ModelBaker var1, BlockModel var2, List<ItemOverride> var3) {
      this.properties = (ResourceLocation[])var3.stream().flatMap(ItemOverride::getPredicates).map(ItemOverride.Predicate::getProperty).distinct().toArray((var0) -> {
         return new ResourceLocation[var0];
      });
      Object2IntOpenHashMap var4 = new Object2IntOpenHashMap();

      for(int var5 = 0; var5 < this.properties.length; ++var5) {
         var4.put(this.properties[var5], var5);
      }

      ArrayList var10 = Lists.newArrayList();

      for(int var6 = var3.size() - 1; var6 >= 0; --var6) {
         ItemOverride var7 = (ItemOverride)var3.get(var6);
         BakedModel var8 = this.bakeModel(var1, var2, var7);
         ItemOverrides.PropertyMatcher[] var9 = (ItemOverrides.PropertyMatcher[])var7.getPredicates().map((var1x) -> {
            int var2 = var4.getInt(var1x.getProperty());
            return new ItemOverrides.PropertyMatcher(var2, var1x.getValue());
         }).toArray((var0) -> {
            return new ItemOverrides.PropertyMatcher[var0];
         });
         var10.add(new ItemOverrides.BakedOverride(var9, var8));
      }

      this.overrides = (ItemOverrides.BakedOverride[])var10.toArray(new ItemOverrides.BakedOverride[0]);
   }

   @Nullable
   private BakedModel bakeModel(ModelBaker var1, BlockModel var2, ItemOverride var3) {
      UnbakedModel var4 = var1.getModel(var3.getModel());
      return Objects.equals(var4, var2) ? null : var1.bake(var3.getModel(), BlockModelRotation.X0_Y0);
   }

   @Nullable
   public BakedModel resolve(BakedModel var1, ItemStack var2, @Nullable ClientLevel var3, @Nullable LivingEntity var4, int var5) {
      if (this.overrides.length != 0) {
         Item var6 = var2.getItem();
         int var7 = this.properties.length;
         float[] var8 = new float[var7];

         for(int var9 = 0; var9 < var7; ++var9) {
            ResourceLocation var10 = this.properties[var9];
            ItemPropertyFunction var11 = ItemProperties.getProperty(var6, var10);
            if (var11 != null) {
               var8[var9] = var11.call(var2, var3, var4, var5);
            } else {
               var8[var9] = Float.NEGATIVE_INFINITY;
            }
         }

         ItemOverrides.BakedOverride[] var16 = this.overrides;
         int var14 = var16.length;

         for(int var15 = 0; var15 < var14; ++var15) {
            ItemOverrides.BakedOverride var12 = var16[var15];
            if (var12.test(var8)) {
               BakedModel var13 = var12.model;
               if (var13 == null) {
                  return var1;
               }

               return var13;
            }
         }
      }

      return var1;
   }

   static class BakedOverride {
      private final ItemOverrides.PropertyMatcher[] matchers;
      @Nullable
      final BakedModel model;

      BakedOverride(ItemOverrides.PropertyMatcher[] var1, @Nullable BakedModel var2) {
         this.matchers = var1;
         this.model = var2;
      }

      boolean test(float[] var1) {
         ItemOverrides.PropertyMatcher[] var2 = this.matchers;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            ItemOverrides.PropertyMatcher var5 = var2[var4];
            float var6 = var1[var5.index];
            if (var6 < var5.value) {
               return false;
            }
         }

         return true;
      }
   }

   private static class PropertyMatcher {
      public final int index;
      public final float value;

      PropertyMatcher(int var1, float var2) {
         this.index = var1;
         this.value = var2;
      }
   }
}
