package net.minecraft.util;

import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypt {
   private static final String SYMMETRIC_ALGORITHM = "AES";
   private static final int SYMMETRIC_BITS = 128;
   private static final String ASYMMETRIC_ALGORITHM = "RSA";
   private static final int ASYMMETRIC_BITS = 1024;
   private static final String BYTE_ENCODING = "ISO_8859_1";
   private static final String HASH_ALGORITHM = "SHA-1";

   /**
    * Generate a new shared secret AES key from a secure random source
    */
   public static SecretKey generateSecretKey() throws CryptException {
      try {
         KeyGenerator keygenerator = KeyGenerator.getInstance("AES");
         keygenerator.init(128);
         return keygenerator.generateKey();
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Generates RSA KeyPair
    */
   public static KeyPair generateKeyPair() throws CryptException {
      try {
         KeyPairGenerator keypairgenerator = KeyPairGenerator.getInstance("RSA");
         keypairgenerator.initialize(1024);
         return keypairgenerator.generateKeyPair();
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Compute a serverId hash for use by sendSessionRequest()
    */
   public static byte[] digestData(String pServerId, PublicKey pPublicKey, SecretKey pSecretKey) throws CryptException {
      try {
         return digestData(pServerId.getBytes("ISO_8859_1"), pSecretKey.getEncoded(), pPublicKey.getEncoded());
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   private static byte[] digestData(byte[]... p_13603_) throws Exception {
      MessageDigest messagedigest = MessageDigest.getInstance("SHA-1");

      for(byte[] abyte : p_13603_) {
         messagedigest.update(abyte);
      }

      return messagedigest.digest();
   }

   /**
    * Create a new PublicKey from encoded X.509 data
    */
   public static PublicKey byteToPublicKey(byte[] pEncodedKey) throws CryptException {
      try {
         EncodedKeySpec encodedkeyspec = new X509EncodedKeySpec(pEncodedKey);
         KeyFactory keyfactory = KeyFactory.getInstance("RSA");
         return keyfactory.generatePublic(encodedkeyspec);
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Decrypt shared secret AES key using RSA private key
    */
   public static SecretKey decryptByteToSecretKey(PrivateKey pKey, byte[] pSecretKeyEncrypted) throws CryptException {
      byte[] abyte = decryptUsingKey(pKey, pSecretKeyEncrypted);

      try {
         return new SecretKeySpec(abyte, "AES");
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Encrypt byte[] data with RSA public key
    */
   public static byte[] encryptUsingKey(Key pKey, byte[] pData) throws CryptException {
      return cipherData(1, pKey, pData);
   }

   /**
    * Decrypt byte[] data with RSA private key
    */
   public static byte[] decryptUsingKey(Key pKey, byte[] pData) throws CryptException {
      return cipherData(2, pKey, pData);
   }

   /**
    * Encrypt or decrypt byte[] data using the specified key
    */
   private static byte[] cipherData(int pOpMode, Key pKey, byte[] pData) throws CryptException {
      try {
         return setupCipher(pOpMode, pKey.getAlgorithm(), pKey).doFinal(pData);
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }

   /**
    * Creates the Cipher Instance.
    */
   private static Cipher setupCipher(int pOpMode, String pTransformation, Key pKey) throws Exception {
      Cipher cipher = Cipher.getInstance(pTransformation);
      cipher.init(pOpMode, pKey);
      return cipher;
   }

   /**
    * Creates an Cipher instance using the AES/CFB8/NoPadding algorithm. Used for protocol encryption.
    */
   public static Cipher getCipher(int pOpMode, Key pKey) throws CryptException {
      try {
         Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
         cipher.init(pOpMode, pKey, new IvParameterSpec(pKey.getEncoded()));
         return cipher;
      } catch (Exception exception) {
         throw new CryptException(exception);
      }
   }
}