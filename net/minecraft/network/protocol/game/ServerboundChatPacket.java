package net.minecraft.network.protocol.game;

import java.time.Instant;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatPacket(String a, Instant b, long c, @Nullable MessageSignature d, LastSeenMessages.Update e) implements Packet<ServerGamePacketListener> {
   private final String message;
   private final Instant timeStamp;
   private final long salt;
   @Nullable
   private final MessageSignature signature;
   private final LastSeenMessages.Update lastSeenMessages;

   public ServerboundChatPacket(FriendlyByteBuf var1) {
      this(var1.readUtf(256), var1.readInstant(), var1.readLong(), (MessageSignature)var1.readNullable(MessageSignature::read), new LastSeenMessages.Update(var1));
   }

   public ServerboundChatPacket(String var1, Instant var2, long var3, @Nullable MessageSignature var5, LastSeenMessages.Update var6) {
      this.message = var1;
      this.timeStamp = var2;
      this.salt = var3;
      this.signature = var5;
      this.lastSeenMessages = var6;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeUtf(this.message, 256);
      var1.writeInstant(this.timeStamp);
      var1.writeLong(this.salt);
      var1.writeNullable(this.signature, MessageSignature::write);
      this.lastSeenMessages.write(var1);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleChat(this);
   }

   public String message() {
      return this.message;
   }

   public Instant timeStamp() {
      return this.timeStamp;
   }

   public long salt() {
      return this.salt;
   }

   @Nullable
   public MessageSignature signature() {
      return this.signature;
   }

   public LastSeenMessages.Update lastSeenMessages() {
      return this.lastSeenMessages;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
