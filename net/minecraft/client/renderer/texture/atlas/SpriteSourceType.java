package net.minecraft.client.renderer.texture.atlas;

import com.mojang.serialization.Codec;

public record SpriteSourceType(Codec<? extends SpriteSource> a) {
   private final Codec<? extends SpriteSource> codec;

   public SpriteSourceType(Codec<? extends SpriteSource> var1) {
      this.codec = var1;
   }

   public Codec<? extends SpriteSource> codec() {
      return this.codec;
   }
}
