package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;

public record ClientboundDeleteChatPacket(MessageSignature.Packed a) implements Packet<ClientGamePacketListener> {
   private final MessageSignature.Packed messageSignature;

   public ClientboundDeleteChatPacket(FriendlyByteBuf var1) {
      this(MessageSignature.Packed.read(var1));
   }

   public ClientboundDeleteChatPacket(MessageSignature.Packed var1) {
      this.messageSignature = var1;
   }

   public void write(FriendlyByteBuf var1) {
      MessageSignature.Packed.write(var1, this.messageSignature);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleDeleteChat(this);
   }

   public MessageSignature.Packed messageSignature() {
      return this.messageSignature;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
