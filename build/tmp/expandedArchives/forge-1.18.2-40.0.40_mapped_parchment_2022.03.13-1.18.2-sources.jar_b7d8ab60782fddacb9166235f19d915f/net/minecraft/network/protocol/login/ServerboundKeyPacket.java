package net.minecraft.network.protocol.login;

import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;

public class ServerboundKeyPacket implements Packet<ServerLoginPacketListener> {
   private final byte[] keybytes;
   private final byte[] nonce;

   public ServerboundKeyPacket(SecretKey pSecretKey, PublicKey pPublicKey, byte[] pNonce) throws CryptException {
      this.keybytes = Crypt.encryptUsingKey(pPublicKey, pSecretKey.getEncoded());
      this.nonce = Crypt.encryptUsingKey(pPublicKey, pNonce);
   }

   public ServerboundKeyPacket(FriendlyByteBuf p_179829_) {
      this.keybytes = p_179829_.readByteArray();
      this.nonce = p_179829_.readByteArray();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeByteArray(this.keybytes);
      pBuffer.writeByteArray(this.nonce);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleKey(this);
   }

   public SecretKey getSecretKey(PrivateKey pKey) throws CryptException {
      return Crypt.decryptByteToSecretKey(pKey, this.keybytes);
   }

   public byte[] getNonce(PrivateKey pKey) throws CryptException {
      return Crypt.decryptUsingKey(pKey, this.nonce);
   }
}