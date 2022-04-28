package net.minecraft.world.level.gameevent;

import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;

public class GameEventListenerRegistrar {
   private final GameEventListener listener;
   @Nullable
   private SectionPos sectionPos;

   public GameEventListenerRegistrar(GameEventListener pListener) {
      this.listener = pListener;
   }

   public void onListenerRemoved(Level pLevel) {
      this.ifEventDispatcherExists(pLevel, this.sectionPos, (p_157867_) -> {
         p_157867_.unregister(this.listener);
      });
   }

   public void onListenerMove(Level pLevel) {
      Optional<BlockPos> optional = this.listener.getListenerSource().getPosition(pLevel);
      if (optional.isPresent()) {
         long i = SectionPos.blockToSection(optional.get().asLong());
         if (this.sectionPos == null || this.sectionPos.asLong() != i) {
            SectionPos sectionpos = this.sectionPos;
            this.sectionPos = SectionPos.of(i);
            this.ifEventDispatcherExists(pLevel, sectionpos, (p_157865_) -> {
               p_157865_.unregister(this.listener);
            });
            this.ifEventDispatcherExists(pLevel, this.sectionPos, (p_157861_) -> {
               p_157861_.register(this.listener);
            });
         }
      }

   }

   private void ifEventDispatcherExists(Level pLevel, @Nullable SectionPos pPos, Consumer<GameEventDispatcher> pConsumer) {
      if (pPos != null) {
         ChunkAccess chunkaccess = pLevel.getChunk(pPos.x(), pPos.z(), ChunkStatus.FULL, false);
         if (chunkaccess != null) {
            pConsumer.accept(chunkaccess.getEventDispatcher(pPos.y()));
         }

      }
   }
}