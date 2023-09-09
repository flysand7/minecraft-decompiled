package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class Passthrough implements RuleBlockEntityModifier {
   public static final Passthrough INSTANCE = new Passthrough();
   public static final Codec<Passthrough> CODEC;

   public Passthrough() {
   }

   @Nullable
   public CompoundTag apply(RandomSource var1, @Nullable CompoundTag var2) {
      return var2;
   }

   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.PASSTHROUGH;
   }

   static {
      CODEC = Codec.unit(INSTANCE);
   }
}
