package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public record ClientboundStatusResponsePacket(ServerStatus a) implements Packet<ClientStatusPacketListener> {
   private final ServerStatus status;

   public ClientboundStatusResponsePacket(FriendlyByteBuf var1) {
      this((ServerStatus)var1.readJsonWithCodec(ServerStatus.CODEC));
   }

   public ClientboundStatusResponsePacket(ServerStatus var1) {
      this.status = var1;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeJsonWithCodec(ServerStatus.CODEC, this.status);
   }

   public void handle(ClientStatusPacketListener var1) {
      var1.handleStatusResponse(this);
   }

   public ServerStatus status() {
      return this.status;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientStatusPacketListener)var1);
   }
}
