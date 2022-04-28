package net.minecraft.network.protocol.game;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;

public class ClientboundAwardStatsPacket implements Packet<ClientGamePacketListener> {
   private final Object2IntMap<Stat<?>> stats;

   public ClientboundAwardStatsPacket(Object2IntMap<Stat<?>> pStats) {
      this.stats = pStats;
   }

   public ClientboundAwardStatsPacket(FriendlyByteBuf pBuffer) {
      this.stats = pBuffer.readMap(Object2IntOpenHashMap::new, (p_178602_) -> {
         int i = p_178602_.readVarInt();
         int j = p_178602_.readVarInt();
         return readStatCap(Registry.STAT_TYPE.byId(i), j);
      }, FriendlyByteBuf::readVarInt);
   }

   private static <T> Stat<T> readStatCap(StatType<T> p_178596_, int p_178597_) {
      return p_178596_.get(p_178596_.getRegistry().byId(p_178597_));
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleAwardStats(this);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeMap(this.stats, (p_178599_, p_178600_) -> {
         p_178599_.writeVarInt(Registry.STAT_TYPE.getId(p_178600_.getType()));
         p_178599_.writeVarInt(this.getStatIdCap(p_178600_));
      }, FriendlyByteBuf::writeVarInt);
   }

   private <T> int getStatIdCap(Stat<T> pStat) {
      return pStat.getType().getRegistry().getId(pStat.getValue());
   }

   public Map<Stat<?>, Integer> getStats() {
      return this.stats;
   }
}