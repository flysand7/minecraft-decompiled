package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

public class BannerPattern {
   final String hashname;

   public BannerPattern(String var1) {
      this.hashname = var1;
   }

   public static ResourceLocation location(ResourceKey<BannerPattern> var0, boolean var1) {
      String var2 = var1 ? "banner" : "shield";
      return var0.location().withPrefix("entity/" + var2 + "/");
   }

   public String getHashname() {
      return this.hashname;
   }

   @Nullable
   public static Holder<BannerPattern> byHash(String var0) {
      return (Holder)BuiltInRegistries.BANNER_PATTERN.holders().filter((var1) -> {
         return ((BannerPattern)var1.value()).hashname.equals(var0);
      }).findAny().orElse((Object)null);
   }

   public static class Builder {
      private final List<Pair<Holder<BannerPattern>, DyeColor>> patterns = Lists.newArrayList();

      public Builder() {
      }

      public BannerPattern.Builder addPattern(ResourceKey<BannerPattern> var1, DyeColor var2) {
         return this.addPattern((Holder)BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(var1), var2);
      }

      public BannerPattern.Builder addPattern(Holder<BannerPattern> var1, DyeColor var2) {
         return this.addPattern(Pair.of(var1, var2));
      }

      public BannerPattern.Builder addPattern(Pair<Holder<BannerPattern>, DyeColor> var1) {
         this.patterns.add(var1);
         return this;
      }

      public ListTag toListTag() {
         ListTag var1 = new ListTag();
         Iterator var2 = this.patterns.iterator();

         while(var2.hasNext()) {
            Pair var3 = (Pair)var2.next();
            CompoundTag var4 = new CompoundTag();
            var4.putString("Pattern", ((BannerPattern)((Holder)var3.getFirst()).value()).hashname);
            var4.putInt("Color", ((DyeColor)var3.getSecond()).getId());
            var1.add(var4);
         }

         return var1;
      }
   }
}
