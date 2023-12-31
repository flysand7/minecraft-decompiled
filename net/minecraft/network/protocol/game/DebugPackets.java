package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class DebugPackets {
   private static final Logger LOGGER = LogUtils.getLogger();

   public DebugPackets() {
   }

   public static void sendGameTestAddMarker(ServerLevel var0, BlockPos var1, String var2, int var3, int var4) {
      FriendlyByteBuf var5 = new FriendlyByteBuf(Unpooled.buffer());
      var5.writeBlockPos(var1);
      var5.writeInt(var3);
      var5.writeUtf(var2);
      var5.writeInt(var4);
      sendPacketToAllPlayers(var0, var5, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER);
   }

   public static void sendGameTestClearPacket(ServerLevel var0) {
      FriendlyByteBuf var1 = new FriendlyByteBuf(Unpooled.buffer());
      sendPacketToAllPlayers(var0, var1, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR);
   }

   public static void sendPoiPacketsForChunk(ServerLevel var0, ChunkPos var1) {
   }

   public static void sendPoiAddedPacket(ServerLevel var0, BlockPos var1) {
      sendVillageSectionsPacket(var0, var1);
   }

   public static void sendPoiRemovedPacket(ServerLevel var0, BlockPos var1) {
      sendVillageSectionsPacket(var0, var1);
   }

   public static void sendPoiTicketCountPacket(ServerLevel var0, BlockPos var1) {
      sendVillageSectionsPacket(var0, var1);
   }

   private static void sendVillageSectionsPacket(ServerLevel var0, BlockPos var1) {
   }

   public static void sendPathFindingPacket(Level var0, Mob var1, @Nullable Path var2, float var3) {
   }

   public static void sendNeighborsUpdatePacket(Level var0, BlockPos var1) {
   }

   public static void sendStructurePacket(WorldGenLevel var0, StructureStart var1) {
   }

   public static void sendGoalSelector(Level var0, Mob var1, GoalSelector var2) {
      if (var0 instanceof ServerLevel) {
         ;
      }
   }

   public static void sendRaids(ServerLevel var0, Collection<Raid> var1) {
   }

   public static void sendEntityBrain(LivingEntity var0) {
   }

   public static void sendBeeInfo(Bee var0) {
   }

   public static void sendGameEventInfo(Level var0, GameEvent var1, Vec3 var2) {
   }

   public static void sendGameEventListenerInfo(Level var0, GameEventListener var1) {
   }

   public static void sendHiveInfo(Level var0, BlockPos var1, BlockState var2, BeehiveBlockEntity var3) {
   }

   private static void writeBrain(LivingEntity var0, FriendlyByteBuf var1) {
      Brain var2 = var0.getBrain();
      long var3 = var0.level().getGameTime();
      if (var0 instanceof InventoryCarrier) {
         SimpleContainer var5 = ((InventoryCarrier)var0).getInventory();
         var1.writeUtf(var5.isEmpty() ? "" : var5.toString());
      } else {
         var1.writeUtf("");
      }

      var1.writeOptional(var2.hasMemoryValue(MemoryModuleType.PATH) ? var2.getMemory(MemoryModuleType.PATH) : Optional.empty(), (var0x, var1x) -> {
         var1x.writeToStream(var0x);
      });
      if (var0 instanceof Villager) {
         Villager var8 = (Villager)var0;
         boolean var6 = var8.wantsToSpawnGolem(var3);
         var1.writeBoolean(var6);
      } else {
         var1.writeBoolean(false);
      }

      if (var0.getType() == EntityType.WARDEN) {
         Warden var9 = (Warden)var0;
         var1.writeInt(var9.getClientAngerLevel());
      } else {
         var1.writeInt(-1);
      }

      var1.writeCollection(var2.getActiveActivities(), (var0x, var1x) -> {
         var0x.writeUtf(var1x.getName());
      });
      Set var10 = (Set)var2.getRunningBehaviors().stream().map(BehaviorControl::debugString).collect(Collectors.toSet());
      var1.writeCollection(var10, FriendlyByteBuf::writeUtf);
      var1.writeCollection(getMemoryDescriptions(var0, var3), (var0x, var1x) -> {
         String var2 = StringUtil.truncateStringIfNecessary(var1x, 255, true);
         var0x.writeUtf(var2);
      });
      Set var11;
      Stream var10000;
      if (var0 instanceof Villager) {
         var10000 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);
         Objects.requireNonNull(var2);
         var11 = (Set)var10000.map(var2::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
         var1.writeCollection(var11, FriendlyByteBuf::writeBlockPos);
      } else {
         var1.writeVarInt(0);
      }

      if (var0 instanceof Villager) {
         var10000 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE);
         Objects.requireNonNull(var2);
         var11 = (Set)var10000.map(var2::getMemory).flatMap(Optional::stream).map(GlobalPos::pos).collect(Collectors.toSet());
         var1.writeCollection(var11, FriendlyByteBuf::writeBlockPos);
      } else {
         var1.writeVarInt(0);
      }

      if (var0 instanceof Villager) {
         Map var12 = ((Villager)var0).getGossips().getGossipEntries();
         ArrayList var7 = Lists.newArrayList();
         var12.forEach((var1x, var2x) -> {
            String var3 = DebugEntityNameGenerator.getEntityName(var1x);
            var2x.forEach((var2, var3x) -> {
               var7.add(var3 + ": " + var2 + ": " + var3x);
            });
         });
         var1.writeCollection(var7, FriendlyByteBuf::writeUtf);
      } else {
         var1.writeVarInt(0);
      }

   }

   private static List<String> getMemoryDescriptions(LivingEntity var0, long var1) {
      Map var3 = var0.getBrain().getMemories();
      ArrayList var4 = Lists.newArrayList();
      Iterator var5 = var3.entrySet().iterator();

      while(var5.hasNext()) {
         Entry var6 = (Entry)var5.next();
         MemoryModuleType var7 = (MemoryModuleType)var6.getKey();
         Optional var8 = (Optional)var6.getValue();
         String var9;
         if (var8.isPresent()) {
            ExpirableValue var10 = (ExpirableValue)var8.get();
            Object var11 = var10.getValue();
            if (var7 == MemoryModuleType.HEARD_BELL_TIME) {
               long var12 = var1 - (Long)var11;
               var9 = var12 + " ticks ago";
            } else if (var10.canExpire()) {
               String var10000 = getShortDescription((ServerLevel)var0.level(), var11);
               var9 = var10000 + " (ttl: " + var10.getTimeToLive() + ")";
            } else {
               var9 = getShortDescription((ServerLevel)var0.level(), var11);
            }
         } else {
            var9 = "-";
         }

         String var10001 = BuiltInRegistries.MEMORY_MODULE_TYPE.getKey(var7).getPath();
         var4.add(var10001 + ": " + var9);
      }

      var4.sort(String::compareTo);
      return var4;
   }

   private static String getShortDescription(ServerLevel var0, @Nullable Object var1) {
      if (var1 == null) {
         return "-";
      } else if (var1 instanceof UUID) {
         return getShortDescription(var0, var0.getEntity((UUID)var1));
      } else {
         Entity var5;
         if (var1 instanceof LivingEntity) {
            var5 = (Entity)var1;
            return DebugEntityNameGenerator.getEntityName(var5);
         } else if (var1 instanceof Nameable) {
            return ((Nameable)var1).getName().getString();
         } else if (var1 instanceof WalkTarget) {
            return getShortDescription(var0, ((WalkTarget)var1).getTarget());
         } else if (var1 instanceof EntityTracker) {
            return getShortDescription(var0, ((EntityTracker)var1).getEntity());
         } else if (var1 instanceof GlobalPos) {
            return getShortDescription(var0, ((GlobalPos)var1).pos());
         } else if (var1 instanceof BlockPosTracker) {
            return getShortDescription(var0, ((BlockPosTracker)var1).currentBlockPosition());
         } else if (var1 instanceof DamageSource) {
            var5 = ((DamageSource)var1).getEntity();
            return var5 == null ? var1.toString() : getShortDescription(var0, var5);
         } else if (!(var1 instanceof Collection)) {
            return var1.toString();
         } else {
            ArrayList var2 = Lists.newArrayList();
            Iterator var3 = ((Iterable)var1).iterator();

            while(var3.hasNext()) {
               Object var4 = var3.next();
               var2.add(getShortDescription(var0, var4));
            }

            return var2.toString();
         }
      }
   }

   private static void sendPacketToAllPlayers(ServerLevel var0, FriendlyByteBuf var1, ResourceLocation var2) {
      ClientboundCustomPayloadPacket var3 = new ClientboundCustomPayloadPacket(var2, var1);
      Iterator var4 = var0.players().iterator();

      while(var4.hasNext()) {
         ServerPlayer var5 = (ServerPlayer)var4.next();
         var5.connection.send(var3);
      }

   }

   // $FF: synthetic method
   private static void lambda$sendBeeInfo$5(FriendlyByteBuf var0, Path var1) {
      var1.writeToStream(var0);
   }

   // $FF: synthetic method
   private static void lambda$sendRaids$4(FriendlyByteBuf var0, Raid var1) {
      var0.writeBlockPos(var1.getCenter());
   }

   // $FF: synthetic method
   private static void lambda$sendGoalSelector$3(FriendlyByteBuf var0, WrappedGoal var1) {
      var0.writeInt(var1.getPriority());
      var0.writeBoolean(var1.isRunning());
      var0.writeUtf(var1.getGoal().getClass().getSimpleName());
   }

   // $FF: synthetic method
   private static String lambda$sendPoiAddedPacket$2(ResourceKey var0) {
      return var0.location().toString();
   }

   // $FF: synthetic method
   private static void lambda$sendPoiPacketsForChunk$1(ServerLevel var0, PoiRecord var1) {
      sendPoiAddedPacket(var0, var1.getPos());
   }

   // $FF: synthetic method
   private static boolean lambda$sendPoiPacketsForChunk$0(Holder var0) {
      return true;
   }
}
