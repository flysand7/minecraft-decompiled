package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

public record WoodType(String l, BlockSetType m, SoundType n, SoundType o, SoundEvent p, SoundEvent q) {
   private final String name;
   private final BlockSetType setType;
   private final SoundType soundType;
   private final SoundType hangingSignSoundType;
   private final SoundEvent fenceGateClose;
   private final SoundEvent fenceGateOpen;
   private static final Set<WoodType> VALUES = new ObjectArraySet();
   public static final WoodType OAK;
   public static final WoodType SPRUCE;
   public static final WoodType BIRCH;
   public static final WoodType ACACIA;
   public static final WoodType CHERRY;
   public static final WoodType JUNGLE;
   public static final WoodType DARK_OAK;
   public static final WoodType CRIMSON;
   public static final WoodType WARPED;
   public static final WoodType MANGROVE;
   public static final WoodType BAMBOO;

   public WoodType(String var1, BlockSetType var2) {
      this(var1, var2, SoundType.WOOD, SoundType.HANGING_SIGN, SoundEvents.FENCE_GATE_CLOSE, SoundEvents.FENCE_GATE_OPEN);
   }

   public WoodType(String var1, BlockSetType var2, SoundType var3, SoundType var4, SoundEvent var5, SoundEvent var6) {
      this.name = var1;
      this.setType = var2;
      this.soundType = var3;
      this.hangingSignSoundType = var4;
      this.fenceGateClose = var5;
      this.fenceGateOpen = var6;
   }

   private static WoodType register(WoodType var0) {
      VALUES.add(var0);
      return var0;
   }

   public static Stream<WoodType> values() {
      return VALUES.stream();
   }

   public String name() {
      return this.name;
   }

   public BlockSetType setType() {
      return this.setType;
   }

   public SoundType soundType() {
      return this.soundType;
   }

   public SoundType hangingSignSoundType() {
      return this.hangingSignSoundType;
   }

   public SoundEvent fenceGateClose() {
      return this.fenceGateClose;
   }

   public SoundEvent fenceGateOpen() {
      return this.fenceGateOpen;
   }

   static {
      OAK = register(new WoodType("oak", BlockSetType.OAK));
      SPRUCE = register(new WoodType("spruce", BlockSetType.SPRUCE));
      BIRCH = register(new WoodType("birch", BlockSetType.BIRCH));
      ACACIA = register(new WoodType("acacia", BlockSetType.ACACIA));
      CHERRY = register(new WoodType("cherry", BlockSetType.CHERRY, SoundType.CHERRY_WOOD, SoundType.CHERRY_WOOD_HANGING_SIGN, SoundEvents.CHERRY_WOOD_FENCE_GATE_CLOSE, SoundEvents.CHERRY_WOOD_FENCE_GATE_OPEN));
      JUNGLE = register(new WoodType("jungle", BlockSetType.JUNGLE));
      DARK_OAK = register(new WoodType("dark_oak", BlockSetType.DARK_OAK));
      CRIMSON = register(new WoodType("crimson", BlockSetType.CRIMSON, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN));
      WARPED = register(new WoodType("warped", BlockSetType.WARPED, SoundType.NETHER_WOOD, SoundType.NETHER_WOOD_HANGING_SIGN, SoundEvents.NETHER_WOOD_FENCE_GATE_CLOSE, SoundEvents.NETHER_WOOD_FENCE_GATE_OPEN));
      MANGROVE = register(new WoodType("mangrove", BlockSetType.MANGROVE));
      BAMBOO = register(new WoodType("bamboo", BlockSetType.BAMBOO, SoundType.BAMBOO_WOOD, SoundType.BAMBOO_WOOD_HANGING_SIGN, SoundEvents.BAMBOO_WOOD_FENCE_GATE_CLOSE, SoundEvents.BAMBOO_WOOD_FENCE_GATE_OPEN));
   }
}
