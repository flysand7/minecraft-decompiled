package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public record ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action a, List<String> b) implements Packet<ClientGamePacketListener> {
   private final ClientboundCustomChatCompletionsPacket.Action action;
   private final List<String> entries;

   public ClientboundCustomChatCompletionsPacket(FriendlyByteBuf var1) {
      this((ClientboundCustomChatCompletionsPacket.Action)var1.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), var1.readList(FriendlyByteBuf::readUtf));
   }

   public ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action var1, List<String> var2) {
      this.action = var1;
      this.entries = var2;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeEnum(this.action);
      var1.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleCustomChatCompletions(this);
   }

   public ClientboundCustomChatCompletionsPacket.Action action() {
      return this.action;
   }

   public List<String> entries() {
      return this.entries;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }

   public static enum Action {
      ADD,
      REMOVE,
      SET;

      private Action() {
      }

      // $FF: synthetic method
      private static ClientboundCustomChatCompletionsPacket.Action[] $values() {
         return new ClientboundCustomChatCompletionsPacket.Action[]{ADD, REMOVE, SET};
      }
   }
}
