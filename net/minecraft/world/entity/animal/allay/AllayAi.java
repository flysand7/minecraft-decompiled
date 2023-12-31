package net.minecraft.world.entity.animal.allay;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.AnimalPanic;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.CountDownCooldownTicks;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.GoAndGiveItemsToTarget;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StayCloseToTarget;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class AllayAi {
   private static final float SPEED_MULTIPLIER_WHEN_IDLING = 1.0F;
   private static final float SPEED_MULTIPLIER_WHEN_FOLLOWING_DEPOSIT_TARGET = 2.25F;
   private static final float SPEED_MULTIPLIER_WHEN_RETRIEVING_ITEM = 1.75F;
   private static final float SPEED_MULTIPLIER_WHEN_PANICKING = 2.5F;
   private static final int CLOSE_ENOUGH_TO_TARGET = 4;
   private static final int TOO_FAR_FROM_TARGET = 16;
   private static final int MAX_LOOK_DISTANCE = 6;
   private static final int MIN_WAIT_DURATION = 30;
   private static final int MAX_WAIT_DURATION = 60;
   private static final int TIME_TO_FORGET_NOTEBLOCK = 600;
   private static final int DISTANCE_TO_WANTED_ITEM = 32;
   private static final int GIVE_ITEM_TIMEOUT_DURATION = 20;

   public AllayAi() {
   }

   protected static Brain<?> makeBrain(Brain<Allay> var0) {
      initCoreActivity(var0);
      initIdleActivity(var0);
      var0.setCoreActivities(ImmutableSet.of(Activity.CORE));
      var0.setDefaultActivity(Activity.IDLE);
      var0.useDefaultActivity();
      return var0;
   }

   private static void initCoreActivity(Brain<Allay> var0) {
      var0.addActivity(Activity.CORE, 0, ImmutableList.of(new Swim(0.8F), new AnimalPanic(2.5F), new LookAtTargetSink(45, 90), new MoveToTargetSink(), new CountDownCooldownTicks(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS), new CountDownCooldownTicks(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS)));
   }

   private static void initIdleActivity(Brain<Allay> var0) {
      var0.addActivityWithConditions(Activity.IDLE, ImmutableList.of(Pair.of(0, GoToWantedItem.create((var0x) -> {
         return true;
      }, 1.75F, true, 32)), Pair.of(1, new GoAndGiveItemsToTarget(AllayAi::getItemDepositPosition, 2.25F, 20)), Pair.of(2, StayCloseToTarget.create(AllayAi::getItemDepositPosition, Predicate.not(AllayAi::hasWantedItem), 4, 16, 2.25F)), Pair.of(3, SetEntityLookTargetSometimes.create(6.0F, UniformInt.of(30, 60))), Pair.of(4, new RunOne(ImmutableList.of(Pair.of(RandomStroll.fly(1.0F), 2), Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 2), Pair.of(new DoNothing(30, 60), 1))))), ImmutableSet.of());
   }

   public static void updateActivity(Allay var0) {
      var0.getBrain().setActiveActivityToFirstValid(ImmutableList.of(Activity.IDLE));
   }

   public static void hearNoteblock(LivingEntity var0, BlockPos var1) {
      Brain var2 = var0.getBrain();
      GlobalPos var3 = GlobalPos.of(var0.level().dimension(), var1);
      Optional var4 = var2.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
      if (var4.isEmpty()) {
         var2.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION, (Object)var3);
         var2.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, (int)600);
      } else if (((GlobalPos)var4.get()).equals(var3)) {
         var2.setMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS, (int)600);
      }

   }

   private static Optional<PositionTracker> getItemDepositPosition(LivingEntity var0) {
      Brain var1 = var0.getBrain();
      Optional var2 = var1.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
      if (var2.isPresent()) {
         GlobalPos var3 = (GlobalPos)var2.get();
         if (shouldDepositItemsAtLikedNoteblock(var0, var1, var3)) {
            return Optional.of(new BlockPosTracker(var3.pos().above()));
         }

         var1.eraseMemory(MemoryModuleType.LIKED_NOTEBLOCK_POSITION);
      }

      return getLikedPlayerPositionTracker(var0);
   }

   private static boolean hasWantedItem(LivingEntity var0) {
      Brain var1 = var0.getBrain();
      return var1.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
   }

   private static boolean shouldDepositItemsAtLikedNoteblock(LivingEntity var0, Brain<?> var1, GlobalPos var2) {
      Optional var3 = var1.getMemory(MemoryModuleType.LIKED_NOTEBLOCK_COOLDOWN_TICKS);
      Level var4 = var0.level();
      return var4.dimension() == var2.dimension() && var4.getBlockState(var2.pos()).is(Blocks.NOTE_BLOCK) && var3.isPresent();
   }

   private static Optional<PositionTracker> getLikedPlayerPositionTracker(LivingEntity var0) {
      return getLikedPlayer(var0).map((var0x) -> {
         return new EntityTracker(var0x, true);
      });
   }

   public static Optional<ServerPlayer> getLikedPlayer(LivingEntity var0) {
      Level var1 = var0.level();
      if (!var1.isClientSide() && var1 instanceof ServerLevel) {
         ServerLevel var2 = (ServerLevel)var1;
         Optional var3 = var0.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
         if (var3.isPresent()) {
            Entity var4 = var2.getEntity((UUID)var3.get());
            if (var4 instanceof ServerPlayer) {
               ServerPlayer var5 = (ServerPlayer)var4;
               if ((var5.gameMode.isSurvival() || var5.gameMode.isCreative()) && var5.closerThan(var0, 64.0D)) {
                  return Optional.of(var5);
               }
            }

            return Optional.empty();
         }
      }

      return Optional.empty();
   }
}
