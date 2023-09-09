package net.minecraft.commands.arguments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignableCommand;

public record ArgumentSignatures(List<ArgumentSignatures.Entry> b) {
   private final List<ArgumentSignatures.Entry> entries;
   public static final ArgumentSignatures EMPTY = new ArgumentSignatures(List.of());
   private static final int MAX_ARGUMENT_COUNT = 8;
   private static final int MAX_ARGUMENT_NAME_LENGTH = 16;

   public ArgumentSignatures(FriendlyByteBuf var1) {
      this((List)var1.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 8), ArgumentSignatures.Entry::new));
   }

   public ArgumentSignatures(List<ArgumentSignatures.Entry> var1) {
      this.entries = var1;
   }

   @Nullable
   public MessageSignature get(String var1) {
      Iterator var2 = this.entries.iterator();

      ArgumentSignatures.Entry var3;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         var3 = (ArgumentSignatures.Entry)var2.next();
      } while(!var3.name.equals(var1));

      return var3.signature;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeCollection(this.entries, (var0, var1x) -> {
         var1x.write(var0);
      });
   }

   public static ArgumentSignatures signCommand(SignableCommand<?> var0, ArgumentSignatures.Signer var1) {
      List var2 = var0.arguments().stream().map((var1x) -> {
         MessageSignature var2 = var1.sign(var1x.value());
         return var2 != null ? new ArgumentSignatures.Entry(var1x.name(), var2) : null;
      }).filter(Objects::nonNull).toList();
      return new ArgumentSignatures(var2);
   }

   public List<ArgumentSignatures.Entry> entries() {
      return this.entries;
   }

   public static record Entry(String a, MessageSignature b) {
      final String name;
      final MessageSignature signature;

      public Entry(FriendlyByteBuf var1) {
         this(var1.readUtf(16), MessageSignature.read(var1));
      }

      public Entry(String var1, MessageSignature var2) {
         this.name = var1;
         this.signature = var2;
      }

      public void write(FriendlyByteBuf var1) {
         var1.writeUtf(this.name, 16);
         MessageSignature.write(var1, this.signature);
      }

      public String name() {
         return this.name;
      }

      public MessageSignature signature() {
         return this.signature;
      }
   }

   @FunctionalInterface
   public interface Signer {
      @Nullable
      MessageSignature sign(String var1);
   }
}
