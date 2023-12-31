package com.mojang.realmsclient.util;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public class RealmsTextureManager {
   private static final Map<String, RealmsTextureManager.RealmsTexture> TEXTURES = Maps.newHashMap();
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation TEMPLATE_ICON_LOCATION = new ResourceLocation("textures/gui/presets/isles.png");

   public RealmsTextureManager() {
   }

   public static ResourceLocation worldTemplate(String var0, @Nullable String var1) {
      return var1 == null ? TEMPLATE_ICON_LOCATION : getTexture(var0, var1);
   }

   private static ResourceLocation getTexture(String var0, String var1) {
      RealmsTextureManager.RealmsTexture var2 = (RealmsTextureManager.RealmsTexture)TEXTURES.get(var0);
      if (var2 != null && var2.image().equals(var1)) {
         return var2.textureId;
      } else {
         NativeImage var3 = loadImage(var1);
         ResourceLocation var4;
         if (var3 == null) {
            var4 = MissingTextureAtlasSprite.getLocation();
            TEXTURES.put(var0, new RealmsTextureManager.RealmsTexture(var1, var4));
            return var4;
         } else {
            var4 = new ResourceLocation("realms", "dynamic/" + var0);
            Minecraft.getInstance().getTextureManager().register((ResourceLocation)var4, (AbstractTexture)(new DynamicTexture(var3)));
            TEXTURES.put(var0, new RealmsTextureManager.RealmsTexture(var1, var4));
            return var4;
         }
      }
   }

   @Nullable
   private static NativeImage loadImage(String var0) {
      byte[] var1 = Base64.getDecoder().decode(var0);
      ByteBuffer var2 = MemoryUtil.memAlloc(var1.length);

      try {
         NativeImage var3 = NativeImage.read(var2.put(var1).flip());
         return var3;
      } catch (IOException var7) {
         LOGGER.warn("Failed to load world image: {}", var0, var7);
      } finally {
         MemoryUtil.memFree(var2);
      }

      return null;
   }

   public static record RealmsTexture(String a, ResourceLocation b) {
      private final String image;
      final ResourceLocation textureId;

      public RealmsTexture(String var1, ResourceLocation var2) {
         this.image = var1;
         this.textureId = var2;
      }

      public String image() {
         return this.image;
      }

      public ResourceLocation textureId() {
         return this.textureId;
      }
   }
}
