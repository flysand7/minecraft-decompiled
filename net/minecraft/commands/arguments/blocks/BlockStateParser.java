package net.minecraft.commands.arguments.blocks;

import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockStateParser {
   public static final SimpleCommandExceptionType ERROR_NO_TAGS_ALLOWED = new SimpleCommandExceptionType(Component.translatable("argument.block.tag.disallowed"));
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_BLOCK = new DynamicCommandExceptionType((var0) -> {
      return Component.translatable("argument.block.id.invalid", var0);
   });
   public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_PROPERTY = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatable("argument.block.property.unknown", var0, var1);
   });
   public static final Dynamic2CommandExceptionType ERROR_DUPLICATE_PROPERTY = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatable("argument.block.property.duplicate", var1, var0);
   });
   public static final Dynamic3CommandExceptionType ERROR_INVALID_VALUE = new Dynamic3CommandExceptionType((var0, var1, var2) -> {
      return Component.translatable("argument.block.property.invalid", var0, var2, var1);
   });
   public static final Dynamic2CommandExceptionType ERROR_EXPECTED_VALUE = new Dynamic2CommandExceptionType((var0, var1) -> {
      return Component.translatable("argument.block.property.novalue", var0, var1);
   });
   public static final SimpleCommandExceptionType ERROR_EXPECTED_END_OF_PROPERTIES = new SimpleCommandExceptionType(Component.translatable("argument.block.property.unclosed"));
   public static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((var0) -> {
      return Component.translatable("arguments.block.tag.unknown", var0);
   });
   private static final char SYNTAX_START_PROPERTIES = '[';
   private static final char SYNTAX_START_NBT = '{';
   private static final char SYNTAX_END_PROPERTIES = ']';
   private static final char SYNTAX_EQUALS = '=';
   private static final char SYNTAX_PROPERTY_SEPARATOR = ',';
   private static final char SYNTAX_TAG = '#';
   private static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
   private final HolderLookup<Block> blocks;
   private final StringReader reader;
   private final boolean forTesting;
   private final boolean allowNbt;
   private final Map<Property<?>, Comparable<?>> properties = Maps.newHashMap();
   private final Map<String, String> vagueProperties = Maps.newHashMap();
   private ResourceLocation id = new ResourceLocation("");
   @Nullable
   private StateDefinition<Block, BlockState> definition;
   @Nullable
   private BlockState state;
   @Nullable
   private CompoundTag nbt;
   @Nullable
   private HolderSet<Block> tag;
   private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions;

   private BlockStateParser(HolderLookup<Block> var1, StringReader var2, boolean var3, boolean var4) {
      this.suggestions = SUGGEST_NOTHING;
      this.blocks = var1;
      this.reader = var2;
      this.forTesting = var3;
      this.allowNbt = var4;
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> var0, String var1, boolean var2) throws CommandSyntaxException {
      return parseForBlock(var0, new StringReader(var1), var2);
   }

   public static BlockStateParser.BlockResult parseForBlock(HolderLookup<Block> var0, StringReader var1, boolean var2) throws CommandSyntaxException {
      int var3 = var1.getCursor();

      try {
         BlockStateParser var4 = new BlockStateParser(var0, var1, false, var2);
         var4.parse();
         return new BlockStateParser.BlockResult(var4.state, var4.properties, var4.nbt);
      } catch (CommandSyntaxException var5) {
         var1.setCursor(var3);
         throw var5;
      }
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(HolderLookup<Block> var0, String var1, boolean var2) throws CommandSyntaxException {
      return parseForTesting(var0, new StringReader(var1), var2);
   }

   public static Either<BlockStateParser.BlockResult, BlockStateParser.TagResult> parseForTesting(HolderLookup<Block> var0, StringReader var1, boolean var2) throws CommandSyntaxException {
      int var3 = var1.getCursor();

      try {
         BlockStateParser var4 = new BlockStateParser(var0, var1, true, var2);
         var4.parse();
         return var4.tag != null ? Either.right(new BlockStateParser.TagResult(var4.tag, var4.vagueProperties, var4.nbt)) : Either.left(new BlockStateParser.BlockResult(var4.state, var4.properties, var4.nbt));
      } catch (CommandSyntaxException var5) {
         var1.setCursor(var3);
         throw var5;
      }
   }

   public static CompletableFuture<Suggestions> fillSuggestions(HolderLookup<Block> var0, SuggestionsBuilder var1, boolean var2, boolean var3) {
      StringReader var4 = new StringReader(var1.getInput());
      var4.setCursor(var1.getStart());
      BlockStateParser var5 = new BlockStateParser(var0, var4, var2, var3);

      try {
         var5.parse();
      } catch (CommandSyntaxException var7) {
      }

      return (CompletableFuture)var5.suggestions.apply(var1.createOffset(var4.getCursor()));
   }

   private void parse() throws CommandSyntaxException {
      if (this.forTesting) {
         this.suggestions = this::suggestBlockIdOrTag;
      } else {
         this.suggestions = this::suggestItem;
      }

      if (this.reader.canRead() && this.reader.peek() == '#') {
         this.readTag();
         this.suggestions = this::suggestOpenVaguePropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readVagueProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      } else {
         this.readBlock();
         this.suggestions = this::suggestOpenPropertiesOrNbt;
         if (this.reader.canRead() && this.reader.peek() == '[') {
            this.readProperties();
            this.suggestions = this::suggestOpenNbt;
         }
      }

      if (this.allowNbt && this.reader.canRead() && this.reader.peek() == '{') {
         this.suggestions = SUGGEST_NOTHING;
         this.readNbt();
      }

   }

   private CompletableFuture<Suggestions> suggestPropertyNameOrEnd(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty()) {
         var1.suggest(String.valueOf(']'));
      }

      return this.suggestPropertyName(var1);
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyNameOrEnd(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty()) {
         var1.suggest(String.valueOf(']'));
      }

      return this.suggestVaguePropertyName(var1);
   }

   private CompletableFuture<Suggestions> suggestPropertyName(SuggestionsBuilder var1) {
      String var2 = var1.getRemaining().toLowerCase(Locale.ROOT);
      Iterator var3 = this.state.getProperties().iterator();

      while(var3.hasNext()) {
         Property var4 = (Property)var3.next();
         if (!this.properties.containsKey(var4) && var4.getName().startsWith(var2)) {
            var1.suggest(var4.getName() + "=");
         }
      }

      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyName(SuggestionsBuilder var1) {
      String var2 = var1.getRemaining().toLowerCase(Locale.ROOT);
      if (this.tag != null) {
         Iterator var3 = this.tag.iterator();

         while(var3.hasNext()) {
            Holder var4 = (Holder)var3.next();
            Iterator var5 = ((Block)var4.value()).getStateDefinition().getProperties().iterator();

            while(var5.hasNext()) {
               Property var6 = (Property)var5.next();
               if (!this.vagueProperties.containsKey(var6.getName()) && var6.getName().startsWith(var2)) {
                  var1.suggest(var6.getName() + "=");
               }
            }
         }
      }

      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenNbt(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty() && this.hasBlockEntity()) {
         var1.suggest(String.valueOf('{'));
      }

      return var1.buildFuture();
   }

   private boolean hasBlockEntity() {
      if (this.state != null) {
         return this.state.hasBlockEntity();
      } else {
         if (this.tag != null) {
            Iterator var1 = this.tag.iterator();

            while(var1.hasNext()) {
               Holder var2 = (Holder)var1.next();
               if (((Block)var2.value()).defaultBlockState().hasBlockEntity()) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private CompletableFuture<Suggestions> suggestEquals(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty()) {
         var1.suggest(String.valueOf('='));
      }

      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestNextPropertyOrEnd(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty()) {
         var1.suggest(String.valueOf(']'));
      }

      if (var1.getRemaining().isEmpty() && this.properties.size() < this.state.getProperties().size()) {
         var1.suggest(String.valueOf(','));
      }

      return var1.buildFuture();
   }

   private static <T extends Comparable<T>> SuggestionsBuilder addSuggestions(SuggestionsBuilder var0, Property<T> var1) {
      Iterator var2 = var1.getPossibleValues().iterator();

      while(var2.hasNext()) {
         Comparable var3 = (Comparable)var2.next();
         if (var3 instanceof Integer) {
            Integer var4 = (Integer)var3;
            var0.suggest(var4);
         } else {
            var0.suggest(var1.getName(var3));
         }
      }

      return var0;
   }

   private CompletableFuture<Suggestions> suggestVaguePropertyValue(SuggestionsBuilder var1, String var2) {
      boolean var3 = false;
      if (this.tag != null) {
         Iterator var4 = this.tag.iterator();

         label38:
         while(true) {
            while(true) {
               Block var6;
               do {
                  if (!var4.hasNext()) {
                     break label38;
                  }

                  Holder var5 = (Holder)var4.next();
                  var6 = (Block)var5.value();
                  Property var7 = var6.getStateDefinition().getProperty(var2);
                  if (var7 != null) {
                     addSuggestions(var1, var7);
                  }
               } while(var3);

               Iterator var8 = var6.getStateDefinition().getProperties().iterator();

               while(var8.hasNext()) {
                  Property var9 = (Property)var8.next();
                  if (!this.vagueProperties.containsKey(var9.getName())) {
                     var3 = true;
                     break;
                  }
               }
            }
         }
      }

      if (var3) {
         var1.suggest(String.valueOf(','));
      }

      var1.suggest(String.valueOf(']'));
      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenVaguePropertiesOrNbt(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty() && this.tag != null) {
         boolean var2 = false;
         boolean var3 = false;
         Iterator var4 = this.tag.iterator();

         while(var4.hasNext()) {
            Holder var5 = (Holder)var4.next();
            Block var6 = (Block)var5.value();
            var2 |= !var6.getStateDefinition().getProperties().isEmpty();
            var3 |= var6.defaultBlockState().hasBlockEntity();
            if (var2 && var3) {
               break;
            }
         }

         if (var2) {
            var1.suggest(String.valueOf('['));
         }

         if (var3) {
            var1.suggest(String.valueOf('{'));
         }
      }

      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestOpenPropertiesOrNbt(SuggestionsBuilder var1) {
      if (var1.getRemaining().isEmpty()) {
         if (!this.definition.getProperties().isEmpty()) {
            var1.suggest(String.valueOf('['));
         }

         if (this.state.hasBlockEntity()) {
            var1.suggest(String.valueOf('{'));
         }
      }

      return var1.buildFuture();
   }

   private CompletableFuture<Suggestions> suggestTag(SuggestionsBuilder var1) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listTagIds().map(TagKey::location), var1, String.valueOf('#'));
   }

   private CompletableFuture<Suggestions> suggestItem(SuggestionsBuilder var1) {
      return SharedSuggestionProvider.suggestResource(this.blocks.listElementIds().map(ResourceKey::location), var1);
   }

   private CompletableFuture<Suggestions> suggestBlockIdOrTag(SuggestionsBuilder var1) {
      this.suggestTag(var1);
      this.suggestItem(var1);
      return var1.buildFuture();
   }

   private void readBlock() throws CommandSyntaxException {
      int var1 = this.reader.getCursor();
      this.id = ResourceLocation.read(this.reader);
      Block var2 = (Block)((Holder.Reference)this.blocks.get(ResourceKey.create(Registries.BLOCK, this.id)).orElseThrow(() -> {
         this.reader.setCursor(var1);
         return ERROR_UNKNOWN_BLOCK.createWithContext(this.reader, this.id.toString());
      })).value();
      this.definition = var2.getStateDefinition();
      this.state = var2.defaultBlockState();
   }

   private void readTag() throws CommandSyntaxException {
      if (!this.forTesting) {
         throw ERROR_NO_TAGS_ALLOWED.createWithContext(this.reader);
      } else {
         int var1 = this.reader.getCursor();
         this.reader.expect('#');
         this.suggestions = this::suggestTag;
         ResourceLocation var2 = ResourceLocation.read(this.reader);
         this.tag = (HolderSet)this.blocks.get(TagKey.create(Registries.BLOCK, var2)).orElseThrow(() -> {
            this.reader.setCursor(var1);
            return ERROR_UNKNOWN_TAG.createWithContext(this.reader, var2.toString());
         });
      }
   }

   private void readProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestPropertyNameOrEnd;
      this.reader.skipWhitespace();

      while(this.reader.canRead() && this.reader.peek() != ']') {
         this.reader.skipWhitespace();
         int var1 = this.reader.getCursor();
         String var2 = this.reader.readString();
         Property var3 = this.definition.getProperty(var2);
         if (var3 == null) {
            this.reader.setCursor(var1);
            throw ERROR_UNKNOWN_PROPERTY.createWithContext(this.reader, this.id.toString(), var2);
         }

         if (this.properties.containsKey(var3)) {
            this.reader.setCursor(var1);
            throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), var2);
         }

         this.reader.skipWhitespace();
         this.suggestions = this::suggestEquals;
         if (this.reader.canRead() && this.reader.peek() == '=') {
            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggestions = (var1x) -> {
               return addSuggestions(var1x, var3).buildFuture();
            };
            int var4 = this.reader.getCursor();
            this.setValue(var3, this.reader.readString(), var4);
            this.suggestions = this::suggestNextPropertyOrEnd;
            this.reader.skipWhitespace();
            if (!this.reader.canRead()) {
               continue;
            }

            if (this.reader.peek() == ',') {
               this.reader.skip();
               this.suggestions = this::suggestPropertyName;
               continue;
            }

            if (this.reader.peek() != ']') {
               throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
            }
            break;
         }

         throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), var2);
      }

      if (this.reader.canRead()) {
         this.reader.skip();
      } else {
         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readVagueProperties() throws CommandSyntaxException {
      this.reader.skip();
      this.suggestions = this::suggestVaguePropertyNameOrEnd;
      int var1 = -1;
      this.reader.skipWhitespace();

      while(true) {
         if (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int var2 = this.reader.getCursor();
            String var3 = this.reader.readString();
            if (this.vagueProperties.containsKey(var3)) {
               this.reader.setCursor(var2);
               throw ERROR_DUPLICATE_PROPERTY.createWithContext(this.reader, this.id.toString(), var3);
            }

            this.reader.skipWhitespace();
            if (!this.reader.canRead() || this.reader.peek() != '=') {
               this.reader.setCursor(var2);
               throw ERROR_EXPECTED_VALUE.createWithContext(this.reader, this.id.toString(), var3);
            }

            this.reader.skip();
            this.reader.skipWhitespace();
            this.suggestions = (var2x) -> {
               return this.suggestVaguePropertyValue(var2x, var3);
            };
            var1 = this.reader.getCursor();
            String var4 = this.reader.readString();
            this.vagueProperties.put(var3, var4);
            this.reader.skipWhitespace();
            if (!this.reader.canRead()) {
               continue;
            }

            var1 = -1;
            if (this.reader.peek() == ',') {
               this.reader.skip();
               this.suggestions = this::suggestVaguePropertyName;
               continue;
            }

            if (this.reader.peek() != ']') {
               throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
            }
         }

         if (this.reader.canRead()) {
            this.reader.skip();
            return;
         }

         if (var1 >= 0) {
            this.reader.setCursor(var1);
         }

         throw ERROR_EXPECTED_END_OF_PROPERTIES.createWithContext(this.reader);
      }
   }

   private void readNbt() throws CommandSyntaxException {
      this.nbt = (new TagParser(this.reader)).readStruct();
   }

   private <T extends Comparable<T>> void setValue(Property<T> var1, String var2, int var3) throws CommandSyntaxException {
      Optional var4 = var1.getValue(var2);
      if (var4.isPresent()) {
         this.state = (BlockState)this.state.setValue(var1, (Comparable)var4.get());
         this.properties.put(var1, (Comparable)var4.get());
      } else {
         this.reader.setCursor(var3);
         throw ERROR_INVALID_VALUE.createWithContext(this.reader, this.id.toString(), var1.getName(), var2);
      }
   }

   public static String serialize(BlockState var0) {
      StringBuilder var1 = new StringBuilder((String)var0.getBlockHolder().unwrapKey().map((var0x) -> {
         return var0x.location().toString();
      }).orElse("air"));
      if (!var0.getProperties().isEmpty()) {
         var1.append('[');
         boolean var2 = false;

         for(UnmodifiableIterator var3 = var0.getValues().entrySet().iterator(); var3.hasNext(); var2 = true) {
            Entry var4 = (Entry)var3.next();
            if (var2) {
               var1.append(',');
            }

            appendProperty(var1, (Property)var4.getKey(), (Comparable)var4.getValue());
         }

         var1.append(']');
      }

      return var1.toString();
   }

   private static <T extends Comparable<T>> void appendProperty(StringBuilder var0, Property<T> var1, Comparable<?> var2) {
      var0.append(var1.getName());
      var0.append('=');
      var0.append(var1.getName(var2));
   }

   public static record BlockResult(BlockState a, Map<Property<?>, Comparable<?>> b, @Nullable CompoundTag c) {
      private final BlockState blockState;
      private final Map<Property<?>, Comparable<?>> properties;
      @Nullable
      private final CompoundTag nbt;

      public BlockResult(BlockState var1, Map<Property<?>, Comparable<?>> var2, @Nullable CompoundTag var3) {
         this.blockState = var1;
         this.properties = var2;
         this.nbt = var3;
      }

      public BlockState blockState() {
         return this.blockState;
      }

      public Map<Property<?>, Comparable<?>> properties() {
         return this.properties;
      }

      @Nullable
      public CompoundTag nbt() {
         return this.nbt;
      }
   }

   public static record TagResult(HolderSet<Block> a, Map<String, String> b, @Nullable CompoundTag c) {
      private final HolderSet<Block> tag;
      private final Map<String, String> vagueProperties;
      @Nullable
      private final CompoundTag nbt;

      public TagResult(HolderSet<Block> var1, Map<String, String> var2, @Nullable CompoundTag var3) {
         this.tag = var1;
         this.vagueProperties = var2;
         this.nbt = var3;
      }

      public HolderSet<Block> tag() {
         return this.tag;
      }

      public Map<String, String> vagueProperties() {
         return this.vagueProperties;
      }

      @Nullable
      public CompoundTag nbt() {
         return this.nbt;
      }
   }
}
