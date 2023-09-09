package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundSelectAdvancementsTabPacket implements Packet<ClientGamePacketListener> {
   @Nullable
   private final ResourceLocation tab;

   public ClientboundSelectAdvancementsTabPacket(@Nullable ResourceLocation var1) {
      this.tab = var1;
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleSelectAdvancementsTab(this);
   }

   public ClientboundSelectAdvancementsTabPacket(FriendlyByteBuf var1) {
      this.tab = (ResourceLocation)var1.readNullable(FriendlyByteBuf::readResourceLocation);
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeNullable(this.tab, FriendlyByteBuf::writeResourceLocation);
   }

   @Nullable
   public ResourceLocation getTab() {
      return this.tab;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
