package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.Util;

public final class SerializableUUID {
   public static final Codec<UUID> CODEC = Codec.INT_STREAM.comapFlatMap((p_123280_) -> {
      return Util.fixedSize(p_123280_, 4).map(SerializableUUID::uuidFromIntArray);
   }, (p_123284_) -> {
      return Arrays.stream(uuidToIntArray(p_123284_));
   });

   private SerializableUUID() {
   }

   public static UUID uuidFromIntArray(int[] p_123282_) {
      return new UUID((long)p_123282_[0] << 32 | (long)p_123282_[1] & 4294967295L, (long)p_123282_[2] << 32 | (long)p_123282_[3] & 4294967295L);
   }

   public static int[] uuidToIntArray(UUID pUuid) {
      long i = pUuid.getMostSignificantBits();
      long j = pUuid.getLeastSignificantBits();
      return leastMostToIntArray(i, j);
   }

   private static int[] leastMostToIntArray(long pMost, long pLeast) {
      return new int[]{(int)(pMost >> 32), (int)pMost, (int)(pLeast >> 32), (int)pLeast};
   }

   public static UUID readUUID(Dynamic<?> pDynamic) {
      int[] aint = pDynamic.asIntStream().toArray();
      if (aint.length != 4) {
         throw new IllegalArgumentException("Could not read UUID. Expected int-array of length 4, got " + aint.length + ".");
      } else {
         return uuidFromIntArray(aint);
      }
   }
}