package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.mutable.MutableDouble;

/**
 * Aquifers are responsible for non-sea level fluids found in terrain generation, but also managing that different
 * aquifers don't intersect with each other in ways that would create undesirable fluid placement.
 * The aquifer interface itself is a modifier on a per-block basis. It computes a block state to be placed for each
 * position in the world.
 * <p>
 * Aquifers work by first partitioning a single chunk into a low resolution grid. They then generate, via various noise
 * layers, an {@link NoiseBasedAquifer.AquiferStatus} at each grid point.
 * At each point, the grid cell containing that point is calculated, and then of the eight grid corners, the three
 * closest aquifers are found, by square euclidean distance.
 * Borders between aquifers are created by comparing nearby aquifers to see if the given point is near-equidistant from
 * them, indicating a border if so, or fluid/air depending on the aquifer height if not.
 */
public interface Aquifer {
   /**
    * Creates a standard noise based aquifer. This aquifer will place liquid (both water and lava), air, and stone as
    * described above.
    */
   static Aquifer create(NoiseChunk pChunk, ChunkPos pChunkPos, NormalNoise pBarrierNoise, NormalNoise pFluidLevelFloodedNoise, NormalNoise pFluidLevelSpreadNoise, NormalNoise pLavaNoise, PositionalRandomFactory pPositionalRandomFactory, int pMinY, int pHeight, Aquifer.FluidPicker pGlobalFluidPicker) {
      return new Aquifer.NoiseBasedAquifer(pChunk, pChunkPos, pBarrierNoise, pFluidLevelFloodedNoise, pFluidLevelSpreadNoise, pLavaNoise, pPositionalRandomFactory, pMinY, pHeight, pGlobalFluidPicker);
   }

   /**
    * reates a disabled, or no-op aquifer. This will fill any open areas below sea level with the default fluid.
    */
   static Aquifer createDisabled(final Aquifer.FluidPicker pDefaultFluid) {
      return new Aquifer() {
         @Nullable
         public BlockState computeSubstance(int p_188392_, int p_188393_, int p_188394_, double p_188395_, double p_188396_) {
            return p_188396_ > 0.0D ? null : pDefaultFluid.computeFluid(p_188392_, p_188393_, p_188394_).at(p_188393_);
         }

         /**
          * Returns {@code true} if there should be a fluid update scheduled - due to a fluid block being placed in a
          * possibly unsteady position - at the last position passed into {@link #computeState}.
          * This <strong>must</strong> be invoked only after {@link #computeState}, and will be using the same
          * parameters as that method.
          */
         public boolean shouldScheduleFluidUpdate() {
            return false;
         }
      };
   }

   @Nullable
   BlockState computeSubstance(int p_188369_, int p_188370_, int p_188371_, double p_188372_, double p_188373_);

   /**
    * Returns {@code true} if there should be a fluid update scheduled - due to a fluid block being placed in a possibly
    * unsteady position - at the last position passed into {@link #computeState}.
    * This <strong>must</strong> be invoked only after {@link #computeState}, and will be using the same parameters as
    * that method.
    */
   boolean shouldScheduleFluidUpdate();

   public interface FluidPicker {
      /**
       * Computes the aquifer which is centered at the given (x, y, z) position.
       * Aquifers are placed at a positive offset from their grid corner and so the grid corner can always be extracted
       * from the aquifer position.
       * If the aquifer y level is above {@link #ALWAYS_USE_SEA_LEVEL_WHEN_ABOVE}, then the aquifer fluid level will be
       * at sea level.
       * Otherwise, this queries the internal noise functions to determine both the height of the aquifer, and the fluid
       * (either lava or water).
       */
      Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ);
   }

   public static final class FluidStatus {
      /** The y height of the aquifer. */
      final int fluidLevel;
      /** The fluid state the aquifer is filled with. */
      final BlockState fluidType;

      public FluidStatus(int pFluidLevel, BlockState pFluidType) {
         this.fluidLevel = pFluidLevel;
         this.fluidType = pFluidType;
      }

      public BlockState at(int p_188406_) {
         return p_188406_ < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
      }
   }

   public static class NoiseBasedAquifer implements Aquifer, Aquifer.FluidPicker {
      private static final int X_RANGE = 10;
      private static final int Y_RANGE = 9;
      private static final int Z_RANGE = 10;
      private static final int X_SEPARATION = 6;
      private static final int Y_SEPARATION = 3;
      private static final int Z_SEPARATION = 6;
      private static final int X_SPACING = 16;
      private static final int Y_SPACING = 12;
      private static final int Z_SPACING = 16;
      private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
      private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
      private final NoiseChunk noiseChunk;
      protected final NormalNoise barrierNoise;
      private final NormalNoise fluidLevelFloodednessNoise;
      private final NormalNoise fluidLevelSpreadNoise;
      protected final NormalNoise lavaNoise;
      private final PositionalRandomFactory positionalRandomFactory;
      protected final Aquifer.FluidStatus[] aquiferCache;
      protected final long[] aquiferLocationCache;
      private final Aquifer.FluidPicker globalFluidPicker;
      protected boolean shouldScheduleFluidUpdate;
      protected final int minGridX;
      protected final int minGridY;
      protected final int minGridZ;
      protected final int gridSizeX;
      protected final int gridSizeZ;
      private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{{-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}};

      NoiseBasedAquifer(NoiseChunk pNoiseChunk, ChunkPos pPos, NormalNoise pBarrierNoise, NormalNoise pFluidLevelFloodedNoise, NormalNoise pFluidLevelSpreadNoise, NormalNoise pLavaNoise, PositionalRandomFactory pPositionalRandomFactory, int pMinY, int pHeight, Aquifer.FluidPicker pGlobalFluidPicker) {
         this.noiseChunk = pNoiseChunk;
         this.barrierNoise = pBarrierNoise;
         this.fluidLevelFloodednessNoise = pFluidLevelFloodedNoise;
         this.fluidLevelSpreadNoise = pFluidLevelSpreadNoise;
         this.lavaNoise = pLavaNoise;
         this.positionalRandomFactory = pPositionalRandomFactory;
         this.minGridX = this.gridX(pPos.getMinBlockX()) - 1;
         this.globalFluidPicker = pGlobalFluidPicker;
         int i = this.gridX(pPos.getMaxBlockX()) + 1;
         this.gridSizeX = i - this.minGridX + 1;
         this.minGridY = this.gridY(pMinY) - 1;
         int j = this.gridY(pMinY + pHeight) + 1;
         int k = j - this.minGridY + 1;
         this.minGridZ = this.gridZ(pPos.getMinBlockZ()) - 1;
         int l = this.gridZ(pPos.getMaxBlockZ()) + 1;
         this.gridSizeZ = l - this.minGridZ + 1;
         int i1 = this.gridSizeX * k * this.gridSizeZ;
         this.aquiferCache = new Aquifer.FluidStatus[i1];
         this.aquiferLocationCache = new long[i1];
         Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
      }

      /**
       * @return A cache index based on grid positions.
       */
      protected int getIndex(int pGridX, int pGridY, int pGridZ) {
         int i = pGridX - this.minGridX;
         int j = pGridY - this.minGridY;
         int k = pGridZ - this.minGridZ;
         return (j * this.gridSizeZ + k) * this.gridSizeX + i;
      }

      @Nullable
      public BlockState computeSubstance(int p_188427_, int p_188428_, int p_188429_, double p_188430_, double p_188431_) {
         if (p_188430_ <= -64.0D) {
            return this.globalFluidPicker.computeFluid(p_188427_, p_188428_, p_188429_).at(p_188428_);
         } else {
            if (p_188431_ <= 0.0D) {
               Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(p_188427_, p_188428_, p_188429_);
               double d0;
               BlockState blockstate;
               boolean flag;
               if (aquifer$fluidstatus.at(p_188428_).is(Blocks.LAVA)) {
                  blockstate = Blocks.LAVA.defaultBlockState();
                  d0 = 0.0D;
                  flag = false;
               } else {
                  int i = Math.floorDiv(p_188427_ - 5, 16);
                  int j = Math.floorDiv(p_188428_ + 1, 12);
                  int k = Math.floorDiv(p_188429_ - 5, 16);
                  int l = Integer.MAX_VALUE;
                  int i1 = Integer.MAX_VALUE;
                  int j1 = Integer.MAX_VALUE;
                  long k1 = 0L;
                  long l1 = 0L;
                  long i2 = 0L;

                  for(int j2 = 0; j2 <= 1; ++j2) {
                     for(int k2 = -1; k2 <= 1; ++k2) {
                        for(int l2 = 0; l2 <= 1; ++l2) {
                           int i3 = i + j2;
                           int j3 = j + k2;
                           int k3 = k + l2;
                           int l3 = this.getIndex(i3, j3, k3);
                           long j4 = this.aquiferLocationCache[l3];
                           long i4;
                           if (j4 != Long.MAX_VALUE) {
                              i4 = j4;
                           } else {
                              RandomSource randomsource = this.positionalRandomFactory.at(i3, j3, k3);
                              i4 = BlockPos.asLong(i3 * 16 + randomsource.nextInt(10), j3 * 12 + randomsource.nextInt(9), k3 * 16 + randomsource.nextInt(10));
                              this.aquiferLocationCache[l3] = i4;
                           }

                           int j5 = BlockPos.getX(i4) - p_188427_;
                           int k4 = BlockPos.getY(i4) - p_188428_;
                           int l4 = BlockPos.getZ(i4) - p_188429_;
                           int i5 = j5 * j5 + k4 * k4 + l4 * l4;
                           if (l >= i5) {
                              i2 = l1;
                              l1 = k1;
                              k1 = i4;
                              j1 = i1;
                              i1 = l;
                              l = i5;
                           } else if (i1 >= i5) {
                              i2 = l1;
                              l1 = i4;
                              j1 = i1;
                              i1 = i5;
                           } else if (j1 >= i5) {
                              i2 = i4;
                              j1 = i5;
                           }
                        }
                     }
                  }

                  Aquifer.FluidStatus aquifer$fluidstatus1 = this.getAquiferStatus(k1);
                  Aquifer.FluidStatus aquifer$fluidstatus2 = this.getAquiferStatus(l1);
                  Aquifer.FluidStatus aquifer$fluidstatus3 = this.getAquiferStatus(i2);
                  double d6 = similarity(l, i1);
                  double d7 = similarity(l, j1);
                  double d8 = similarity(i1, j1);
                  flag = d6 >= FLOWING_UPDATE_SIMULARITY;
                  if (aquifer$fluidstatus1.at(p_188428_).is(Blocks.WATER) && this.globalFluidPicker.computeFluid(p_188427_, p_188428_ - 1, p_188429_).at(p_188428_ - 1).is(Blocks.LAVA)) {
                     d0 = 1.0D;
                  } else if (d6 > -1.0D) {
                     MutableDouble mutabledouble = new MutableDouble(Double.NaN);
                     double d1 = this.calculatePressure(p_188427_, p_188428_, p_188429_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus2);
                     double d9 = this.calculatePressure(p_188427_, p_188428_, p_188429_, mutabledouble, aquifer$fluidstatus1, aquifer$fluidstatus3);
                     double d10 = this.calculatePressure(p_188427_, p_188428_, p_188429_, mutabledouble, aquifer$fluidstatus2, aquifer$fluidstatus3);
                     double d2 = Math.max(0.0D, d6);
                     double d3 = Math.max(0.0D, d7);
                     double d4 = Math.max(0.0D, d8);
                     double d5 = 2.0D * d2 * Math.max(d1, Math.max(d9 * d3, d10 * d4));
                     d0 = Math.max(0.0D, d5);
                  } else {
                     d0 = 0.0D;
                  }

                  blockstate = aquifer$fluidstatus1.at(p_188428_);
               }

               if (p_188431_ + d0 <= 0.0D) {
                  this.shouldScheduleFluidUpdate = flag;
                  return blockstate;
               }
            }

            this.shouldScheduleFluidUpdate = false;
            return null;
         }
      }

      /**
       * Returns {@code true} if there should be a fluid update scheduled - due to a fluid block being placed in a
       * possibly unsteady position - at the last position passed into {@link #computeState}.
       * This <strong>must</strong> be invoked only after {@link #computeState}, and will be using the same parameters
       * as that method.
       */
      public boolean shouldScheduleFluidUpdate() {
         return this.shouldScheduleFluidUpdate;
      }

      /**
       * Compares two distances (between aquifers).
       * @return {@code 1.0} if the distances are equal, and returns smaller values the more different in absolute value
       * the two distances are.
       */
      protected static double similarity(int pFirstDistance, int pSecondDistance) {
         double d0 = 25.0D;
         return 1.0D - (double)Math.abs(pSecondDistance - pFirstDistance) / 25.0D;
      }

      private double calculatePressure(int p_188439_, int p_188440_, int p_188441_, MutableDouble p_188442_, Aquifer.FluidStatus p_188443_, Aquifer.FluidStatus p_188444_) {
         BlockState blockstate = p_188443_.at(p_188440_);
         BlockState blockstate1 = p_188444_.at(p_188440_);
         if ((!blockstate.is(Blocks.LAVA) || !blockstate1.is(Blocks.WATER)) && (!blockstate.is(Blocks.WATER) || !blockstate1.is(Blocks.LAVA))) {
            int i = Math.abs(p_188443_.fluidLevel - p_188444_.fluidLevel);
            if (i == 0) {
               return 0.0D;
            } else {
               double d0 = 0.5D * (double)(p_188443_.fluidLevel + p_188444_.fluidLevel);
               double d1 = (double)p_188440_ + 0.5D - d0;
               double d2 = (double)i / 2.0D;
               double d3 = 0.0D;
               double d4 = 2.5D;
               double d5 = 1.5D;
               double d6 = 3.0D;
               double d7 = 10.0D;
               double d8 = 3.0D;
               double d9 = d2 - Math.abs(d1);
               double d10;
               if (d1 > 0.0D) {
                  double d11 = 0.0D + d9;
                  if (d11 > 0.0D) {
                     d10 = d11 / 1.5D;
                  } else {
                     d10 = d11 / 2.5D;
                  }
               } else {
                  double d14 = 3.0D + d9;
                  if (d14 > 0.0D) {
                     d10 = d14 / 3.0D;
                  } else {
                     d10 = d14 / 10.0D;
                  }
               }

               if (!(d10 < -2.0D) && !(d10 > 2.0D)) {
                  double d15 = p_188442_.getValue();
                  if (Double.isNaN(d15)) {
                     double d12 = 0.5D;
                     double d13 = this.barrierNoise.getValue((double)p_188439_, (double)p_188440_ * 0.5D, (double)p_188441_);
                     p_188442_.setValue(d13);
                     return d13 + d10;
                  } else {
                     return d15 + d10;
                  }
               } else {
                  return d10;
               }
            }
         } else {
            return 1.0D;
         }
      }

      protected int gridX(int pX) {
         return Math.floorDiv(pX, 16);
      }

      protected int gridY(int pY) {
         return Math.floorDiv(pY, 12);
      }

      protected int gridZ(int pZ) {
         return Math.floorDiv(pZ, 16);
      }

      /**
       * Calculates the aquifer at a given location. Internally references a cache using the grid positions as an index.
       * If the cache is not populated, computes a new aquifer at that grid location using {@link #computeFluid.
       * @param pPackedPos The aquifer block position, packed into a {@code long}.
       */
      private Aquifer.FluidStatus getAquiferStatus(long pPackedPos) {
         int i = BlockPos.getX(pPackedPos);
         int j = BlockPos.getY(pPackedPos);
         int k = BlockPos.getZ(pPackedPos);
         int l = this.gridX(i);
         int i1 = this.gridY(j);
         int j1 = this.gridZ(k);
         int k1 = this.getIndex(l, i1, j1);
         Aquifer.FluidStatus aquifer$fluidstatus = this.aquiferCache[k1];
         if (aquifer$fluidstatus != null) {
            return aquifer$fluidstatus;
         } else {
            Aquifer.FluidStatus aquifer$fluidstatus1 = this.computeFluid(i, j, k);
            this.aquiferCache[k1] = aquifer$fluidstatus1;
            return aquifer$fluidstatus1;
         }
      }

      /**
       * Computes the aquifer which is centered at the given (x, y, z) position.
       * Aquifers are placed at a positive offset from their grid corner and so the grid corner can always be extracted
       * from the aquifer position.
       * If the aquifer y level is above {@link #ALWAYS_USE_SEA_LEVEL_WHEN_ABOVE}, then the aquifer fluid level will be
       * at sea level.
       * Otherwise, this queries the internal noise functions to determine both the height of the aquifer, and the fluid
       * (either lava or water).
       */
      public Aquifer.FluidStatus computeFluid(int pX, int pY, int pZ) {
         Aquifer.FluidStatus aquifer$fluidstatus = this.globalFluidPicker.computeFluid(pX, pY, pZ);
         int i = Integer.MAX_VALUE;
         int j = pY + 12;
         int k = pY - 12;
         boolean flag = false;

         for(int[] aint : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int l = pX + SectionPos.sectionToBlockCoord(aint[0]);
            int i1 = pZ + SectionPos.sectionToBlockCoord(aint[1]);
            int j1 = this.noiseChunk.preliminarySurfaceLevel(l, i1);
            int k1 = j1 + 8;
            boolean flag1 = aint[0] == 0 && aint[1] == 0;
            if (flag1 && k > k1) {
               return aquifer$fluidstatus;
            }

            boolean flag2 = j > k1;
            if (flag2 || flag1) {
               Aquifer.FluidStatus aquifer$fluidstatus1 = this.globalFluidPicker.computeFluid(l, k1, i1);
               if (!aquifer$fluidstatus1.at(k1).isAir()) {
                  if (flag1) {
                     flag = true;
                  }

                  if (flag2) {
                     return aquifer$fluidstatus1;
                  }
               }
            }

            i = Math.min(i, j1);
         }

         int j4 = i + 8 - pY;
         int k4 = 64;
         double d1 = flag ? Mth.clampedMap((double)j4, 0.0D, 64.0D, 1.0D, 0.0D) : 0.0D;
         double d2 = 0.67D;
         double d3 = Mth.clamp(this.fluidLevelFloodednessNoise.getValue((double)pX, (double)pY * 0.67D, (double)pZ), -1.0D, 1.0D);
         double d4 = Mth.map(d1, 1.0D, 0.0D, -0.3D, 0.8D);
         if (d3 > d4) {
            return aquifer$fluidstatus;
         } else {
            double d5 = Mth.map(d1, 1.0D, 0.0D, -0.8D, 0.4D);
            if (d3 <= d5) {
               return new Aquifer.FluidStatus(DimensionType.WAY_BELOW_MIN_Y, aquifer$fluidstatus.fluidType);
            } else {
               int l1 = 16;
               int i2 = 40;
               int j2 = Math.floorDiv(pX, 16);
               int k2 = Math.floorDiv(pY, 40);
               int l2 = Math.floorDiv(pZ, 16);
               int i3 = k2 * 40 + 20;
               int j3 = 10;
               double d0 = this.fluidLevelSpreadNoise.getValue((double)j2, (double)k2 / 1.4D, (double)l2) * 10.0D;
               int k3 = Mth.quantize(d0, 3);
               int l3 = i3 + k3;
               int i4 = Math.min(i, l3);
               BlockState blockstate = this.getFluidType(pX, pY, pZ, aquifer$fluidstatus, l3);
               return new Aquifer.FluidStatus(i4, blockstate);
            }
         }
      }

      private BlockState getFluidType(int p_188433_, int p_188434_, int p_188435_, Aquifer.FluidStatus p_188436_, int p_188437_) {
         if (p_188437_ <= -10) {
            int i = 64;
            int j = 40;
            int k = Math.floorDiv(p_188433_, 64);
            int l = Math.floorDiv(p_188434_, 40);
            int i1 = Math.floorDiv(p_188435_, 64);
            double d0 = this.lavaNoise.getValue((double)k, (double)l, (double)i1);
            if (Math.abs(d0) > 0.3D) {
               return Blocks.LAVA.defaultBlockState();
            }
         }

         return p_188436_.fluidType;
      }
   }
}