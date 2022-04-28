package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomSource;

/**
 * This class generates multiple octaves of perlin noise. Each individual octave is an instance of {@link
 * net.minecraft.world.level.levelgen.synth.ImprovedNoise}.
 * Mojang uses the term 'Perlin' to describe octaves or fBm (Fractal Brownian Motion) noise.
 */
public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;

   /** @deprecated */
   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource pRandomSource, IntStream pOctaves) {
      return new PerlinNoise(pRandomSource, makeAmplitudes(new IntRBTreeSet(pOctaves.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   /** @deprecated */
   @Deprecated
   public static PerlinNoise createLegacyForLegacyNormalNoise(RandomSource p_192879_, int p_192880_, DoubleList p_192881_) {
      return new PerlinNoise(p_192879_, Pair.of(p_192880_, p_192881_), false);
   }

   public static PerlinNoise create(RandomSource pRandomSource, IntStream pOctaves) {
      return create(pRandomSource, pOctaves.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource pRandomSource, List<Integer> pOctaves) {
      return new PerlinNoise(pRandomSource, makeAmplitudes(new IntRBTreeSet(pOctaves)), true);
   }

   public static PerlinNoise create(RandomSource p_192874_, int p_192875_, double p_192876_, double... p_192877_) {
      DoubleArrayList doublearraylist = new DoubleArrayList(p_192877_);
      doublearraylist.add(0, p_192876_);
      return new PerlinNoise(p_192874_, Pair.of(p_192875_, doublearraylist), true);
   }

   public static PerlinNoise create(RandomSource pRandomSource, int pOctaves, DoubleList pAmplitudes) {
      return new PerlinNoise(pRandomSource, Pair.of(pOctaves, pAmplitudes), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet pOctaves) {
      if (pOctaves.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int i = -pOctaves.firstInt();
         int j = pOctaves.lastInt();
         int k = i + j + 1;
         if (k < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleList doublelist = new DoubleArrayList(new double[k]);
            IntBidirectionalIterator intbidirectionaliterator = pOctaves.iterator();

            while(intbidirectionaliterator.hasNext()) {
               int l = intbidirectionaliterator.nextInt();
               doublelist.set(l + i, 1.0D);
            }

            return Pair.of(-i, doublelist);
         }
      }
   }

   protected PerlinNoise(RandomSource p_192869_, Pair<Integer, DoubleList> p_192870_, boolean p_192871_) {
      this.firstOctave = p_192870_.getFirst();
      this.amplitudes = p_192870_.getSecond();
      int i = this.amplitudes.size();
      int j = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[i];
      if (p_192871_) {
         PositionalRandomFactory positionalrandomfactory = p_192869_.forkPositional();

         for(int k = 0; k < i; ++k) {
            if (this.amplitudes.getDouble(k) != 0.0D) {
               int l = this.firstOctave + k;
               this.noiseLevels[k] = new ImprovedNoise(positionalrandomfactory.fromHashOf("octave_" + l));
            }
         }
      } else {
         ImprovedNoise improvednoise = new ImprovedNoise(p_192869_);
         if (j >= 0 && j < i) {
            double d0 = this.amplitudes.getDouble(j);
            if (d0 != 0.0D) {
               this.noiseLevels[j] = improvednoise;
            }
         }

         for(int i1 = j - 1; i1 >= 0; --i1) {
            if (i1 < i) {
               double d1 = this.amplitudes.getDouble(i1);
               if (d1 != 0.0D) {
                  this.noiseLevels[i1] = new ImprovedNoise(p_192869_);
               } else {
                  skipOctave(p_192869_);
               }
            } else {
               skipOctave(p_192869_);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter((p_192897_) -> {
            return p_192897_ != 0.0D;
         }).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if (j < i - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0D, (double)(-j));
      this.lowestFreqValueFactor = Math.pow(2.0D, (double)(i - 1)) / (Math.pow(2.0D, (double)i) - 1.0D);
   }

   private static void skipOctave(RandomSource pRandomSource) {
      pRandomSource.consumeCount(262);
   }

   public double getValue(double pX, double pY, double pZ) {
      return this.getValue(pX, pY, pZ, 0.0D, 0.0D, false);
   }

   /** @deprecated */
   @Deprecated
   public double getValue(double pX, double pY, double pZ, double pYScale, double pYMax, boolean pUseFixedY) {
      double d0 = 0.0D;
      double d1 = this.lowestFreqInputFactor;
      double d2 = this.lowestFreqValueFactor;

      for(int i = 0; i < this.noiseLevels.length; ++i) {
         ImprovedNoise improvednoise = this.noiseLevels[i];
         if (improvednoise != null) {
            double d3 = improvednoise.noise(wrap(pX * d1), pUseFixedY ? -improvednoise.yo : wrap(pY * d1), wrap(pZ * d1), pYScale * d1, pYMax * d1);
            d0 += this.amplitudes.getDouble(i) * d3 * d2;
         }

         d1 *= 2.0D;
         d2 /= 2.0D;
      }

      return d0;
   }

   /**
    * @return A single octave of Perlin noise.
    */
   @Nullable
   public ImprovedNoise getOctaveNoise(int pOctave) {
      return this.noiseLevels[this.noiseLevels.length - 1 - pOctave];
   }

   public static double wrap(double pValue) {
      return pValue - (double)Mth.lfloor(pValue / 3.3554432E7D + 0.5D) * 3.3554432E7D;
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder p_192891_) {
      p_192891_.append("PerlinNoise{");
      List<String> list = this.amplitudes.stream().map((p_192889_) -> {
         return String.format("%.2f", p_192889_);
      }).toList();
      p_192891_.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append((Object)list).append(", noise levels: [");

      for(int i = 0; i < this.noiseLevels.length; ++i) {
         p_192891_.append(i).append(": ");
         ImprovedNoise improvednoise = this.noiseLevels[i];
         if (improvednoise == null) {
            p_192891_.append("null");
         } else {
            improvednoise.parityConfigString(p_192891_);
         }

         p_192891_.append(", ");
      }

      p_192891_.append("]");
      p_192891_.append("}");
   }
}