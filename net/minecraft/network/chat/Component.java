package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.Message;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.network.chat.contents.BlockDataSource;
import net.minecraft.network.chat.contents.DataSource;
import net.minecraft.network.chat.contents.EntityDataSource;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.StorageDataSource;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.LowerCaseEnumTypeAdapterFactory;

public interface Component extends Message, FormattedText {
   Style getStyle();

   ComponentContents getContents();

   default String getString() {
      return FormattedText.super.getString();
   }

   default String getString(int var1) {
      StringBuilder var2 = new StringBuilder();
      this.visit((var2x) -> {
         int var3 = var1 - var2.length();
         if (var3 <= 0) {
            return STOP_ITERATION;
         } else {
            var2.append(var2x.length() <= var3 ? var2x : var2x.substring(0, var3));
            return Optional.empty();
         }
      });
      return var2.toString();
   }

   List<Component> getSiblings();

   default MutableComponent plainCopy() {
      return MutableComponent.create(this.getContents());
   }

   default MutableComponent copy() {
      return new MutableComponent(this.getContents(), new ArrayList(this.getSiblings()), this.getStyle());
   }

   FormattedCharSequence getVisualOrderText();

   default <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> var1, Style var2) {
      Style var3 = this.getStyle().applyTo(var2);
      Optional var4 = this.getContents().visit(var1, var3);
      if (var4.isPresent()) {
         return var4;
      } else {
         Iterator var5 = this.getSiblings().iterator();

         Optional var7;
         do {
            if (!var5.hasNext()) {
               return Optional.empty();
            }

            Component var6 = (Component)var5.next();
            var7 = var6.visit(var1, var3);
         } while(!var7.isPresent());

         return var7;
      }
   }

   default <T> Optional<T> visit(FormattedText.ContentConsumer<T> var1) {
      Optional var2 = this.getContents().visit(var1);
      if (var2.isPresent()) {
         return var2;
      } else {
         Iterator var3 = this.getSiblings().iterator();

         Optional var5;
         do {
            if (!var3.hasNext()) {
               return Optional.empty();
            }

            Component var4 = (Component)var3.next();
            var5 = var4.visit(var1);
         } while(!var5.isPresent());

         return var5;
      }
   }

   default List<Component> toFlatList() {
      return this.toFlatList(Style.EMPTY);
   }

   default List<Component> toFlatList(Style var1) {
      ArrayList var2 = Lists.newArrayList();
      this.visit((var1x, var2x) -> {
         if (!var2x.isEmpty()) {
            var2.add(literal(var2x).withStyle(var1x));
         }

         return Optional.empty();
      }, var1);
      return var2;
   }

   default boolean contains(Component var1) {
      if (this.equals(var1)) {
         return true;
      } else {
         List var2 = this.toFlatList();
         List var3 = var1.toFlatList(this.getStyle());
         return Collections.indexOfSubList(var2, var3) != -1;
      }
   }

   static Component nullToEmpty(@Nullable String var0) {
      return (Component)(var0 != null ? literal(var0) : CommonComponents.EMPTY);
   }

   static MutableComponent literal(String var0) {
      return MutableComponent.create(new LiteralContents(var0));
   }

   static MutableComponent translatable(String var0) {
      return MutableComponent.create(new TranslatableContents(var0, (String)null, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatable(String var0, Object... var1) {
      return MutableComponent.create(new TranslatableContents(var0, (String)null, var1));
   }

   static MutableComponent translatableWithFallback(String var0, @Nullable String var1) {
      return MutableComponent.create(new TranslatableContents(var0, var1, TranslatableContents.NO_ARGS));
   }

   static MutableComponent translatableWithFallback(String var0, @Nullable String var1, Object... var2) {
      return MutableComponent.create(new TranslatableContents(var0, var1, var2));
   }

   static MutableComponent empty() {
      return MutableComponent.create(ComponentContents.EMPTY);
   }

   static MutableComponent keybind(String var0) {
      return MutableComponent.create(new KeybindContents(var0));
   }

   static MutableComponent nbt(String var0, boolean var1, Optional<Component> var2, DataSource var3) {
      return MutableComponent.create(new NbtContents(var0, var1, var2, var3));
   }

   static MutableComponent score(String var0, String var1) {
      return MutableComponent.create(new ScoreContents(var0, var1));
   }

   static MutableComponent selector(String var0, Optional<Component> var1) {
      return MutableComponent.create(new SelectorContents(var0, var1));
   }

   public static class Serializer implements JsonDeserializer<MutableComponent>, JsonSerializer<Component> {
      private static final Gson GSON = (Gson)Util.make(() -> {
         GsonBuilder var0 = new GsonBuilder();
         var0.disableHtmlEscaping();
         var0.registerTypeHierarchyAdapter(Component.class, new Component.Serializer());
         var0.registerTypeHierarchyAdapter(Style.class, new Style.Serializer());
         var0.registerTypeAdapterFactory(new LowerCaseEnumTypeAdapterFactory());
         return var0.create();
      });
      private static final Field JSON_READER_POS = (Field)Util.make(() -> {
         try {
            new JsonReader(new StringReader(""));
            Field var0 = JsonReader.class.getDeclaredField("pos");
            var0.setAccessible(true);
            return var0;
         } catch (NoSuchFieldException var1) {
            throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
         }
      });
      private static final Field JSON_READER_LINESTART = (Field)Util.make(() -> {
         try {
            new JsonReader(new StringReader(""));
            Field var0 = JsonReader.class.getDeclaredField("lineStart");
            var0.setAccessible(true);
            return var0;
         } catch (NoSuchFieldException var1) {
            throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
         }
      });

      public Serializer() {
      }

      public MutableComponent deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         if (var1.isJsonPrimitive()) {
            return Component.literal(var1.getAsString());
         } else {
            MutableComponent var5;
            if (!var1.isJsonObject()) {
               if (var1.isJsonArray()) {
                  JsonArray var11 = var1.getAsJsonArray();
                  var5 = null;
                  Iterator var17 = var11.iterator();

                  while(var17.hasNext()) {
                     JsonElement var18 = (JsonElement)var17.next();
                     MutableComponent var20 = this.deserialize(var18, var18.getClass(), var3);
                     if (var5 == null) {
                        var5 = var20;
                     } else {
                        var5.append((Component)var20);
                     }
                  }

                  return var5;
               } else {
                  throw new JsonParseException("Don't know how to turn " + var1 + " into a Component");
               }
            } else {
               JsonObject var4 = var1.getAsJsonObject();
               String var12;
               if (var4.has("text")) {
                  var12 = GsonHelper.getAsString(var4, "text");
                  var5 = var12.isEmpty() ? Component.empty() : Component.literal(var12);
               } else if (var4.has("translate")) {
                  var12 = GsonHelper.getAsString(var4, "translate");
                  String var14 = GsonHelper.getAsString(var4, "fallback", (String)null);
                  if (var4.has("with")) {
                     JsonArray var19 = GsonHelper.getAsJsonArray(var4, "with");
                     Object[] var21 = new Object[var19.size()];

                     for(int var10 = 0; var10 < var21.length; ++var10) {
                        var21[var10] = unwrapTextArgument(this.deserialize(var19.get(var10), var2, var3));
                     }

                     var5 = Component.translatableWithFallback(var12, var14, var21);
                  } else {
                     var5 = Component.translatableWithFallback(var12, var14);
                  }
               } else if (var4.has("score")) {
                  JsonObject var13 = GsonHelper.getAsJsonObject(var4, "score");
                  if (!var13.has("name") || !var13.has("objective")) {
                     throw new JsonParseException("A score component needs a least a name and an objective");
                  }

                  var5 = Component.score(GsonHelper.getAsString(var13, "name"), GsonHelper.getAsString(var13, "objective"));
               } else if (var4.has("selector")) {
                  Optional var6 = this.parseSeparator(var2, var3, var4);
                  var5 = Component.selector(GsonHelper.getAsString(var4, "selector"), var6);
               } else if (var4.has("keybind")) {
                  var5 = Component.keybind(GsonHelper.getAsString(var4, "keybind"));
               } else {
                  if (!var4.has("nbt")) {
                     throw new JsonParseException("Don't know how to turn " + var1 + " into a Component");
                  }

                  var12 = GsonHelper.getAsString(var4, "nbt");
                  Optional var7 = this.parseSeparator(var2, var3, var4);
                  boolean var8 = GsonHelper.getAsBoolean(var4, "interpret", false);
                  Object var9;
                  if (var4.has("block")) {
                     var9 = new BlockDataSource(GsonHelper.getAsString(var4, "block"));
                  } else if (var4.has("entity")) {
                     var9 = new EntityDataSource(GsonHelper.getAsString(var4, "entity"));
                  } else {
                     if (!var4.has("storage")) {
                        throw new JsonParseException("Don't know how to turn " + var1 + " into a Component");
                     }

                     var9 = new StorageDataSource(new ResourceLocation(GsonHelper.getAsString(var4, "storage")));
                  }

                  var5 = Component.nbt(var12, var8, var7, (DataSource)var9);
               }

               if (var4.has("extra")) {
                  JsonArray var15 = GsonHelper.getAsJsonArray(var4, "extra");
                  if (var15.size() <= 0) {
                     throw new JsonParseException("Unexpected empty array of components");
                  }

                  for(int var16 = 0; var16 < var15.size(); ++var16) {
                     var5.append((Component)this.deserialize(var15.get(var16), var2, var3));
                  }
               }

               var5.setStyle((Style)var3.deserialize(var1, Style.class));
               return var5;
            }
         }
      }

      private static Object unwrapTextArgument(Object var0) {
         if (var0 instanceof Component) {
            Component var1 = (Component)var0;
            if (var1.getStyle().isEmpty() && var1.getSiblings().isEmpty()) {
               ComponentContents var2 = var1.getContents();
               if (var2 instanceof LiteralContents) {
                  LiteralContents var3 = (LiteralContents)var2;
                  return var3.text();
               }
            }
         }

         return var0;
      }

      private Optional<Component> parseSeparator(Type var1, JsonDeserializationContext var2, JsonObject var3) {
         return var3.has("separator") ? Optional.of(this.deserialize(var3.get("separator"), var1, var2)) : Optional.empty();
      }

      private void serializeStyle(Style var1, JsonObject var2, JsonSerializationContext var3) {
         JsonElement var4 = var3.serialize(var1);
         if (var4.isJsonObject()) {
            JsonObject var5 = (JsonObject)var4;
            Iterator var6 = var5.entrySet().iterator();

            while(var6.hasNext()) {
               Entry var7 = (Entry)var6.next();
               var2.add((String)var7.getKey(), (JsonElement)var7.getValue());
            }
         }

      }

      public JsonElement serialize(Component var1, Type var2, JsonSerializationContext var3) {
         JsonObject var4 = new JsonObject();
         if (!var1.getStyle().isEmpty()) {
            this.serializeStyle(var1.getStyle(), var4, var3);
         }

         if (!var1.getSiblings().isEmpty()) {
            JsonArray var5 = new JsonArray();
            Iterator var6 = var1.getSiblings().iterator();

            while(var6.hasNext()) {
               Component var7 = (Component)var6.next();
               var5.add(this.serialize((Component)var7, Component.class, var3));
            }

            var4.add("extra", var5);
         }

         ComponentContents var18 = var1.getContents();
         if (var18 == ComponentContents.EMPTY) {
            var4.addProperty("text", "");
         } else if (var18 instanceof LiteralContents) {
            LiteralContents var20 = (LiteralContents)var18;
            var4.addProperty("text", var20.text());
         } else if (var18 instanceof TranslatableContents) {
            TranslatableContents var8 = (TranslatableContents)var18;
            var4.addProperty("translate", var8.getKey());
            String var12 = var8.getFallback();
            if (var12 != null) {
               var4.addProperty("fallback", var12);
            }

            if (var8.getArgs().length > 0) {
               JsonArray var13 = new JsonArray();
               Object[] var14 = var8.getArgs();
               int var15 = var14.length;

               for(int var16 = 0; var16 < var15; ++var16) {
                  Object var17 = var14[var16];
                  if (var17 instanceof Component) {
                     var13.add(this.serialize((Component)((Component)var17), var17.getClass(), var3));
                  } else {
                     var13.add(new JsonPrimitive(String.valueOf(var17)));
                  }
               }

               var4.add("with", var13);
            }
         } else if (var18 instanceof ScoreContents) {
            ScoreContents var9 = (ScoreContents)var18;
            JsonObject var21 = new JsonObject();
            var21.addProperty("name", var9.getName());
            var21.addProperty("objective", var9.getObjective());
            var4.add("score", var21);
         } else if (var18 instanceof SelectorContents) {
            SelectorContents var10 = (SelectorContents)var18;
            var4.addProperty("selector", var10.getPattern());
            this.serializeSeparator(var3, var4, var10.getSeparator());
         } else if (var18 instanceof KeybindContents) {
            KeybindContents var11 = (KeybindContents)var18;
            var4.addProperty("keybind", var11.getName());
         } else {
            if (!(var18 instanceof NbtContents)) {
               throw new IllegalArgumentException("Don't know how to serialize " + var18 + " as a Component");
            }

            NbtContents var19 = (NbtContents)var18;
            var4.addProperty("nbt", var19.getNbtPath());
            var4.addProperty("interpret", var19.isInterpreting());
            this.serializeSeparator(var3, var4, var19.getSeparator());
            DataSource var22 = var19.getDataSource();
            if (var22 instanceof BlockDataSource) {
               BlockDataSource var24 = (BlockDataSource)var22;
               var4.addProperty("block", var24.posPattern());
            } else if (var22 instanceof EntityDataSource) {
               EntityDataSource var25 = (EntityDataSource)var22;
               var4.addProperty("entity", var25.selectorPattern());
            } else {
               if (!(var22 instanceof StorageDataSource)) {
                  throw new IllegalArgumentException("Don't know how to serialize " + var18 + " as a Component");
               }

               StorageDataSource var23 = (StorageDataSource)var22;
               var4.addProperty("storage", var23.id().toString());
            }
         }

         return var4;
      }

      private void serializeSeparator(JsonSerializationContext var1, JsonObject var2, Optional<Component> var3) {
         var3.ifPresent((var3x) -> {
            var2.add("separator", this.serialize((Component)var3x, var3x.getClass(), var1));
         });
      }

      public static String toJson(Component var0) {
         return GSON.toJson(var0);
      }

      public static String toStableJson(Component var0) {
         return GsonHelper.toStableString(toJsonTree(var0));
      }

      public static JsonElement toJsonTree(Component var0) {
         return GSON.toJsonTree(var0);
      }

      @Nullable
      public static MutableComponent fromJson(String var0) {
         return (MutableComponent)GsonHelper.fromNullableJson(GSON, var0, MutableComponent.class, false);
      }

      @Nullable
      public static MutableComponent fromJson(JsonElement var0) {
         return (MutableComponent)GSON.fromJson(var0, MutableComponent.class);
      }

      @Nullable
      public static MutableComponent fromJsonLenient(String var0) {
         return (MutableComponent)GsonHelper.fromNullableJson(GSON, var0, MutableComponent.class, true);
      }

      public static MutableComponent fromJson(com.mojang.brigadier.StringReader var0) {
         try {
            JsonReader var1 = new JsonReader(new StringReader(var0.getRemaining()));
            var1.setLenient(false);
            MutableComponent var2 = (MutableComponent)GSON.getAdapter(MutableComponent.class).read(var1);
            var0.setCursor(var0.getCursor() + getPos(var1));
            return var2;
         } catch (StackOverflowError | IOException var3) {
            throw new JsonParseException(var3);
         }
      }

      private static int getPos(JsonReader var0) {
         try {
            return JSON_READER_POS.getInt(var0) - JSON_READER_LINESTART.getInt(var0) + 1;
         } catch (IllegalAccessException var2) {
            throw new IllegalStateException("Couldn't read position of JsonReader", var2);
         }
      }

      // $FF: synthetic method
      public JsonElement serialize(Object var1, Type var2, JsonSerializationContext var3) {
         return this.serialize((Component)var1, var2, var3);
      }

      // $FF: synthetic method
      public Object deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3) throws JsonParseException {
         return this.deserialize(var1, var2, var3);
      }
   }
}
