package net.minecraft.advancements.critereon;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.FrogVariant;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;

public interface EntitySubPredicate {
   EntitySubPredicate ANY = new EntitySubPredicate() {
      public boolean matches(Entity var1, ServerLevel var2, @Nullable Vec3 var3) {
         return true;
      }

      public JsonObject serializeCustomData() {
         return new JsonObject();
      }

      public EntitySubPredicate.Type type() {
         return EntitySubPredicate.Types.ANY;
      }
   };

   static EntitySubPredicate fromJson(@Nullable JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(var0, "type_specific");
         String var2 = GsonHelper.getAsString(var1, "type", (String)null);
         if (var2 == null) {
            return ANY;
         } else {
            EntitySubPredicate.Type var3 = (EntitySubPredicate.Type)EntitySubPredicate.Types.TYPES.get(var2);
            if (var3 == null) {
               throw new JsonSyntaxException("Unknown sub-predicate type: " + var2);
            } else {
               return var3.deserialize(var1);
            }
         }
      } else {
         return ANY;
      }
   }

   boolean matches(Entity var1, ServerLevel var2, @Nullable Vec3 var3);

   JsonObject serializeCustomData();

   default JsonElement serialize() {
      if (this.type() == EntitySubPredicate.Types.ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject var1 = this.serializeCustomData();
         String var2 = (String)EntitySubPredicate.Types.TYPES.inverse().get(this.type());
         var1.addProperty("type", var2);
         return var1;
      }
   }

   EntitySubPredicate.Type type();

   static EntitySubPredicate variant(CatVariant var0) {
      return EntitySubPredicate.Types.CAT.createPredicate(var0);
   }

   static EntitySubPredicate variant(FrogVariant var0) {
      return EntitySubPredicate.Types.FROG.createPredicate(var0);
   }

   public static final class Types {
      public static final EntitySubPredicate.Type ANY = (var0) -> {
         return EntitySubPredicate.ANY;
      };
      public static final EntitySubPredicate.Type LIGHTNING = LighthingBoltPredicate::fromJson;
      public static final EntitySubPredicate.Type FISHING_HOOK = FishingHookPredicate::fromJson;
      public static final EntitySubPredicate.Type PLAYER = PlayerPredicate::fromJson;
      public static final EntitySubPredicate.Type SLIME = SlimePredicate::fromJson;
      public static final EntityVariantPredicate<CatVariant> CAT;
      public static final EntityVariantPredicate<FrogVariant> FROG;
      public static final EntityVariantPredicate<Axolotl.Variant> AXOLOTL;
      public static final EntityVariantPredicate<Boat.Type> BOAT;
      public static final EntityVariantPredicate<Fox.Type> FOX;
      public static final EntityVariantPredicate<MushroomCow.MushroomType> MOOSHROOM;
      public static final EntityVariantPredicate<Holder<PaintingVariant>> PAINTING;
      public static final EntityVariantPredicate<Rabbit.Variant> RABBIT;
      public static final EntityVariantPredicate<Variant> HORSE;
      public static final EntityVariantPredicate<Llama.Variant> LLAMA;
      public static final EntityVariantPredicate<VillagerType> VILLAGER;
      public static final EntityVariantPredicate<Parrot.Variant> PARROT;
      public static final EntityVariantPredicate<TropicalFish.Pattern> TROPICAL_FISH;
      public static final BiMap<String, EntitySubPredicate.Type> TYPES;

      public Types() {
      }

      static {
         CAT = EntityVariantPredicate.create(BuiltInRegistries.CAT_VARIANT, (var0) -> {
            Optional var10000;
            if (var0 instanceof Cat) {
               Cat var1 = (Cat)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         FROG = EntityVariantPredicate.create(BuiltInRegistries.FROG_VARIANT, (var0) -> {
            Optional var10000;
            if (var0 instanceof Frog) {
               Frog var1 = (Frog)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         AXOLOTL = EntityVariantPredicate.create(Axolotl.Variant.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Axolotl) {
               Axolotl var1 = (Axolotl)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         BOAT = EntityVariantPredicate.create((Codec)Boat.Type.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Boat) {
               Boat var1 = (Boat)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         FOX = EntityVariantPredicate.create((Codec)Fox.Type.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Fox) {
               Fox var1 = (Fox)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         MOOSHROOM = EntityVariantPredicate.create((Codec)MushroomCow.MushroomType.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof MushroomCow) {
               MushroomCow var1 = (MushroomCow)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         PAINTING = EntityVariantPredicate.create(BuiltInRegistries.PAINTING_VARIANT.holderByNameCodec(), (var0) -> {
            Optional var10000;
            if (var0 instanceof Painting) {
               Painting var1 = (Painting)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         RABBIT = EntityVariantPredicate.create(Rabbit.Variant.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Rabbit) {
               Rabbit var1 = (Rabbit)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         HORSE = EntityVariantPredicate.create(Variant.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Horse) {
               Horse var1 = (Horse)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         LLAMA = EntityVariantPredicate.create(Llama.Variant.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Llama) {
               Llama var1 = (Llama)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         VILLAGER = EntityVariantPredicate.create(BuiltInRegistries.VILLAGER_TYPE.byNameCodec(), (var0) -> {
            Optional var10000;
            if (var0 instanceof VillagerDataHolder) {
               VillagerDataHolder var1 = (VillagerDataHolder)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         PARROT = EntityVariantPredicate.create(Parrot.Variant.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof Parrot) {
               Parrot var1 = (Parrot)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         TROPICAL_FISH = EntityVariantPredicate.create(TropicalFish.Pattern.CODEC, (var0) -> {
            Optional var10000;
            if (var0 instanceof TropicalFish) {
               TropicalFish var1 = (TropicalFish)var0;
               var10000 = Optional.of(var1.getVariant());
            } else {
               var10000 = Optional.empty();
            }

            return var10000;
         });
         TYPES = ImmutableBiMap.builder().put("any", ANY).put("lightning", LIGHTNING).put("fishing_hook", FISHING_HOOK).put("player", PLAYER).put("slime", SLIME).put("cat", CAT.type()).put("frog", FROG.type()).put("axolotl", AXOLOTL.type()).put("boat", BOAT.type()).put("fox", FOX.type()).put("mooshroom", MOOSHROOM.type()).put("painting", PAINTING.type()).put("rabbit", RABBIT.type()).put("horse", HORSE.type()).put("llama", LLAMA.type()).put("villager", VILLAGER.type()).put("parrot", PARROT.type()).put("tropical_fish", TROPICAL_FISH.type()).buildOrThrow();
      }
   }

   public interface Type {
      EntitySubPredicate deserialize(JsonObject var1);
   }
}
