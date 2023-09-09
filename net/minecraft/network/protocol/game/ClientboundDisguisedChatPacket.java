package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public record ClientboundDisguisedChatPacket(Component a, ChatType.BoundNetwork b) implements Packet<ClientGamePacketListener> {
   private final Component message;
   private final ChatType.BoundNetwork chatType;

   public ClientboundDisguisedChatPacket(FriendlyByteBuf var1) {
      this(var1.readComponent(), new ChatType.BoundNetwork(var1));
   }

   public ClientboundDisguisedChatPacket(Component var1, ChatType.BoundNetwork var2) {
      this.message = var1;
      this.chatType = var2;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeComponent(this.message);
      this.chatType.write(var1);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleDisguisedChat(this);
   }

   public boolean isSkippable() {
      return true;
   }

   public Component message() {
      return this.message;
   }

   public ChatType.BoundNetwork chatType() {
      return this.chatType;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
