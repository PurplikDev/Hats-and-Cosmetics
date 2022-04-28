package net.minecraft.world.level.gameevent;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public interface GameEventListener {
   /**
    * Gets the position of the listener itself.
    */
   PositionSource getListenerSource();

   /**
    * Gets the listening radius of the listener. Events within this radius will notify the listener when broadcasted.
    */
   int getListenerRadius();

   /**
    * Called when a game event within range of the listener has been broadcasted.
    * @param pLevel The level where the event was broadcasted.
    * @param pEvent The event being detected.
    * @param pEntity The entity that caused the event to happen.
    * @param pPos The originating position of the event.
    */
   boolean handleGameEvent(Level pLevel, GameEvent pEvent, @Nullable Entity pEntity, BlockPos pPos);
}