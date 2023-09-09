package net.minecraft.network.protocol.login;

import java.security.PublicKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ClientboundHelloPacket implements Packet<ClientLoginPacketListener> {
   private final String serverId;
   private final byte[] publicKey;
   private final byte[] challenge;

   public ClientboundHelloPacket(String var1, byte[] var2, byte[] var3) {
      this.serverId = var1;
      this.publicKey = var2;
      this.challenge = var3;
   }

   public ClientboundHelloPacket(FriendlyByteBuf var1) {
      this.serverId = var1.readUtf(20);
      this.publicKey = var1.readByteArray();
      this.challenge = var1.readByteArray();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeUtf(this.serverId);
      var1.writeByteArray(this.publicKey);
      var1.writeByteArray(this.challenge);
   }

   public void handle(ClientLoginPacketListener var1) {
      var1.handleHello(this);
   }

   public String getServerId() {
      return this.serverId;
   }

   public PublicKey getPublicKey() throws CryptException {
      return Crypt.byteToPublicKey(this.publicKey);
   }

   public byte[] getChallenge() {
      return this.challenge;
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ClientLoginPacketListener)var1);
   }
}
