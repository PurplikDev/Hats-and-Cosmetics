package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ProcessorChunkProgressListener implements ChunkProgressListener {
   private final ChunkProgressListener delegate;
   private final ProcessorMailbox<Runnable> mailbox;

   private ProcessorChunkProgressListener(ChunkProgressListener p_9640_, Executor p_9641_) {
      this.delegate = p_9640_;
      this.mailbox = ProcessorMailbox.create(p_9641_, "progressListener");
   }

   public static ProcessorChunkProgressListener createStarted(ChunkProgressListener p_143584_, Executor p_143585_) {
      ProcessorChunkProgressListener processorchunkprogresslistener = new ProcessorChunkProgressListener(p_143584_, p_143585_);
      processorchunkprogresslistener.start();
      return processorchunkprogresslistener;
   }

   public void updateSpawnPos(ChunkPos pCenter) {
      this.mailbox.tell(() -> {
         this.delegate.updateSpawnPos(pCenter);
      });
   }

   public void onStatusChange(ChunkPos pChunkPosition, @Nullable ChunkStatus pNewStatus) {
      this.mailbox.tell(() -> {
         this.delegate.onStatusChange(pChunkPosition, pNewStatus);
      });
   }

   public void start() {
      this.mailbox.tell(this.delegate::start);
   }

   public void stop() {
      this.mailbox.tell(this.delegate::stop);
   }
}