package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

public class ServerboundCommandSuggestionPacket implements Packet<ServerGamePacketListener> {
   private final int id;
   private final String command;

   public ServerboundCommandSuggestionPacket(int var1, String var2) {
      this.id = var1;
      this.command = var2;
   }

   public ServerboundCommandSuggestionPacket(FriendlyByteBuf var1) {
      this.id = var1.readVarInt();
      this.command = var1.readUtf(32500);
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeVarInt(this.id);
      var1.writeUtf(this.command, 32500);
   }

   public void handle(ServerGamePacketListener var1) {
      var1.handleCustomCommandSuggestions(this);
   }

   public int getId() {
      return this.id;
   }

   public String getCommand() {
      return this.command;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerGamePacketListener)var1);
   }
}
