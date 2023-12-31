package net.minecraft.client.gui.font.providers;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryUtil;

public record TrueTypeGlyphProviderDefinition(ResourceLocation c, float d, float e, TrueTypeGlyphProviderDefinition.Shift f, String g) implements GlyphProviderDefinition {
   private final ResourceLocation location;
   private final float size;
   private final float oversample;
   private final TrueTypeGlyphProviderDefinition.Shift shift;
   private final String skip;
   private static final Codec<String> SKIP_LIST_CODEC;
   public static final MapCodec<TrueTypeGlyphProviderDefinition> CODEC;

   public TrueTypeGlyphProviderDefinition(ResourceLocation var1, float var2, float var3, TrueTypeGlyphProviderDefinition.Shift var4, String var5) {
      this.location = var1;
      this.size = var2;
      this.oversample = var3;
      this.shift = var4;
      this.skip = var5;
   }

   public GlyphProviderType type() {
      return GlyphProviderType.TTF;
   }

   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.left(this::load);
   }

   private GlyphProvider load(ResourceManager var1) throws IOException {
      STBTTFontinfo var2 = null;
      ByteBuffer var3 = null;

      try {
         InputStream var4 = var1.open(this.location.withPrefix("font/"));

         TrueTypeGlyphProvider var5;
         try {
            var2 = STBTTFontinfo.malloc();
            var3 = TextureUtil.readResource(var4);
            var3.flip();
            if (!STBTruetype.stbtt_InitFont(var2, var3)) {
               throw new IOException("Invalid ttf");
            }

            var5 = new TrueTypeGlyphProvider(var3, var2, this.size, this.oversample, this.shift.x, this.shift.y, this.skip);
         } catch (Throwable var8) {
            if (var4 != null) {
               try {
                  var4.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (var4 != null) {
            var4.close();
         }

         return var5;
      } catch (Exception var9) {
         if (var2 != null) {
            var2.free();
         }

         MemoryUtil.memFree(var3);
         throw var9;
      }
   }

   public ResourceLocation location() {
      return this.location;
   }

   public float size() {
      return this.size;
   }

   public float oversample() {
      return this.oversample;
   }

   public TrueTypeGlyphProviderDefinition.Shift shift() {
      return this.shift;
   }

   public String skip() {
      return this.skip;
   }

   static {
      SKIP_LIST_CODEC = Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap((var0) -> {
         return (String)var0.map((var0x) -> {
            return var0x;
         }, (var0x) -> {
            return String.join("", var0x);
         });
      }, Either::left);
      CODEC = RecordCodecBuilder.mapCodec((var0) -> {
         return var0.group(ResourceLocation.CODEC.fieldOf("file").forGetter(TrueTypeGlyphProviderDefinition::location), Codec.FLOAT.optionalFieldOf("size", 11.0F).forGetter(TrueTypeGlyphProviderDefinition::size), Codec.FLOAT.optionalFieldOf("oversample", 1.0F).forGetter(TrueTypeGlyphProviderDefinition::oversample), TrueTypeGlyphProviderDefinition.Shift.CODEC.optionalFieldOf("shift", TrueTypeGlyphProviderDefinition.Shift.NONE).forGetter(TrueTypeGlyphProviderDefinition::shift), SKIP_LIST_CODEC.optionalFieldOf("skip", "").forGetter(TrueTypeGlyphProviderDefinition::skip)).apply(var0, TrueTypeGlyphProviderDefinition::new);
      });
   }

   public static record Shift(float c, float d) {
      final float x;
      final float y;
      public static final TrueTypeGlyphProviderDefinition.Shift NONE = new TrueTypeGlyphProviderDefinition.Shift(0.0F, 0.0F);
      public static final Codec<TrueTypeGlyphProviderDefinition.Shift> CODEC;

      public Shift(float var1, float var2) {
         this.x = var1;
         this.y = var2;
      }

      public float x() {
         return this.x;
      }

      public float y() {
         return this.y;
      }

      static {
         CODEC = Codec.FLOAT.listOf().comapFlatMap((var0) -> {
            return Util.fixedSize((List)var0, 2).map((var0x) -> {
               return new TrueTypeGlyphProviderDefinition.Shift((Float)var0x.get(0), (Float)var0x.get(1));
            });
         }, (var0) -> {
            return List.of(var0.x, var0.y);
         });
      }
   }
}
