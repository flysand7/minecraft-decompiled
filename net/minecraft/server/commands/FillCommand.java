package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FillCommand {
   private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatable("commands.fill.toobig", var0, var1);
   });
   static final BlockInput HOLLOW_CORE;
   private static final SimpleCommandExceptionType ERROR_FAILED;

   public FillCommand() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> var0, CommandBuildContext var1) {
      var0.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fill").requires((var0x) -> {
         return var0x.hasPermission(2);
      })).then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("block", BlockStateArgument.block(var1)).executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.REPLACE, (Predicate)null);
      })).then(((LiteralArgumentBuilder)Commands.literal("replace").executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.REPLACE, (Predicate)null);
      })).then(Commands.argument("filter", BlockPredicateArgument.blockPredicate(var1)).executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.REPLACE, BlockPredicateArgument.getBlockPredicate(var0x, "filter"));
      })))).then(Commands.literal("keep").executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.REPLACE, (var0) -> {
            return var0.getLevel().isEmptyBlock(var0.getPos());
         });
      }))).then(Commands.literal("outline").executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.OUTLINE, (Predicate)null);
      }))).then(Commands.literal("hollow").executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.HOLLOW, (Predicate)null);
      }))).then(Commands.literal("destroy").executes((var0x) -> {
         return fillBlocks((CommandSourceStack)var0x.getSource(), BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(var0x, "from"), BlockPosArgument.getLoadedBlockPos(var0x, "to")), BlockStateArgument.getBlock(var0x, "block"), FillCommand.Mode.DESTROY, (Predicate)null);
      }))))));
   }

   private static int fillBlocks(CommandSourceStack var0, BoundingBox var1, BlockInput var2, FillCommand.Mode var3, @Nullable Predicate<BlockInWorld> var4) throws CommandSyntaxException {
      int var5 = var1.getXSpan() * var1.getYSpan() * var1.getZSpan();
      int var6 = var0.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if (var5 > var6) {
         throw ERROR_AREA_TOO_LARGE.create(var6, var5);
      } else {
         ArrayList var7 = Lists.newArrayList();
         ServerLevel var8 = var0.getLevel();
         int var9 = 0;
         Iterator var10 = BlockPos.betweenClosed(var1.minX(), var1.minY(), var1.minZ(), var1.maxX(), var1.maxY(), var1.maxZ()).iterator();

         while(true) {
            BlockPos var11;
            do {
               if (!var10.hasNext()) {
                  var10 = var7.iterator();

                  while(var10.hasNext()) {
                     var11 = (BlockPos)var10.next();
                     Block var14 = var8.getBlockState(var11).getBlock();
                     var8.blockUpdated(var11, var14);
                  }

                  if (var9 == 0) {
                     throw ERROR_FAILED.create();
                  }

                  var0.sendSuccess(() -> {
                     return Component.translatable("commands.fill.success", var9);
                  }, true);
                  return var9;
               }

               var11 = (BlockPos)var10.next();
            } while(var4 != null && !var4.test(new BlockInWorld(var8, var11, true)));

            BlockInput var12 = var3.filter.filter(var1, var11, var2, var8);
            if (var12 != null) {
               BlockEntity var13 = var8.getBlockEntity(var11);
               Clearable.tryClear(var13);
               if (var12.place(var8, var11, 2)) {
                  var7.add(var11.immutable());
                  ++var9;
               }
            }
         }
      }
   }

   static {
      HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), (CompoundTag)null);
      ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));
   }

   private static enum Mode {
      REPLACE((var0, var1, var2, var3) -> {
         return var2;
      }),
      OUTLINE((var0, var1, var2, var3) -> {
         return var1.getX() != var0.minX() && var1.getX() != var0.maxX() && var1.getY() != var0.minY() && var1.getY() != var0.maxY() && var1.getZ() != var0.minZ() && var1.getZ() != var0.maxZ() ? null : var2;
      }),
      HOLLOW((var0, var1, var2, var3) -> {
         return var1.getX() != var0.minX() && var1.getX() != var0.maxX() && var1.getY() != var0.minY() && var1.getY() != var0.maxY() && var1.getZ() != var0.minZ() && var1.getZ() != var0.maxZ() ? FillCommand.HOLLOW_CORE : var2;
      }),
      DESTROY((var0, var1, var2, var3) -> {
         var3.destroyBlock(var1, true);
         return var2;
      });

      public final SetBlockCommand.Filter filter;

      private Mode(SetBlockCommand.Filter var3) {
         this.filter = var3;
      }

      // $FF: synthetic method
      private static FillCommand.Mode[] $values() {
         return new FillCommand.Mode[]{REPLACE, OUTLINE, HOLLOW, DESTROY};
      }
   }
}
