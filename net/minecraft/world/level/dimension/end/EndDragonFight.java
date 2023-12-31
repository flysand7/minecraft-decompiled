package net.minecraft.world.level.dimension.end;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockPredicate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

public class EndDragonFight {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_TICKS_BEFORE_DRAGON_RESPAWN = 1200;
   private static final int TIME_BETWEEN_CRYSTAL_SCANS = 100;
   public static final int TIME_BETWEEN_PLAYER_SCANS = 20;
   private static final int ARENA_SIZE_CHUNKS = 8;
   public static final int ARENA_TICKET_LEVEL = 9;
   private static final int GATEWAY_COUNT = 20;
   private static final int GATEWAY_DISTANCE = 96;
   public static final int DRAGON_SPAWN_Y = 128;
   private final Predicate<Entity> validPlayer;
   private final ServerBossEvent dragonEvent;
   private final ServerLevel level;
   private final BlockPos origin;
   private final ObjectArrayList<Integer> gateways;
   private final BlockPattern exitPortalPattern;
   private int ticksSinceDragonSeen;
   private int crystalsAlive;
   private int ticksSinceCrystalsScanned;
   private int ticksSinceLastPlayerScan;
   private boolean dragonKilled;
   private boolean previouslyKilled;
   private boolean skipArenaLoadedCheck;
   @Nullable
   private UUID dragonUUID;
   private boolean needsStateScanning;
   @Nullable
   private BlockPos portalLocation;
   @Nullable
   private DragonRespawnAnimation respawnStage;
   private int respawnTime;
   @Nullable
   private List<EndCrystal> respawnCrystals;

   public EndDragonFight(ServerLevel var1, long var2, EndDragonFight.Data var4) {
      this(var1, var2, var4, BlockPos.ZERO);
   }

   public EndDragonFight(ServerLevel var1, long var2, EndDragonFight.Data var4, BlockPos var5) {
      this.dragonEvent = (ServerBossEvent)(new ServerBossEvent(Component.translatable("entity.minecraft.ender_dragon"), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)).setPlayBossMusic(true).setCreateWorldFog(true);
      this.gateways = new ObjectArrayList();
      this.ticksSinceLastPlayerScan = 21;
      this.skipArenaLoadedCheck = false;
      this.needsStateScanning = true;
      this.level = var1;
      this.origin = var5;
      this.validPlayer = EntitySelector.ENTITY_STILL_ALIVE.and(EntitySelector.withinDistance((double)var5.getX(), (double)(128 + var5.getY()), (double)var5.getZ(), 192.0D));
      this.needsStateScanning = var4.needsStateScanning;
      this.dragonUUID = (UUID)var4.dragonUUID.orElse((Object)null);
      this.dragonKilled = var4.dragonKilled;
      this.previouslyKilled = var4.previouslyKilled;
      if (var4.isRespawning) {
         this.respawnStage = DragonRespawnAnimation.START;
      }

      this.portalLocation = (BlockPos)var4.exitPortalLocation.orElse((Object)null);
      this.gateways.addAll((Collection)var4.gateways.orElseGet(() -> {
         ObjectArrayList var2x = new ObjectArrayList(ContiguousSet.create(Range.closedOpen(0, 20), DiscreteDomain.integers()));
         Util.shuffle(var2x, RandomSource.create(var2));
         return var2x;
      }));
      this.exitPortalPattern = BlockPatternBuilder.start().aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ").aisle("  ###  ", " #   # ", "#     #", "#  #  #", "#     #", " #   # ", "  ###  ").aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ").where('#', BlockInWorld.hasState(BlockPredicate.forBlock(Blocks.BEDROCK))).build();
   }

   /** @deprecated */
   @Deprecated
   @VisibleForTesting
   public void skipArenaLoadedCheck() {
      this.skipArenaLoadedCheck = true;
   }

   public EndDragonFight.Data saveData() {
      return new EndDragonFight.Data(this.needsStateScanning, this.dragonKilled, this.previouslyKilled, false, Optional.ofNullable(this.dragonUUID), Optional.ofNullable(this.portalLocation), Optional.of(this.gateways));
   }

   public void tick() {
      this.dragonEvent.setVisible(!this.dragonKilled);
      if (++this.ticksSinceLastPlayerScan >= 20) {
         this.updatePlayers();
         this.ticksSinceLastPlayerScan = 0;
      }

      if (!this.dragonEvent.getPlayers().isEmpty()) {
         this.level.getChunkSource().addRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
         boolean var1 = this.isArenaLoaded();
         if (this.needsStateScanning && var1) {
            this.scanState();
            this.needsStateScanning = false;
         }

         if (this.respawnStage != null) {
            if (this.respawnCrystals == null && var1) {
               this.respawnStage = null;
               this.tryRespawn();
            }

            this.respawnStage.tick(this.level, this, this.respawnCrystals, this.respawnTime++, this.portalLocation);
         }

         if (!this.dragonKilled) {
            if ((this.dragonUUID == null || ++this.ticksSinceDragonSeen >= 1200) && var1) {
               this.findOrCreateDragon();
               this.ticksSinceDragonSeen = 0;
            }

            if (++this.ticksSinceCrystalsScanned >= 100 && var1) {
               this.updateCrystalCount();
               this.ticksSinceCrystalsScanned = 0;
            }
         }
      } else {
         this.level.getChunkSource().removeRegionTicket(TicketType.DRAGON, new ChunkPos(0, 0), 9, Unit.INSTANCE);
      }

   }

   private void scanState() {
      LOGGER.info("Scanning for legacy world dragon fight...");
      boolean var1 = this.hasActiveExitPortal();
      if (var1) {
         LOGGER.info("Found that the dragon has been killed in this world already.");
         this.previouslyKilled = true;
      } else {
         LOGGER.info("Found that the dragon has not yet been killed in this world.");
         this.previouslyKilled = false;
         if (this.findExitPortal() == null) {
            this.spawnExitPortal(false);
         }
      }

      List var2 = this.level.getDragons();
      if (var2.isEmpty()) {
         this.dragonKilled = true;
      } else {
         EnderDragon var3 = (EnderDragon)var2.get(0);
         this.dragonUUID = var3.getUUID();
         LOGGER.info("Found that there's a dragon still alive ({})", var3);
         this.dragonKilled = false;
         if (!var1) {
            LOGGER.info("But we didn't have a portal, let's remove it.");
            var3.discard();
            this.dragonUUID = null;
         }
      }

      if (!this.previouslyKilled && this.dragonKilled) {
         this.dragonKilled = false;
      }

   }

   private void findOrCreateDragon() {
      List var1 = this.level.getDragons();
      if (var1.isEmpty()) {
         LOGGER.debug("Haven't seen the dragon, respawning it");
         this.createNewDragon();
      } else {
         LOGGER.debug("Haven't seen our dragon, but found another one to use.");
         this.dragonUUID = ((EnderDragon)var1.get(0)).getUUID();
      }

   }

   protected void setRespawnStage(DragonRespawnAnimation var1) {
      if (this.respawnStage == null) {
         throw new IllegalStateException("Dragon respawn isn't in progress, can't skip ahead in the animation.");
      } else {
         this.respawnTime = 0;
         if (var1 == DragonRespawnAnimation.END) {
            this.respawnStage = null;
            this.dragonKilled = false;
            EnderDragon var2 = this.createNewDragon();
            if (var2 != null) {
               Iterator var3 = this.dragonEvent.getPlayers().iterator();

               while(var3.hasNext()) {
                  ServerPlayer var4 = (ServerPlayer)var3.next();
                  CriteriaTriggers.SUMMONED_ENTITY.trigger(var4, var2);
               }
            }
         } else {
            this.respawnStage = var1;
         }

      }
   }

   private boolean hasActiveExitPortal() {
      for(int var1 = -8; var1 <= 8; ++var1) {
         for(int var2 = -8; var2 <= 8; ++var2) {
            LevelChunk var3 = this.level.getChunk(var1, var2);
            Iterator var4 = var3.getBlockEntities().values().iterator();

            while(var4.hasNext()) {
               BlockEntity var5 = (BlockEntity)var4.next();
               if (var5 instanceof TheEndPortalBlockEntity) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Nullable
   private BlockPattern.BlockPatternMatch findExitPortal() {
      ChunkPos var1 = new ChunkPos(this.origin);

      int var3;
      for(int var2 = -8 + var1.x; var2 <= 8 + var1.x; ++var2) {
         for(var3 = -8 + var1.z; var3 <= 8 + var1.z; ++var3) {
            LevelChunk var4 = this.level.getChunk(var2, var3);
            Iterator var5 = var4.getBlockEntities().values().iterator();

            while(var5.hasNext()) {
               BlockEntity var6 = (BlockEntity)var5.next();
               if (var6 instanceof TheEndPortalBlockEntity) {
                  BlockPattern.BlockPatternMatch var7 = this.exitPortalPattern.find(this.level, var6.getBlockPos());
                  if (var7 != null) {
                     BlockPos var8 = var7.getBlock(3, 3, 3).getPos();
                     if (this.portalLocation == null) {
                        this.portalLocation = var8;
                     }

                     return var7;
                  }
               }
            }
         }
      }

      BlockPos var9 = EndPodiumFeature.getLocation(this.origin);
      var3 = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, var9).getY();

      for(int var10 = var3; var10 >= this.level.getMinBuildHeight(); --var10) {
         BlockPattern.BlockPatternMatch var11 = this.exitPortalPattern.find(this.level, new BlockPos(var9.getX(), var10, var9.getZ()));
         if (var11 != null) {
            if (this.portalLocation == null) {
               this.portalLocation = var11.getBlock(3, 3, 3).getPos();
            }

            return var11;
         }
      }

      return null;
   }

   private boolean isArenaLoaded() {
      if (this.skipArenaLoadedCheck) {
         return true;
      } else {
         ChunkPos var1 = new ChunkPos(this.origin);

         for(int var2 = -8 + var1.x; var2 <= 8 + var1.x; ++var2) {
            for(int var3 = 8 + var1.z; var3 <= 8 + var1.z; ++var3) {
               ChunkAccess var4 = this.level.getChunk(var2, var3, ChunkStatus.FULL, false);
               if (!(var4 instanceof LevelChunk)) {
                  return false;
               }

               FullChunkStatus var5 = ((LevelChunk)var4).getFullStatus();
               if (!var5.isOrAfter(FullChunkStatus.BLOCK_TICKING)) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   private void updatePlayers() {
      HashSet var1 = Sets.newHashSet();
      Iterator var2 = this.level.getPlayers(this.validPlayer).iterator();

      while(var2.hasNext()) {
         ServerPlayer var3 = (ServerPlayer)var2.next();
         this.dragonEvent.addPlayer(var3);
         var1.add(var3);
      }

      HashSet var5 = Sets.newHashSet(this.dragonEvent.getPlayers());
      var5.removeAll(var1);
      Iterator var6 = var5.iterator();

      while(var6.hasNext()) {
         ServerPlayer var4 = (ServerPlayer)var6.next();
         this.dragonEvent.removePlayer(var4);
      }

   }

   private void updateCrystalCount() {
      this.ticksSinceCrystalsScanned = 0;
      this.crystalsAlive = 0;

      SpikeFeature.EndSpike var2;
      for(Iterator var1 = SpikeFeature.getSpikesForLevel(this.level).iterator(); var1.hasNext(); this.crystalsAlive += this.level.getEntitiesOfClass(EndCrystal.class, var2.getTopBoundingBox()).size()) {
         var2 = (SpikeFeature.EndSpike)var1.next();
      }

      LOGGER.debug("Found {} end crystals still alive", this.crystalsAlive);
   }

   public void setDragonKilled(EnderDragon var1) {
      if (var1.getUUID().equals(this.dragonUUID)) {
         this.dragonEvent.setProgress(0.0F);
         this.dragonEvent.setVisible(false);
         this.spawnExitPortal(true);
         this.spawnNewGateway();
         if (!this.previouslyKilled) {
            this.level.setBlockAndUpdate(this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(this.origin)), Blocks.DRAGON_EGG.defaultBlockState());
         }

         this.previouslyKilled = true;
         this.dragonKilled = true;
      }

   }

   /** @deprecated */
   @Deprecated
   @VisibleForTesting
   public void removeAllGateways() {
      this.gateways.clear();
   }

   private void spawnNewGateway() {
      if (!this.gateways.isEmpty()) {
         int var1 = (Integer)this.gateways.remove(this.gateways.size() - 1);
         int var2 = Mth.floor(96.0D * Math.cos(2.0D * (-3.141592653589793D + 0.15707963267948966D * (double)var1)));
         int var3 = Mth.floor(96.0D * Math.sin(2.0D * (-3.141592653589793D + 0.15707963267948966D * (double)var1)));
         this.spawnNewGateway(new BlockPos(var2, 75, var3));
      }
   }

   private void spawnNewGateway(BlockPos var1) {
      this.level.levelEvent(3000, var1, 0);
      this.level.registryAccess().registry(Registries.CONFIGURED_FEATURE).flatMap((var0) -> {
         return var0.getHolder(EndFeatures.END_GATEWAY_DELAYED);
      }).ifPresent((var2) -> {
         ((ConfiguredFeature)var2.value()).place(this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), var1);
      });
   }

   private void spawnExitPortal(boolean var1) {
      EndPodiumFeature var2 = new EndPodiumFeature(var1);
      if (this.portalLocation == null) {
         for(this.portalLocation = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(this.origin)).below(); this.level.getBlockState(this.portalLocation).is(Blocks.BEDROCK) && this.portalLocation.getY() > this.level.getSeaLevel(); this.portalLocation = this.portalLocation.below()) {
         }
      }

      var2.place(FeatureConfiguration.NONE, this.level, this.level.getChunkSource().getGenerator(), RandomSource.create(), this.portalLocation);
   }

   @Nullable
   private EnderDragon createNewDragon() {
      this.level.getChunkAt(new BlockPos(this.origin.getX(), 128 + this.origin.getY(), this.origin.getZ()));
      EnderDragon var1 = (EnderDragon)EntityType.ENDER_DRAGON.create(this.level);
      if (var1 != null) {
         var1.setDragonFight(this);
         var1.setFightOrigin(this.origin);
         var1.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
         var1.moveTo((double)this.origin.getX(), (double)(128 + this.origin.getY()), (double)this.origin.getZ(), this.level.random.nextFloat() * 360.0F, 0.0F);
         this.level.addFreshEntity(var1);
         this.dragonUUID = var1.getUUID();
      }

      return var1;
   }

   public void updateDragon(EnderDragon var1) {
      if (var1.getUUID().equals(this.dragonUUID)) {
         this.dragonEvent.setProgress(var1.getHealth() / var1.getMaxHealth());
         this.ticksSinceDragonSeen = 0;
         if (var1.hasCustomName()) {
            this.dragonEvent.setName(var1.getDisplayName());
         }
      }

   }

   public int getCrystalsAlive() {
      return this.crystalsAlive;
   }

   public void onCrystalDestroyed(EndCrystal var1, DamageSource var2) {
      if (this.respawnStage != null && this.respawnCrystals.contains(var1)) {
         LOGGER.debug("Aborting respawn sequence");
         this.respawnStage = null;
         this.respawnTime = 0;
         this.resetSpikeCrystals();
         this.spawnExitPortal(true);
      } else {
         this.updateCrystalCount();
         Entity var3 = this.level.getEntity(this.dragonUUID);
         if (var3 instanceof EnderDragon) {
            ((EnderDragon)var3).onCrystalDestroyed(var1, var1.blockPosition(), var2);
         }
      }

   }

   public boolean hasPreviouslyKilledDragon() {
      return this.previouslyKilled;
   }

   public void tryRespawn() {
      if (this.dragonKilled && this.respawnStage == null) {
         BlockPos var1 = this.portalLocation;
         if (var1 == null) {
            LOGGER.debug("Tried to respawn, but need to find the portal first.");
            BlockPattern.BlockPatternMatch var2 = this.findExitPortal();
            if (var2 == null) {
               LOGGER.debug("Couldn't find a portal, so we made one.");
               this.spawnExitPortal(true);
            } else {
               LOGGER.debug("Found the exit portal & saved its location for next time.");
            }

            var1 = this.portalLocation;
         }

         ArrayList var7 = Lists.newArrayList();
         BlockPos var3 = var1.above(1);
         Iterator var4 = Direction.Plane.HORIZONTAL.iterator();

         while(var4.hasNext()) {
            Direction var5 = (Direction)var4.next();
            List var6 = this.level.getEntitiesOfClass(EndCrystal.class, new AABB(var3.relative((Direction)var5, 2)));
            if (var6.isEmpty()) {
               return;
            }

            var7.addAll(var6);
         }

         LOGGER.debug("Found all crystals, respawning dragon.");
         this.respawnDragon(var7);
      }

   }

   private void respawnDragon(List<EndCrystal> var1) {
      if (this.dragonKilled && this.respawnStage == null) {
         for(BlockPattern.BlockPatternMatch var2 = this.findExitPortal(); var2 != null; var2 = this.findExitPortal()) {
            for(int var3 = 0; var3 < this.exitPortalPattern.getWidth(); ++var3) {
               for(int var4 = 0; var4 < this.exitPortalPattern.getHeight(); ++var4) {
                  for(int var5 = 0; var5 < this.exitPortalPattern.getDepth(); ++var5) {
                     BlockInWorld var6 = var2.getBlock(var3, var4, var5);
                     if (var6.getState().is(Blocks.BEDROCK) || var6.getState().is(Blocks.END_PORTAL)) {
                        this.level.setBlockAndUpdate(var6.getPos(), Blocks.END_STONE.defaultBlockState());
                     }
                  }
               }
            }
         }

         this.respawnStage = DragonRespawnAnimation.START;
         this.respawnTime = 0;
         this.spawnExitPortal(false);
         this.respawnCrystals = var1;
      }

   }

   public void resetSpikeCrystals() {
      Iterator var1 = SpikeFeature.getSpikesForLevel(this.level).iterator();

      while(var1.hasNext()) {
         SpikeFeature.EndSpike var2 = (SpikeFeature.EndSpike)var1.next();
         List var3 = this.level.getEntitiesOfClass(EndCrystal.class, var2.getTopBoundingBox());
         Iterator var4 = var3.iterator();

         while(var4.hasNext()) {
            EndCrystal var5 = (EndCrystal)var4.next();
            var5.setInvulnerable(false);
            var5.setBeamTarget((BlockPos)null);
         }
      }

   }

   @Nullable
   public UUID getDragonUUID() {
      return this.dragonUUID;
   }

   public static record Data(boolean c, boolean d, boolean e, boolean f, Optional<UUID> g, Optional<BlockPos> h, Optional<List<Integer>> i) {
      final boolean needsStateScanning;
      final boolean dragonKilled;
      final boolean previouslyKilled;
      final boolean isRespawning;
      final Optional<UUID> dragonUUID;
      final Optional<BlockPos> exitPortalLocation;
      final Optional<List<Integer>> gateways;
      public static final Codec<EndDragonFight.Data> CODEC = RecordCodecBuilder.create((var0) -> {
         return var0.group(Codec.BOOL.fieldOf("NeedsStateScanning").orElse(true).forGetter(EndDragonFight.Data::needsStateScanning), Codec.BOOL.fieldOf("DragonKilled").orElse(false).forGetter(EndDragonFight.Data::dragonKilled), Codec.BOOL.fieldOf("PreviouslyKilled").orElse(false).forGetter(EndDragonFight.Data::previouslyKilled), Codec.BOOL.optionalFieldOf("IsRespawning", false).forGetter(EndDragonFight.Data::isRespawning), UUIDUtil.CODEC.optionalFieldOf("Dragon").forGetter(EndDragonFight.Data::dragonUUID), BlockPos.CODEC.optionalFieldOf("ExitPortalLocation").forGetter(EndDragonFight.Data::exitPortalLocation), Codec.list(Codec.INT).optionalFieldOf("Gateways").forGetter(EndDragonFight.Data::gateways)).apply(var0, EndDragonFight.Data::new);
      });
      public static final EndDragonFight.Data DEFAULT = new EndDragonFight.Data(true, false, false, false, Optional.empty(), Optional.empty(), Optional.empty());

      public Data(boolean var1, boolean var2, boolean var3, boolean var4, Optional<UUID> var5, Optional<BlockPos> var6, Optional<List<Integer>> var7) {
         this.needsStateScanning = var1;
         this.dragonKilled = var2;
         this.previouslyKilled = var3;
         this.isRespawning = var4;
         this.dragonUUID = var5;
         this.exitPortalLocation = var6;
         this.gateways = var7;
      }

      public boolean needsStateScanning() {
         return this.needsStateScanning;
      }

      public boolean dragonKilled() {
         return this.dragonKilled;
      }

      public boolean previouslyKilled() {
         return this.previouslyKilled;
      }

      public boolean isRespawning() {
         return this.isRespawning;
      }

      public Optional<UUID> dragonUUID() {
         return this.dragonUUID;
      }

      public Optional<BlockPos> exitPortalLocation() {
         return this.exitPortalLocation;
      }

      public Optional<List<Integer>> gateways() {
         return this.gateways;
      }
   }
}
