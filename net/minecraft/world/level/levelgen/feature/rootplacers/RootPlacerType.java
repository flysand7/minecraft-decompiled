package net.minecraft.world.level.levelgen.feature.rootplacers;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class RootPlacerType<P extends RootPlacer> {
   public static final RootPlacerType<MangroveRootPlacer> MANGROVE_ROOT_PLACER;
   private final Codec<P> codec;

   private static <P extends RootPlacer> RootPlacerType<P> register(String var0, Codec<P> var1) {
      return (RootPlacerType)Registry.register(BuiltInRegistries.ROOT_PLACER_TYPE, (String)var0, new RootPlacerType(var1));
   }

   private RootPlacerType(Codec<P> var1) {
      this.codec = var1;
   }

   public Codec<P> codec() {
      return this.codec;
   }

   static {
      MANGROVE_ROOT_PLACER = register("mangrove_root_placer", MangroveRootPlacer.CODEC);
   }
}
