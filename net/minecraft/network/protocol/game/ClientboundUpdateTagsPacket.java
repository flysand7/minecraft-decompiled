package net.minecraft.network.protocol.game;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagNetworkSerialization;

public class ClientboundUpdateTagsPacket implements Packet<ClientGamePacketListener> {
   private final Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> tags;

   public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> var1) {
      this.tags = var1;
   }

   public ClientboundUpdateTagsPacket(FriendlyByteBuf var1) {
      this.tags = var1.readMap((var0) -> {
         return ResourceKey.createRegistryKey(var0.readResourceLocation());
      }, TagNetworkSerialization.NetworkPayload::read);
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeMap(this.tags, (var0, var1x) -> {
         var0.writeResourceLocation(var1x.location());
      }, (var0, var1x) -> {
         var1x.write(var0);
      });
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleUpdateTags(this);
   }

   public Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> getTags() {
      return this.tags;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
