package net.minecraft.network.protocol.game;

import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;

public class ClientboundBundlePacket extends BundlePacket<ClientGamePacketListener> {
   public ClientboundBundlePacket(Iterable<Packet<ClientGamePacketListener>> var1) {
      super(var1);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleBundlePacket(this);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
