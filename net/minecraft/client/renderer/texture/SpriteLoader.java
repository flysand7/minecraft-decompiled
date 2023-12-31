package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.slf4j.Logger;

public class SpriteLoader {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final ResourceLocation location;
   private final int maxSupportedTextureSize;
   private final int minWidth;
   private final int minHeight;

   public SpriteLoader(ResourceLocation var1, int var2, int var3, int var4) {
      this.location = var1;
      this.maxSupportedTextureSize = var2;
      this.minWidth = var3;
      this.minHeight = var4;
   }

   public static SpriteLoader create(TextureAtlas var0) {
      return new SpriteLoader(var0.location(), var0.maxSupportedTextureSize(), var0.getWidth(), var0.getHeight());
   }

   public SpriteLoader.Preparations stitch(List<SpriteContents> var1, int var2, Executor var3) {
      int var4 = this.maxSupportedTextureSize;
      Stitcher var5 = new Stitcher(var4, var4, var2);
      int var6 = Integer.MAX_VALUE;
      int var7 = 1 << var2;

      SpriteContents var9;
      int var10;
      for(Iterator var8 = var1.iterator(); var8.hasNext(); var5.registerSprite(var9)) {
         var9 = (SpriteContents)var8.next();
         var6 = Math.min(var6, Math.min(var9.width(), var9.height()));
         var10 = Math.min(Integer.lowestOneBit(var9.width()), Integer.lowestOneBit(var9.height()));
         if (var10 < var7) {
            LOGGER.warn("Texture {} with size {}x{} limits mip level from {} to {}", new Object[]{var9.name(), var9.width(), var9.height(), Mth.log2(var7), Mth.log2(var10)});
            var7 = var10;
         }
      }

      int var17 = Math.min(var6, var7);
      int var18 = Mth.log2(var17);
      if (var18 < var2) {
         LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", new Object[]{this.location, var2, var18, var17});
         var10 = var18;
      } else {
         var10 = var2;
      }

      try {
         var5.stitch();
      } catch (StitcherException var16) {
         CrashReport var12 = CrashReport.forThrowable(var16, "Stitching");
         CrashReportCategory var13 = var12.addCategory("Stitcher");
         var13.setDetail("Sprites", var16.getAllSprites().stream().map((var0) -> {
            return String.format(Locale.ROOT, "%s[%dx%d]", var0.name(), var0.width(), var0.height());
         }).collect(Collectors.joining(",")));
         var13.setDetail("Max Texture Size", (Object)var4);
         throw new ReportedException(var12);
      }

      int var11 = Math.max(var5.getWidth(), this.minWidth);
      int var19 = Math.max(var5.getHeight(), this.minHeight);
      Map var20 = this.getStitchedSprites(var5, var11, var19);
      TextureAtlasSprite var14 = (TextureAtlasSprite)var20.get(MissingTextureAtlasSprite.getLocation());
      CompletableFuture var15;
      if (var10 > 0) {
         var15 = CompletableFuture.runAsync(() -> {
            var20.values().forEach((var1) -> {
               var1.contents().increaseMipLevel(var10);
            });
         }, var3);
      } else {
         var15 = CompletableFuture.completedFuture((Object)null);
      }

      return new SpriteLoader.Preparations(var11, var19, var10, var14, var20, var15);
   }

   public static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(List<Supplier<SpriteContents>> var0, Executor var1) {
      List var2 = var0.stream().map((var1x) -> {
         return CompletableFuture.supplyAsync(var1x, var1);
      }).toList();
      return Util.sequence(var2).thenApply((var0x) -> {
         return var0x.stream().filter(Objects::nonNull).toList();
      });
   }

   public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(ResourceManager var1, ResourceLocation var2, int var3, Executor var4) {
      return CompletableFuture.supplyAsync(() -> {
         return SpriteResourceLoader.load(var1, var2).list(var1);
      }, var4).thenCompose((var1x) -> {
         return runSpriteSuppliers(var1x, var4);
      }).thenApply((var3x) -> {
         return this.stitch(var3x, var3, var4);
      });
   }

   @Nullable
   public static SpriteContents loadSprite(ResourceLocation var0, Resource var1) {
      AnimationMetadataSection var2;
      try {
         var2 = (AnimationMetadataSection)var1.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(AnimationMetadataSection.EMPTY);
      } catch (Exception var8) {
         LOGGER.error("Unable to parse metadata from {}", var0, var8);
         return null;
      }

      NativeImage var3;
      try {
         InputStream var4 = var1.open();

         try {
            var3 = NativeImage.read(var4);
         } catch (Throwable var9) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var9.addSuppressed(var7);
               }
            }

            throw var9;
         }

         if (var4 != null) {
            var4.close();
         }
      } catch (IOException var10) {
         LOGGER.error("Using missing texture, unable to load {}", var0, var10);
         return null;
      }

      FrameSize var11 = var2.calculateFrameSize(var3.getWidth(), var3.getHeight());
      if (Mth.isMultipleOf(var3.getWidth(), var11.width()) && Mth.isMultipleOf(var3.getHeight(), var11.height())) {
         return new SpriteContents(var0, var11, var3, var2);
      } else {
         LOGGER.error("Image {} size {},{} is not multiple of frame size {},{}", new Object[]{var0, var3.getWidth(), var3.getHeight(), var11.width(), var11.height()});
         var3.close();
         return null;
      }
   }

   private Map<ResourceLocation, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> var1, int var2, int var3) {
      HashMap var4 = new HashMap();
      var1.gatherSprites((var4x, var5, var6) -> {
         var4.put(var4x.name(), new TextureAtlasSprite(this.location, var4x, var2, var3, var5, var6));
      });
      return var4;
   }

   public static record Preparations(int a, int b, int c, TextureAtlasSprite d, Map<ResourceLocation, TextureAtlasSprite> e, CompletableFuture<Void> f) {
      private final int width;
      private final int height;
      private final int mipLevel;
      private final TextureAtlasSprite missing;
      private final Map<ResourceLocation, TextureAtlasSprite> regions;
      private final CompletableFuture<Void> readyForUpload;

      public Preparations(int var1, int var2, int var3, TextureAtlasSprite var4, Map<ResourceLocation, TextureAtlasSprite> var5, CompletableFuture<Void> var6) {
         this.width = var1;
         this.height = var2;
         this.mipLevel = var3;
         this.missing = var4;
         this.regions = var5;
         this.readyForUpload = var6;
      }

      public CompletableFuture<SpriteLoader.Preparations> waitForUpload() {
         return this.readyForUpload.thenApply((var1) -> {
            return this;
         });
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }

      public int mipLevel() {
         return this.mipLevel;
      }

      public TextureAtlasSprite missing() {
         return this.missing;
      }

      public Map<ResourceLocation, TextureAtlasSprite> regions() {
         return this.regions;
      }

      public CompletableFuture<Void> readyForUpload() {
         return this.readyForUpload;
      }
   }
}
