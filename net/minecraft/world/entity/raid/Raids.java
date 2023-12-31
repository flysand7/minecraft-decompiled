package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   private final Map<Integer, Raid> raidMap = Maps.newHashMap();
   private final ServerLevel level;
   private int nextAvailableID;
   private int tick;

   public Raids(ServerLevel var1) {
      this.level = var1;
      this.nextAvailableID = 1;
      this.setDirty();
   }

   public Raid get(int var1) {
      return (Raid)this.raidMap.get(var1);
   }

   public void tick() {
      ++this.tick;
      Iterator var1 = this.raidMap.values().iterator();

      while(var1.hasNext()) {
         Raid var2 = (Raid)var1.next();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            var2.stop();
         }

         if (var2.isStopped()) {
            var1.remove();
            this.setDirty();
         } else {
            var2.tick();
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }

      DebugPackets.sendRaids(this.level, this.raidMap.values());
   }

   public static boolean canJoinRaid(Raider var0, Raid var1) {
      if (var0 != null && var1 != null && var1.getLevel() != null) {
         return var0.isAlive() && var0.canJoinRaid() && var0.getNoActionTime() <= 2400 && var0.level().dimensionType() == var1.getLevel().dimensionType();
      } else {
         return false;
      }
   }

   @Nullable
   public Raid createOrExtendRaid(ServerPlayer var1) {
      if (var1.isSpectator()) {
         return null;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
         return null;
      } else {
         DimensionType var2 = var1.level().dimensionType();
         if (!var2.hasRaids()) {
            return null;
         } else {
            BlockPos var3 = var1.blockPosition();
            List var5 = this.level.getPoiManager().getInRange((var0) -> {
               return var0.is(PoiTypeTags.VILLAGE);
            }, var3, 64, PoiManager.Occupancy.IS_OCCUPIED).toList();
            int var6 = 0;
            Vec3 var7 = Vec3.ZERO;

            for(Iterator var8 = var5.iterator(); var8.hasNext(); ++var6) {
               PoiRecord var9 = (PoiRecord)var8.next();
               BlockPos var10 = var9.getPos();
               var7 = var7.add((double)var10.getX(), (double)var10.getY(), (double)var10.getZ());
            }

            BlockPos var4;
            if (var6 > 0) {
               var7 = var7.scale(1.0D / (double)var6);
               var4 = BlockPos.containing(var7);
            } else {
               var4 = var3;
            }

            Raid var11 = this.getOrCreateRaid(var1.serverLevel(), var4);
            boolean var12 = false;
            if (!var11.isStarted()) {
               if (!this.raidMap.containsKey(var11.getId())) {
                  this.raidMap.put(var11.getId(), var11);
               }

               var12 = true;
            } else if (var11.getBadOmenLevel() < var11.getMaxBadOmenLevel()) {
               var12 = true;
            } else {
               var1.removeEffect(MobEffects.BAD_OMEN);
               var1.connection.send(new ClientboundEntityEventPacket(var1, (byte)43));
            }

            if (var12) {
               var11.absorbBadOmen(var1);
               var1.connection.send(new ClientboundEntityEventPacket(var1, (byte)43));
               if (!var11.hasFirstWaveSpawned()) {
                  var1.awardStat(Stats.RAID_TRIGGER);
                  CriteriaTriggers.BAD_OMEN.trigger(var1);
               }
            }

            this.setDirty();
            return var11;
         }
      }
   }

   private Raid getOrCreateRaid(ServerLevel var1, BlockPos var2) {
      Raid var3 = var1.getRaidAt(var2);
      return var3 != null ? var3 : new Raid(this.getUniqueId(), var1, var2);
   }

   public static Raids load(ServerLevel var0, CompoundTag var1) {
      Raids var2 = new Raids(var0);
      var2.nextAvailableID = var1.getInt("NextAvailableID");
      var2.tick = var1.getInt("Tick");
      ListTag var3 = var1.getList("Raids", 10);

      for(int var4 = 0; var4 < var3.size(); ++var4) {
         CompoundTag var5 = var3.getCompound(var4);
         Raid var6 = new Raid(var0, var5);
         var2.raidMap.put(var6.getId(), var6);
      }

      return var2;
   }

   public CompoundTag save(CompoundTag var1) {
      var1.putInt("NextAvailableID", this.nextAvailableID);
      var1.putInt("Tick", this.tick);
      ListTag var2 = new ListTag();
      Iterator var3 = this.raidMap.values().iterator();

      while(var3.hasNext()) {
         Raid var4 = (Raid)var3.next();
         CompoundTag var5 = new CompoundTag();
         var4.save(var5);
         var2.add(var5);
      }

      var1.put("Raids", var2);
      return var1;
   }

   public static String getFileId(Holder<DimensionType> var0) {
      return var0.is(BuiltinDimensionTypes.END) ? "raids_end" : "raids";
   }

   private int getUniqueId() {
      return ++this.nextAvailableID;
   }

   @Nullable
   public Raid getNearbyRaid(BlockPos var1, int var2) {
      Raid var3 = null;
      double var4 = (double)var2;
      Iterator var6 = this.raidMap.values().iterator();

      while(var6.hasNext()) {
         Raid var7 = (Raid)var6.next();
         double var8 = var7.getCenter().distSqr(var1);
         if (var7.isActive() && var8 < var4) {
            var3 = var7;
            var4 = var8;
         }
      }

      return var3;
   }
}
