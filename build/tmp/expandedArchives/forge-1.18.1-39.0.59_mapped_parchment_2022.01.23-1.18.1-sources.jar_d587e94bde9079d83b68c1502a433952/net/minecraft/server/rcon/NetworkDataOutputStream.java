package net.minecraft.server.rcon;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetworkDataOutputStream {
   private final ByteArrayOutputStream outputStream;
   private final DataOutputStream dataOutputStream;

   public NetworkDataOutputStream(int p_11470_) {
      this.outputStream = new ByteArrayOutputStream(p_11470_);
      this.dataOutputStream = new DataOutputStream(this.outputStream);
   }

   /**
    * Writes the given byte array to the output stream
    */
   public void writeBytes(byte[] pData) throws IOException {
      this.dataOutputStream.write(pData, 0, pData.length);
   }

   /**
    * Writes the given String to the output stream
    */
   public void writeString(String pData) throws IOException {
      this.dataOutputStream.writeBytes(pData);
      this.dataOutputStream.write(0);
   }

   /**
    * Writes the given int to the output stream
    */
   public void write(int pData) throws IOException {
      this.dataOutputStream.write(pData);
   }

   /**
    * Writes the given short to the output stream
    */
   public void writeShort(short pData) throws IOException {
      this.dataOutputStream.writeShort(Short.reverseBytes(pData));
   }

   public void writeInt(int p_144019_) throws IOException {
      this.dataOutputStream.writeInt(Integer.reverseBytes(p_144019_));
   }

   public void writeFloat(float p_144017_) throws IOException {
      this.dataOutputStream.writeInt(Integer.reverseBytes(Float.floatToIntBits(p_144017_)));
   }

   /**
    * Returns the contents of the output stream as a byte array
    */
   public byte[] toByteArray() {
      return this.outputStream.toByteArray();
   }

   /**
    * Resets the byte array output.
    */
   public void reset() {
      this.outputStream.reset();
   }
}