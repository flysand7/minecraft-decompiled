package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<Holder<E>> elementCodec;
   private final Codec<List<Holder<E>>> homogenousListCodec;
   private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

   private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> var0, boolean var1) {
      Codec var2 = ExtraCodecs.validate(var0.listOf(), ExtraCodecs.ensureHomogenous(Holder::kind));
      return var1 ? var2 : Codec.either(var2, var0).xmap((var0x) -> {
         return (List)var0x.map((var0) -> {
            return var0;
         }, List::of);
      }, (var0x) -> {
         return var0x.size() == 1 ? Either.right((Holder)var0x.get(0)) : Either.left(var0x);
      });
   }

   public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> var0, Codec<Holder<E>> var1, boolean var2) {
      return new HolderSetCodec(var0, var1, var2);
   }

   private HolderSetCodec(ResourceKey<? extends Registry<E>> var1, Codec<Holder<E>> var2, boolean var3) {
      this.registryKey = var1;
      this.elementCodec = var2;
      this.homogenousListCodec = homogenousList(var2, var3);
      this.registryAwareCodec = Codec.either(TagKey.hashedCodec(var1), this.homogenousListCodec);
   }

   public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> var1, T var2) {
      if (var1 instanceof RegistryOps) {
         RegistryOps var3 = (RegistryOps)var1;
         Optional var4 = var3.getter(this.registryKey);
         if (var4.isPresent()) {
            HolderGetter var5 = (HolderGetter)var4.get();
            return this.registryAwareCodec.decode(var1, var2).map((var1x) -> {
               return var1x.mapFirst((var1) -> {
                  Objects.requireNonNull(var5);
                  return (HolderSet)var1.map(var5::getOrThrow, HolderSet::direct);
               });
            });
         }
      }

      return this.decodeWithoutRegistry(var1, var2);
   }

   public <T> DataResult<T> encode(HolderSet<E> var1, DynamicOps<T> var2, T var3) {
      if (var2 instanceof RegistryOps) {
         RegistryOps var4 = (RegistryOps)var2;
         Optional var5 = var4.owner(this.registryKey);
         if (var5.isPresent()) {
            if (!var1.canSerializeIn((HolderOwner)var5.get())) {
               return DataResult.error(() -> {
                  return "HolderSet " + var1 + " is not valid in current registry set";
               });
            }

            return this.registryAwareCodec.encode(var1.unwrap().mapRight(List::copyOf), var2, var3);
         }
      }

      return this.encodeWithoutRegistry(var1, var2, var3);
   }

   private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> var1, T var2) {
      return this.elementCodec.listOf().decode(var1, var2).flatMap((var0) -> {
         ArrayList var1 = new ArrayList();
         Iterator var2 = ((List)var0.getFirst()).iterator();

         while(var2.hasNext()) {
            Holder var3 = (Holder)var2.next();
            if (!(var3 instanceof Holder.Direct)) {
               return DataResult.error(() -> {
                  return "Can't decode element " + var3 + " without registry";
               });
            }

            Holder.Direct var4 = (Holder.Direct)var3;
            var1.add(var4);
         }

         return DataResult.success(new Pair(HolderSet.direct((List)var1), var0.getSecond()));
      });
   }

   private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> var1, DynamicOps<T> var2, T var3) {
      return this.homogenousListCodec.encode(var1.stream().toList(), var2, var3);
   }

   // $FF: synthetic method
   public DataResult encode(Object var1, DynamicOps var2, Object var3) {
      return this.encode((HolderSet)var1, var2, var3);
   }
}
