package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public abstract class IntProvider {
   private static final Codec<Either<Integer, IntProvider>> CONSTANT_OR_DISPATCH_CODEC;
   public static final Codec<IntProvider> CODEC;
   public static final Codec<IntProvider> NON_NEGATIVE_CODEC;
   public static final Codec<IntProvider> POSITIVE_CODEC;

   public IntProvider() {
   }

   public static Codec<IntProvider> codec(int var0, int var1) {
      return codec(var0, var1, CODEC);
   }

   public static <T extends IntProvider> Codec<T> codec(int var0, int var1, Codec<T> var2) {
      return ExtraCodecs.validate(var2, (var2x) -> {
         if (var2x.getMinValue() < var0) {
            return DataResult.error(() -> {
               return "Value provider too low: " + var0 + " [" + var2x.getMinValue() + "-" + var2x.getMaxValue() + "]";
            });
         } else {
            return var2x.getMaxValue() > var1 ? DataResult.error(() -> {
               return "Value provider too high: " + var1 + " [" + var2x.getMinValue() + "-" + var2x.getMaxValue() + "]";
            }) : DataResult.success(var2x);
         }
      });
   }

   public abstract int sample(RandomSource var1);

   public abstract int getMinValue();

   public abstract int getMaxValue();

   public abstract IntProviderType<?> getType();

   static {
      CONSTANT_OR_DISPATCH_CODEC = Codec.either(Codec.INT, BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().dispatch(IntProvider::getType, IntProviderType::codec));
      CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap((var0) -> {
         return (IntProvider)var0.map(ConstantInt::of, (var0x) -> {
            return var0x;
         });
      }, (var0) -> {
         return var0.getType() == IntProviderType.CONSTANT ? Either.left(((ConstantInt)var0).getValue()) : Either.right(var0);
      });
      NON_NEGATIVE_CODEC = codec(0, Integer.MAX_VALUE);
      POSITIVE_CODEC = codec(1, Integer.MAX_VALUE);
   }
}
