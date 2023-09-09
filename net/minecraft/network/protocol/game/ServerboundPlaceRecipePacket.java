package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

public class ServerboundPlaceRecipePacket implements Packet<ServerGamePacketListener> {
   private final int containerId;
   private final ResourceLocation recipe;
   private final boolean shiftDown;

   public ServerboundPlaceRecipePacket(int var1, Recipe<?> var2, boolean var3) {
      this.containerId = var1;
      this.recipe = var2.getId();
      this.shiftDown = var3;
   }

   public ServerboundPlaceRecipePacket(FriendlyByteBuf var1) {
      this.containerId = var1.readByte();
      this.recipe = var1.readResourceLocation();
      this.shiftDown = var1.readBoolean();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByte(this.containerId);
      var1.writeResourceLocation(this.recipe);
      var1.writeBoolean(this.shiftDown);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handlePlaceRecipe(this);
   }

   public int getContainerId() {
      return this.containerId;
   }

   public ResourceLocation getRecipe() {
      return this.recipe;
   }

   public boolean isShiftDown() {
      return this.shiftDown;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
