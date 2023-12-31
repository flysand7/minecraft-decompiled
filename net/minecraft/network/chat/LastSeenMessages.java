package net.minecraft.network.chat;

import com.google.common.primitives.Ints;
import com.mojang.serialization.Codec;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SignatureUpdater;

public record LastSeenMessages(List<MessageSignature> d) {
   private final List<MessageSignature> entries;
   public static final Codec<LastSeenMessages> CODEC;
   public static LastSeenMessages EMPTY;
   public static final int LAST_SEEN_MESSAGES_MAX_LENGTH = 20;

   public LastSeenMessages(List<MessageSignature> var1) {
      this.entries = var1;
   }

   public void updateSignature(SignatureUpdater.Output var1) throws SignatureException {
      var1.update(Ints.toByteArray(this.entries.size()));
      Iterator var2 = this.entries.iterator();

      while(var2.hasNext()) {
         MessageSignature var3 = (MessageSignature)var2.next();
         var1.update(var3.bytes());
      }

   }

   public LastSeenMessages.Packed pack(MessageSignatureCache var1) {
      return new LastSeenMessages.Packed(this.entries.stream().map((var1x) -> {
         return var1x.pack(var1);
      }).toList());
   }

   public List<MessageSignature> entries() {
      return this.entries;
   }

   static {
      CODEC = MessageSignature.CODEC.listOf().xmap(LastSeenMessages::new, LastSeenMessages::entries);
      EMPTY = new LastSeenMessages(List.of());
   }

   public static record Packed(List<MessageSignature.Packed> b) {
      private final List<MessageSignature.Packed> entries;
      public static final LastSeenMessages.Packed EMPTY = new LastSeenMessages.Packed(List.of());

      public Packed(FriendlyByteBuf var1) {
         this((List)var1.readCollection(FriendlyByteBuf.limitValue(ArrayList::new, 20), MessageSignature.Packed::read));
      }

      public Packed(List<MessageSignature.Packed> var1) {
         this.entries = var1;
      }

      public void write(FriendlyByteBuf var1) {
         var1.writeCollection(this.entries, MessageSignature.Packed::write);
      }

      public Optional<LastSeenMessages> unpack(MessageSignatureCache var1) {
         ArrayList var2 = new ArrayList(this.entries.size());
         Iterator var3 = this.entries.iterator();

         while(var3.hasNext()) {
            MessageSignature.Packed var4 = (MessageSignature.Packed)var3.next();
            Optional var5 = var4.unpack(var1);
            if (var5.isEmpty()) {
               return Optional.empty();
            }

            var2.add((MessageSignature)var5.get());
         }

         return Optional.of(new LastSeenMessages(var2));
      }

      public List<MessageSignature.Packed> entries() {
         return this.entries;
      }
   }

   public static record Update(int a, BitSet b) {
      private final int offset;
      private final BitSet acknowledged;

      public Update(FriendlyByteBuf var1) {
         this(var1.readVarInt(), var1.readFixedBitSet(20));
      }

      public Update(int var1, BitSet var2) {
         this.offset = var1;
         this.acknowledged = var2;
      }

      public void write(FriendlyByteBuf var1) {
         var1.writeVarInt(this.offset);
         var1.writeFixedBitSet(this.acknowledged, 20);
      }

      public int offset() {
         return this.offset;
      }

      public BitSet acknowledged() {
         return this.acknowledged;
      }
   }
}
