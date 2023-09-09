package net.minecraft.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface StringRepresentable {
   int PRE_BUILT_MAP_THRESHOLD = 16;

   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnum(Supplier<E[]> var0) {
      return fromEnumWithMapping(var0, (var0x) -> {
         return var0x;
      });
   }

   static <E extends Enum<E> & StringRepresentable> StringRepresentable.EnumCodec<E> fromEnumWithMapping(Supplier<E[]> var0, Function<String, String> var1) {
      Enum[] var2 = (Enum[])var0.get();
      if (var2.length > 16) {
         Map var3 = (Map)Arrays.stream(var2).collect(Collectors.toMap((var1x) -> {
            return (String)var1.apply(((StringRepresentable)var1x).getSerializedName());
         }, (var0x) -> {
            return var0x;
         }));
         return new StringRepresentable.EnumCodec(var2, (var1x) -> {
            return var1x == null ? null : (Enum)var3.get(var1x);
         });
      } else {
         return new StringRepresentable.EnumCodec(var2, (var2x) -> {
            Enum[] var3 = var2;
            int var4 = var2.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               Enum var6 = var3[var5];
               if (((String)var1.apply(((StringRepresentable)var6).getSerializedName())).equals(var2x)) {
                  return var6;
               }
            }

            return null;
         });
      }
   }

   static Keyable keys(final StringRepresentable[] var0) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> var1) {
            Stream var10000 = Arrays.stream(var0).map(StringRepresentable::getSerializedName);
            Objects.requireNonNull(var1);
            return var10000.map(var1::createString);
         }
      };
   }

   /** @deprecated */
   @Deprecated
   public static class EnumCodec<E extends Enum<E> & StringRepresentable> implements Codec<E> {
      private final Codec<E> codec;
      private final Function<String, E> resolver;

      public EnumCodec(E[] var1, Function<String, E> var2) {
         this.codec = ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((var0) -> {
            return ((StringRepresentable)var0).getSerializedName();
         }, var2), ExtraCodecs.idResolverCodec((var0) -> {
            return ((Enum)var0).ordinal();
         }, (var1x) -> {
            return var1x >= 0 && var1x < var1.length ? var1[var1x] : null;
         }, -1));
         this.resolver = var2;
      }

      public <T> DataResult<Pair<E, T>> decode(DynamicOps<T> var1, T var2) {
         return this.codec.decode(var1, var2);
      }

      public <T> DataResult<T> encode(E var1, DynamicOps<T> var2, T var3) {
         return this.codec.encode(var1, var2, var3);
      }

      @Nullable
      public E byName(@Nullable String var1) {
         return (Enum)this.resolver.apply(var1);
      }

      public E byName(@Nullable String var1, E var2) {
         return (Enum)Objects.requireNonNullElse(this.byName(var1), var2);
      }

      // $FF: synthetic method
      public DataResult encode(Object var1, DynamicOps var2, Object var3) {
         return this.encode((Enum)var1, var2, var3);
      }
   }
}
