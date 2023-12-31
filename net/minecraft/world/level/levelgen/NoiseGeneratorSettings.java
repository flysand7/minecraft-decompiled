package net.minecraft.world.level.levelgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public record NoiseGeneratorSettings(NoiseSettings j, BlockState k, BlockState l, NoiseRouter m, SurfaceRules.RuleSource n, List<Climate.ParameterPoint> o, int p, boolean q, boolean r, boolean s, boolean t) {
   private final NoiseSettings noiseSettings;
   private final BlockState defaultBlock;
   private final BlockState defaultFluid;
   private final NoiseRouter noiseRouter;
   private final SurfaceRules.RuleSource surfaceRule;
   private final List<Climate.ParameterPoint> spawnTarget;
   private final int seaLevel;
   private final boolean disableMobGeneration;
   private final boolean aquifersEnabled;
   private final boolean oreVeinsEnabled;
   private final boolean useLegacyRandomSource;
   public static final Codec<NoiseGeneratorSettings> DIRECT_CODEC = RecordCodecBuilder.create((var0) -> {
      return var0.group(NoiseSettings.CODEC.fieldOf("noise").forGetter(NoiseGeneratorSettings::noiseSettings), BlockState.CODEC.fieldOf("default_block").forGetter(NoiseGeneratorSettings::defaultBlock), BlockState.CODEC.fieldOf("default_fluid").forGetter(NoiseGeneratorSettings::defaultFluid), NoiseRouter.CODEC.fieldOf("noise_router").forGetter(NoiseGeneratorSettings::noiseRouter), SurfaceRules.RuleSource.CODEC.fieldOf("surface_rule").forGetter(NoiseGeneratorSettings::surfaceRule), Climate.ParameterPoint.CODEC.listOf().fieldOf("spawn_target").forGetter(NoiseGeneratorSettings::spawnTarget), Codec.INT.fieldOf("sea_level").forGetter(NoiseGeneratorSettings::seaLevel), Codec.BOOL.fieldOf("disable_mob_generation").forGetter(NoiseGeneratorSettings::disableMobGeneration), Codec.BOOL.fieldOf("aquifers_enabled").forGetter(NoiseGeneratorSettings::isAquifersEnabled), Codec.BOOL.fieldOf("ore_veins_enabled").forGetter(NoiseGeneratorSettings::oreVeinsEnabled), Codec.BOOL.fieldOf("legacy_random_source").forGetter(NoiseGeneratorSettings::useLegacyRandomSource)).apply(var0, NoiseGeneratorSettings::new);
   });
   public static final Codec<Holder<NoiseGeneratorSettings>> CODEC;
   public static final ResourceKey<NoiseGeneratorSettings> OVERWORLD;
   public static final ResourceKey<NoiseGeneratorSettings> LARGE_BIOMES;
   public static final ResourceKey<NoiseGeneratorSettings> AMPLIFIED;
   public static final ResourceKey<NoiseGeneratorSettings> NETHER;
   public static final ResourceKey<NoiseGeneratorSettings> END;
   public static final ResourceKey<NoiseGeneratorSettings> CAVES;
   public static final ResourceKey<NoiseGeneratorSettings> FLOATING_ISLANDS;

   public NoiseGeneratorSettings(NoiseSettings var1, BlockState var2, BlockState var3, NoiseRouter var4, SurfaceRules.RuleSource var5, List<Climate.ParameterPoint> var6, int var7, boolean var8, boolean var9, boolean var10, boolean var11) {
      this.noiseSettings = var1;
      this.defaultBlock = var2;
      this.defaultFluid = var3;
      this.noiseRouter = var4;
      this.surfaceRule = var5;
      this.spawnTarget = var6;
      this.seaLevel = var7;
      this.disableMobGeneration = var8;
      this.aquifersEnabled = var9;
      this.oreVeinsEnabled = var10;
      this.useLegacyRandomSource = var11;
   }

   /** @deprecated */
   @Deprecated
   public boolean disableMobGeneration() {
      return this.disableMobGeneration;
   }

   public boolean isAquifersEnabled() {
      return this.aquifersEnabled;
   }

   public boolean oreVeinsEnabled() {
      return this.oreVeinsEnabled;
   }

   public WorldgenRandom.Algorithm getRandomSource() {
      return this.useLegacyRandomSource ? WorldgenRandom.Algorithm.LEGACY : WorldgenRandom.Algorithm.XOROSHIRO;
   }

   public static void bootstrap(BootstapContext<NoiseGeneratorSettings> var0) {
      var0.register(OVERWORLD, overworld(var0, false, false));
      var0.register(LARGE_BIOMES, overworld(var0, false, true));
      var0.register(AMPLIFIED, overworld(var0, true, false));
      var0.register(NETHER, nether(var0));
      var0.register(END, end(var0));
      var0.register(CAVES, caves(var0));
      var0.register(FLOATING_ISLANDS, floatingIslands(var0));
   }

   private static NoiseGeneratorSettings end(BootstapContext<?> var0) {
      return new NoiseGeneratorSettings(NoiseSettings.END_NOISE_SETTINGS, Blocks.END_STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), NoiseRouterData.end(var0.lookup(Registries.DENSITY_FUNCTION)), SurfaceRuleData.end(), List.of(), 0, true, false, false, true);
   }

   private static NoiseGeneratorSettings nether(BootstapContext<?> var0) {
      return new NoiseGeneratorSettings(NoiseSettings.NETHER_NOISE_SETTINGS, Blocks.NETHERRACK.defaultBlockState(), Blocks.LAVA.defaultBlockState(), NoiseRouterData.nether(var0.lookup(Registries.DENSITY_FUNCTION), var0.lookup(Registries.NOISE)), SurfaceRuleData.nether(), List.of(), 32, false, false, false, true);
   }

   private static NoiseGeneratorSettings overworld(BootstapContext<?> var0, boolean var1, boolean var2) {
      return new NoiseGeneratorSettings(NoiseSettings.OVERWORLD_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.overworld(var0.lookup(Registries.DENSITY_FUNCTION), var0.lookup(Registries.NOISE), var2, var1), SurfaceRuleData.overworld(), (new OverworldBiomeBuilder()).spawnTarget(), 63, false, true, true, false);
   }

   private static NoiseGeneratorSettings caves(BootstapContext<?> var0) {
      return new NoiseGeneratorSettings(NoiseSettings.CAVES_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.caves(var0.lookup(Registries.DENSITY_FUNCTION), var0.lookup(Registries.NOISE)), SurfaceRuleData.overworldLike(false, true, true), List.of(), 32, false, false, false, true);
   }

   private static NoiseGeneratorSettings floatingIslands(BootstapContext<?> var0) {
      return new NoiseGeneratorSettings(NoiseSettings.FLOATING_ISLANDS_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), NoiseRouterData.floatingIslands(var0.lookup(Registries.DENSITY_FUNCTION), var0.lookup(Registries.NOISE)), SurfaceRuleData.overworldLike(false, false, false), List.of(), -64, false, false, false, true);
   }

   public static NoiseGeneratorSettings dummy() {
      return new NoiseGeneratorSettings(NoiseSettings.OVERWORLD_NOISE_SETTINGS, Blocks.STONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), NoiseRouterData.none(), SurfaceRuleData.air(), List.of(), 63, true, false, false, false);
   }

   public NoiseSettings noiseSettings() {
      return this.noiseSettings;
   }

   public BlockState defaultBlock() {
      return this.defaultBlock;
   }

   public BlockState defaultFluid() {
      return this.defaultFluid;
   }

   public NoiseRouter noiseRouter() {
      return this.noiseRouter;
   }

   public SurfaceRules.RuleSource surfaceRule() {
      return this.surfaceRule;
   }

   public List<Climate.ParameterPoint> spawnTarget() {
      return this.spawnTarget;
   }

   public int seaLevel() {
      return this.seaLevel;
   }

   public boolean aquifersEnabled() {
      return this.aquifersEnabled;
   }

   public boolean useLegacyRandomSource() {
      return this.useLegacyRandomSource;
   }

   static {
      CODEC = RegistryFileCodec.create(Registries.NOISE_SETTINGS, DIRECT_CODEC);
      OVERWORLD = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("overworld"));
      LARGE_BIOMES = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("large_biomes"));
      AMPLIFIED = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("amplified"));
      NETHER = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("nether"));
      END = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("end"));
      CAVES = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("caves"));
      FLOATING_ISLANDS = ResourceKey.create(Registries.NOISE_SETTINGS, new ResourceLocation("floating_islands"));
   }
}
