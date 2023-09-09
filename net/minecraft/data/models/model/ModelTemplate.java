package net.minecraft.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class ModelTemplate {
   private final Optional<ResourceLocation> model;
   private final Set<TextureSlot> requiredSlots;
   private final Optional<String> suffix;

   public ModelTemplate(Optional<ResourceLocation> var1, Optional<String> var2, TextureSlot... var3) {
      this.model = var1;
      this.suffix = var2;
      this.requiredSlots = ImmutableSet.copyOf(var3);
   }

   public ResourceLocation create(Block var1, TextureMapping var2, BiConsumer<ResourceLocation, Supplier<JsonElement>> var3) {
      return this.create(ModelLocationUtils.getModelLocation(var1, (String)this.suffix.orElse("")), var2, var3);
   }

   public ResourceLocation createWithSuffix(Block var1, String var2, TextureMapping var3, BiConsumer<ResourceLocation, Supplier<JsonElement>> var4) {
      return this.create(ModelLocationUtils.getModelLocation(var1, var2 + (String)this.suffix.orElse("")), var3, var4);
   }

   public ResourceLocation createWithOverride(Block var1, String var2, TextureMapping var3, BiConsumer<ResourceLocation, Supplier<JsonElement>> var4) {
      return this.create(ModelLocationUtils.getModelLocation(var1, var2), var3, var4);
   }

   public ResourceLocation create(ResourceLocation var1, TextureMapping var2, BiConsumer<ResourceLocation, Supplier<JsonElement>> var3) {
      return this.create(var1, var2, var3, this::createBaseTemplate);
   }

   public ResourceLocation create(ResourceLocation var1, TextureMapping var2, BiConsumer<ResourceLocation, Supplier<JsonElement>> var3, ModelTemplate.JsonFactory var4) {
      Map var5 = this.createMap(var2);
      var3.accept(var1, () -> {
         return var4.create(var1, var5);
      });
      return var1;
   }

   public JsonObject createBaseTemplate(ResourceLocation var1, Map<TextureSlot, ResourceLocation> var2) {
      JsonObject var3 = new JsonObject();
      this.model.ifPresent((var1x) -> {
         var3.addProperty("parent", var1x.toString());
      });
      if (!var2.isEmpty()) {
         JsonObject var4 = new JsonObject();
         var2.forEach((var1x, var2x) -> {
            var4.addProperty(var1x.getId(), var2x.toString());
         });
         var3.add("textures", var4);
      }

      return var3;
   }

   private Map<TextureSlot, ResourceLocation> createMap(TextureMapping var1) {
      Stream var10000 = Streams.concat(new Stream[]{this.requiredSlots.stream(), var1.getForced()});
      Function var10001 = Function.identity();
      Objects.requireNonNull(var1);
      return (Map)var10000.collect(ImmutableMap.toImmutableMap(var10001, var1::get));
   }

   public interface JsonFactory {
      JsonObject create(ResourceLocation var1, Map<TextureSlot, ResourceLocation> var2);
   }
}
