package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Objects;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class SculkShriekerBlockEntity extends BlockEntity implements GameEventListener.Holder<VibrationSystem.Listener>, VibrationSystem {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int WARNING_SOUND_RADIUS = 10;
   private static final int WARDEN_SPAWN_ATTEMPTS = 20;
   private static final int WARDEN_SPAWN_RANGE_XZ = 5;
   private static final int WARDEN_SPAWN_RANGE_Y = 6;
   private static final int DARKNESS_RADIUS = 40;
   private static final int SHRIEKING_TICKS = 90;
   private static final Int2ObjectMap<SoundEvent> SOUND_BY_LEVEL = (Int2ObjectMap)Util.make(new Int2ObjectOpenHashMap(), (var0) -> {
      var0.put(1, SoundEvents.WARDEN_NEARBY_CLOSE);
      var0.put(2, SoundEvents.WARDEN_NEARBY_CLOSER);
      var0.put(3, SoundEvents.WARDEN_NEARBY_CLOSEST);
      var0.put(4, SoundEvents.WARDEN_LISTENING_ANGRY);
   });
   private int warningLevel;
   private final VibrationSystem.User vibrationUser = new SculkShriekerBlockEntity.VibrationUser();
   private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
   private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);

   public SculkShriekerBlockEntity(BlockPos var1, BlockState var2) {
      super(BlockEntityType.SCULK_SHRIEKER, var1, var2);
   }

   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   public void load(CompoundTag var1) {
      super.load(var1);
      if (var1.contains("warning_level", 99)) {
         this.warningLevel = var1.getInt("warning_level");
      }

      if (var1.contains("listener", 10)) {
         DataResult var10000 = VibrationSystem.Data.CODEC.parse(new Dynamic(NbtOps.INSTANCE, var1.getCompound("listener")));
         Logger var10001 = LOGGER;
         Objects.requireNonNull(var10001);
         var10000.resultOrPartial(var10001::error).ifPresent((var1x) -> {
            this.vibrationData = var1x;
         });
      }

   }

   protected void saveAdditional(CompoundTag var1) {
      super.saveAdditional(var1);
      var1.putInt("warning_level", this.warningLevel);
      DataResult var10000 = VibrationSystem.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.vibrationData);
      Logger var10001 = LOGGER;
      Objects.requireNonNull(var10001);
      var10000.resultOrPartial(var10001::error).ifPresent((var1x) -> {
         var1.put("listener", var1x);
      });
   }

   @Nullable
   public static ServerPlayer tryGetPlayer(@Nullable Entity var0) {
      ServerPlayer var5;
      if (var0 instanceof ServerPlayer) {
         var5 = (ServerPlayer)var0;
         return var5;
      } else {
         if (var0 != null) {
            LivingEntity var2 = var0.getControllingPassenger();
            if (var2 instanceof ServerPlayer) {
               var5 = (ServerPlayer)var2;
               return var5;
            }
         }

         Entity var3;
         ServerPlayer var6;
         if (var0 instanceof Projectile) {
            Projectile var1 = (Projectile)var0;
            var3 = var1.getOwner();
            if (var3 instanceof ServerPlayer) {
               var6 = (ServerPlayer)var3;
               return var6;
            }
         }

         if (var0 instanceof ItemEntity) {
            ItemEntity var4 = (ItemEntity)var0;
            var3 = var4.getOwner();
            if (var3 instanceof ServerPlayer) {
               var6 = (ServerPlayer)var3;
               return var6;
            }
         }

         return null;
      }
   }

   public void tryShriek(ServerLevel var1, @Nullable ServerPlayer var2) {
      if (var2 != null) {
         BlockState var3 = this.getBlockState();
         if (!(Boolean)var3.getValue(SculkShriekerBlock.SHRIEKING)) {
            this.warningLevel = 0;
            if (!this.canRespond(var1) || this.tryToWarn(var1, var2)) {
               this.shriek(var1, var2);
            }
         }
      }
   }

   private boolean tryToWarn(ServerLevel var1, ServerPlayer var2) {
      OptionalInt var3 = WardenSpawnTracker.tryWarn(var1, this.getBlockPos(), var2);
      var3.ifPresent((var1x) -> {
         this.warningLevel = var1x;
      });
      return var3.isPresent();
   }

   private void shriek(ServerLevel var1, @Nullable Entity var2) {
      BlockPos var3 = this.getBlockPos();
      BlockState var4 = this.getBlockState();
      var1.setBlock(var3, (BlockState)var4.setValue(SculkShriekerBlock.SHRIEKING, true), 2);
      var1.scheduleTick(var3, var4.getBlock(), 90);
      var1.levelEvent(3007, var3, 0);
      var1.gameEvent(GameEvent.SHRIEK, var3, GameEvent.Context.of(var2));
   }

   private boolean canRespond(ServerLevel var1) {
      return (Boolean)this.getBlockState().getValue(SculkShriekerBlock.CAN_SUMMON) && var1.getDifficulty() != Difficulty.PEACEFUL && var1.getGameRules().getBoolean(GameRules.RULE_DO_WARDEN_SPAWNING);
   }

   public void tryRespond(ServerLevel var1) {
      if (this.canRespond(var1) && this.warningLevel > 0) {
         if (!this.trySummonWarden(var1)) {
            this.playWardenReplySound(var1);
         }

         Warden.applyDarknessAround(var1, Vec3.atCenterOf(this.getBlockPos()), (Entity)null, 40);
      }

   }

   private void playWardenReplySound(Level var1) {
      SoundEvent var2 = (SoundEvent)SOUND_BY_LEVEL.get(this.warningLevel);
      if (var2 != null) {
         BlockPos var3 = this.getBlockPos();
         int var4 = var3.getX() + Mth.randomBetweenInclusive(var1.random, -10, 10);
         int var5 = var3.getY() + Mth.randomBetweenInclusive(var1.random, -10, 10);
         int var6 = var3.getZ() + Mth.randomBetweenInclusive(var1.random, -10, 10);
         var1.playSound((Player)null, (double)var4, (double)var5, (double)var6, var2, SoundSource.HOSTILE, 5.0F, 1.0F);
      }

   }

   private boolean trySummonWarden(ServerLevel var1) {
      return this.warningLevel < 4 ? false : SpawnUtil.trySpawnMob(EntityType.WARDEN, MobSpawnType.TRIGGERED, var1, this.getBlockPos(), 20, 5, 6, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER).isPresent();
   }

   public VibrationSystem.Listener getListener() {
      return this.vibrationListener;
   }

   // $FF: synthetic method
   public GameEventListener getListener() {
      return this.getListener();
   }

   class VibrationUser implements VibrationSystem.User {
      private static final int LISTENER_RADIUS = 8;
      private final PositionSource positionSource;

      public VibrationUser() {
         this.positionSource = new BlockPositionSource(SculkShriekerBlockEntity.this.worldPosition);
      }

      public int getListenerRadius() {
         return 8;
      }

      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.SHRIEKER_CAN_LISTEN;
      }

      public boolean canReceiveVibration(ServerLevel var1, BlockPos var2, GameEvent var3, GameEvent.Context var4) {
         return !(Boolean)SculkShriekerBlockEntity.this.getBlockState().getValue(SculkShriekerBlock.SHRIEKING) && SculkShriekerBlockEntity.tryGetPlayer(var4.sourceEntity()) != null;
      }

      public void onReceiveVibration(ServerLevel var1, BlockPos var2, GameEvent var3, @Nullable Entity var4, @Nullable Entity var5, float var6) {
         SculkShriekerBlockEntity.this.tryShriek(var1, SculkShriekerBlockEntity.tryGetPlayer(var5 != null ? var5 : var4));
      }

      public void onDataChanged() {
         SculkShriekerBlockEntity.this.setChanged();
      }

      public boolean requiresAdjacentChunksToBeTicking() {
         return true;
      }
   }
}
