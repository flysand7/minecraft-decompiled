package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetTitleTextPacket implements Packet<ClientGamePacketListener> {
   private final Component text;

   public ClientboundSetTitleTextPacket(Component var1) {
      this.text = var1;
   }

   public ClientboundSetTitleTextPacket(FriendlyByteBuf var1) {
      this.text = var1.readComponent();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeComponent(this.text);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.setTitleText(this);
   }

   public Component getText() {
      return this.text;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
