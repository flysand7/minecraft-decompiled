package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetSimulationDistancePacket(int a) implements Packet<ClientGamePacketListener> {
   private final int simulationDistance;

   public ClientboundSetSimulationDistancePacket(FriendlyByteBuf var1) {
      this(var1.readVarInt());
   }

   public ClientboundSetSimulationDistancePacket(int var1) {
      this.simulationDistance = var1;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.simulationDistance);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSetSimulationDistance(this);
   }

   public int simulationDistance() {
      return this.simulationDistance;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
