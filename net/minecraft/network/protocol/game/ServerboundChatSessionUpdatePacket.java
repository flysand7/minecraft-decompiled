package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.RemoteChatSession;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatSessionUpdatePacket(RemoteChatSession.Data a) implements Packet<ServerGamePacketListener> {
   private final RemoteChatSession.Data chatSession;

   public ServerboundChatSessionUpdatePacket(FriendlyByteBuf var1) {
      this(RemoteChatSession.Data.read(var1));
   }

   public ServerboundChatSessionUpdatePacket(RemoteChatSession.Data var1) {
      this.chatSession = var1;
   }

   public void write(FriendlyByteBuf var1) {
      RemoteChatSession.Data.write(var1, this.chatSession);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleChatSessionUpdate(this);
   }

   public RemoteChatSession.Data chatSession() {
      return this.chatSession;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
