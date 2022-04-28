package net.minecraft.world.level.levelgen.synth;

import net.minecraft.util.Mth;

public class NoiseUtils {
   /**
    * Samples the value of {@code noise}, and maps it linearly to the range [{@code min}, {@code max}].
    */
   public static double sampleNoiseAndMapToRange(NormalNoise pNoise, double pX, double pY, double pZ, double pMin, double pMax) {
      double d0 = pNoise.getValue(pX, pY, pZ);
      return Mth.map(d0, -1.0D, 1.0D, pMin, pMax);
   }

   /**
    * Takes an input value and biases it using a sine function towards two larger magnitude values.
    * @param pValue A value in the range [-1, 1]
    * @param pBias The effect of the bias. At {@code 0.0}, there will be no bias. Mojang only uses {@code 1.0} here.
    */
   public static double biasTowardsExtreme(double pValue, double pBias) {
      return pValue + Math.sin(Math.PI * pValue) * pBias / Math.PI;
   }

   public static void parityNoiseOctaveConfigString(StringBuilder p_192826_, double p_192827_, double p_192828_, double p_192829_, byte[] p_192830_) {
      p_192826_.append(String.format("xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)p_192827_, (float)p_192828_, (float)p_192829_, p_192830_[0], p_192830_[255]));
   }

   public static void parityNoiseOctaveConfigString(StringBuilder p_192832_, double p_192833_, double p_192834_, double p_192835_, int[] p_192836_) {
      p_192832_.append(String.format("xo=%.3f, yo=%.3f, zo=%.3f, p0=%d, p255=%d", (float)p_192833_, (float)p_192834_, (float)p_192835_, p_192836_[0], p_192836_[255]));
   }
}