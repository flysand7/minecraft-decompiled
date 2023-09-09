package net.minecraft.network.protocol.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.Item;

public class ClientboundCooldownPacket implements Packet<ClientGamePacketListener> {
   private final Item item;
   private final int duration;

   public ClientboundCooldownPacket(Item var1, int var2) {
      this.item = var1;
      this.duration = var2;
   }

   public ClientboundCooldownPacket(FriendlyByteBuf var1) {
      this.item = (Item)var1.readById(BuiltInRegistries.ITEM);
      this.duration = var1.readVarInt();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeId(BuiltInRegistries.ITEM, this.item);
      var1.writeVarInt(this.duration);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleItemCooldown(this);
   }

   public Item getItem() {
      return this.item;
   }

   public int getDuration() {
      return this.duration;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
