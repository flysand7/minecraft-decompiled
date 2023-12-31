package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

public class SimpleTexture extends AbstractTexture {
   static final Logger LOGGER = LogUtils.getLogger();
   protected final ResourceLocation location;

   public SimpleTexture(ResourceLocation var1) {
      this.location = var1;
   }

   public void load(ResourceManager var1) throws IOException {
      SimpleTexture.TextureImage var2 = this.getTextureImage(var1);
      var2.throwIfError();
      TextureMetadataSection var5 = var2.getTextureMetadata();
      boolean var3;
      boolean var4;
      if (var5 != null) {
         var3 = var5.isBlur();
         var4 = var5.isClamp();
      } else {
         var3 = false;
         var4 = false;
      }

      NativeImage var6 = var2.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> {
            this.doLoad(var6, var3, var4);
         });
      } else {
         this.doLoad(var6, var3, var4);
      }

   }

   private void doLoad(NativeImage var1, boolean var2, boolean var3) {
      TextureUtil.prepareImage(this.getId(), 0, var1.getWidth(), var1.getHeight());
      var1.upload(0, 0, 0, 0, 0, var1.getWidth(), var1.getHeight(), var2, var3, false, true);
   }

   protected SimpleTexture.TextureImage getTextureImage(ResourceManager var1) {
      return SimpleTexture.TextureImage.load(var1, this.location);
   }

   protected static class TextureImage implements Closeable {
      @Nullable
      private final TextureMetadataSection metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureImage(IOException var1) {
         this.exception = var1;
         this.metadata = null;
         this.image = null;
      }

      public TextureImage(@Nullable TextureMetadataSection var1, NativeImage var2) {
         this.exception = null;
         this.metadata = var1;
         this.image = var2;
      }

      public static SimpleTexture.TextureImage load(ResourceManager var0, ResourceLocation var1) {
         try {
            Resource var2 = var0.getResourceOrThrow(var1);
            InputStream var4 = var2.open();

            NativeImage var3;
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

            TextureMetadataSection var11 = null;

            try {
               var11 = (TextureMetadataSection)var2.metadata().getSection(TextureMetadataSection.SERIALIZER).orElse((Object)null);
            } catch (RuntimeException var8) {
               SimpleTexture.LOGGER.warn("Failed reading metadata of: {}", var1, var8);
            }

            return new SimpleTexture.TextureImage(var11, var3);
         } catch (IOException var10) {
            return new SimpleTexture.TextureImage(var10);
         }
      }

      @Nullable
      public TextureMetadataSection getTextureMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      public void close() {
         if (this.image != null) {
            this.image.close();
         }

      }

      public void throwIfError() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
