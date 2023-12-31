package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
   private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatable("commands.clone.toobig", var0, var1);
   });
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
   public static final Predicate<BlockInWorld> FILTER_AIR = (var0) -> {
      return !var0.getState().isAir();
   };

   public CloneCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0, CommandBuildContext var1) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires((var0x) -> {
         return var0x.hasPermission(2);
      })).then(beginEndDestinationAndModeSuffix(var1, (var0x) -> {
         return ((CommandSourceStack)var0x.getSource()).getLevel();
      }))).then(Commands.literal("from").then(Commands.argument("sourceDimension", DimensionArgument.dimension()).then(beginEndDestinationAndModeSuffix(var1, (var0x) -> {
         return DimensionArgument.getDimension(var0x, "sourceDimension");
      })))));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(CommandBuildContext var0, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var1) {
      return Commands.argument("begin", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)Commands.argument("end", BlockPosArgument.blockPos()).then(destinationAndModeSuffix(var0, var1, (var0x) -> {
         return ((CommandSourceStack)var0x.getSource()).getLevel();
      }))).then(Commands.literal("to").then(Commands.argument("targetDimension", DimensionArgument.dimension()).then(destinationAndModeSuffix(var0, var1, (var0x) -> {
         return DimensionArgument.getDimension(var0x, "targetDimension");
      })))));
   }

   private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> var0, ServerLevel var1, String var2) throws CommandSyntaxException {
      BlockPos var3 = BlockPosArgument.getLoadedBlockPos(var0, var1, var2);
      return new CloneCommands.DimensionAndPosition(var1, var3);
   }

   private static ArgumentBuilder<CommandSourceStack, ?> destinationAndModeSuffix(CommandBuildContext var0, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var1, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, ServerLevel> var2) {
      CloneCommands.CommandFunction var3 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var1.apply(var1x), "begin");
      };
      CloneCommands.CommandFunction var4 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var1.apply(var1x), "end");
      };
      CloneCommands.CommandFunction var5 = (var1x) -> {
         return getLoadedDimensionAndPosition(var1x, (ServerLevel)var2.apply(var1x), "destination");
      };
      return ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos()).executes((var3x) -> {
         return clone((CommandSourceStack)var3x.getSource(), (CloneCommands.DimensionAndPosition)var3.apply(var3x), (CloneCommands.DimensionAndPosition)var4.apply(var3x), (CloneCommands.DimensionAndPosition)var5.apply(var3x), (var0) -> {
            return true;
         }, CloneCommands.Mode.NORMAL);
      })).then(wrapWithCloneMode(var3, var4, var5, (var0x) -> {
         return (var0) -> {
            return true;
         };
      }, Commands.literal("replace").executes((var3x) -> {
         return clone((CommandSourceStack)var3x.getSource(), (CloneCommands.DimensionAndPosition)var3.apply(var3x), (CloneCommands.DimensionAndPosition)var4.apply(var3x), (CloneCommands.DimensionAndPosition)var5.apply(var3x), (var0) -> {
            return true;
         }, CloneCommands.Mode.NORMAL);
      })))).then(wrapWithCloneMode(var3, var4, var5, (var0x) -> {
         return FILTER_AIR;
      }, Commands.literal("masked").executes((var3x) -> {
         return clone((CommandSourceStack)var3x.getSource(), (CloneCommands.DimensionAndPosition)var3.apply(var3x), (CloneCommands.DimensionAndPosition)var4.apply(var3x), (CloneCommands.DimensionAndPosition)var5.apply(var3x), FILTER_AIR, CloneCommands.Mode.NORMAL);
      })))).then(Commands.literal("filtered").then(wrapWithCloneMode(var3, var4, var5, (var0x) -> {
         return BlockPredicateArgument.getBlockPredicate(var0x, "filter");
      }, Commands.argument("filter", BlockPredicateArgument.blockPredicate(var0)).executes((var3x) -> {
         return clone((CommandSourceStack)var3x.getSource(), (CloneCommands.DimensionAndPosition)var3.apply(var3x), (CloneCommands.DimensionAndPosition)var4.apply(var3x), (CloneCommands.DimensionAndPosition)var5.apply(var3x), BlockPredicateArgument.getBlockPredicate(var3x, "filter"), CloneCommands.Mode.NORMAL);
      }))));
   }

   private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var0, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var1, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> var2, CloneCommands.CommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> var3, ArgumentBuilder<CommandSourceStack, ?> var4) {
      return var4.then(Commands.literal("force").executes((var4x) -> {
         return clone((CommandSourceStack)var4x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var4x), (CloneCommands.DimensionAndPosition)var1.apply(var4x), (CloneCommands.DimensionAndPosition)var2.apply(var4x), (Predicate)var3.apply(var4x), CloneCommands.Mode.FORCE);
      })).then(Commands.literal("move").executes((var4x) -> {
         return clone((CommandSourceStack)var4x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var4x), (CloneCommands.DimensionAndPosition)var1.apply(var4x), (CloneCommands.DimensionAndPosition)var2.apply(var4x), (Predicate)var3.apply(var4x), CloneCommands.Mode.MOVE);
      })).then(Commands.literal("normal").executes((var4x) -> {
         return clone((CommandSourceStack)var4x.getSource(), (CloneCommands.DimensionAndPosition)var0.apply(var4x), (CloneCommands.DimensionAndPosition)var1.apply(var4x), (CloneCommands.DimensionAndPosition)var2.apply(var4x), (Predicate)var3.apply(var4x), CloneCommands.Mode.NORMAL);
      }));
   }

   private static int clone(CommandSourceStack var0, CloneCommands.DimensionAndPosition var1, CloneCommands.DimensionAndPosition var2, CloneCommands.DimensionAndPosition var3, Predicate<BlockInWorld> var4, CloneCommands.Mode var5) throws CommandSyntaxException {
      BlockPos var6 = var1.position();
      BlockPos var7 = var2.position();
      BoundingBox var8 = BoundingBox.fromCorners(var6, var7);
      BlockPos var9 = var3.position();
      BlockPos var10 = var9.offset(var8.getLength());
      BoundingBox var11 = BoundingBox.fromCorners(var9, var10);
      ServerLevel var12 = var1.dimension();
      ServerLevel var13 = var3.dimension();
      if (!var5.canOverlap() && var12 == var13 && var11.intersects(var8)) {
         throw ERROR_OVERLAP.create();
      } else {
         int var14 = var8.getXSpan() * var8.getYSpan() * var8.getZSpan();
         int var15 = var0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
         if (var14 > var15) {
            throw ERROR_AREA_TOO_LARGE.create(var15, var14);
         } else if (var12.hasChunksAt(var6, var7) && var13.hasChunksAt(var9, var10)) {
            ArrayList var16 = Lists.newArrayList();
            ArrayList var17 = Lists.newArrayList();
            ArrayList var18 = Lists.newArrayList();
            LinkedList var19 = Lists.newLinkedList();
            BlockPos var20 = new BlockPos(var11.minX() - var8.minX(), var11.minY() - var8.minY(), var11.minZ() - var8.minZ());

            int var23;
            for(int var21 = var8.minZ(); var21 <= var8.maxZ(); ++var21) {
               for(int var22 = var8.minY(); var22 <= var8.maxY(); ++var22) {
                  for(var23 = var8.minX(); var23 <= var8.maxX(); ++var23) {
                     BlockPos var24 = new BlockPos(var23, var22, var21);
                     BlockPos var25 = var24.offset(var20);
                     BlockInWorld var26 = new BlockInWorld(var12, var24, false);
                     BlockState var27 = var26.getState();
                     if (var4.test(var26)) {
                        BlockEntity var28 = var12.getBlockEntity(var24);
                        if (var28 != null) {
                           CompoundTag var29 = var28.saveWithoutMetadata();
                           var17.add(new CloneCommands.CloneBlockInfo(var25, var27, var29));
                           var19.addLast(var24);
                        } else if (!var27.isSolidRender(var12, var24) && !var27.isCollisionShapeFullBlock(var12, var24)) {
                           var18.add(new CloneCommands.CloneBlockInfo(var25, var27, (CompoundTag)null));
                           var19.addFirst(var24);
                        } else {
                           var16.add(new CloneCommands.CloneBlockInfo(var25, var27, (CompoundTag)null));
                           var19.addLast(var24);
                        }
                     }
                  }
               }
            }

            if (var5 == CloneCommands.Mode.MOVE) {
               Iterator var30 = var19.iterator();

               BlockPos var32;
               while(var30.hasNext()) {
                  var32 = (BlockPos)var30.next();
                  BlockEntity var34 = var12.getBlockEntity(var32);
                  Clearable.tryClear(var34);
                  var12.setBlock(var32, Blocks.BARRIER.defaultBlockState(), 2);
               }

               var30 = var19.iterator();

               while(var30.hasNext()) {
                  var32 = (BlockPos)var30.next();
                  var12.setBlock(var32, Blocks.AIR.defaultBlockState(), 3);
               }
            }

            ArrayList var31 = Lists.newArrayList();
            var31.addAll(var16);
            var31.addAll(var17);
            var31.addAll(var18);
            List var33 = Lists.reverse(var31);
            Iterator var35 = var33.iterator();

            while(var35.hasNext()) {
               CloneCommands.CloneBlockInfo var36 = (CloneCommands.CloneBlockInfo)var35.next();
               BlockEntity var38 = var13.getBlockEntity(var36.pos);
               Clearable.tryClear(var38);
               var13.setBlock(var36.pos, Blocks.BARRIER.defaultBlockState(), 2);
            }

            var23 = 0;
            Iterator var37 = var31.iterator();

            CloneCommands.CloneBlockInfo var39;
            while(var37.hasNext()) {
               var39 = (CloneCommands.CloneBlockInfo)var37.next();
               if (var13.setBlock(var39.pos, var39.state, 2)) {
                  ++var23;
               }
            }

            for(var37 = var17.iterator(); var37.hasNext(); var13.setBlock(var39.pos, var39.state, 2)) {
               var39 = (CloneCommands.CloneBlockInfo)var37.next();
               BlockEntity var40 = var13.getBlockEntity(var39.pos);
               if (var39.tag != null && var40 != null) {
                  var40.load(var39.tag);
                  var40.setChanged();
               }
            }

            var37 = var33.iterator();

            while(var37.hasNext()) {
               var39 = (CloneCommands.CloneBlockInfo)var37.next();
               var13.blockUpdated(var39.pos, var39.state.getBlock());
            }

            var13.getBlockTicks().copyAreaFrom(var12.getBlockTicks(), var8, var20);
            if (var23 == 0) {
               throw ERROR_FAILED.create();
            } else {
               var0.sendSuccess(() -> {
                  return Component.translatable("commands.clone.success", var23);
               }, true);
               return var23;
            }
         } else {
            throw BlockPosArgument.ERROR_NOT_LOADED.create();
         }
      }
   }

   @FunctionalInterface
   interface CommandFunction<T, R> {
      R apply(T var1) throws CommandSyntaxException;
   }

   private static record DimensionAndPosition(ServerLevel a, BlockPos b) {
      private final ServerLevel dimension;
      private final BlockPos position;

      DimensionAndPosition(ServerLevel var1, BlockPos var2) {
         this.dimension = var1;
         this.position = var2;
      }

      public ServerLevel dimension() {
         return this.dimension;
      }

      public BlockPos position() {
         return this.position;
      }
   }

   private static enum Mode {
      FORCE(true),
      MOVE(true),
      NORMAL(false);

      private final boolean canOverlap;

      private Mode(boolean var3) {
         this.canOverlap = var3;
      }

      public boolean canOverlap() {
         return this.canOverlap;
      }

      // $FF: synthetic method
      private static CloneCommands.Mode[] $values() {
         return new CloneCommands.Mode[]{FORCE, MOVE, NORMAL};
      }
   }

   static class CloneBlockInfo {
      public final BlockPos pos;
      public final BlockState state;
      @Nullable
      public final CompoundTag tag;

      public CloneBlockInfo(BlockPos var1, BlockState var2, @Nullable CompoundTag var3) {
         this.pos = var1;
         this.state = var2;
         this.tag = var3;
      }
   }
}
