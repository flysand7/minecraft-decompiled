package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundClientCommandPacket implements Packet<ServerGamePacketListener> {
   private final ServerboundClientCommandPacket.Action action;

   public ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action var1) {
      this.action = var1;
   }

   public ServerboundClientCommandPacket(FriendlyByteBuf var1) {
      this.action = (ServerboundClientCommandPacket.Action)var1.readEnum(ServerboundClientCommandPacket.Action.class);
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeEnum(this.action);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleClientCommand(this);
   }

   public ServerboundClientCommandPacket.Action getAction() {
      return this.action;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }

   public static enum Action {
      PERFORM_RESPAWN,
      REQUEST_STATS;

      private Action() {
      }

      // $FF: synthetic method
      private static ServerboundClientCommandPacket.Action[] $values() {
         return new ServerboundClientCommandPacket.Action[]{PERFORM_RESPAWN, REQUEST_STATS};
      }
   }
}
