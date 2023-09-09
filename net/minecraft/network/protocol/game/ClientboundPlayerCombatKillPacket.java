package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundPlayerCombatKillPacket implements Packet<ClientGamePacketListener> {
   private final int playerId;
   private final Component message;

   public ClientboundPlayerCombatKillPacket(int var1, Component var2) {
      this.playerId = var1;
      this.message = var2;
   }

   public ClientboundPlayerCombatKillPacket(FriendlyByteBuf var1) {
      this.playerId = var1.readVarInt();
      this.message = var1.readComponent();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.playerId);
      var1.writeComponent(this.message);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePlayerCombatKill(this);
   }

   public boolean isSkippable() {
      return true;
   }

   public int getPlayerId() {
      return this.playerId;
   }

   public Component getMessage() {
      return this.message;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
