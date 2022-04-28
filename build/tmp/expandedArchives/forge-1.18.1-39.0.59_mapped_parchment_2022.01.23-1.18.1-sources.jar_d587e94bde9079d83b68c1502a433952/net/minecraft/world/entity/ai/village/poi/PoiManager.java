package net.minecraft.world.entity.ai.village.poi;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.SectionTracker;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.SectionStorage;

public class PoiManager extends SectionStorage<PoiSection> {
   public static final int MAX_VILLAGE_DISTANCE = 6;
   public static final int VILLAGE_SECTION_SIZE = 1;
   private final PoiManager.DistanceTracker distanceTracker;
   private final LongSet loadedChunks = new LongOpenHashSet();

   public PoiManager(Path p_196651_, DataFixer p_196652_, boolean p_196653_, LevelHeightAccessor p_196654_) {
      super(p_196651_, PoiSection::codec, PoiSection::new, p_196652_, DataFixTypes.POI_CHUNK, p_196653_, p_196654_);
      this.distanceTracker = new PoiManager.DistanceTracker();
   }

   public void add(BlockPos pPos, PoiType pPoiType) {
      this.getOrCreate(SectionPos.asLong(pPos)).add(pPos, pPoiType);
   }

   public void remove(BlockPos pPos) {
      this.getOrLoad(SectionPos.asLong(pPos)).ifPresent((p_148657_) -> {
         p_148657_.remove(pPos);
      });
   }

   public long getCountInRange(Predicate<PoiType> p_27122_, BlockPos p_27123_, int p_27124_, PoiManager.Occupancy p_27125_) {
      return this.getInRange(p_27122_, p_27123_, p_27124_, p_27125_).count();
   }

   public boolean existsAtPosition(PoiType pType, BlockPos pPos) {
      return this.exists(pPos, pType::equals);
   }

   public Stream<PoiRecord> getInSquare(Predicate<PoiType> pTypePredicate, BlockPos pPos, int pDistance, PoiManager.Occupancy pStatus) {
      int i = Math.floorDiv(pDistance, 16) + 1;
      return ChunkPos.rangeClosed(new ChunkPos(pPos), i).flatMap((p_148616_) -> {
         return this.getInChunk(pTypePredicate, p_148616_, pStatus);
      }).filter((p_148635_) -> {
         BlockPos blockpos = p_148635_.getPos();
         return Math.abs(blockpos.getX() - pPos.getX()) <= pDistance && Math.abs(blockpos.getZ() - pPos.getZ()) <= pDistance;
      });
   }

   public Stream<PoiRecord> getInRange(Predicate<PoiType> p_27182_, BlockPos p_27183_, int p_27184_, PoiManager.Occupancy p_27185_) {
      int i = p_27184_ * p_27184_;
      return this.getInSquare(p_27182_, p_27183_, p_27184_, p_27185_).filter((p_148598_) -> {
         return p_148598_.getPos().distSqr(p_27183_) <= (double)i;
      });
   }

   @VisibleForDebug
   public Stream<PoiRecord> getInChunk(Predicate<PoiType> p_27118_, ChunkPos p_27119_, PoiManager.Occupancy p_27120_) {
      return IntStream.range(this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).boxed().map((p_148578_) -> {
         return this.getOrLoad(SectionPos.of(p_27119_, p_148578_).asLong());
      }).filter(Optional::isPresent).flatMap((p_148620_) -> {
         return p_148620_.get().getRecords(p_27118_, p_27120_);
      });
   }

   public Stream<BlockPos> findAll(Predicate<PoiType> pTypePredicate, Predicate<BlockPos> pPosPredicate, BlockPos pPos, int pDistance, PoiManager.Occupancy pStatus) {
      return this.getInRange(pTypePredicate, pPos, pDistance, pStatus).map(PoiRecord::getPos).filter(pPosPredicate);
   }

   public Stream<BlockPos> findAllClosestFirst(Predicate<PoiType> p_27172_, Predicate<BlockPos> p_27173_, BlockPos p_27174_, int p_27175_, PoiManager.Occupancy p_27176_) {
      return this.findAll(p_27172_, p_27173_, p_27174_, p_27175_, p_27176_).sorted(Comparator.comparingDouble((p_148652_) -> {
         return p_148652_.distSqr(p_27174_);
      }));
   }

   public Optional<BlockPos> find(Predicate<PoiType> pTypePredicate, Predicate<BlockPos> pPosPredicate, BlockPos pPos, int pDistance, PoiManager.Occupancy pStatus) {
      return this.findAll(pTypePredicate, pPosPredicate, pPos, pDistance, pStatus).findFirst();
   }

   public Optional<BlockPos> findClosest(Predicate<PoiType> p_27193_, BlockPos p_27194_, int p_27195_, PoiManager.Occupancy p_27196_) {
      return this.getInRange(p_27193_, p_27194_, p_27195_, p_27196_).map(PoiRecord::getPos).min(Comparator.comparingDouble((p_148641_) -> {
         return p_148641_.distSqr(p_27194_);
      }));
   }

   public Optional<BlockPos> findClosest(Predicate<PoiType> p_148659_, Predicate<BlockPos> p_148660_, BlockPos p_148661_, int p_148662_, PoiManager.Occupancy p_148663_) {
      return this.getInRange(p_148659_, p_148661_, p_148662_, p_148663_).map(PoiRecord::getPos).filter(p_148660_).min(Comparator.comparingDouble((p_148604_) -> {
         return p_148604_.distSqr(p_148661_);
      }));
   }

   public Optional<BlockPos> take(Predicate<PoiType> pTypePredicate, Predicate<BlockPos> pPosPredicate, BlockPos pPos, int pDistance) {
      return this.getInRange(pTypePredicate, pPos, pDistance, PoiManager.Occupancy.HAS_SPACE).filter((p_148646_) -> {
         return pPosPredicate.test(p_148646_.getPos());
      }).findFirst().map((p_148573_) -> {
         p_148573_.acquireTicket();
         return p_148573_.getPos();
      });
   }

   public Optional<BlockPos> getRandom(Predicate<PoiType> pTypePredicate, Predicate<BlockPos> pPosPredicate, PoiManager.Occupancy pStatus, BlockPos pPos, int pDistance, Random pRand) {
      List<PoiRecord> list = this.getInRange(pTypePredicate, pPos, pDistance, pStatus).collect(Collectors.toList());
      Collections.shuffle(list, pRand);
      return list.stream().filter((p_148623_) -> {
         return pPosPredicate.test(p_148623_.getPos());
      }).findFirst().map(PoiRecord::getPos);
   }

   public boolean release(BlockPos pPos) {
      return this.getOrLoad(SectionPos.asLong(pPos)).map((p_148649_) -> {
         return p_148649_.release(pPos);
      }).orElseThrow(() -> {
         return Util.pauseInIde(new IllegalStateException("POI never registered at " + pPos));
      });
   }

   public boolean exists(BlockPos p_27092_, Predicate<PoiType> p_27093_) {
      return this.getOrLoad(SectionPos.asLong(p_27092_)).map((p_148608_) -> {
         return p_148608_.exists(p_27092_, p_27093_);
      }).orElse(false);
   }

   public Optional<PoiType> getType(BlockPos pPos) {
      return this.getOrLoad(SectionPos.asLong(pPos)).flatMap((p_148638_) -> {
         return p_148638_.getType(pPos);
      });
   }

   /** @deprecated */
   @Deprecated
   @VisibleForDebug
   public int getFreeTickets(BlockPos p_148654_) {
      return this.getOrLoad(SectionPos.asLong(p_148654_)).map((p_148601_) -> {
         return p_148601_.getFreeTickets(p_148654_);
      }).orElse(0);
   }

   public int sectionsToVillage(SectionPos pSectionPos) {
      this.distanceTracker.runAllUpdates();
      return this.distanceTracker.getLevel(pSectionPos.asLong());
   }

   boolean isVillageCenter(long p_27198_) {
      Optional<PoiSection> optional = this.get(p_27198_);
      return optional == null ? false : optional.map((p_148575_) -> {
         return p_148575_.getRecords(PoiType.ALL, PoiManager.Occupancy.IS_OCCUPIED).count() > 0L;
      }).orElse(false);
   }

   public void tick(BooleanSupplier p_27105_) {
      super.tick(p_27105_);
      this.distanceTracker.runAllUpdates();
   }

   protected void setDirty(long pSectionPos) {
      super.setDirty(pSectionPos);
      this.distanceTracker.update(pSectionPos, this.distanceTracker.getLevelFromSource(pSectionPos), false);
   }

   protected void onSectionLoad(long p_27145_) {
      this.distanceTracker.update(p_27145_, this.distanceTracker.getLevelFromSource(p_27145_), false);
   }

   public void checkConsistencyWithBlocks(ChunkPos pPos, LevelChunkSection pSection) {
      SectionPos sectionpos = SectionPos.of(pPos, SectionPos.blockToSectionCoord(pSection.bottomBlockY()));
      Util.ifElse(this.getOrLoad(sectionpos.asLong()), (p_148588_) -> {
         p_148588_.refresh((p_148629_) -> {
            if (mayHavePoi(pSection)) {
               this.updateFromSection(pSection, sectionpos, p_148629_);
            }

         });
      }, () -> {
         if (mayHavePoi(pSection)) {
            PoiSection poisection = this.getOrCreate(sectionpos.asLong());
            this.updateFromSection(pSection, sectionpos, poisection::add);
         }

      });
   }

   private static boolean mayHavePoi(LevelChunkSection pSection) {
      return pSection.maybeHas(PoiType.ALL_STATES::contains);
   }

   private void updateFromSection(LevelChunkSection pSection, SectionPos pSectionPos, BiConsumer<BlockPos, PoiType> pPosToTypeConsumer) {
      pSectionPos.blocksInside().forEach((p_148592_) -> {
         BlockState blockstate = pSection.getBlockState(SectionPos.sectionRelative(p_148592_.getX()), SectionPos.sectionRelative(p_148592_.getY()), SectionPos.sectionRelative(p_148592_.getZ()));
         PoiType.forState(blockstate).ifPresent((p_148612_) -> {
            pPosToTypeConsumer.accept(p_148592_, p_148612_);
         });
      });
   }

   public void ensureLoadedAndValid(LevelReader pLevelReader, BlockPos pPos, int pCoordinateOffset) {
      SectionPos.aroundChunk(new ChunkPos(pPos), Math.floorDiv(pCoordinateOffset, 16), this.levelHeightAccessor.getMinSection(), this.levelHeightAccessor.getMaxSection()).map((p_148643_) -> {
         return Pair.of(p_148643_, this.getOrLoad(p_148643_.asLong()));
      }).filter((p_148631_) -> {
         return !p_148631_.getSecond().map(PoiSection::isValid).orElse(false);
      }).map((p_148594_) -> {
         return p_148594_.getFirst().chunk();
      }).filter((p_148625_) -> {
         return this.loadedChunks.add(p_148625_.toLong());
      }).forEach((p_148581_) -> {
         pLevelReader.getChunk(p_148581_.x, p_148581_.z, ChunkStatus.EMPTY);
      });
   }

   final class DistanceTracker extends SectionTracker {
      private final Long2ByteMap levels = new Long2ByteOpenHashMap();

      protected DistanceTracker() {
         super(7, 16, 256);
         this.levels.defaultReturnValue((byte)7);
      }

      protected int getLevelFromSource(long pPos) {
         return PoiManager.this.isVillageCenter(pPos) ? 0 : 7;
      }

      protected int getLevel(long pSectionPos) {
         return this.levels.get(pSectionPos);
      }

      protected void setLevel(long pSectionPos, int pLevel) {
         if (pLevel > 6) {
            this.levels.remove(pSectionPos);
         } else {
            this.levels.put(pSectionPos, (byte)pLevel);
         }

      }

      public void runAllUpdates() {
         super.runUpdates(Integer.MAX_VALUE);
      }
   }

   public static enum Occupancy {
      HAS_SPACE(PoiRecord::hasSpace),
      IS_OCCUPIED(PoiRecord::isOccupied),
      ANY((p_27223_) -> {
         return true;
      });

      private final Predicate<? super PoiRecord> test;

      private Occupancy(Predicate<? super PoiRecord> p_27220_) {
         this.test = p_27220_;
      }

      public Predicate<? super PoiRecord> getTest() {
         return this.test;
      }
   }
}