package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   private final Map<Integer, Raid> raidMap = Maps.newHashMap();
   private final ServerLevel level;
   private int nextAvailableID;
   private int tick;

   public Raids(ServerLevel p_37956_) {
      this.level = p_37956_;
      this.nextAvailableID = 1;
      this.setDirty();
   }

   public Raid get(int pId) {
      return this.raidMap.get(pId);
   }

   public void tick() {
      ++this.tick;
      Iterator<Raid> iterator = this.raidMap.values().iterator();

      while(iterator.hasNext()) {
         Raid raid = iterator.next();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            raid.stop();
         }

         if (raid.isStopped()) {
            iterator.remove();
            this.setDirty();
         } else {
            raid.tick();
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }

      DebugPackets.sendRaids(this.level, this.raidMap.values());
   }

   public static boolean canJoinRaid(Raider pRaider, Raid pRaid) {
      if (pRaider != null && pRaid != null && pRaid.getLevel() != null) {
         return pRaider.isAlive() && pRaider.canJoinRaid() && pRaider.getNoActionTime() <= 2400 && pRaider.level.dimensionType() == pRaid.getLevel().dimensionType();
      } else {
         return false;
      }
   }

   @Nullable
   public Raid createOrExtendRaid(ServerPlayer pPlayer) {
      if (pPlayer.isSpectator()) {
         return null;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
         return null;
      } else {
         DimensionType dimensiontype = pPlayer.level.dimensionType();
         if (!dimensiontype.hasRaids()) {
            return null;
         } else {
            BlockPos blockpos = pPlayer.blockPosition();
            List<PoiRecord> list = this.level.getPoiManager().getInRange(PoiType.ALL, blockpos, 64, PoiManager.Occupancy.IS_OCCUPIED).collect(Collectors.toList());
            int i = 0;
            Vec3 vec3 = Vec3.ZERO;

            for(PoiRecord poirecord : list) {
               BlockPos blockpos2 = poirecord.getPos();
               vec3 = vec3.add((double)blockpos2.getX(), (double)blockpos2.getY(), (double)blockpos2.getZ());
               ++i;
            }

            BlockPos blockpos1;
            if (i > 0) {
               vec3 = vec3.scale(1.0D / (double)i);
               blockpos1 = new BlockPos(vec3);
            } else {
               blockpos1 = blockpos;
            }

            Raid raid = this.getOrCreateRaid(pPlayer.getLevel(), blockpos1);
            boolean flag = false;
            if (!raid.isStarted()) {
               if (!this.raidMap.containsKey(raid.getId())) {
                  this.raidMap.put(raid.getId(), raid);
               }

               flag = true;
            } else if (raid.getBadOmenLevel() < raid.getMaxBadOmenLevel()) {
               flag = true;
            } else {
               pPlayer.removeEffect(MobEffects.BAD_OMEN);
               pPlayer.connection.send(new ClientboundEntityEventPacket(pPlayer, (byte)43));
            }

            if (flag) {
               raid.absorbBadOmen(pPlayer);
               pPlayer.connection.send(new ClientboundEntityEventPacket(pPlayer, (byte)43));
               if (!raid.hasFirstWaveSpawned()) {
                  pPlayer.awardStat(Stats.RAID_TRIGGER);
                  CriteriaTriggers.BAD_OMEN.trigger(pPlayer);
               }
            }

            this.setDirty();
            return raid;
         }
      }
   }

   private Raid getOrCreateRaid(ServerLevel pLevel, BlockPos pPos) {
      Raid raid = pLevel.getRaidAt(pPos);
      return raid != null ? raid : new Raid(this.getUniqueId(), pLevel, pPos);
   }

   public static Raids load(ServerLevel p_150236_, CompoundTag p_150237_) {
      Raids raids = new Raids(p_150236_);
      raids.nextAvailableID = p_150237_.getInt("NextAvailableID");
      raids.tick = p_150237_.getInt("Tick");
      ListTag listtag = p_150237_.getList("Raids", 10);

      for(int i = 0; i < listtag.size(); ++i) {
         CompoundTag compoundtag = listtag.getCompound(i);
         Raid raid = new Raid(p_150236_, compoundtag);
         raids.raidMap.put(raid.getId(), raid);
      }

      return raids;
   }

   /**
    * Used to save the {@code SavedData} to a {@code CompoundTag}
    * @param pCompoundTag the {@code CompoundTag} to save the {@code SavedData} to
    */
   public CompoundTag save(CompoundTag pCompound) {
      pCompound.putInt("NextAvailableID", this.nextAvailableID);
      pCompound.putInt("Tick", this.tick);
      ListTag listtag = new ListTag();

      for(Raid raid : this.raidMap.values()) {
         CompoundTag compoundtag = new CompoundTag();
         raid.save(compoundtag);
         listtag.add(compoundtag);
      }

      pCompound.put("Raids", listtag);
      return pCompound;
   }

   public static String getFileId(DimensionType p_37969_) {
      return "raids" + p_37969_.getFileSuffix();
   }

   private int getUniqueId() {
      return ++this.nextAvailableID;
   }

   @Nullable
   public Raid getNearbyRaid(BlockPos pPos, int pDistance) {
      Raid raid = null;
      double d0 = (double)pDistance;

      for(Raid raid1 : this.raidMap.values()) {
         double d1 = raid1.getCenter().distSqr(pPos);
         if (raid1.isActive() && d1 < d0) {
            raid = raid1;
            d0 = d1;
         }
      }

      return raid;
   }
}