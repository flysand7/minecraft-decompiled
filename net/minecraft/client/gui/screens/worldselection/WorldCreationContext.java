package net.minecraft.client.gui.screens.worldselection;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;

public record WorldCreationContext(WorldOptions a, Registry<LevelStem> b, WorldDimensions c, LayeredRegistryAccess<RegistryLayer> d, ReloadableServerResources e, WorldDataConfiguration f) {
   private final WorldOptions options;
   private final Registry<LevelStem> datapackDimensions;
   private final WorldDimensions selectedDimensions;
   private final LayeredRegistryAccess<RegistryLayer> worldgenRegistries;
   private final ReloadableServerResources dataPackResources;
   private final WorldDataConfiguration dataConfiguration;

   public WorldCreationContext(WorldGenSettings var1, LayeredRegistryAccess<RegistryLayer> var2, ReloadableServerResources var3, WorldDataConfiguration var4) {
      this(var1.options(), var1.dimensions(), var2, var3, var4);
   }

   public WorldCreationContext(WorldOptions var1, WorldDimensions var2, LayeredRegistryAccess<RegistryLayer> var3, ReloadableServerResources var4, WorldDataConfiguration var5) {
      this(var1, var3.getLayer(RegistryLayer.DIMENSIONS).registryOrThrow(Registries.LEVEL_STEM), var2, var3.replaceFrom(RegistryLayer.DIMENSIONS, (RegistryAccess.Frozen[])()), var4, var5);
   }

   public WorldCreationContext(WorldOptions var1, Registry<LevelStem> var2, WorldDimensions var3, LayeredRegistryAccess<RegistryLayer> var4, ReloadableServerResources var5, WorldDataConfiguration var6) {
      this.options = var1;
      this.datapackDimensions = var2;
      this.selectedDimensions = var3;
      this.worldgenRegistries = var4;
      this.dataPackResources = var5;
      this.dataConfiguration = var6;
   }

   public WorldCreationContext withSettings(WorldOptions var1, WorldDimensions var2) {
      return new WorldCreationContext(var1, this.datapackDimensions, var2, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public WorldCreationContext withOptions(WorldCreationContext.OptionsModifier var1) {
      return new WorldCreationContext((WorldOptions)var1.apply(this.options), this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public WorldCreationContext withDimensions(WorldCreationContext.DimensionsUpdater var1) {
      return new WorldCreationContext(this.options, this.datapackDimensions, (WorldDimensions)var1.apply(this.worldgenLoadContext(), this.selectedDimensions), this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
   }

   public RegistryAccess.Frozen worldgenLoadContext() {
      return this.worldgenRegistries.compositeAccess();
   }

   public WorldOptions options() {
      return this.options;
   }

   public Registry<LevelStem> datapackDimensions() {
      return this.datapackDimensions;
   }

   public WorldDimensions selectedDimensions() {
      return this.selectedDimensions;
   }

   public LayeredRegistryAccess<RegistryLayer> worldgenRegistries() {
      return this.worldgenRegistries;
   }

   public ReloadableServerResources dataPackResources() {
      return this.dataPackResources;
   }

   public WorldDataConfiguration dataConfiguration() {
      return this.dataConfiguration;
   }

   public interface OptionsModifier extends UnaryOperator<WorldOptions> {
   }

   @FunctionalInterface
   public interface DimensionsUpdater extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
   }
}
