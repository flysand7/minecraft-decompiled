package net.minecraft.server.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;

public class ChunkMap extends ChunkStorage implements ChunkHolder.PlayerProvider {
   private static final byte CHUNK_TYPE_REPLACEABLE = -1;
   private static final byte CHUNK_TYPE_UNKNOWN = 0;
   private static final byte CHUNK_TYPE_FULL = 1;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int CHUNK_SAVED_PER_TICK = 200;
   private static final int CHUNK_SAVED_EAGERLY_PER_TICK = 20;
   private static final int EAGER_CHUNK_SAVE_COOLDOWN_IN_MILLIS = 10000;
   private static final int MIN_VIEW_DISTANCE = 2;
   public static final int MAX_VIEW_DISTANCE = 32;
   public static final int FORCED_TICKET_LEVEL;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap = new Long2ObjectLinkedOpenHashMap();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingUnloads;
   private final LongSet entitiesInLevel;
   final ServerLevel level;
   private final ThreadedLevelLightEngine lightEngine;
   private final BlockableEventLoop<Runnable> mainThreadExecutor;
   private ChunkGenerator generator;
   private final RandomState randomState;
   private final ChunkGeneratorStructureState chunkGeneratorState;
   private final Supplier<DimensionDataStorage> overworldDataStorage;
   private final PoiManager poiManager;
   final LongSet toDrop;
   private boolean modified;
   private final ChunkTaskPriorityQueueSorter queueSorter;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> worldgenMailbox;
   private final ProcessorHandle<ChunkTaskPriorityQueueSorter.Message<Runnable>> mainThreadMailbox;
   private final ChunkProgressListener progressListener;
   private final ChunkStatusUpdateListener chunkStatusListener;
   private final ChunkMap.DistanceManager distanceManager;
   private final AtomicInteger tickingGenerated;
   private final StructureTemplateManager structureTemplateManager;
   private final String storageName;
   private final PlayerMap playerMap;
   private final Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
   private final Long2ByteMap chunkTypeCache;
   private final Long2LongMap chunkSaveCooldowns;
   private final Queue<Runnable> unloadQueue;
   int viewDistance;

   public ChunkMap(ServerLevel var1, LevelStorageSource.LevelStorageAccess var2, DataFixer var3, StructureTemplateManager var4, Executor var5, BlockableEventLoop<Runnable> var6, LightChunkGetter var7, ChunkGenerator var8, ChunkProgressListener var9, ChunkStatusUpdateListener var10, Supplier<DimensionDataStorage> var11, int var12, boolean var13) {
      super(var2.getDimensionPath(var1.dimension()).resolve("region"), var3, var13);
      this.visibleChunkMap = this.updatingChunkMap.clone();
      this.pendingUnloads = new Long2ObjectLinkedOpenHashMap();
      this.entitiesInLevel = new LongOpenHashSet();
      this.toDrop = new LongOpenHashSet();
      this.tickingGenerated = new AtomicInteger();
      this.playerMap = new PlayerMap();
      this.entityMap = new Int2ObjectOpenHashMap();
      this.chunkTypeCache = new Long2ByteOpenHashMap();
      this.chunkSaveCooldowns = new Long2LongOpenHashMap();
      this.unloadQueue = Queues.newConcurrentLinkedQueue();
      this.structureTemplateManager = var4;
      Path var14 = var2.getDimensionPath(var1.dimension());
      this.storageName = var14.getFileName().toString();
      this.level = var1;
      this.generator = var8;
      RegistryAccess var15 = var1.registryAccess();
      long var16 = var1.getSeed();
      if (var8 instanceof NoiseBasedChunkGenerator) {
         NoiseBasedChunkGenerator var18 = (NoiseBasedChunkGenerator)var8;
         this.randomState = RandomState.create((NoiseGeneratorSettings)((NoiseGeneratorSettings)var18.generatorSettings().value()), (HolderGetter)var15.lookupOrThrow(Registries.NOISE), var16);
      } else {
         this.randomState = RandomState.create((NoiseGeneratorSettings)NoiseGeneratorSettings.dummy(), (HolderGetter)var15.lookupOrThrow(Registries.NOISE), var16);
      }

      this.chunkGeneratorState = var8.createState(var15.lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, var16);
      this.mainThreadExecutor = var6;
      ProcessorMailbox var21 = ProcessorMailbox.create(var5, "worldgen");
      Objects.requireNonNull(var6);
      ProcessorHandle var19 = ProcessorHandle.of("main", var6::tell);
      this.progressListener = var9;
      this.chunkStatusListener = var10;
      ProcessorMailbox var20 = ProcessorMailbox.create(var5, "light");
      this.queueSorter = new ChunkTaskPriorityQueueSorter(ImmutableList.of(var21, var19, var20), var5, Integer.MAX_VALUE);
      this.worldgenMailbox = this.queueSorter.getProcessor(var21, false);
      this.mainThreadMailbox = this.queueSorter.getProcessor(var19, false);
      this.lightEngine = new ThreadedLevelLightEngine(var7, this, this.level.dimensionType().hasSkyLight(), var20, this.queueSorter.getProcessor(var20, false));
      this.distanceManager = new ChunkMap.DistanceManager(var5, var6);
      this.overworldDataStorage = var11;
      this.poiManager = new PoiManager(var14.resolve("poi"), var3, var13, var15, var1);
      this.setViewDistance(var12);
   }

   protected ChunkGenerator generator() {
      return this.generator;
   }

   protected ChunkGeneratorStructureState generatorState() {
      return this.chunkGeneratorState;
   }

   protected RandomState randomState() {
      return this.randomState;
   }

   public void debugReloadGenerator() {
      DataResult var1 = ChunkGenerator.CODEC.encodeStart(JsonOps.INSTANCE, this.generator);
      DataResult var2 = var1.flatMap((var0) -> {
         return ChunkGenerator.CODEC.parse(JsonOps.INSTANCE, var0);
      });
      var2.result().ifPresent((var1x) -> {
         this.generator = var1x;
      });
   }

   private static double euclideanDistanceSquared(ChunkPos var0, Entity var1) {
      double var2 = (double)SectionPos.sectionToBlockCoord(var0.x, 8);
      double var4 = (double)SectionPos.sectionToBlockCoord(var0.z, 8);
      double var6 = var2 - var1.getX();
      double var8 = var4 - var1.getZ();
      return var6 * var6 + var8 * var8;
   }

   public static boolean isChunkInRange(int var0, int var1, int var2, int var3, int var4) {
      int var5 = Math.max(0, Math.abs(var0 - var2) - 1);
      int var6 = Math.max(0, Math.abs(var1 - var3) - 1);
      long var7 = (long)Math.max(0, Math.max(var5, var6) - 1);
      long var9 = (long)Math.min(var5, var6);
      long var11 = var9 * var9 + var7 * var7;
      int var13 = var4 * var4;
      return var11 < (long)var13;
   }

   private static boolean isChunkOnRangeBorder(int var0, int var1, int var2, int var3, int var4) {
      if (!isChunkInRange(var0, var1, var2, var3, var4)) {
         return false;
      } else {
         return !isChunkInRange(var0 + 1, var1 + 1, var2, var3, var4) || !isChunkInRange(var0 - 1, var1 + 1, var2, var3, var4) || !isChunkInRange(var0 + 1, var1 - 1, var2, var3, var4) || !isChunkInRange(var0 - 1, var1 - 1, var2, var3, var4);
      }
   }

   protected ThreadedLevelLightEngine getLightEngine() {
      return this.lightEngine;
   }

   @Nullable
   protected ChunkHolder getUpdatingChunkIfPresent(long var1) {
      return (ChunkHolder)this.updatingChunkMap.get(var1);
   }

   @Nullable
   protected ChunkHolder getVisibleChunkIfPresent(long var1) {
      return (ChunkHolder)this.visibleChunkMap.get(var1);
   }

   protected IntSupplier getChunkQueueLevel(long var1) {
      return () -> {
         ChunkHolder var3 = this.getVisibleChunkIfPresent(var1);
         return var3 == null ? ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1 : Math.min(var3.getQueueLevel(), ChunkTaskPriorityQueue.PRIORITY_LEVEL_COUNT - 1);
      };
   }

   public String getChunkDebugData(ChunkPos var1) {
      ChunkHolder var2 = this.getVisibleChunkIfPresent(var1.toLong());
      if (var2 == null) {
         return "null";
      } else {
         String var3 = var2.getTicketLevel() + "\n";
         ChunkStatus var4 = var2.getLastAvailableStatus();
         ChunkAccess var5 = var2.getLastAvailable();
         if (var4 != null) {
            var3 = var3 + "St: \u00a7" + var4.getIndex() + var4 + "\u00a7r\n";
         }

         if (var5 != null) {
            var3 = var3 + "Ch: \u00a7" + var5.getStatus().getIndex() + var5.getStatus() + "\u00a7r\n";
         }

         FullChunkStatus var6 = var2.getFullStatus();
         var3 = var3 + "\u00a7" + var6.ordinal() + var6;
         return var3 + "\u00a7r";
      }
   }

   private CompletableFuture<Either<List<ChunkAccess>, ChunkHolder.ChunkLoadingFailure>> getChunkRangeFuture(ChunkHolder var1, int var2, IntFunction<ChunkStatus> var3) {
      if (var2 == 0) {
         ChunkStatus var18 = (ChunkStatus)var3.apply(0);
         return var1.getOrScheduleFuture(var18, this).thenApply((var0) -> {
            return var0.mapLeft(List::of);
         });
      } else {
         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();
         ChunkPos var6 = var1.getPos();
         int var7 = var6.x;
         int var8 = var6.z;

         for(int var9 = -var2; var9 <= var2; ++var9) {
            for(int var10 = -var2; var10 <= var2; ++var10) {
               int var11 = Math.max(Math.abs(var10), Math.abs(var9));
               final ChunkPos var12 = new ChunkPos(var7 + var10, var8 + var9);
               long var13 = var12.toLong();
               ChunkHolder var15 = this.getUpdatingChunkIfPresent(var13);
               if (var15 == null) {
                  return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                     public String toString() {
                        return "Unloaded " + var12;
                     }
                  }));
               }

               ChunkStatus var16 = (ChunkStatus)var3.apply(var11);
               CompletableFuture var17 = var15.getOrScheduleFuture(var16, this);
               var5.add(var15);
               var4.add(var17);
            }
         }

         CompletableFuture var19 = Util.sequence(var4);
         CompletableFuture var20 = var19.thenApply((var4x) -> {
            ArrayList var5 = Lists.newArrayList();
            final int var6 = 0;

            for(Iterator var7x = var4x.iterator(); var7x.hasNext(); ++var6) {
               final Either var8x = (Either)var7x.next();
               if (var8x == null) {
                  throw this.debugFuturesAndCreateReportedException(new IllegalStateException("At least one of the chunk futures were null"), "n/a");
               }

               Optional var9 = var8x.left();
               if (!var9.isPresent()) {
                  return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                     public String toString() {
                        ChunkPos var10000 = new ChunkPos(var1 + var6 % (var2 * 2 + 1), var3 + var6 / (var2 * 2 + 1));
                        return "Unloaded " + var10000 + " " + var8.right().get();
                     }
                  });
               }

               var5.add((ChunkAccess)var9.get());
            }

            return Either.left(var5);
         });
         Iterator var21 = var5.iterator();

         while(var21.hasNext()) {
            ChunkHolder var22 = (ChunkHolder)var21.next();
            var22.addSaveDependency("getChunkRangeFuture " + var6 + " " + var2, var20);
         }

         return var20;
      }
   }

   public ReportedException debugFuturesAndCreateReportedException(IllegalStateException var1, String var2) {
      StringBuilder var3 = new StringBuilder();
      Consumer var4 = (var1x) -> {
         var1x.getAllFutures().forEach((var2) -> {
            ChunkStatus var3x = (ChunkStatus)var2.getFirst();
            CompletableFuture var4 = (CompletableFuture)var2.getSecond();
            if (var4 != null && var4.isDone() && var4.join() == null) {
               var3.append(var1x.getPos()).append(" - status: ").append(var3x).append(" future: ").append(var4).append(System.lineSeparator());
            }

         });
      };
      var3.append("Updating:").append(System.lineSeparator());
      this.updatingChunkMap.values().forEach(var4);
      var3.append("Visible:").append(System.lineSeparator());
      this.visibleChunkMap.values().forEach(var4);
      CrashReport var5 = CrashReport.forThrowable(var1, "Chunk loading");
      CrashReportCategory var6 = var5.addCategory("Chunk loading");
      var6.setDetail("Details", (Object)var2);
      var6.setDetail("Futures", (Object)var3);
      return new ReportedException(var5);
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingChunk(ChunkHolder var1) {
      return this.getChunkRangeFuture(var1, 2, (var0) -> {
         return ChunkStatus.FULL;
      }).thenApplyAsync((var0) -> {
         return var0.mapLeft((var0x) -> {
            return (LevelChunk)var0x.get(var0x.size() / 2);
         });
      }, this.mainThreadExecutor);
   }

   @Nullable
   ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5) {
      if (!ChunkLevel.isLoaded(var5) && !ChunkLevel.isLoaded(var3)) {
         return var4;
      } else {
         if (var4 != null) {
            var4.setTicketLevel(var3);
         }

         if (var4 != null) {
            if (!ChunkLevel.isLoaded(var3)) {
               this.toDrop.add(var1);
            } else {
               this.toDrop.remove(var1);
            }
         }

         if (ChunkLevel.isLoaded(var3) && var4 == null) {
            var4 = (ChunkHolder)this.pendingUnloads.remove(var1);
            if (var4 != null) {
               var4.setTicketLevel(var3);
            } else {
               var4 = new ChunkHolder(new ChunkPos(var1), var3, this.level, this.lightEngine, this.queueSorter, this);
            }

            this.updatingChunkMap.put(var1, var4);
            this.modified = true;
         }

         return var4;
      }
   }

   public void close() throws IOException {
      try {
         this.queueSorter.close();
         this.poiManager.close();
      } finally {
         super.close();
      }

   }

   protected void saveAllChunks(boolean var1) {
      if (var1) {
         List var2 = (List)this.visibleChunkMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).collect(Collectors.toList());
         MutableBoolean var3 = new MutableBoolean();

         do {
            var3.setFalse();
            var2.stream().map((var1x) -> {
               CompletableFuture var2;
               do {
                  var2 = var1x.getChunkToSave();
                  BlockableEventLoop var10000 = this.mainThreadExecutor;
                  Objects.requireNonNull(var2);
                  var10000.managedBlock(var2::isDone);
               } while(var2 != var1x.getChunkToSave());

               return (ChunkAccess)var2.join();
            }).filter((var0) -> {
               return var0 instanceof ImposterProtoChunk || var0 instanceof LevelChunk;
            }).filter(this::save).forEach((var1x) -> {
               var3.setTrue();
            });
         } while(var3.isTrue());

         this.processUnloads(() -> {
            return true;
         });
         this.flushWorker();
      } else {
         this.visibleChunkMap.values().forEach(this::saveChunkIfNeeded);
      }

   }

   protected void tick(BooleanSupplier var1) {
      ProfilerFiller var2 = this.level.getProfiler();
      var2.push("poi");
      this.poiManager.tick(var1);
      var2.popPush("chunk_unload");
      if (!this.level.noSave()) {
         this.processUnloads(var1);
      }

      var2.pop();
   }

   public boolean hasWork() {
      return this.lightEngine.hasLightWork() || !this.pendingUnloads.isEmpty() || !this.updatingChunkMap.isEmpty() || this.poiManager.hasWork() || !this.toDrop.isEmpty() || !this.unloadQueue.isEmpty() || this.queueSorter.hasWork() || this.distanceManager.hasTickets();
   }

   private void processUnloads(BooleanSupplier var1) {
      LongIterator var2 = this.toDrop.iterator();

      for(int var3 = 0; var2.hasNext() && (var1.getAsBoolean() || var3 < 200 || this.toDrop.size() > 2000); var2.remove()) {
         long var4 = var2.nextLong();
         ChunkHolder var6 = (ChunkHolder)this.updatingChunkMap.remove(var4);
         if (var6 != null) {
            this.pendingUnloads.put(var4, var6);
            this.modified = true;
            ++var3;
            this.scheduleUnload(var4, var6);
         }
      }

      int var5 = Math.max(0, this.unloadQueue.size() - 2000);

      Runnable var8;
      while((var1.getAsBoolean() || var5 > 0) && (var8 = (Runnable)this.unloadQueue.poll()) != null) {
         --var5;
         var8.run();
      }

      int var9 = 0;
      ObjectIterator var7 = this.visibleChunkMap.values().iterator();

      while(var9 < 20 && var1.getAsBoolean() && var7.hasNext()) {
         if (this.saveChunkIfNeeded((ChunkHolder)var7.next())) {
            ++var9;
         }
      }

   }

   private void scheduleUnload(long var1, ChunkHolder var3) {
      CompletableFuture var4 = var3.getChunkToSave();
      Consumer var10001 = (var5) -> {
         CompletableFuture var6 = var3.getChunkToSave();
         if (var6 != var4) {
            this.scheduleUnload(var1, var3);
         } else {
            if (this.pendingUnloads.remove(var1, var3) && var5 != null) {
               if (var5 instanceof LevelChunk) {
                  ((LevelChunk)var5).setLoaded(false);
               }

               this.save(var5);
               if (this.entitiesInLevel.remove(var1) && var5 instanceof LevelChunk) {
                  LevelChunk var7 = (LevelChunk)var5;
                  this.level.unload(var7);
               }

               this.lightEngine.updateChunkStatus(var5.getPos());
               this.lightEngine.tryScheduleUpdate();
               this.progressListener.onStatusChange(var5.getPos(), (ChunkStatus)null);
               this.chunkSaveCooldowns.remove(var5.getPos().toLong());
            }

         }
      };
      Queue var10002 = this.unloadQueue;
      Objects.requireNonNull(var10002);
      var4.thenAcceptAsync(var10001, var10002::add).whenComplete((var1x, var2) -> {
         if (var2 != null) {
            LOGGER.error("Failed to save chunk {}", var3.getPos(), var2);
         }

      });
   }

   protected boolean promoteChunkMap() {
      if (!this.modified) {
         return false;
      } else {
         this.visibleChunkMap = this.updatingChunkMap.clone();
         this.modified = false;
         return true;
      }
   }

   public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> schedule(ChunkHolder var1, ChunkStatus var2) {
      ChunkPos var3 = var1.getPos();
      if (var2 == ChunkStatus.EMPTY) {
         return this.scheduleChunkLoad(var3);
      } else {
         if (var2 == ChunkStatus.LIGHT) {
            this.distanceManager.addTicket(TicketType.LIGHT, var3, ChunkLevel.byStatus(ChunkStatus.LIGHT), var3);
         }

         if (!var2.hasLoadDependencies()) {
            Optional var4 = ((Either)var1.getOrScheduleFuture(var2.getParent(), this).getNow(ChunkHolder.UNLOADED_CHUNK)).left();
            if (var4.isPresent() && ((ChunkAccess)var4.get()).getStatus().isOrAfter(var2)) {
               CompletableFuture var5 = var2.load(this.level, this.structureTemplateManager, this.lightEngine, (var2x) -> {
                  return this.protoChunkToFullChunk(var1);
               }, (ChunkAccess)var4.get());
               this.progressListener.onStatusChange(var3, var2);
               return var5;
            }
         }

         return this.scheduleChunkGeneration(var1, var2);
      }
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkLoad(ChunkPos var1) {
      return this.readChunk(var1).thenApply((var1x) -> {
         return var1x.filter((var1xx) -> {
            boolean var2 = isChunkDataValid(var1xx);
            if (!var2) {
               LOGGER.error("Chunk file at {} is missing level data, skipping", var1);
            }

            return var2;
         });
      }).thenApplyAsync((var2) -> {
         this.level.getProfiler().incrementCounter("chunkLoad");
         if (var2.isPresent()) {
            ProtoChunk var3 = ChunkSerializer.read(this.level, this.poiManager, var1, (CompoundTag)var2.get());
            this.markPosition(var1, var3.getStatus().getChunkType());
            return Either.left(var3);
         } else {
            return Either.left(this.createEmptyChunk(var1));
         }
      }, this.mainThreadExecutor).exceptionallyAsync((var2) -> {
         return this.handleChunkLoadFailure(var2, var1);
      }, this.mainThreadExecutor);
   }

   private static boolean isChunkDataValid(CompoundTag var0) {
      return var0.contains("Status", 8);
   }

   private Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> handleChunkLoadFailure(Throwable var1, ChunkPos var2) {
      if (var1 instanceof ReportedException) {
         ReportedException var3 = (ReportedException)var1;
         Throwable var4 = var3.getCause();
         if (!(var4 instanceof IOException)) {
            this.markPositionReplaceable(var2);
            throw var3;
         }

         LOGGER.error("Couldn't load chunk {}", var2, var4);
      } else if (var1 instanceof IOException) {
         LOGGER.error("Couldn't load chunk {}", var2, var1);
      }

      return Either.left(this.createEmptyChunk(var2));
   }

   private ChunkAccess createEmptyChunk(ChunkPos var1) {
      this.markPositionReplaceable(var1);
      return new ProtoChunk(var1, UpgradeData.EMPTY, this.level, this.level.registryAccess().registryOrThrow(Registries.BIOME), (BlendingData)null);
   }

   private void markPositionReplaceable(ChunkPos var1) {
      this.chunkTypeCache.put(var1.toLong(), (byte)-1);
   }

   private byte markPosition(ChunkPos var1, ChunkStatus.ChunkType var2) {
      return this.chunkTypeCache.put(var1.toLong(), (byte)(var2 == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleChunkGeneration(ChunkHolder var1, ChunkStatus var2) {
      ChunkPos var3 = var1.getPos();
      CompletableFuture var4 = this.getChunkRangeFuture(var1, var2.getRange(), (var2x) -> {
         return this.getDependencyStatus(var2, var2x);
      });
      this.level.getProfiler().incrementCounter(() -> {
         return "chunkGenerate " + var2;
      });
      Executor var5 = (var2x) -> {
         this.worldgenMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      };
      return var4.thenComposeAsync((var5x) -> {
         return (CompletionStage)var5x.map((var5xx) -> {
            try {
               ChunkAccess var6 = (ChunkAccess)var5xx.get(var5xx.size() / 2);
               CompletableFuture var10;
               if (var6.getStatus().isOrAfter(var2)) {
                  var10 = var2.load(this.level, this.structureTemplateManager, this.lightEngine, (var2x) -> {
                     return this.protoChunkToFullChunk(var1);
                  }, var6);
               } else {
                  var10 = var2.generate(var5, this.level, this.generator, this.structureTemplateManager, this.lightEngine, (var2x) -> {
                     return this.protoChunkToFullChunk(var1);
                  }, var5xx);
               }

               this.progressListener.onStatusChange(var3, var2);
               return var10;
            } catch (Exception var9) {
               var9.getStackTrace();
               CrashReport var7 = CrashReport.forThrowable(var9, "Exception generating new chunk");
               CrashReportCategory var8 = var7.addCategory("Chunk to be generated");
               var8.setDetail("Location", (Object)String.format(Locale.ROOT, "%d,%d", var3.x, var3.z));
               var8.setDetail("Position hash", (Object)ChunkPos.asLong(var3.x, var3.z));
               var8.setDetail("Generator", (Object)this.generator);
               this.mainThreadExecutor.execute(() -> {
                  throw new ReportedException(var7);
               });
               throw new ReportedException(var7);
            }
         }, (var2x) -> {
            this.releaseLightTicket(var3);
            return CompletableFuture.completedFuture(Either.right(var2x));
         });
      }, var5);
   }

   protected void releaseLightTicket(ChunkPos var1) {
      this.mainThreadExecutor.tell(Util.name(() -> {
         this.distanceManager.removeTicket(TicketType.LIGHT, var1, ChunkLevel.byStatus(ChunkStatus.LIGHT), var1);
      }, () -> {
         return "release light ticket " + var1;
      }));
   }

   private ChunkStatus getDependencyStatus(ChunkStatus var1, int var2) {
      ChunkStatus var3;
      if (var2 == 0) {
         var3 = var1.getParent();
      } else {
         var3 = ChunkStatus.getStatusAroundFullChunk(ChunkStatus.getDistance(var1) + var2);
      }

      return var3;
   }

   private static void postLoadProtoChunk(ServerLevel var0, List<CompoundTag> var1) {
      if (!var1.isEmpty()) {
         var0.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(var1, var0));
      }

   }

   private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> protoChunkToFullChunk(ChunkHolder var1) {
      CompletableFuture var2 = var1.getFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
      return var2.thenApplyAsync((var2x) -> {
         ChunkStatus var3 = ChunkLevel.generationStatus(var1.getTicketLevel());
         return !var3.isOrAfter(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : var2x.mapLeft((var2) -> {
            ChunkPos var3 = var1.getPos();
            ProtoChunk var4 = (ProtoChunk)var2;
            LevelChunk var5;
            if (var4 instanceof ImposterProtoChunk) {
               var5 = ((ImposterProtoChunk)var4).getWrapped();
            } else {
               var5 = new LevelChunk(this.level, var4, (var2x) -> {
                  postLoadProtoChunk(this.level, var4.getEntities());
               });
               var1.replaceProtoChunk(new ImposterProtoChunk(var5, false));
            }

            var5.setFullStatus(() -> {
               return ChunkLevel.fullStatus(var1.getTicketLevel());
            });
            var5.runPostLoad();
            if (this.entitiesInLevel.add(var3.toLong())) {
               var5.setLoaded(true);
               var5.registerAllBlockEntitiesAfterLevelLoad();
               var5.registerTickContainerInLevel(this.level);
            }

            return var5;
         });
      }, (var2x) -> {
         ProcessorHandle var10000 = this.mainThreadMailbox;
         long var10002 = var1.getPos().toLong();
         Objects.requireNonNull(var1);
         var10000.tell(ChunkTaskPriorityQueueSorter.message(var2x, var10002, var1::getTicketLevel));
      });
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareTickingChunk(ChunkHolder var1) {
      CompletableFuture var2 = this.getChunkRangeFuture(var1, 1, (var0) -> {
         return ChunkStatus.FULL;
      });
      CompletableFuture var3 = var2.thenApplyAsync((var0) -> {
         return var0.mapLeft((var0x) -> {
            return (LevelChunk)var0x.get(var0x.size() / 2);
         });
      }, (var2x) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      }).thenApplyAsync((var1x) -> {
         return var1x.ifLeft((var1) -> {
            var1.postProcessGeneration();
            this.level.startTickingChunk(var1);
         });
      }, this.mainThreadExecutor);
      var3.handle((var1x, var2x) -> {
         this.tickingGenerated.getAndIncrement();
         return null;
      });
      var3.thenAcceptAsync((var2x) -> {
         var2x.ifLeft((var2) -> {
            MutableObject var3 = new MutableObject();
            this.getPlayers(var1.getPos(), false).forEach((var3x) -> {
               this.playerLoadedChunk(var3x, var3, var2);
            });
         });
      }, (var2x) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2x));
      });
      return var3;
   }

   public CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleChunk(ChunkHolder var1) {
      return this.getChunkRangeFuture(var1, 1, ChunkStatus::getStatusAroundFullChunk).thenApplyAsync((var0) -> {
         return var0.mapLeft((var0x) -> {
            LevelChunk var1 = (LevelChunk)var0x.get(var0x.size() / 2);
            return var1;
         });
      }, (var2) -> {
         this.mainThreadMailbox.tell(ChunkTaskPriorityQueueSorter.message(var1, var2));
      });
   }

   public int getTickingGenerated() {
      return this.tickingGenerated.get();
   }

   private boolean saveChunkIfNeeded(ChunkHolder var1) {
      if (!var1.wasAccessibleSinceLastSave()) {
         return false;
      } else {
         ChunkAccess var2 = (ChunkAccess)var1.getChunkToSave().getNow((Object)null);
         if (!(var2 instanceof ImposterProtoChunk) && !(var2 instanceof LevelChunk)) {
            return false;
         } else {
            long var3 = var2.getPos().toLong();
            long var5 = this.chunkSaveCooldowns.getOrDefault(var3, -1L);
            long var7 = System.currentTimeMillis();
            if (var7 < var5) {
               return false;
            } else {
               boolean var9 = this.save(var2);
               var1.refreshAccessibility();
               if (var9) {
                  this.chunkSaveCooldowns.put(var3, var7 + 10000L);
               }

               return var9;
            }
         }
      }
   }

   private boolean save(ChunkAccess var1) {
      this.poiManager.flush(var1.getPos());
      if (!var1.isUnsaved()) {
         return false;
      } else {
         var1.setUnsaved(false);
         ChunkPos var2 = var1.getPos();

         try {
            ChunkStatus var3 = var1.getStatus();
            if (var3.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
               if (this.isExistingChunkFull(var2)) {
                  return false;
               }

               if (var3 == ChunkStatus.EMPTY && var1.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                  return false;
               }
            }

            this.level.getProfiler().incrementCounter("chunkSave");
            CompoundTag var4 = ChunkSerializer.write(this.level, var1);
            this.write(var2, var4);
            this.markPosition(var2, var3.getChunkType());
            return true;
         } catch (Exception var5) {
            LOGGER.error("Failed to save chunk {},{}", new Object[]{var2.x, var2.z, var5});
            return false;
         }
      }
   }

   private boolean isExistingChunkFull(ChunkPos var1) {
      byte var2 = this.chunkTypeCache.get(var1.toLong());
      if (var2 != 0) {
         return var2 == 1;
      } else {
         CompoundTag var3;
         try {
            var3 = (CompoundTag)((Optional)this.readChunk(var1).join()).orElse((Object)null);
            if (var3 == null) {
               this.markPositionReplaceable(var1);
               return false;
            }
         } catch (Exception var5) {
            LOGGER.error("Failed to read chunk {}", var1, var5);
            this.markPositionReplaceable(var1);
            return false;
         }

         ChunkStatus.ChunkType var4 = ChunkSerializer.getChunkTypeFromTag(var3);
         return this.markPosition(var1, var4) == 1;
      }
   }

   protected void setViewDistance(int var1) {
      int var2 = Mth.clamp(var1, 2, 32);
      if (var2 != this.viewDistance) {
         int var3 = this.viewDistance;
         this.viewDistance = var2;
         this.distanceManager.updatePlayerTickets(this.viewDistance);
         ObjectIterator var4 = this.updatingChunkMap.values().iterator();

         while(var4.hasNext()) {
            ChunkHolder var5 = (ChunkHolder)var4.next();
            ChunkPos var6 = var5.getPos();
            MutableObject var7 = new MutableObject();
            this.getPlayers(var6, false).forEach((var4x) -> {
               SectionPos var5 = var4x.getLastSectionPos();
               boolean var6x = isChunkInRange(var6.x, var6.z, var5.x(), var5.z(), var3);
               boolean var7x = isChunkInRange(var6.x, var6.z, var5.x(), var5.z(), this.viewDistance);
               this.updateChunkTracking(var4x, var6, var7, var6x, var7x);
            });
         }
      }

   }

   protected void updateChunkTracking(ServerPlayer var1, ChunkPos var2, MutableObject<ClientboundLevelChunkWithLightPacket> var3, boolean var4, boolean var5) {
      if (var1.level() == this.level) {
         if (var5 && !var4) {
            ChunkHolder var6 = this.getVisibleChunkIfPresent(var2.toLong());
            if (var6 != null) {
               LevelChunk var7 = var6.getTickingChunk();
               if (var7 != null) {
                  this.playerLoadedChunk(var1, var3, var7);
               }

               DebugPackets.sendPoiPacketsForChunk(this.level, var2);
            }
         }

         if (!var5 && var4) {
            var1.untrackChunk(var2);
         }

      }
   }

   public int size() {
      return this.visibleChunkMap.size();
   }

   public net.minecraft.server.level.DistanceManager getDistanceManager() {
      return this.distanceManager;
   }

   protected Iterable<ChunkHolder> getChunks() {
      return Iterables.unmodifiableIterable(this.visibleChunkMap.values());
   }

   void dumpChunks(Writer var1) throws IOException {
      CsvOutput var2 = CsvOutput.builder().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").addColumn("ticking_ticket").addColumn("ticking_level").addColumn("block_ticks").addColumn("fluid_ticks").build(var1);
      TickingTracker var3 = this.distanceManager.tickingTracker();
      ObjectBidirectionalIterator var4 = this.visibleChunkMap.long2ObjectEntrySet().iterator();

      while(var4.hasNext()) {
         Entry var5 = (Entry)var4.next();
         long var6 = var5.getLongKey();
         ChunkPos var8 = new ChunkPos(var6);
         ChunkHolder var9 = (ChunkHolder)var5.getValue();
         Optional var10 = Optional.ofNullable(var9.getLastAvailable());
         Optional var11 = var10.flatMap((var0) -> {
            return var0 instanceof LevelChunk ? Optional.of((LevelChunk)var0) : Optional.empty();
         });
         var2.writeRow(var8.x, var8.z, var9.getTicketLevel(), var10.isPresent(), var10.map(ChunkAccess::getStatus).orElse((Object)null), var11.map(LevelChunk::getFullStatus).orElse((Object)null), printFuture(var9.getFullChunkFuture()), printFuture(var9.getTickingChunkFuture()), printFuture(var9.getEntityTickingChunkFuture()), this.distanceManager.getTicketDebugString(var6), this.anyPlayerCloseEnoughForSpawning(var8), var11.map((var0) -> {
            return var0.getBlockEntities().size();
         }).orElse(0), var3.getTicketDebugString(var6), var3.getLevel(var6), var11.map((var0) -> {
            return var0.getBlockTicks().count();
         }).orElse(0), var11.map((var0) -> {
            return var0.getFluidTicks().count();
         }).orElse(0));
      }

   }

   private static String printFuture(CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>> var0) {
      try {
         Either var1 = (Either)var0.getNow((Object)null);
         return var1 != null ? (String)var1.map((var0x) -> {
            return "done";
         }, (var0x) -> {
            return "unloaded";
         }) : "not completed";
      } catch (CompletionException var2) {
         return "failed " + var2.getCause().getMessage();
      } catch (CancellationException var3) {
         return "cancelled";
      }
   }

   private CompletableFuture<Optional<CompoundTag>> readChunk(ChunkPos var1) {
      return this.read(var1).thenApplyAsync((var1x) -> {
         return var1x.map(this::upgradeChunkTag);
      }, Util.backgroundExecutor());
   }

   private CompoundTag upgradeChunkTag(CompoundTag var1) {
      return this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, var1, this.generator.getTypeNameForDataFixer());
   }

   boolean anyPlayerCloseEnoughForSpawning(ChunkPos var1) {
      long var2 = var1.toLong();
      if (!this.distanceManager.hasPlayersNearby(var2)) {
         return false;
      } else {
         Iterator var4 = this.playerMap.getPlayers(var2).iterator();

         ServerPlayer var5;
         do {
            if (!var4.hasNext()) {
               return false;
            }

            var5 = (ServerPlayer)var4.next();
         } while(!this.playerIsCloseEnoughForSpawning(var5, var1));

         return true;
      }
   }

   public List<ServerPlayer> getPlayersCloseForSpawning(ChunkPos var1) {
      long var2 = var1.toLong();
      if (!this.distanceManager.hasPlayersNearby(var2)) {
         return List.of();
      } else {
         Builder var4 = ImmutableList.builder();
         Iterator var5 = this.playerMap.getPlayers(var2).iterator();

         while(var5.hasNext()) {
            ServerPlayer var6 = (ServerPlayer)var5.next();
            if (this.playerIsCloseEnoughForSpawning(var6, var1)) {
               var4.add(var6);
            }
         }

         return var4.build();
      }
   }

   private boolean playerIsCloseEnoughForSpawning(ServerPlayer var1, ChunkPos var2) {
      if (var1.isSpectator()) {
         return false;
      } else {
         double var3 = euclideanDistanceSquared(var2, var1);
         return var3 < 16384.0D;
      }
   }

   private boolean skipPlayer(ServerPlayer var1) {
      return var1.isSpectator() && !this.level.getGameRules().getBoolean(GameRules.RULE_SPECTATORSGENERATECHUNKS);
   }

   void updatePlayerStatus(ServerPlayer var1, boolean var2) {
      boolean var3 = this.skipPlayer(var1);
      boolean var4 = this.playerMap.ignoredOrUnknown(var1);
      int var5 = SectionPos.blockToSectionCoord(var1.getBlockX());
      int var6 = SectionPos.blockToSectionCoord(var1.getBlockZ());
      if (var2) {
         this.playerMap.addPlayer(ChunkPos.asLong(var5, var6), var1, var3);
         this.updatePlayerPos(var1);
         if (!var3) {
            this.distanceManager.addPlayer(SectionPos.of((EntityAccess)var1), var1);
         }
      } else {
         SectionPos var7 = var1.getLastSectionPos();
         this.playerMap.removePlayer(var7.chunk().toLong(), var1);
         if (!var4) {
            this.distanceManager.removePlayer(var7, var1);
         }
      }

      for(int var10 = var5 - this.viewDistance - 1; var10 <= var5 + this.viewDistance + 1; ++var10) {
         for(int var8 = var6 - this.viewDistance - 1; var8 <= var6 + this.viewDistance + 1; ++var8) {
            if (isChunkInRange(var10, var8, var5, var6, this.viewDistance)) {
               ChunkPos var9 = new ChunkPos(var10, var8);
               this.updateChunkTracking(var1, var9, new MutableObject(), !var2, var2);
            }
         }
      }

   }

   private SectionPos updatePlayerPos(ServerPlayer var1) {
      SectionPos var2 = SectionPos.of((EntityAccess)var1);
      var1.setLastSectionPos(var2);
      var1.connection.send(new ClientboundSetChunkCacheCenterPacket(var2.x(), var2.z()));
      return var2;
   }

   public void move(ServerPlayer var1) {
      ObjectIterator var2 = this.entityMap.values().iterator();

      while(var2.hasNext()) {
         ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)var2.next();
         if (var3.entity == var1) {
            var3.updatePlayers(this.level.players());
         } else {
            var3.updatePlayer(var1);
         }
      }

      int var24 = SectionPos.blockToSectionCoord(var1.getBlockX());
      int var25 = SectionPos.blockToSectionCoord(var1.getBlockZ());
      SectionPos var4 = var1.getLastSectionPos();
      SectionPos var5 = SectionPos.of((EntityAccess)var1);
      long var6 = var4.chunk().toLong();
      long var8 = var5.chunk().toLong();
      boolean var10 = this.playerMap.ignored(var1);
      boolean var11 = this.skipPlayer(var1);
      boolean var12 = var4.asLong() != var5.asLong();
      if (var12 || var10 != var11) {
         this.updatePlayerPos(var1);
         if (!var10) {
            this.distanceManager.removePlayer(var4, var1);
         }

         if (!var11) {
            this.distanceManager.addPlayer(var5, var1);
         }

         if (!var10 && var11) {
            this.playerMap.ignorePlayer(var1);
         }

         if (var10 && !var11) {
            this.playerMap.unIgnorePlayer(var1);
         }

         if (var6 != var8) {
            this.playerMap.updatePlayer(var6, var8, var1);
         }
      }

      int var13 = var4.x();
      int var14 = var4.z();
      int var15 = this.viewDistance + 1;
      int var16;
      int var17;
      if (Math.abs(var13 - var24) <= var15 * 2 && Math.abs(var14 - var25) <= var15 * 2) {
         var16 = Math.min(var24, var13) - var15;
         var17 = Math.min(var25, var14) - var15;
         int var26 = Math.max(var24, var13) + var15;
         int var27 = Math.max(var25, var14) + var15;

         for(int var20 = var16; var20 <= var26; ++var20) {
            for(int var21 = var17; var21 <= var27; ++var21) {
               boolean var22 = isChunkInRange(var20, var21, var13, var14, this.viewDistance);
               boolean var23 = isChunkInRange(var20, var21, var24, var25, this.viewDistance);
               this.updateChunkTracking(var1, new ChunkPos(var20, var21), new MutableObject(), var22, var23);
            }
         }
      } else {
         boolean var18;
         boolean var19;
         for(var16 = var13 - var15; var16 <= var13 + var15; ++var16) {
            for(var17 = var14 - var15; var17 <= var14 + var15; ++var17) {
               if (isChunkInRange(var16, var17, var13, var14, this.viewDistance)) {
                  var18 = true;
                  var19 = false;
                  this.updateChunkTracking(var1, new ChunkPos(var16, var17), new MutableObject(), true, false);
               }
            }
         }

         for(var16 = var24 - var15; var16 <= var24 + var15; ++var16) {
            for(var17 = var25 - var15; var17 <= var25 + var15; ++var17) {
               if (isChunkInRange(var16, var17, var24, var25, this.viewDistance)) {
                  var18 = false;
                  var19 = true;
                  this.updateChunkTracking(var1, new ChunkPos(var16, var17), new MutableObject(), false, true);
               }
            }
         }
      }

   }

   public List<ServerPlayer> getPlayers(ChunkPos var1, boolean var2) {
      Set var3 = this.playerMap.getPlayers(var1.toLong());
      Builder var4 = ImmutableList.builder();
      Iterator var5 = var3.iterator();

      while(true) {
         ServerPlayer var6;
         SectionPos var7;
         do {
            if (!var5.hasNext()) {
               return var4.build();
            }

            var6 = (ServerPlayer)var5.next();
            var7 = var6.getLastSectionPos();
         } while((!var2 || !isChunkOnRangeBorder(var1.x, var1.z, var7.x(), var7.z(), this.viewDistance)) && (var2 || !isChunkInRange(var1.x, var1.z, var7.x(), var7.z(), this.viewDistance)));

         var4.add(var6);
      }
   }

   protected void addEntity(Entity var1) {
      if (!(var1 instanceof EnderDragonPart)) {
         EntityType var2 = var1.getType();
         int var3 = var2.clientTrackingRange() * 16;
         if (var3 != 0) {
            int var4 = var2.updateInterval();
            if (this.entityMap.containsKey(var1.getId())) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Entity is already tracked!"));
            } else {
               ChunkMap.TrackedEntity var5 = new ChunkMap.TrackedEntity(var1, var3, var4, var2.trackDeltas());
               this.entityMap.put(var1.getId(), var5);
               var5.updatePlayers(this.level.players());
               if (var1 instanceof ServerPlayer) {
                  ServerPlayer var6 = (ServerPlayer)var1;
                  this.updatePlayerStatus(var6, true);
                  ObjectIterator var7 = this.entityMap.values().iterator();

                  while(var7.hasNext()) {
                     ChunkMap.TrackedEntity var8 = (ChunkMap.TrackedEntity)var7.next();
                     if (var8.entity != var6) {
                        var8.updatePlayer(var6);
                     }
                  }
               }

            }
         }
      }
   }

   protected void removeEntity(Entity var1) {
      if (var1 instanceof ServerPlayer) {
         ServerPlayer var2 = (ServerPlayer)var1;
         this.updatePlayerStatus(var2, false);
         ObjectIterator var3 = this.entityMap.values().iterator();

         while(var3.hasNext()) {
            ChunkMap.TrackedEntity var4 = (ChunkMap.TrackedEntity)var3.next();
            var4.removePlayer(var2);
         }
      }

      ChunkMap.TrackedEntity var5 = (ChunkMap.TrackedEntity)this.entityMap.remove(var1.getId());
      if (var5 != null) {
         var5.broadcastRemoved();
      }

   }

   protected void tick() {
      ArrayList var1 = Lists.newArrayList();
      List var2 = this.level.players();
      ObjectIterator var3 = this.entityMap.values().iterator();

      ChunkMap.TrackedEntity var4;
      while(var3.hasNext()) {
         var4 = (ChunkMap.TrackedEntity)var3.next();
         SectionPos var5 = var4.lastSectionPos;
         SectionPos var6 = SectionPos.of((EntityAccess)var4.entity);
         boolean var7 = !Objects.equals(var5, var6);
         if (var7) {
            var4.updatePlayers(var2);
            Entity var8 = var4.entity;
            if (var8 instanceof ServerPlayer) {
               var1.add((ServerPlayer)var8);
            }

            var4.lastSectionPos = var6;
         }

         if (var7 || this.distanceManager.inEntityTickingRange(var6.chunk().toLong())) {
            var4.serverEntity.sendChanges();
         }
      }

      if (!var1.isEmpty()) {
         var3 = this.entityMap.values().iterator();

         while(var3.hasNext()) {
            var4 = (ChunkMap.TrackedEntity)var3.next();
            var4.updatePlayers(var1);
         }
      }

   }

   public void broadcast(Entity var1, Packet<?> var2) {
      ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)this.entityMap.get(var1.getId());
      if (var3 != null) {
         var3.broadcast(var2);
      }

   }

   protected void broadcastAndSend(Entity var1, Packet<?> var2) {
      ChunkMap.TrackedEntity var3 = (ChunkMap.TrackedEntity)this.entityMap.get(var1.getId());
      if (var3 != null) {
         var3.broadcastAndSend(var2);
      }

   }

   public void resendBiomesForChunks(List<ChunkAccess> var1) {
      HashMap var2 = new HashMap();
      Iterator var3 = var1.iterator();

      while(var3.hasNext()) {
         ChunkAccess var4 = (ChunkAccess)var3.next();
         ChunkPos var5 = var4.getPos();
         LevelChunk var6;
         if (var4 instanceof LevelChunk) {
            LevelChunk var7 = (LevelChunk)var4;
            var6 = var7;
         } else {
            var6 = this.level.getChunk(var5.x, var5.z);
         }

         Iterator var9 = this.getPlayers(var5, false).iterator();

         while(var9.hasNext()) {
            ServerPlayer var8 = (ServerPlayer)var9.next();
            ((List)var2.computeIfAbsent(var8, (var0) -> {
               return new ArrayList();
            })).add(var6);
         }
      }

      var2.forEach((var0, var1x) -> {
         var0.connection.send(ClientboundChunksBiomesPacket.forChunks(var1x));
      });
   }

   private void playerLoadedChunk(ServerPlayer var1, MutableObject<ClientboundLevelChunkWithLightPacket> var2, LevelChunk var3) {
      if (var2.getValue() == null) {
         var2.setValue(new ClientboundLevelChunkWithLightPacket(var3, this.lightEngine, (BitSet)null, (BitSet)null));
      }

      var1.trackChunk(var3.getPos(), (Packet)var2.getValue());
      DebugPackets.sendPoiPacketsForChunk(this.level, var3.getPos());
      ArrayList var4 = Lists.newArrayList();
      ArrayList var5 = Lists.newArrayList();
      ObjectIterator var6 = this.entityMap.values().iterator();

      while(var6.hasNext()) {
         ChunkMap.TrackedEntity var7 = (ChunkMap.TrackedEntity)var6.next();
         Entity var8 = var7.entity;
         if (var8 != var1 && var8.chunkPosition().equals(var3.getPos())) {
            var7.updatePlayer(var1);
            if (var8 instanceof Mob && ((Mob)var8).getLeashHolder() != null) {
               var4.add(var8);
            }

            if (!var8.getPassengers().isEmpty()) {
               var5.add(var8);
            }
         }
      }

      Iterator var9;
      Entity var10;
      if (!var4.isEmpty()) {
         var9 = var4.iterator();

         while(var9.hasNext()) {
            var10 = (Entity)var9.next();
            var1.connection.send(new ClientboundSetEntityLinkPacket(var10, ((Mob)var10).getLeashHolder()));
         }
      }

      if (!var5.isEmpty()) {
         var9 = var5.iterator();

         while(var9.hasNext()) {
            var10 = (Entity)var9.next();
            var1.connection.send(new ClientboundSetPassengersPacket(var10));
         }
      }

   }

   protected PoiManager getPoiManager() {
      return this.poiManager;
   }

   public String getStorageName() {
      return this.storageName;
   }

   void onFullChunkStatusChange(ChunkPos var1, FullChunkStatus var2) {
      this.chunkStatusListener.onChunkStatusChange(var1, var2);
   }

   static {
      FORCED_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
   }

   class DistanceManager extends net.minecraft.server.level.DistanceManager {
      protected DistanceManager(Executor var2, Executor var3) {
         super(var2, var3);
      }

      protected boolean isChunkToRemove(long var1) {
         return ChunkMap.this.toDrop.contains(var1);
      }

      @Nullable
      protected ChunkHolder getChunk(long var1) {
         return ChunkMap.this.getUpdatingChunkIfPresent(var1);
      }

      @Nullable
      protected ChunkHolder updateChunkScheduling(long var1, int var3, @Nullable ChunkHolder var4, int var5) {
         return ChunkMap.this.updateChunkScheduling(var1, var3, var4, var5);
      }
   }

   class TrackedEntity {
      final ServerEntity serverEntity;
      final Entity entity;
      private final int range;
      SectionPos lastSectionPos;
      private final Set<ServerPlayerConnection> seenBy = Sets.newIdentityHashSet();

      public TrackedEntity(Entity var2, int var3, int var4, boolean var5) {
         this.serverEntity = new ServerEntity(ChunkMap.this.level, var2, var4, var5, this::broadcast);
         this.entity = var2;
         this.range = var3;
         this.lastSectionPos = SectionPos.of((EntityAccess)var2);
      }

      public boolean equals(Object var1) {
         if (var1 instanceof ChunkMap.TrackedEntity) {
            return ((ChunkMap.TrackedEntity)var1).entity.getId() == this.entity.getId();
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.entity.getId();
      }

      public void broadcast(Packet<?> var1) {
         Iterator var2 = this.seenBy.iterator();

         while(var2.hasNext()) {
            ServerPlayerConnection var3 = (ServerPlayerConnection)var2.next();
            var3.send(var1);
         }

      }

      public void broadcastAndSend(Packet<?> var1) {
         this.broadcast(var1);
         if (this.entity instanceof ServerPlayer) {
            ((ServerPlayer)this.entity).connection.send(var1);
         }

      }

      public void broadcastRemoved() {
         Iterator var1 = this.seenBy.iterator();

         while(var1.hasNext()) {
            ServerPlayerConnection var2 = (ServerPlayerConnection)var1.next();
            this.serverEntity.removePairing(var2.getPlayer());
         }

      }

      public void removePlayer(ServerPlayer var1) {
         if (this.seenBy.remove(var1.connection)) {
            this.serverEntity.removePairing(var1);
         }

      }

      public void updatePlayer(ServerPlayer var1) {
         if (var1 != this.entity) {
            Vec3 var2 = var1.position().subtract(this.entity.position());
            double var3 = (double)Math.min(this.getEffectiveRange(), ChunkMap.this.viewDistance * 16);
            double var5 = var2.x * var2.x + var2.z * var2.z;
            double var7 = var3 * var3;
            boolean var9 = var5 <= var7 && this.entity.broadcastToPlayer(var1);
            if (var9) {
               if (this.seenBy.add(var1.connection)) {
                  this.serverEntity.addPairing(var1);
               }
            } else if (this.seenBy.remove(var1.connection)) {
               this.serverEntity.removePairing(var1);
            }

         }
      }

      private int scaledRange(int var1) {
         return ChunkMap.this.level.getServer().getScaledTrackingDistance(var1);
      }

      private int getEffectiveRange() {
         int var1 = this.range;
         Iterator var2 = this.entity.getIndirectPassengers().iterator();

         while(var2.hasNext()) {
            Entity var3 = (Entity)var2.next();
            int var4 = var3.getType().clientTrackingRange() * 16;
            if (var4 > var1) {
               var1 = var4;
            }
         }

         return this.scaledRange(var1);
      }

      public void updatePlayers(List<ServerPlayer> var1) {
         Iterator var2 = var1.iterator();

         while(var2.hasNext()) {
            ServerPlayer var3 = (ServerPlayer)var2.next();
            this.updatePlayer(var3);
         }

      }
   }
}
