package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;

public record BlockSetType(String p, boolean q, SoundType r, SoundEvent s, SoundEvent t, SoundEvent u, SoundEvent v, SoundEvent w, SoundEvent x, SoundEvent y, SoundEvent z) {
   private final String name;
   private final boolean canOpenByHand;
   private final SoundType soundType;
   private final SoundEvent doorClose;
   private final SoundEvent doorOpen;
   private final SoundEvent trapdoorClose;
   private final SoundEvent trapdoorOpen;
   private final SoundEvent pressurePlateClickOff;
   private final SoundEvent pressurePlateClickOn;
   private final SoundEvent buttonClickOff;
   private final SoundEvent buttonClickOn;
   private static final Set<BlockSetType> VALUES = new ObjectArraySet();
   public static final BlockSetType IRON;
   public static final BlockSetType GOLD;
   public static final BlockSetType STONE;
   public static final BlockSetType POLISHED_BLACKSTONE;
   public static final BlockSetType OAK;
   public static final BlockSetType SPRUCE;
   public static final BlockSetType BIRCH;
   public static final BlockSetType ACACIA;
   public static final BlockSetType CHERRY;
   public static final BlockSetType JUNGLE;
   public static final BlockSetType DARK_OAK;
   public static final BlockSetType CRIMSON;
   public static final BlockSetType WARPED;
   public static final BlockSetType MANGROVE;
   public static final BlockSetType BAMBOO;

   public BlockSetType(String var1) {
      this(var1, true, SoundType.WOOD, SoundEvents.WOODEN_DOOR_CLOSE, SoundEvents.WOODEN_DOOR_OPEN, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_OFF, SoundEvents.WOODEN_PRESSURE_PLATE_CLICK_ON, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundEvents.WOODEN_BUTTON_CLICK_ON);
   }

   public BlockSetType(String var1, boolean var2, SoundType var3, SoundEvent var4, SoundEvent var5, SoundEvent var6, SoundEvent var7, SoundEvent var8, SoundEvent var9, SoundEvent var10, SoundEvent var11) {
      this.name = var1;
      this.canOpenByHand = var2;
      this.soundType = var3;
      this.doorClose = var4;
      this.doorOpen = var5;
      this.trapdoorClose = var6;
      this.trapdoorOpen = var7;
      this.pressurePlateClickOff = var8;
      this.pressurePlateClickOn = var9;
      this.buttonClickOff = var10;
      this.buttonClickOn = var11;
   }

   private static BlockSetType register(BlockSetType var0) {
      VALUES.add(var0);
      return var0;
   }

   public static Stream<BlockSetType> values() {
      return VALUES.stream();
   }

   public String name() {
      return this.name;
   }

   public boolean canOpenByHand() {
      return this.canOpenByHand;
   }

   public SoundType soundType() {
      return this.soundType;
   }

   public SoundEvent doorClose() {
      return this.doorClose;
   }

   public SoundEvent doorOpen() {
      return this.doorOpen;
   }

   public SoundEvent trapdoorClose() {
      return this.trapdoorClose;
   }

   public SoundEvent trapdoorOpen() {
      return this.trapdoorOpen;
   }

   public SoundEvent pressurePlateClickOff() {
      return this.pressurePlateClickOff;
   }

   public SoundEvent pressurePlateClickOn() {
      return this.pressurePlateClickOn;
   }

   public SoundEvent buttonClickOff() {
      return this.buttonClickOff;
   }

   public SoundEvent buttonClickOn() {
      return this.buttonClickOn;
   }

   static {
      IRON = register(new BlockSetType("iron", false, SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));
      GOLD = register(new BlockSetType("gold", false, SoundType.METAL, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.METAL_PRESSURE_PLATE_CLICK_OFF, SoundEvents.METAL_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));
      STONE = register(new BlockSetType("stone", true, SoundType.STONE, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));
      POLISHED_BLACKSTONE = register(new BlockSetType("polished_blackstone", true, SoundType.STONE, SoundEvents.IRON_DOOR_CLOSE, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundEvents.STONE_BUTTON_CLICK_ON));
      OAK = register(new BlockSetType("oak"));
      SPRUCE = register(new BlockSetType("spruce"));
      BIRCH = register(new BlockSetType("birch"));
      ACACIA = register(new BlockSetType("acacia"));
      CHERRY = register(new BlockSetType("cherry", true, SoundType.CHERRY_WOOD, SoundEvents.CHERRY_WOOD_DOOR_CLOSE, SoundEvents.CHERRY_WOOD_DOOR_OPEN, SoundEvents.CHERRY_WOOD_TRAPDOOR_CLOSE, SoundEvents.CHERRY_WOOD_TRAPDOOR_OPEN, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.CHERRY_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_OFF, SoundEvents.CHERRY_WOOD_BUTTON_CLICK_ON));
      JUNGLE = register(new BlockSetType("jungle"));
      DARK_OAK = register(new BlockSetType("dark_oak"));
      CRIMSON = register(new BlockSetType("crimson", true, SoundType.NETHER_WOOD, SoundEvents.NETHER_WOOD_DOOR_CLOSE, SoundEvents.NETHER_WOOD_DOOR_OPEN, SoundEvents.NETHER_WOOD_TRAPDOOR_CLOSE, SoundEvents.NETHER_WOOD_TRAPDOOR_OPEN, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.NETHER_WOOD_BUTTON_CLICK_OFF, SoundEvents.NETHER_WOOD_BUTTON_CLICK_ON));
      WARPED = register(new BlockSetType("warped", true, SoundType.NETHER_WOOD, SoundEvents.NETHER_WOOD_DOOR_CLOSE, SoundEvents.NETHER_WOOD_DOOR_OPEN, SoundEvents.NETHER_WOOD_TRAPDOOR_CLOSE, SoundEvents.NETHER_WOOD_TRAPDOOR_OPEN, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.NETHER_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.NETHER_WOOD_BUTTON_CLICK_OFF, SoundEvents.NETHER_WOOD_BUTTON_CLICK_ON));
      MANGROVE = register(new BlockSetType("mangrove"));
      BAMBOO = register(new BlockSetType("bamboo", true, SoundType.BAMBOO_WOOD, SoundEvents.BAMBOO_WOOD_DOOR_CLOSE, SoundEvents.BAMBOO_WOOD_DOOR_OPEN, SoundEvents.BAMBOO_WOOD_TRAPDOOR_CLOSE, SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN, SoundEvents.BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, SoundEvents.BAMBOO_WOOD_PRESSURE_PLATE_CLICK_ON, SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_OFF, SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON));
   }
}
