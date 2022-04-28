package net.minecraft.world.level.gameevent;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EuclideanGameEventDispatcher implements GameEventDispatcher {
   private final List<GameEventListener> listeners = Lists.newArrayList();
   private final Level level;

   public EuclideanGameEventDispatcher(Level pLevel) {
      this.level = pLevel;
   }

   public boolean isEmpty() {
      return this.listeners.isEmpty();
   }

   public void register(GameEventListener pListener) {
      this.listeners.add(pListener);
      DebugPackets.sendGameEventListenerInfo(this.level, pListener);
   }

   public void unregister(GameEventListener pListener) {
      this.listeners.remove(pListener);
   }

   public void post(GameEvent pEvent, @Nullable Entity pEntity, BlockPos pPos) {
      boolean flag = false;

      for(GameEventListener gameeventlistener : this.listeners) {
         if (this.postToListener(this.level, pEvent, pEntity, pPos, gameeventlistener)) {
            flag = true;
         }
      }

      if (flag) {
         DebugPackets.sendGameEventInfo(this.level, pEvent, pPos);
      }

   }

   private boolean postToListener(Level pLevel, GameEvent pEvent, @Nullable Entity pEntity, BlockPos pPos, GameEventListener pListener) {
      Optional<BlockPos> optional = pListener.getListenerSource().getPosition(pLevel);
      if (!optional.isPresent()) {
         return false;
      } else {
         double d0 = optional.get().distSqr(pPos, false);
         int i = pListener.getListenerRadius() * pListener.getListenerRadius();
         return d0 <= (double)i && pListener.handleGameEvent(pLevel, pEvent, pEntity, pPos);
      }
   }
}