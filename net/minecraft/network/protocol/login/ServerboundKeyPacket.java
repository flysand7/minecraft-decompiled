package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
   private final byte[] keybytes;
   private final byte[] encryptedChallenge;

   public ServerboundKeyPacket(SecretKey var1, PublicKey var2, byte[] var3) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(var2, var1.getEncoded());
      this.encryptedChallenge = Crypt.encryptUsingKey(var2, var3);
   }

   public ServerboundKeyPacket(FriendlyByteBuf var1) {
      this.keybytes = var1.readByteArray();
      this.encryptedChallenge = var1.readByteArray();
   }

   public void write(FriendlyByteBuf var1) {
      var1.writeByteArray(this.keybytes);
      var1.writeByteArray(this.encryptedChallenge);
   }

   public void handle(ServerLoginPacketListener var1) {
      var1.handleKey(this);
   }

   public SecretKey getSecretKey(PrivateKey var1) throws CryptException {
      return Crypt.decryptByteToSecretKey(var1, this.keybytes);
   }

   public boolean isChallengeValid(byte[] var1, PrivateKey var2) {
      try {
         return Arrays.equals(var1, Crypt.decryptUsingKey(var2, this.encryptedChallenge));
      } catch (CryptException var4) {
         return false;
      }
   }

   // $FF: synthetic method
   // $FF: bridge method
   public void handle(PacketListener var1) {
      this.handle((ServerLoginPacketListener)var1);
   }
}
