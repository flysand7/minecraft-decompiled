package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundPlayerCombatEnterPacket implements Packet<ClientGamePacketListener> {
   public ClientboundPlayerCombatEnterPacket() {
   }

   public ClientboundPlayerCombatEnterPacket(FriendlyByteBuf var1) {
   }

   public void write(FriendlyByteBuf var1) {
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePlayerCombatEnter(this);
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
