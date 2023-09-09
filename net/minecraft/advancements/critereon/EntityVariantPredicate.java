package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVariantPredicate<V> {
   private static final String VARIANT_KEY = "variant";
   final Codec<V> variantCodec;
   final Function<Entity, Optional<V>> getter;
   final EntitySubPredicate.Type type;

   public static <V> EntityVariantPredicate<V> create(Registry<V> var0, Function<Entity, Optional<V>> var1) {
      return new EntityVariantPredicate(var0.byNameCodec(), var1);
   }

   public static <V> EntityVariantPredicate<V> create(Codec<V> var0, Function<Entity, Optional<V>> var1) {
      return new EntityVariantPredicate(var0, var1);
   }

   private EntityVariantPredicate(Codec<V> var1, Function<Entity, Optional<V>> var2) {
      this.variantCodec = var1;
      this.getter = var2;
      this.type = (var2x) -> {
         JsonElement var3 = var2x.get("variant");
         if (var3 == null) {
            throw new JsonParseException("Missing variant field");
         } else {
            Object var4 = ((Pair)Util.getOrThrow(var1.decode(new Dynamic(JsonOps.INSTANCE, var3)), JsonParseException::new)).getFirst();
            return this.createPredicate(var4);
         }
      };
   }

   public EntitySubPredicate.Type type() {
      return this.type;
   }

   public EntitySubPredicate createPredicate(final V var1) {
      return new EntitySubPredicate() {
         public boolean matches(Entity var1x, ServerLevel var2, @Nullable Vec3 var3) {
            return ((Optional)EntityVariantPredicate.this.getter.apply(var1x)).filter((var1xx) -> {
               return var1xx.equals(var1);
            }).isPresent();
         }

         public JsonObject serializeCustomData() {
            JsonObject var1x = new JsonObject();
            var1x.add("variant", (JsonElement)Util.getOrThrow(EntityVariantPredicate.this.variantCodec.encodeStart(JsonOps.INSTANCE, var1), (var1xx) -> {
               return new JsonParseException("Can't serialize variant " + var1 + ", message " + var1xx);
            }));
            return var1x;
         }

         public EntitySubPredicate.Type type() {
            return EntityVariantPredicate.this.type;
         }
      };
   }
}
