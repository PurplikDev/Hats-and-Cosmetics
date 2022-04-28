package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicBoolean shutdownRequested = new AtomicBoolean();
   private final ProcessorMailbox<StrictQueue.IntRunnable> mailbox;
   private final RegionFileStorage storage;
   private final Map<ChunkPos, IOWorker.PendingStore> pendingWrites = Maps.newLinkedHashMap();

   protected IOWorker(Path pFolder, boolean pSync, String pWorkerName) {
      this.storage = new RegionFileStorage(pFolder, pSync);
      this.mailbox = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(IOWorker.Priority.values().length), Util.ioPool(), "IOWorker-" + pWorkerName);
   }

   public CompletableFuture<Void> store(ChunkPos pChunkPos, @Nullable CompoundTag pChunkData) {
      return this.submitTask(() -> {
         IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.computeIfAbsent(pChunkPos, (p_156584_) -> {
            return new IOWorker.PendingStore(pChunkData);
         });
         ioworker$pendingstore.data = pChunkData;
         return Either.left(ioworker$pendingstore.result);
      }).thenCompose(Function.identity());
   }

   @Nullable
   public CompoundTag load(ChunkPos pChunkPos) throws IOException {
      CompletableFuture<CompoundTag> completablefuture = this.loadAsync(pChunkPos);

      try {
         return completablefuture.join();
      } catch (CompletionException completionexception) {
         if (completionexception.getCause() instanceof IOException) {
            throw (IOException)completionexception.getCause();
         } else {
            throw completionexception;
         }
      }
   }

   protected CompletableFuture<CompoundTag> loadAsync(ChunkPos pChunkPos) {
      return this.submitTask(() -> {
         IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(pChunkPos);
         if (ioworker$pendingstore != null) {
            return Either.left(ioworker$pendingstore.data);
         } else {
            try {
               CompoundTag compoundtag = this.storage.read(pChunkPos);
               return Either.left(compoundtag);
            } catch (Exception exception) {
               LOGGER.warn("Failed to read chunk {}", pChunkPos, exception);
               return Either.right(exception);
            }
         }
      });
   }

   public CompletableFuture<Void> synchronize(boolean pFlushStorage) {
      CompletableFuture<Void> completablefuture = this.submitTask(() -> {
         return Either.left(CompletableFuture.allOf(this.pendingWrites.values().stream().map((p_156581_) -> {
            return p_156581_.result;
         }).toArray((p_156576_) -> {
            return new CompletableFuture[p_156576_];
         })));
      }).thenCompose(Function.identity());
      return pFlushStorage ? completablefuture.thenCompose((p_63544_) -> {
         return this.submitTask(() -> {
            try {
               this.storage.flush();
               return Either.left((Void)null);
            } catch (Exception exception) {
               LOGGER.warn("Failed to synchronize chunks", (Throwable)exception);
               return Either.right(exception);
            }
         });
      }) : completablefuture.thenCompose((p_182494_) -> {
         return this.submitTask(() -> {
            return Either.left((Void)null);
         });
      });
   }

   public CompletableFuture<Void> scanChunk(ChunkPos pChunkPos, StreamTagVisitor pVisitor) {
      return this.submitTask(() -> {
         try {
            IOWorker.PendingStore ioworker$pendingstore = this.pendingWrites.get(pChunkPos);
            if (ioworker$pendingstore != null) {
               if (ioworker$pendingstore.data != null) {
                  ioworker$pendingstore.data.acceptAsRoot(pVisitor);
               }
            } else {
               this.storage.scanChunk(pChunkPos, pVisitor);
            }

            return Either.left((Void)null);
         } catch (Exception exception) {
            LOGGER.warn("Failed to bulk scan chunk {}", pChunkPos, exception);
            return Either.right(exception);
         }
      });
   }

   private <T> CompletableFuture<T> submitTask(Supplier<Either<T, Exception>> pTask) {
      return this.mailbox.askEither((p_196943_) -> {
         return new StrictQueue.IntRunnable(IOWorker.Priority.FOREGROUND.ordinal(), () -> {
            if (!this.shutdownRequested.get()) {
               p_196943_.tell(pTask.get());
            }

            this.tellStorePending();
         });
      });
   }

   private void storePendingChunk() {
      if (!this.pendingWrites.isEmpty()) {
         Iterator<Entry<ChunkPos, IOWorker.PendingStore>> iterator = this.pendingWrites.entrySet().iterator();
         Entry<ChunkPos, IOWorker.PendingStore> entry = iterator.next();
         iterator.remove();
         this.runStore(entry.getKey(), entry.getValue());
         this.tellStorePending();
      }
   }

   private void tellStorePending() {
      this.mailbox.tell(new StrictQueue.IntRunnable(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
   }

   private void runStore(ChunkPos pChunkPos, IOWorker.PendingStore pPendingStore) {
      try {
         this.storage.write(pChunkPos, pPendingStore.data);
         pPendingStore.result.complete((Void)null);
      } catch (Exception exception) {
         LOGGER.error("Failed to store chunk {}", pChunkPos, exception);
         pPendingStore.result.completeExceptionally(exception);
      }

   }

   public void close() throws IOException {
      if (this.shutdownRequested.compareAndSet(false, true)) {
         this.mailbox.ask((p_196934_) -> {
            return new StrictQueue.IntRunnable(IOWorker.Priority.SHUTDOWN.ordinal(), () -> {
               p_196934_.tell(Unit.INSTANCE);
            });
         }).join();
         this.mailbox.close();

         try {
            this.storage.close();
         } catch (Exception exception) {
            LOGGER.error("Failed to close storage", (Throwable)exception);
         }

      }
   }

   static class PendingStore {
      @Nullable
      CompoundTag data;
      final CompletableFuture<Void> result = new CompletableFuture<>();

      public PendingStore(@Nullable CompoundTag pData) {
         this.data = pData;
      }
   }

   static enum Priority {
      FOREGROUND,
      BACKGROUND,
      SHUTDOWN;
   }
}