package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSigningContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;

public class MessageArgument implements SignedArgument<MessageArgument.Message> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Hello world!", "foo", "@e", "Hello @p :)");

   public MessageArgument() {
   }

   public static MessageArgument message() {
      return new MessageArgument();
   }

   public static Component getMessage(CommandContext<CommandSourceStack> var0, String var1) throws CommandSyntaxException {
      MessageArgument.Message var2 = (MessageArgument.Message)var0.getArgument(var1, MessageArgument.Message.class);
      return var2.resolveComponent((CommandSourceStack)var0.getSource());
   }

   public static void resolveChatMessage(CommandContext<CommandSourceStack> var0, String var1, Consumer<PlayerChatMessage> var2) throws CommandSyntaxException {
      MessageArgument.Message var3 = (MessageArgument.Message)var0.getArgument(var1, MessageArgument.Message.class);
      CommandSourceStack var4 = (CommandSourceStack)var0.getSource();
      Component var5 = var3.resolveComponent(var4);
      CommandSigningContext var6 = var4.getSigningContext();
      PlayerChatMessage var7 = var6.getArgument(var1);
      if (var7 != null) {
         resolveSignedMessage(var2, var4, var7.withUnsignedContent(var5));
      } else {
         resolveDisguisedMessage(var2, var4, PlayerChatMessage.system(var3.text).withUnsignedContent(var5));
      }

   }

   private static void resolveSignedMessage(Consumer<PlayerChatMessage> var0, CommandSourceStack var1, PlayerChatMessage var2) {
      MinecraftServer var3 = var1.getServer();
      CompletableFuture var4 = filterPlainText(var1, var2);
      CompletableFuture var5 = var3.getChatDecorator().decorate(var1.getPlayer(), var2.decoratedContent());
      var1.getChatMessageChainer().append((var4x) -> {
         return CompletableFuture.allOf(var4, var5).thenAcceptAsync((var4xx) -> {
            PlayerChatMessage var5x = var2.withUnsignedContent((Component)var5.join()).filter(((FilteredText)var4.join()).mask());
            var0.accept(var5x);
         }, var4x);
      });
   }

   private static void resolveDisguisedMessage(Consumer<PlayerChatMessage> var0, CommandSourceStack var1, PlayerChatMessage var2) {
      MinecraftServer var3 = var1.getServer();
      CompletableFuture var4 = var3.getChatDecorator().decorate(var1.getPlayer(), var2.decoratedContent());
      var1.getChatMessageChainer().append((var3x) -> {
         return var4.thenAcceptAsync((var2x) -> {
            var0.accept(var2.withUnsignedContent(var2x));
         }, var3x);
      });
   }

   private static CompletableFuture<FilteredText> filterPlainText(CommandSourceStack var0, PlayerChatMessage var1) {
      ServerPlayer var2 = var0.getPlayer();
      return var2 != null && var1.hasSignatureFrom(var2.getUUID()) ? var2.getTextFilter().processStreamMessage(var1.signedContent()) : CompletableFuture.completedFuture(FilteredText.passThrough(var1.signedContent()));
   }

   public MessageArgument.Message parse(StringReader var1) throws CommandSyntaxException {
      return MessageArgument.Message.parseText(var1, true);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   // $FF: synthetic method
   public Object parse(StringReader var1) throws CommandSyntaxException {
      return this.parse(var1);
   }

   public static class Message {
      final String text;
      private final MessageArgument.Part[] parts;

      public Message(String var1, MessageArgument.Part[] var2) {
         this.text = var1;
         this.parts = var2;
      }

      public String getText() {
         return this.text;
      }

      public MessageArgument.Part[] getParts() {
         return this.parts;
      }

      Component resolveComponent(CommandSourceStack var1) throws CommandSyntaxException {
         return this.toComponent(var1, var1.hasPermission(2));
      }

      public Component toComponent(CommandSourceStack var1, boolean var2) throws CommandSyntaxException {
         if (this.parts.length != 0 && var2) {
            MutableComponent var3 = Component.literal(this.text.substring(0, this.parts[0].getStart()));
            int var4 = this.parts[0].getStart();
            MessageArgument.Part[] var5 = this.parts;
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               MessageArgument.Part var8 = var5[var7];
               Component var9 = var8.toComponent(var1);
               if (var4 < var8.getStart()) {
                  var3.append(this.text.substring(var4, var8.getStart()));
               }

               if (var9 != null) {
                  var3.append(var9);
               }

               var4 = var8.getEnd();
            }

            if (var4 < this.text.length()) {
               var3.append(this.text.substring(var4));
            }

            return var3;
         } else {
            return Component.literal(this.text);
         }
      }

      public static MessageArgument.Message parseText(StringReader var0, boolean var1) throws CommandSyntaxException {
         String var2 = var0.getString().substring(var0.getCursor(), var0.getTotalLength());
         if (!var1) {
            var0.setCursor(var0.getTotalLength());
            return new MessageArgument.Message(var2, new MessageArgument.Part[0]);
         } else {
            ArrayList var3 = Lists.newArrayList();
            int var4 = var0.getCursor();

            while(true) {
               int var5;
               EntitySelector var6;
               label38:
               while(true) {
                  while(var0.canRead()) {
                     if (var0.peek() == '@') {
                        var5 = var0.getCursor();

                        try {
                           EntitySelectorParser var7 = new EntitySelectorParser(var0);
                           var6 = var7.parse();
                           break label38;
                        } catch (CommandSyntaxException var8) {
                           if (var8.getType() != EntitySelectorParser.ERROR_MISSING_SELECTOR_TYPE && var8.getType() != EntitySelectorParser.ERROR_UNKNOWN_SELECTOR_TYPE) {
                              throw var8;
                           }

                           var0.setCursor(var5 + 1);
                        }
                     } else {
                        var0.skip();
                     }
                  }

                  return new MessageArgument.Message(var2, (MessageArgument.Part[])var3.toArray(new MessageArgument.Part[0]));
               }

               var3.add(new MessageArgument.Part(var5 - var4, var0.getCursor() - var4, var6));
            }
         }
      }
   }

   public static class Part {
      private final int start;
      private final int end;
      private final EntitySelector selector;

      public Part(int var1, int var2, EntitySelector var3) {
         this.start = var1;
         this.end = var2;
         this.selector = var3;
      }

      public int getStart() {
         return this.start;
      }

      public int getEnd() {
         return this.end;
      }

      public EntitySelector getSelector() {
         return this.selector;
      }

      @Nullable
      public Component toComponent(CommandSourceStack var1) throws CommandSyntaxException {
         return EntitySelector.joinNames(this.selector.findEntities(var1));
      }
   }
}
