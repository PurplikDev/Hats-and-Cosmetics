package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugPackets {
   private static final Logger LOGGER = LogManager.getLogger();

   public static void sendGameTestAddMarker(ServerLevel pLevel, BlockPos pPos, String pText, int pColor, int pLifetimeMillis) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(Unpooled.buffer());
      friendlybytebuf.writeBlockPos(pPos);
      friendlybytebuf.writeInt(pColor);
      friendlybytebuf.writeUtf(pText);
      friendlybytebuf.writeInt(pLifetimeMillis);
      sendPacketToAllPlayers(pLevel, friendlybytebuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_ADD_MARKER);
   }

   public static void sendGameTestClearPacket(ServerLevel pLevel) {
      FriendlyByteBuf friendlybytebuf = new FriendlyByteBuf(Unpooled.buffer());
      sendPacketToAllPlayers(pLevel, friendlybytebuf, ClientboundCustomPayloadPacket.DEBUG_GAME_TEST_CLEAR);
   }

   public static void sendPoiPacketsForChunk(ServerLevel pLevel, ChunkPos pChunkPos) {
   }

   public static void sendPoiAddedPacket(ServerLevel pLevel, BlockPos pPos) {
      sendVillageSectionsPacket(pLevel, pPos);
   }

   public static void sendPoiRemovedPacket(ServerLevel pLevel, BlockPos pPos) {
      sendVillageSectionsPacket(pLevel, pPos);
   }

   public static void sendPoiTicketCountPacket(ServerLevel pLevel, BlockPos pPos) {
      sendVillageSectionsPacket(pLevel, pPos);
   }

   private static void sendVillageSectionsPacket(ServerLevel pLevel, BlockPos pPos) {
   }

   public static void sendPathFindingPacket(Level pLevel, Mob pMob, @Nullable Path pPath, float pMaxDistanceToWaypoint) {
   }

   public static void sendNeighborsUpdatePacket(Level pLevel, BlockPos pPos) {
   }

   public static void sendStructurePacket(WorldGenLevel pLevel, StructureStart<?> pStructureStart) {
   }

   public static void sendGoalSelector(Level pLevel, Mob pMob, GoalSelector pGoalSelector) {
      if (pLevel instanceof ServerLevel) {
         ;
      }
   }

   public static void sendRaids(ServerLevel pLevel, Collection<Raid> pRaids) {
   }

   public static void sendEntityBrain(LivingEntity pLivingEntity) {
   }

   public static void sendBeeInfo(Bee pBee) {
   }

   public static void sendGameEventInfo(Level pLevel, GameEvent pGameEvent, BlockPos pPos) {
   }

   public static void sendGameEventListenerInfo(Level pLevel, GameEventListener pGameEventListener) {
   }

   public static void sendHiveInfo(Level pLevel, BlockPos pPos, BlockState pBlockState, BeehiveBlockEntity pHiveBlockEntity) {
   }

   private static void writeBrain(LivingEntity pLivingEntity, FriendlyByteBuf pBuffer) {
      Brain<?> brain = pLivingEntity.getBrain();
      long i = pLivingEntity.level.getGameTime();
      if (pLivingEntity instanceof InventoryCarrier) {
         Container container = ((InventoryCarrier)pLivingEntity).getInventory();
         pBuffer.writeUtf(container.isEmpty() ? "" : container.toString());
      } else {
         pBuffer.writeUtf("");
      }

      if (brain.hasMemoryValue(MemoryModuleType.PATH)) {
         pBuffer.writeBoolean(true);
         Path path = brain.getMemory(MemoryModuleType.PATH).get();
         path.writeToStream(pBuffer);
      } else {
         pBuffer.writeBoolean(false);
      }

      if (pLivingEntity instanceof Villager) {
         Villager villager = (Villager)pLivingEntity;
         boolean flag = villager.wantsToSpawnGolem(i);
         pBuffer.writeBoolean(flag);
      } else {
         pBuffer.writeBoolean(false);
      }

      pBuffer.writeCollection(brain.getActiveActivities(), (p_179531_, p_179532_) -> {
         p_179531_.writeUtf(p_179532_.getName());
      });
      Set<String> set = brain.getRunningBehaviors().stream().map(Behavior::toString).collect(Collectors.toSet());
      pBuffer.writeCollection(set, FriendlyByteBuf::writeUtf);
      pBuffer.writeCollection(getMemoryDescriptions(pLivingEntity, i), (p_179534_, p_179535_) -> {
         String s = StringUtil.truncateStringIfNecessary(p_179535_, 255, true);
         p_179534_.writeUtf(s);
      });
      if (pLivingEntity instanceof Villager) {
         Set<BlockPos> set1 = Stream.of(MemoryModuleType.JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT).map(brain::getMemory).flatMap(Util::toStream).map(GlobalPos::pos).collect(Collectors.toSet());
         pBuffer.writeCollection(set1, FriendlyByteBuf::writeBlockPos);
      } else {
         pBuffer.writeVarInt(0);
      }

      if (pLivingEntity instanceof Villager) {
         Set<BlockPos> set2 = Stream.of(MemoryModuleType.POTENTIAL_JOB_SITE).map(brain::getMemory).flatMap(Util::toStream).map(GlobalPos::pos).collect(Collectors.toSet());
         pBuffer.writeCollection(set2, FriendlyByteBuf::writeBlockPos);
      } else {
         pBuffer.writeVarInt(0);
      }

      if (pLivingEntity instanceof Villager) {
         Map<UUID, Object2IntMap<GossipType>> map = ((Villager)pLivingEntity).getGossips().getGossipEntries();
         List<String> list = Lists.newArrayList();
         map.forEach((p_179522_, p_179523_) -> {
            String s = DebugEntityNameGenerator.getEntityName(p_179522_);
            p_179523_.forEach((p_179518_, p_179519_) -> {
               list.add(s + ": " + p_179518_ + ": " + p_179519_);
            });
         });
         pBuffer.writeCollection(list, FriendlyByteBuf::writeUtf);
      } else {
         pBuffer.writeVarInt(0);
      }

   }

   private static List<String> getMemoryDescriptions(LivingEntity p_179496_, long p_179497_) {
      Map<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> map = p_179496_.getBrain().getMemories();
      List<String> list = Lists.newArrayList();

      for(Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : map.entrySet()) {
         MemoryModuleType<?> memorymoduletype = entry.getKey();
         Optional<? extends ExpirableValue<?>> optional = entry.getValue();
         String s;
         if (optional.isPresent()) {
            ExpirableValue<?> expirablevalue = optional.get();
            Object object = expirablevalue.getValue();
            if (memorymoduletype == MemoryModuleType.HEARD_BELL_TIME) {
               long i = p_179497_ - (Long)object;
               s = i + " ticks ago";
            } else if (expirablevalue.canExpire()) {
               s = getShortDescription((ServerLevel)p_179496_.level, object) + " (ttl: " + expirablevalue.getTimeToLive() + ")";
            } else {
               s = getShortDescription((ServerLevel)p_179496_.level, object);
            }
         } else {
            s = "-";
         }

         list.add(Registry.MEMORY_MODULE_TYPE.getKey(memorymoduletype).getPath() + ": " + s);
      }

      list.sort(String::compareTo);
      return list;
   }

   private static String getShortDescription(ServerLevel p_179493_, @Nullable Object p_179494_) {
      if (p_179494_ == null) {
         return "-";
      } else if (p_179494_ instanceof UUID) {
         return getShortDescription(p_179493_, p_179493_.getEntity((UUID)p_179494_));
      } else if (p_179494_ instanceof LivingEntity) {
         Entity entity1 = (Entity)p_179494_;
         return DebugEntityNameGenerator.getEntityName(entity1);
      } else if (p_179494_ instanceof Nameable) {
         return ((Nameable)p_179494_).getName().getString();
      } else if (p_179494_ instanceof WalkTarget) {
         return getShortDescription(p_179493_, ((WalkTarget)p_179494_).getTarget());
      } else if (p_179494_ instanceof EntityTracker) {
         return getShortDescription(p_179493_, ((EntityTracker)p_179494_).getEntity());
      } else if (p_179494_ instanceof GlobalPos) {
         return getShortDescription(p_179493_, ((GlobalPos)p_179494_).pos());
      } else if (p_179494_ instanceof BlockPosTracker) {
         return getShortDescription(p_179493_, ((BlockPosTracker)p_179494_).currentBlockPosition());
      } else if (p_179494_ instanceof EntityDamageSource) {
         Entity entity = ((EntityDamageSource)p_179494_).getEntity();
         return entity == null ? p_179494_.toString() : getShortDescription(p_179493_, entity);
      } else if (!(p_179494_ instanceof Collection)) {
         return p_179494_.toString();
      } else {
         List<String> list = Lists.newArrayList();

         for(Object object : (Iterable)p_179494_) {
            list.add(getShortDescription(p_179493_, object));
         }

         return list.toString();
      }
   }

   private static void sendPacketToAllPlayers(ServerLevel p_133692_, FriendlyByteBuf p_133693_, ResourceLocation p_133694_) {
      Packet<?> packet = new ClientboundCustomPayloadPacket(p_133694_, p_133693_);

      for(Player player : p_133692_.getLevel().players()) {
         ((ServerPlayer)player).connection.send(packet);
      }

   }
}