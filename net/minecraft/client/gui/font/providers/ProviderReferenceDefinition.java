package net.minecraft.client.gui.font.providers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public record ProviderReferenceDefinition(ResourceLocation c) implements GlyphProviderDefinition {
   private final ResourceLocation id;
   public static final MapCodec<ProviderReferenceDefinition> CODEC = RecordCodecBuilder.mapCodec((var0) -> {
      return var0.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ProviderReferenceDefinition::id)).apply(var0, ProviderReferenceDefinition::new);
   });

   public ProviderReferenceDefinition(ResourceLocation var1) {
      this.id = var1;
   }

   public GlyphProviderType type() {
      return GlyphProviderType.REFERENCE;
   }

   public Either<GlyphProviderDefinition.Loader, GlyphProviderDefinition.Reference> unpack() {
      return Either.right(new GlyphProviderDefinition.Reference(this.id));
   }

   public ResourceLocation id() {
      return this.id;
   }
}
