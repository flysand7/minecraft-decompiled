package net.minecraft.network.protocol.game;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerInfoRemovePacket(List<UUID> a) implements Packet<ClientGamePacketListener> {
   private final List<UUID> profileIds;

   public ClientboundPlayerInfoRemovePacket(FriendlyByteBuf var1) {
      this(var1.readList(FriendlyByteBuf::readUUID));
   }

   public ClientboundPlayerInfoRemovePacket(List<UUID> var1) {
      this.profileIds = var1;
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeCollection(this.profileIds, FriendlyByteBuf::writeUUID);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handlePlayerInfoRemove(this);
   }

   public List<UUID> profileIds() {
      return this.profileIds;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
