package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPredicate {
   public static final BlockPredicate ANY;
   @Nullable
   private final TagKey<Block> tag;
   @Nullable
   private final Set<Block> blocks;
   private final StatePropertiesPredicate properties;
   private final NbtPredicate nbt;

   public BlockPredicate(@Nullable TagKey<Block> var1, @Nullable Set<Block> var2, StatePropertiesPredicate var3, NbtPredicate var4) {
      this.tag = var1;
      this.blocks = var2;
      this.properties = var3;
      this.nbt = var4;
   }

   public boolean matches(ServerLevel var1, BlockPos var2) {
      if (this == ANY) {
         return true;
      } else if (!var1.isLoaded(var2)) {
         return false;
      } else {
         BlockState var3 = var1.getBlockState(var2);
         if (this.tag != null && !var3.is(this.tag)) {
            return false;
         } else if (this.blocks != null && !this.blocks.contains(var3.getBlock())) {
            return false;
         } else if (!this.properties.matches(var3)) {
            return false;
         } else {
            if (this.nbt != NbtPredicate.ANY) {
               BlockEntity var4 = var1.getBlockEntity(var2);
               if (var4 == null || !this.nbt.matches((Tag)var4.saveWithFullMetadata())) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static BlockPredicate fromJson(@Nullable JsonElement var0) {
      if (var0 != null && !var0.isJsonNull()) {
         JsonObject var1 = GsonHelper.convertToJsonObject(var0, "block");
         NbtPredicate var2 = NbtPredicate.fromJson(var1.get("nbt"));
         ImmutableSet var3 = null;
         JsonArray var4 = GsonHelper.getAsJsonArray(var1, "blocks", (JsonArray)null);
         if (var4 != null) {
            com.google.common.collect.ImmutableSet.Builder var5 = ImmutableSet.builder();
            Iterator var6 = var4.iterator();

            while(var6.hasNext()) {
               JsonElement var7 = (JsonElement)var6.next();
               ResourceLocation var8 = new ResourceLocation(GsonHelper.convertToString(var7, "block"));
               var5.add((Block)BuiltInRegistries.BLOCK.getOptional(var8).orElseThrow(() -> {
                  return new JsonSyntaxException("Unknown block id '" + var8 + "'");
               }));
            }

            var3 = var5.build();
         }

         TagKey var9 = null;
         if (var1.has("tag")) {
            ResourceLocation var10 = new ResourceLocation(GsonHelper.getAsString(var1, "tag"));
            var9 = TagKey.create(Registries.BLOCK, var10);
         }

         StatePropertiesPredicate var11 = StatePropertiesPredicate.fromJson(var1.get("state"));
         return new BlockPredicate(var9, var3, var11, var2);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject var1 = new JsonObject();
         if (this.blocks != null) {
            JsonArray var2 = new JsonArray();
            Iterator var3 = this.blocks.iterator();

            while(var3.hasNext()) {
               Block var4 = (Block)var3.next();
               var2.add(BuiltInRegistries.BLOCK.getKey(var4).toString());
            }

            var1.add("blocks", var2);
         }

         if (this.tag != null) {
            var1.addProperty("tag", this.tag.location().toString());
         }

         var1.add("nbt", this.nbt.serializeToJson());
         var1.add("state", this.properties.serializeToJson());
         return var1;
      }
   }

   static {
      ANY = new BlockPredicate((TagKey)null, (Set)null, StatePropertiesPredicate.ANY, NbtPredicate.ANY);
   }

   public static class Builder {
      @Nullable
      private Set<Block> blocks;
      @Nullable
      private TagKey<Block> tag;
      private StatePropertiesPredicate properties;
      private NbtPredicate nbt;

      private Builder() {
         this.properties = StatePropertiesPredicate.ANY;
         this.nbt = NbtPredicate.ANY;
      }

      public static BlockPredicate.Builder block() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder of(Block... var1) {
         this.blocks = ImmutableSet.copyOf(var1);
         return this;
      }

      public BlockPredicate.Builder of(Iterable<Block> var1) {
         this.blocks = ImmutableSet.copyOf(var1);
         return this;
      }

      public BlockPredicate.Builder of(TagKey<Block> var1) {
         this.tag = var1;
         return this;
      }

      public BlockPredicate.Builder hasNbt(CompoundTag var1) {
         this.nbt = new NbtPredicate(var1);
         return this;
      }

      public BlockPredicate.Builder setProperties(StatePropertiesPredicate var1) {
         this.properties = var1;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.tag, this.blocks, this.properties, this.nbt);
      }
   }
}
