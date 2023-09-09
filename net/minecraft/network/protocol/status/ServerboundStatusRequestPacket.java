package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundStatusRequestPacket implements Packet<ServerStatusPacketListener> {
   public ServerboundStatusRequestPacket() {
   }

   public ServerboundStatusRequestPacket(FriendlyByteBuf var1) {
   }

   public void write(FriendlyByteBuf var1) {
   }

   public void handle(ServerStatusPacketListener var1) {
      var1.handleStatusRequest(this);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerStatusPacketListener)var1);
   }
}
