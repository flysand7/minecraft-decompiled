package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ClientboundRemoveEntitiesPacket implements Packet<ClientGamePacketListener> {
   private final IntList entityIds;

   public ClientboundRemoveEntitiesPacket(IntList var1) {
      this.entityIds = new IntArrayList(var1);
   }

   public ClientboundRemoveEntitiesPacket(int... var1) {
      this.entityIds = new IntArrayList(var1);
   }

   public ClientboundRemoveEntitiesPacket(FriendlyByteBuf var1) {
      this.entityIds = var1.readIntIdList();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeIntIdList(this.entityIds);
   }

   public void handle(ClientGamePacketListener var1) {
      var1.handleRemoveEntities(this);
   }

   public IntList getEntityIds() {
      return this.entityIds;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientGamePacketListener)var1);
   }
}
