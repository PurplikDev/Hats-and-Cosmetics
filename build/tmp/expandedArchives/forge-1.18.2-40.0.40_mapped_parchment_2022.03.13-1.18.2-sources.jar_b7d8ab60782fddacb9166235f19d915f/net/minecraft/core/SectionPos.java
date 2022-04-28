package net.minecraft.core;

import it.unimi.dsi.fastutil.longs.LongConsumer;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public class SectionPos extends Vec3i {
   public static final int SECTION_BITS = 4;
   public static final int SECTION_SIZE = 16;
   public static final int SECTION_MASK = 15;
   public static final int SECTION_HALF_SIZE = 8;
   public static final int SECTION_MAX_INDEX = 15;
   private static final int PACKED_X_LENGTH = 22;
   private static final int PACKED_Y_LENGTH = 20;
   private static final int PACKED_Z_LENGTH = 22;
   private static final long PACKED_X_MASK = 4194303L;
   private static final long PACKED_Y_MASK = 1048575L;
   private static final long PACKED_Z_MASK = 4194303L;
   private static final int Y_OFFSET = 0;
   private static final int Z_OFFSET = 20;
   private static final int X_OFFSET = 42;
   private static final int RELATIVE_X_SHIFT = 8;
   private static final int RELATIVE_Y_SHIFT = 0;
   private static final int RELATIVE_Z_SHIFT = 4;

   SectionPos(int p_123162_, int p_123163_, int p_123164_) {
      super(p_123162_, p_123163_, p_123164_);
   }

   public static SectionPos of(int pChunkX, int pChunkY, int pChunkZ) {
      return new SectionPos(pChunkX, pChunkY, pChunkZ);
   }

   public static SectionPos of(BlockPos pPos) {
      return new SectionPos(blockToSectionCoord(pPos.getX()), blockToSectionCoord(pPos.getY()), blockToSectionCoord(pPos.getZ()));
   }

   public static SectionPos of(ChunkPos pChunkPos, int pY) {
      return new SectionPos(pChunkPos.x, pY, pChunkPos.z);
   }

   public static SectionPos of(Entity pEntity) {
      return new SectionPos(blockToSectionCoord(pEntity.getBlockX()), blockToSectionCoord(pEntity.getBlockY()), blockToSectionCoord(pEntity.getBlockZ()));
   }

   public static SectionPos of(long pPacked) {
      return new SectionPos(x(pPacked), y(pPacked), z(pPacked));
   }

   public static SectionPos bottomOf(ChunkAccess p_175563_) {
      return of(p_175563_.getPos(), p_175563_.getMinSection());
   }

   public static long offset(long p_123192_, Direction p_123193_) {
      return offset(p_123192_, p_123193_.getStepX(), p_123193_.getStepY(), p_123193_.getStepZ());
   }

   public static long offset(long p_123187_, int p_123188_, int p_123189_, int p_123190_) {
      return asLong(x(p_123187_) + p_123188_, y(p_123187_) + p_123189_, z(p_123187_) + p_123190_);
   }

   public static int posToSectionCoord(double p_175553_) {
      return blockToSectionCoord(Mth.floor(p_175553_));
   }

   public static int blockToSectionCoord(int pBlockCoord) {
      return pBlockCoord >> 4;
   }

   public static int sectionRelative(int p_123208_) {
      return p_123208_ & 15;
   }

   public static short sectionRelativePos(BlockPos p_123219_) {
      int i = sectionRelative(p_123219_.getX());
      int j = sectionRelative(p_123219_.getY());
      int k = sectionRelative(p_123219_.getZ());
      return (short)(i << 8 | k << 4 | j << 0);
   }

   public static int sectionRelativeX(short p_123205_) {
      return p_123205_ >>> 8 & 15;
   }

   public static int sectionRelativeY(short p_123221_) {
      return p_123221_ >>> 0 & 15;
   }

   public static int sectionRelativeZ(short p_123228_) {
      return p_123228_ >>> 4 & 15;
   }

   public int relativeToBlockX(short p_123233_) {
      return this.minBlockX() + sectionRelativeX(p_123233_);
   }

   public int relativeToBlockY(short p_123238_) {
      return this.minBlockY() + sectionRelativeY(p_123238_);
   }

   public int relativeToBlockZ(short p_123243_) {
      return this.minBlockZ() + sectionRelativeZ(p_123243_);
   }

   public BlockPos relativeToBlockPos(short p_123246_) {
      return new BlockPos(this.relativeToBlockX(p_123246_), this.relativeToBlockY(p_123246_), this.relativeToBlockZ(p_123246_));
   }

   public static int sectionToBlockCoord(int pSectionCoord) {
      return pSectionCoord << 4;
   }

   public static int sectionToBlockCoord(int p_175555_, int p_175556_) {
      return sectionToBlockCoord(p_175555_) + p_175556_;
   }

   public static int x(long pPacked) {
      return (int)(pPacked << 0 >> 42);
   }

   public static int y(long pPacked) {
      return (int)(pPacked << 44 >> 44);
   }

   public static int z(long pPacked) {
      return (int)(pPacked << 22 >> 42);
   }

   public int x() {
      return this.getX();
   }

   public int y() {
      return this.getY();
   }

   public int z() {
      return this.getZ();
   }

   public int minBlockX() {
      return sectionToBlockCoord(this.x());
   }

   public int minBlockY() {
      return sectionToBlockCoord(this.y());
   }

   public int minBlockZ() {
      return sectionToBlockCoord(this.z());
   }

   public int maxBlockX() {
      return sectionToBlockCoord(this.x(), 15);
   }

   public int maxBlockY() {
      return sectionToBlockCoord(this.y(), 15);
   }

   public int maxBlockZ() {
      return sectionToBlockCoord(this.z(), 15);
   }

   public static long blockToSection(long pLevelPos) {
      return asLong(blockToSectionCoord(BlockPos.getX(pLevelPos)), blockToSectionCoord(BlockPos.getY(pLevelPos)), blockToSectionCoord(BlockPos.getZ(pLevelPos)));
   }

   public static long getZeroNode(long p_123241_) {
      return p_123241_ & -1048576L;
   }

   public BlockPos origin() {
      return new BlockPos(sectionToBlockCoord(this.x()), sectionToBlockCoord(this.y()), sectionToBlockCoord(this.z()));
   }

   public BlockPos center() {
      int i = 8;
      return this.origin().offset(8, 8, 8);
   }

   public ChunkPos chunk() {
      return new ChunkPos(this.x(), this.z());
   }

   public static long asLong(BlockPos pBlockPos) {
      return asLong(blockToSectionCoord(pBlockPos.getX()), blockToSectionCoord(pBlockPos.getY()), blockToSectionCoord(pBlockPos.getZ()));
   }

   public static long asLong(int pX, int pY, int pZ) {
      long i = 0L;
      i |= ((long)pX & 4194303L) << 42;
      i |= ((long)pY & 1048575L) << 0;
      return i | ((long)pZ & 4194303L) << 20;
   }

   public long asLong() {
      return asLong(this.x(), this.y(), this.z());
   }

   public SectionPos offset(int pDx, int pDy, int pDz) {
      return pDx == 0 && pDy == 0 && pDz == 0 ? this : new SectionPos(this.x() + pDx, this.y() + pDy, this.z() + pDz);
   }

   public Stream<BlockPos> blocksInside() {
      return BlockPos.betweenClosedStream(this.minBlockX(), this.minBlockY(), this.minBlockZ(), this.maxBlockX(), this.maxBlockY(), this.maxBlockZ());
   }

   public static Stream<SectionPos> cube(SectionPos pCenter, int pRadius) {
      int i = pCenter.x();
      int j = pCenter.y();
      int k = pCenter.z();
      return betweenClosedStream(i - pRadius, j - pRadius, k - pRadius, i + pRadius, j + pRadius, k + pRadius);
   }

   public static Stream<SectionPos> aroundChunk(ChunkPos p_175558_, int p_175559_, int p_175560_, int p_175561_) {
      int i = p_175558_.x;
      int j = p_175558_.z;
      return betweenClosedStream(i - p_175559_, p_175560_, j - p_175559_, i + p_175559_, p_175561_ - 1, j + p_175559_);
   }

   public static Stream<SectionPos> betweenClosedStream(final int p_123178_, final int p_123179_, final int p_123180_, final int p_123181_, final int p_123182_, final int p_123183_) {
      return StreamSupport.stream(new AbstractSpliterator<SectionPos>((long)((p_123181_ - p_123178_ + 1) * (p_123182_ - p_123179_ + 1) * (p_123183_ - p_123180_ + 1)), 64) {
         final Cursor3D cursor = new Cursor3D(p_123178_, p_123179_, p_123180_, p_123181_, p_123182_, p_123183_);

         public boolean tryAdvance(Consumer<? super SectionPos> p_123271_) {
            if (this.cursor.advance()) {
               p_123271_.accept(new SectionPos(this.cursor.nextX(), this.cursor.nextY(), this.cursor.nextZ()));
               return true;
            } else {
               return false;
            }
         }
      }, false);
   }

   public static void aroundAndAtBlockPos(BlockPos p_194643_, LongConsumer p_194644_) {
      aroundAndAtBlockPos(p_194643_.getX(), p_194643_.getY(), p_194643_.getZ(), p_194644_);
   }

   public static void aroundAndAtBlockPos(long p_194640_, LongConsumer p_194641_) {
      aroundAndAtBlockPos(BlockPos.getX(p_194640_), BlockPos.getY(p_194640_), BlockPos.getZ(p_194640_), p_194641_);
   }

   public static void aroundAndAtBlockPos(int p_194635_, int p_194636_, int p_194637_, LongConsumer p_194638_) {
      int i = blockToSectionCoord(p_194635_ - 1);
      int j = blockToSectionCoord(p_194635_ + 1);
      int k = blockToSectionCoord(p_194636_ - 1);
      int l = blockToSectionCoord(p_194636_ + 1);
      int i1 = blockToSectionCoord(p_194637_ - 1);
      int j1 = blockToSectionCoord(p_194637_ + 1);
      if (i == j && k == l && i1 == j1) {
         p_194638_.accept(asLong(i, k, i1));
      } else {
         for(int k1 = i; k1 <= j; ++k1) {
            for(int l1 = k; l1 <= l; ++l1) {
               for(int i2 = i1; i2 <= j1; ++i2) {
                  p_194638_.accept(asLong(k1, l1, i2));
               }
            }
         }
      }

   }
}