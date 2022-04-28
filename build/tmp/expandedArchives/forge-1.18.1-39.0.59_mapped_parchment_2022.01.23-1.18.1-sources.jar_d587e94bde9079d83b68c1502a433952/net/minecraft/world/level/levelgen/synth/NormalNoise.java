package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.levelgen.RandomSource;

/**
 * This samples the sum of two individual samplers of perlin noise octaves.
 * The input coordinates are scaled by {@link #INPUT_FACTOR}, and the result is scaled by {@link #valueFactor}.
 */
public class NormalNoise {
   private static final double INPUT_FACTOR = 1.0181268882175227D;
   private static final double TARGET_DEVIATION = 0.3333333333333333D;
   private final double valueFactor;
   private final PerlinNoise first;
   private final PerlinNoise second;

   /** @deprecated */
   @Deprecated
   public static NormalNoise createLegacyNetherBiome(RandomSource p_192844_, NormalNoise.NoiseParameters p_192845_) {
      return new NormalNoise(p_192844_, p_192845_.firstOctave(), p_192845_.amplitudes(), false);
   }

   public static NormalNoise create(RandomSource pRandomSource, int pOctaves, double... pAmplitudes) {
      return new NormalNoise(pRandomSource, pOctaves, new DoubleArrayList(pAmplitudes), true);
   }

   public static NormalNoise create(RandomSource p_192849_, NormalNoise.NoiseParameters p_192850_) {
      return new NormalNoise(p_192849_, p_192850_.firstOctave(), p_192850_.amplitudes(), true);
   }

   public static NormalNoise create(RandomSource pRandomSource, int pOctaves, DoubleList pAmplitudes) {
      return new NormalNoise(pRandomSource, pOctaves, pAmplitudes, true);
   }

   private NormalNoise(RandomSource p_192838_, int p_192839_, DoubleList p_192840_, boolean p_192841_) {
      if (p_192841_) {
         this.first = PerlinNoise.create(p_192838_, p_192839_, p_192840_);
         this.second = PerlinNoise.create(p_192838_, p_192839_, p_192840_);
      } else {
         this.first = PerlinNoise.createLegacyForLegacyNormalNoise(p_192838_, p_192839_, p_192840_);
         this.second = PerlinNoise.createLegacyForLegacyNormalNoise(p_192838_, p_192839_, p_192840_);
      }

      int i = Integer.MAX_VALUE;
      int j = Integer.MIN_VALUE;
      DoubleListIterator doublelistiterator = p_192840_.iterator();

      while(doublelistiterator.hasNext()) {
         int k = doublelistiterator.nextIndex();
         double d0 = doublelistiterator.nextDouble();
         if (d0 != 0.0D) {
            i = Math.min(i, k);
            j = Math.max(j, k);
         }
      }

      this.valueFactor = 0.16666666666666666D / expectedDeviation(j - i);
   }

   private static double expectedDeviation(int pOctaves) {
      return 0.1D * (1.0D + 1.0D / (double)(pOctaves + 1));
   }

   public double getValue(double pX, double pY, double pZ) {
      double d0 = pX * 1.0181268882175227D;
      double d1 = pY * 1.0181268882175227D;
      double d2 = pZ * 1.0181268882175227D;
      return (this.first.getValue(pX, pY, pZ) + this.second.getValue(d0, d1, d2)) * this.valueFactor;
   }

   public NormalNoise.NoiseParameters parameters() {
      return new NormalNoise.NoiseParameters(this.first.firstOctave(), this.first.amplitudes());
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder p_192847_) {
      p_192847_.append("NormalNoise {");
      p_192847_.append("first: ");
      this.first.parityConfigString(p_192847_);
      p_192847_.append(", second: ");
      this.second.parityConfigString(p_192847_);
      p_192847_.append("}");
   }

   public static class NoiseParameters {
      private final int firstOctave;
      private final DoubleList amplitudes;
      public static final Codec<NormalNoise.NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create((p_192865_) -> {
         return p_192865_.group(Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise.NoiseParameters::firstOctave), Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise.NoiseParameters::amplitudes)).apply(p_192865_, NormalNoise.NoiseParameters::new);
      });
      public static final Codec<Supplier<NormalNoise.NoiseParameters>> CODEC = RegistryFileCodec.create(Registry.NOISE_REGISTRY, DIRECT_CODEC);

      public NoiseParameters(int p_192861_, List<Double> p_192862_) {
         this.firstOctave = p_192861_;
         this.amplitudes = new DoubleArrayList(p_192862_);
      }

      public NoiseParameters(int p_192857_, double p_192858_, double... p_192859_) {
         this.firstOctave = p_192857_;
         this.amplitudes = new DoubleArrayList(p_192859_);
         this.amplitudes.add(0, p_192858_);
      }

      public int firstOctave() {
         return this.firstOctave;
      }

      public DoubleList amplitudes() {
         return this.amplitudes;
      }
   }
}