package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
   private final HolderLookup<Item> items;

   public ItemPredicateArgument(CommandBuildContext var1) {
      this.items = var1.holderLookup(Registries.ITEM);
   }

   public static ItemPredicateArgument itemPredicate(CommandBuildContext var0) {
      return new ItemPredicateArgument(var0);
   }

   public ItemPredicateArgument.Result parse(StringReader var1) throws CommandSyntaxException {
      Either var2 = ItemParser.parseForTesting(this.items, var1);
      return (ItemPredicateArgument.Result)var2.map((var0) -> {
         return createResult((var1) -> {
            return var1 == var0.item();
         }, var0.nbt());
      }, (var0) -> {
         HolderSet var10000 = var0.tag();
         Objects.requireNonNull(var10000);
         return createResult(var10000::contains, var0.nbt());
      });
   }

   public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandSourceStack> var0, String var1) {
      return (Predicate)var0.getArgument(var1, ItemPredicateArgument.Result.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> var1, SuggestionsBuilder var2) {
      return ItemParser.fillSuggestions(this.items, var2, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static ItemPredicateArgument.Result createResult(Predicate<Holder<Item>> var0, @Nullable CompoundTag var1) {
      return var1 != null ? (var2) -> {
         return var2.is(var0) && NbtUtils.compareNbt(var1, var2.getTag(), true);
      } : (var1x) -> {
         return var1x.is(var0);
      };
   }

   // $FF: synthetic method
   public Object parse(StringReader var1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   public interface Result extends Predicate<ItemStack> {
   }
}
