package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;

public class ClientboundAddVibrationSignalPacket implements Packet<ClientGamePacketListener> {
   private final VibrationPath vibrationPath;

   public ClientboundAddVibrationSignalPacket(VibrationPath pVibrationPath) {
      this.vibrationPath = pVibrationPath;
   }

   public ClientboundAddVibrationSignalPacket(FriendlyByteBuf pBuffer) {
      this.vibrationPath = VibrationPath.read(pBuffer);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      VibrationPath.write(pBuffer, this.vibrationPath);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleAddVibrationSignal(this);
   }

   public VibrationPath getVibrationPath() {
      return this.vibrationPath;
   }
}