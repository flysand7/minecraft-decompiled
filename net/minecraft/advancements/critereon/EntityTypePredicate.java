package net.minecraft.advancements.critereon;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;

public abstract class EntityTypePredicate {
   public static final EntityTypePredicate ANY = new EntityTypePredicate() {
      public boolean matches(EntityType<?> var1) {
         return true;
      }

      public JsonElement serializeToJson() {
         return JsonNull.INSTANCE;
      }
   };
   private static final Joiner COMMA_JOINER = Joiner.on(", ");

   public EntityTypePredicate() {
   }

   public abstract boolean matches(EntityType<?> var1);

   public abstract JsonElement serializeToJson();

   public static EntityTypePredicate fromJson(@Nullable JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         String var1 = GsonHelper.convertToString(var0, "type");
         ResourceLocation var2;
         if (var1.startsWith("#")) {
            var2 = new ResourceLocation(var1.substring(1));
            return new EntityTypePredicate.TagPredicate(TagKey.create(Registries.ENTITY_TYPE, var2));
         } else {
            var2 = new ResourceLocation(var1);
            EntityType var3 = (EntityType)BuiltInRegistries.ENTITY_TYPE.getOptional(var2).orElseThrow(() -> {
               return new JsonSyntaxException("Unknown entity type '" + var2 + "', valid types are: " + COMMA_JOINER.join(BuiltInRegistries.ENTITY_TYPE.keySet()));
            });
            return new EntityTypePredicate.TypePredicate(var3);
         }
      } else {
         return ANY;
      }
   }

   public static EntityTypePredicate of(EntityType<?> var0) {
      return new EntityTypePredicate.TypePredicate(var0);
   }

   public static EntityTypePredicate of(TagKey<EntityType<?>> var0) {
      return new EntityTypePredicate.TagPredicate(var0);
   }

   private static class TagPredicate extends EntityTypePredicate {
      private final TagKey<EntityType<?>> tag;

      public TagPredicate(TagKey<EntityType<?>> var1) {
         this.tag = var1;
      }

      public boolean matches(EntityType<?> var1) {
         return var1.is(this.tag);
      }

      public JsonElement serializeToJson() {
         return new JsonPrimitive("#" + this.tag.location());
      }
   }

   private static class TypePredicate extends EntityTypePredicate {
      private final EntityType<?> type;

      public TypePredicate(EntityType<?> var1) {
         this.type = var1;
      }

      public boolean matches(EntityType<?> var1) {
         return this.type == var1;
      }

      public JsonElement serializeToJson() {
         return new JsonPrimitive(BuiltInRegistries.ENTITY_TYPE.getKey(this.type).toString());
      }
   }
}
