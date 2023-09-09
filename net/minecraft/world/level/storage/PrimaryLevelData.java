package net.minecraft.world.level.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.timers.TimerCallbacks;
import net.minecraft.world.level.timers.TimerQueue;
import org.slf4j.Logger;

public class PrimaryLevelData implements ServerLevelData, WorldData {
   private static final Logger LOGGER = LogUtils.getLogger();
   protected static final String PLAYER = "Player";
   protected static final String WORLD_GEN_SETTINGS = "WorldGenSettings";
   private LevelSettings settings;
   private final WorldOptions worldOptions;
   private final PrimaryLevelData.SpecialWorldProperty specialWorldProperty;
   private final Lifecycle worldGenSettingsLifecycle;
   private int xSpawn;
   private int ySpawn;
   private int zSpawn;
   private float spawnAngle;
   private long gameTime;
   private long dayTime;
   @Nullable
   private final DataFixer fixerUpper;
   private final int playerDataVersion;
   private boolean upgradedPlayerTag;
   @Nullable
   private CompoundTag loadedPlayerTag;
   private final int version;
   private int clearWeatherTime;
   private boolean raining;
   private int rainTime;
   private boolean thundering;
   private int thunderTime;
   private boolean initialized;
   private boolean difficultyLocked;
   private WorldBorder.Settings worldBorder;
   private EndDragonFight.Data endDragonFightData;
   @Nullable
   private CompoundTag customBossEvents;
   private int wanderingTraderSpawnDelay;
   private int wanderingTraderSpawnChance;
   @Nullable
   private UUID wanderingTraderId;
   private final Set<String> knownServerBrands;
   private boolean wasModded;
   private final Set<String> removedFeatureFlags;
   private final TimerQueue<MinecraftServer> scheduledEvents;

   private PrimaryLevelData(@Nullable DataFixer var1, int var2, @Nullable CompoundTag var3, boolean var4, int var5, int var6, int var7, float var8, long var9, long var11, int var13, int var14, int var15, boolean var16, int var17, boolean var18, boolean var19, boolean var20, WorldBorder.Settings var21, int var22, int var23, @Nullable UUID var24, Set<String> var25, Set<String> var26, TimerQueue<MinecraftServer> var27, @Nullable CompoundTag var28, EndDragonFight.Data var29, LevelSettings var30, WorldOptions var31, PrimaryLevelData.SpecialWorldProperty var32, Lifecycle var33) {
      this.fixerUpper = var1;
      this.wasModded = var4;
      this.xSpawn = var5;
      this.ySpawn = var6;
      this.zSpawn = var7;
      this.spawnAngle = var8;
      this.gameTime = var9;
      this.dayTime = var11;
      this.version = var13;
      this.clearWeatherTime = var14;
      this.rainTime = var15;
      this.raining = var16;
      this.thunderTime = var17;
      this.thundering = var18;
      this.initialized = var19;
      this.difficultyLocked = var20;
      this.worldBorder = var21;
      this.wanderingTraderSpawnDelay = var22;
      this.wanderingTraderSpawnChance = var23;
      this.wanderingTraderId = var24;
      this.knownServerBrands = var25;
      this.removedFeatureFlags = var26;
      this.loadedPlayerTag = var3;
      this.playerDataVersion = var2;
      this.scheduledEvents = var27;
      this.customBossEvents = var28;
      this.endDragonFightData = var29;
      this.settings = var30;
      this.worldOptions = var31;
      this.specialWorldProperty = var32;
      this.worldGenSettingsLifecycle = var33;
   }

   public PrimaryLevelData(LevelSettings var1, WorldOptions var2, PrimaryLevelData.SpecialWorldProperty var3, Lifecycle var4) {
      this((DataFixer)null, SharedConstants.getCurrentVersion().getDataVersion().getVersion(), (CompoundTag)null, false, 0, 0, 0, 0.0F, 0L, 0L, 19133, 0, 0, false, 0, false, false, false, WorldBorder.DEFAULT_SETTINGS, 0, 0, (UUID)null, Sets.newLinkedHashSet(), new HashSet(), new TimerQueue(TimerCallbacks.SERVER_CALLBACKS), (CompoundTag)null, EndDragonFight.Data.DEFAULT, var1.copy(), var2, var3, var4);
   }

   public static <T> PrimaryLevelData parse(Dynamic<T> var0, DataFixer var1, int var2, @Nullable CompoundTag var3, LevelSettings var4, LevelVersion var5, PrimaryLevelData.SpecialWorldProperty var6, WorldOptions var7, Lifecycle var8) {
      long var9 = var0.get("Time").asLong(0L);
      boolean var10005 = var0.get("WasModded").asBoolean(false);
      int var10006 = var0.get("SpawnX").asInt(0);
      int var10007 = var0.get("SpawnY").asInt(0);
      int var10008 = var0.get("SpawnZ").asInt(0);
      float var10009 = var0.get("SpawnAngle").asFloat(0.0F);
      long var10011 = var0.get("DayTime").asLong(var9);
      int var10012 = var5.levelDataVersion();
      int var10013 = var0.get("clearWeatherTime").asInt(0);
      int var10014 = var0.get("rainTime").asInt(0);
      boolean var10015 = var0.get("raining").asBoolean(false);
      int var10016 = var0.get("thunderTime").asInt(0);
      boolean var10017 = var0.get("thundering").asBoolean(false);
      boolean var10018 = var0.get("initialized").asBoolean(true);
      boolean var10019 = var0.get("DifficultyLocked").asBoolean(false);
      WorldBorder.Settings var10020 = WorldBorder.Settings.read(var0, WorldBorder.DEFAULT_SETTINGS);
      int var10021 = var0.get("WanderingTraderSpawnDelay").asInt(0);
      int var10022 = var0.get("WanderingTraderSpawnChance").asInt(0);
      UUID var10023 = (UUID)var0.get("WanderingTraderId").read(UUIDUtil.CODEC).result().orElse((Object)null);
      Set var10024 = (Set)var0.get("ServerBrands").asStream().flatMap((var0x) -> {
         return var0x.asString().result().stream();
      }).collect(Collectors.toCollection(Sets::newLinkedHashSet));
      Set var10025 = (Set)var0.get("removed_features").asStream().flatMap((var0x) -> {
         return var0x.asString().result().stream();
      }).collect(Collectors.toSet());
      TimerQueue var10026 = new TimerQueue(TimerCallbacks.SERVER_CALLBACKS, var0.get("ScheduledEvents").asStream());
      CompoundTag var10027 = (CompoundTag)var0.get("CustomBossEvents").orElseEmptyMap().getValue();
      DataResult var10028 = var0.get("DragonFight").read(EndDragonFight.Data.CODEC);
      Logger var10029 = LOGGER;
      Objects.requireNonNull(var10029);
      return new PrimaryLevelData(var1, var2, var3, var10005, var10006, var10007, var10008, var10009, var9, var10011, var10012, var10013, var10014, var10015, var10016, var10017, var10018, var10019, var10020, var10021, var10022, var10023, var10024, var10025, var10026, var10027, (EndDragonFight.Data)var10028.resultOrPartial(var10029::error).orElse(EndDragonFight.Data.DEFAULT), var4, var7, var6, var8);
   }

   public CompoundTag createTag(RegistryAccess var1, @Nullable CompoundTag var2) {
      this.updatePlayerTag();
      if (var2 == null) {
         var2 = this.loadedPlayerTag;
      }

      CompoundTag var3 = new CompoundTag();
      this.setTagData(var1, var3, var2);
      return var3;
   }

   private void setTagData(RegistryAccess var1, CompoundTag var2, @Nullable CompoundTag var3) {
      var2.put("ServerBrands", stringCollectionToTag(this.knownServerBrands));
      var2.putBoolean("WasModded", this.wasModded);
      if (!this.removedFeatureFlags.isEmpty()) {
         var2.put("removed_features", stringCollectionToTag(this.removedFeatureFlags));
      }

      CompoundTag var4 = new CompoundTag();
      var4.putString("Name", SharedConstants.getCurrentVersion().getName());
      var4.putInt("Id", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
      var4.putBoolean("Snapshot", !SharedConstants.getCurrentVersion().isStable());
      var4.putString("Series", SharedConstants.getCurrentVersion().getDataVersion().getSeries());
      var2.put("Version", var4);
      NbtUtils.addCurrentDataVersion(var2);
      RegistryOps var5 = RegistryOps.create(NbtOps.INSTANCE, (HolderLookup.Provider)var1);
      DataResult var10000 = WorldGenSettings.encode(var5, this.worldOptions, (RegistryAccess)var1);
      Logger var10002 = LOGGER;
      Objects.requireNonNull(var10002);
      var10000.resultOrPartial(Util.prefix("WorldGenSettings: ", var10002::error)).ifPresent((var1x) -> {
         var2.put("WorldGenSettings", var1x);
      });
      var2.putInt("GameType", this.settings.gameType().getId());
      var2.putInt("SpawnX", this.xSpawn);
      var2.putInt("SpawnY", this.ySpawn);
      var2.putInt("SpawnZ", this.zSpawn);
      var2.putFloat("SpawnAngle", this.spawnAngle);
      var2.putLong("Time", this.gameTime);
      var2.putLong("DayTime", this.dayTime);
      var2.putLong("LastPlayed", Util.getEpochMillis());
      var2.putString("LevelName", this.settings.levelName());
      var2.putInt("version", 19133);
      var2.putInt("clearWeatherTime", this.clearWeatherTime);
      var2.putInt("rainTime", this.rainTime);
      var2.putBoolean("raining", this.raining);
      var2.putInt("thunderTime", this.thunderTime);
      var2.putBoolean("thundering", this.thundering);
      var2.putBoolean("hardcore", this.settings.hardcore());
      var2.putBoolean("allowCommands", this.settings.allowCommands());
      var2.putBoolean("initialized", this.initialized);
      this.worldBorder.write(var2);
      var2.putByte("Difficulty", (byte)this.settings.difficulty().getId());
      var2.putBoolean("DifficultyLocked", this.difficultyLocked);
      var2.put("GameRules", this.settings.gameRules().createTag());
      var2.put("DragonFight", (Tag)Util.getOrThrow(EndDragonFight.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.endDragonFightData), IllegalStateException::new));
      if (var3 != null) {
         var2.put("Player", var3);
      }

      DataResult var6 = WorldDataConfiguration.CODEC.encodeStart(NbtOps.INSTANCE, this.settings.getDataConfiguration());
      var6.get().ifLeft((var1x) -> {
         var2.merge((CompoundTag)var1x);
      }).ifRight((var0) -> {
         LOGGER.warn("Failed to encode configuration {}", var0.message());
      });
      if (this.customBossEvents != null) {
         var2.put("CustomBossEvents", this.customBossEvents);
      }

      var2.put("ScheduledEvents", this.scheduledEvents.store());
      var2.putInt("WanderingTraderSpawnDelay", this.wanderingTraderSpawnDelay);
      var2.putInt("WanderingTraderSpawnChance", this.wanderingTraderSpawnChance);
      if (this.wanderingTraderId != null) {
         var2.putUUID("WanderingTraderId", this.wanderingTraderId);
      }

   }

   private static ListTag stringCollectionToTag(Set<String> var0) {
      ListTag var1 = new ListTag();
      Stream var10000 = var0.stream().map(StringTag::valueOf);
      Objects.requireNonNull(var1);
      var10000.forEach(var1::add);
      return var1;
   }

   public int getXSpawn() {
      return this.xSpawn;
   }

   public int getYSpawn() {
      return this.ySpawn;
   }

   public int getZSpawn() {
      return this.zSpawn;
   }

   public float getSpawnAngle() {
      return this.spawnAngle;
   }

   public long getGameTime() {
      return this.gameTime;
   }

   public long getDayTime() {
      return this.dayTime;
   }

   private void updatePlayerTag() {
      if (!this.upgradedPlayerTag && this.loadedPlayerTag != null) {
         if (this.playerDataVersion < SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
            if (this.fixerUpper == null) {
               throw (NullPointerException)Util.pauseInIde(new NullPointerException("Fixer Upper not set inside LevelData, and the player tag is not upgraded."));
            }

            this.loadedPlayerTag = DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, this.loadedPlayerTag, this.playerDataVersion);
         }

         this.upgradedPlayerTag = true;
      }
   }

   public CompoundTag getLoadedPlayerTag() {
      this.updatePlayerTag();
      return this.loadedPlayerTag;
   }

   public void setXSpawn(int var1) {
      this.xSpawn = var1;
   }

   public void setYSpawn(int var1) {
      this.ySpawn = var1;
   }

   public void setZSpawn(int var1) {
      this.zSpawn = var1;
   }

   public void setSpawnAngle(float var1) {
      this.spawnAngle = var1;
   }

   public void setGameTime(long var1) {
      this.gameTime = var1;
   }

   public void setDayTime(long var1) {
      this.dayTime = var1;
   }

   public void setSpawn(BlockPos var1, float var2) {
      this.xSpawn = var1.getX();
      this.ySpawn = var1.getY();
      this.zSpawn = var1.getZ();
      this.spawnAngle = var2;
   }

   public String getLevelName() {
      return this.settings.levelName();
   }

   public int getVersion() {
      return this.version;
   }

   public int getClearWeatherTime() {
      return this.clearWeatherTime;
   }

   public void setClearWeatherTime(int var1) {
      this.clearWeatherTime = var1;
   }

   public boolean isThundering() {
      return this.thundering;
   }

   public void setThundering(boolean var1) {
      this.thundering = var1;
   }

   public int getThunderTime() {
      return this.thunderTime;
   }

   public void setThunderTime(int var1) {
      this.thunderTime = var1;
   }

   public boolean isRaining() {
      return this.raining;
   }

   public void setRaining(boolean var1) {
      this.raining = var1;
   }

   public int getRainTime() {
      return this.rainTime;
   }

   public void setRainTime(int var1) {
      this.rainTime = var1;
   }

   public GameType getGameType() {
      return this.settings.gameType();
   }

   public void setGameType(GameType var1) {
      this.settings = this.settings.withGameType(var1);
   }

   public boolean isHardcore() {
      return this.settings.hardcore();
   }

   public boolean getAllowCommands() {
      return this.settings.allowCommands();
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public void setInitialized(boolean var1) {
      this.initialized = var1;
   }

   public GameRules getGameRules() {
      return this.settings.gameRules();
   }

   public WorldBorder.Settings getWorldBorder() {
      return this.worldBorder;
   }

   public void setWorldBorder(WorldBorder.Settings var1) {
      this.worldBorder = var1;
   }

   public Difficulty getDifficulty() {
      return this.settings.difficulty();
   }

   public void setDifficulty(Difficulty var1) {
      this.settings = this.settings.withDifficulty(var1);
   }

   public boolean isDifficultyLocked() {
      return this.difficultyLocked;
   }

   public void setDifficultyLocked(boolean var1) {
      this.difficultyLocked = var1;
   }

   public TimerQueue<MinecraftServer> getScheduledEvents() {
      return this.scheduledEvents;
   }

   public void fillCrashReportCategory(CrashReportCategory var1, LevelHeightAccessor var2) {
      ServerLevelData.super.fillCrashReportCategory(var1, var2);
      WorldData.super.fillCrashReportCategory(var1);
   }

   public WorldOptions worldGenOptions() {
      return this.worldOptions;
   }

   public boolean isFlatWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.FLAT;
   }

   public boolean isDebugWorld() {
      return this.specialWorldProperty == PrimaryLevelData.SpecialWorldProperty.DEBUG;
   }

   public Lifecycle worldGenSettingsLifecycle() {
      return this.worldGenSettingsLifecycle;
   }

   public EndDragonFight.Data endDragonFightData() {
      return this.endDragonFightData;
   }

   public void setEndDragonFightData(EndDragonFight.Data var1) {
      this.endDragonFightData = var1;
   }

   public WorldDataConfiguration getDataConfiguration() {
      return this.settings.getDataConfiguration();
   }

   public void setDataConfiguration(WorldDataConfiguration var1) {
      this.settings = this.settings.withDataConfiguration(var1);
   }

   @Nullable
   public CompoundTag getCustomBossEvents() {
      return this.customBossEvents;
   }

   public void setCustomBossEvents(@Nullable CompoundTag var1) {
      this.customBossEvents = var1;
   }

   public int getWanderingTraderSpawnDelay() {
      return this.wanderingTraderSpawnDelay;
   }

   public void setWanderingTraderSpawnDelay(int var1) {
      this.wanderingTraderSpawnDelay = var1;
   }

   public int getWanderingTraderSpawnChance() {
      return this.wanderingTraderSpawnChance;
   }

   public void setWanderingTraderSpawnChance(int var1) {
      this.wanderingTraderSpawnChance = var1;
   }

   @Nullable
   public UUID getWanderingTraderId() {
      return this.wanderingTraderId;
   }

   public void setWanderingTraderId(UUID var1) {
      this.wanderingTraderId = var1;
   }

   public void setModdedInfo(String var1, boolean var2) {
      this.knownServerBrands.add(var1);
      this.wasModded |= var2;
   }

   public boolean wasModded() {
      return this.wasModded;
   }

   public Set<String> getKnownServerBrands() {
      return ImmutableSet.copyOf(this.knownServerBrands);
   }

   public Set<String> getRemovedFeatureFlags() {
      return Set.copyOf(this.removedFeatureFlags);
   }

   public ServerLevelData overworldData() {
      return this;
   }

   public LevelSettings getLevelSettings() {
      return this.settings.copy();
   }

   /** @deprecated */
   @Deprecated
   public static enum SpecialWorldProperty {
      NONE,
      FLAT,
      DEBUG;

      private SpecialWorldProperty() {
      }

      // $FF: synthetic method
      private static PrimaryLevelData.SpecialWorldProperty[] $values() {
         return new PrimaryLevelData.SpecialWorldProperty[]{NONE, FLAT, DEBUG};
      }
   }
}
