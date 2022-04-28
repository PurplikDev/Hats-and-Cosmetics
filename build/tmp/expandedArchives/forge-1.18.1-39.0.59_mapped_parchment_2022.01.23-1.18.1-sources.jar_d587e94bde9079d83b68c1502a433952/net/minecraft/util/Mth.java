package net.minecraft.util;

import java.util.Random;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.NumberUtils;

public class Mth {
   private static final int BIG_ENOUGH_INT = 1024;
   private static final float BIG_ENOUGH_FLOAT = 1024.0F;
   private static final long UUID_VERSION = 61440L;
   private static final long UUID_VERSION_TYPE_4 = 16384L;
   private static final long UUID_VARIANT = -4611686018427387904L;
   private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
   public static final float PI = (float)Math.PI;
   public static final float HALF_PI = ((float)Math.PI / 2F);
   public static final float TWO_PI = ((float)Math.PI * 2F);
   public static final float DEG_TO_RAD = ((float)Math.PI / 180F);
   public static final float RAD_TO_DEG = (180F / (float)Math.PI);
   public static final float EPSILON = 1.0E-5F;
   public static final float SQRT_OF_TWO = sqrt(2.0F);
   private static final float SIN_SCALE = 10430.378F;
   private static final float[] SIN = Util.make(new float[65536], (p_14077_) -> {
      for(int i = 0; i < p_14077_.length; ++i) {
         p_14077_[i] = (float)Math.sin((double)i * Math.PI * 2.0D / 65536.0D);
      }

   });
   private static final Random RANDOM = new Random();
   /**
    * Though it looks like an array, this is really more like a mapping. Key (index of this array) is the upper 5 bits
    * of the result of multiplying a 32-bit unsigned integer by the B(2, 5) De Bruijn sequence 0x077CB531. Value (value
    * stored in the array) is the unique index (from the right) of the leftmo
    */
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};
   private static final double ONE_SIXTH = 0.16666666666666666D;
   private static final int FRAC_EXP = 8;
   private static final int LUT_SIZE = 257;
   private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
   private static final double[] ASIN_TAB = new double[257];
   private static final double[] COS_TAB = new double[257];

   /**
    * sin looked up in a table
    */
   public static float sin(float pValue) {
      return SIN[(int)(pValue * 10430.378F) & '\uffff'];
   }

   /**
    * cos looked up in the sin table with the appropriate offset
    */
   public static float cos(float pValue) {
      return SIN[(int)(pValue * 10430.378F + 16384.0F) & '\uffff'];
   }

   public static float sqrt(float pValue) {
      return (float)Math.sqrt((double)pValue);
   }

   /**
    * Returns the greatest integer less than or equal to the float argument
    */
   public static int floor(float pValue) {
      int i = (int)pValue;
      return pValue < (float)i ? i - 1 : i;
   }

   /**
    * returns par0 cast as an int, and no greater than Integer.MAX_VALUE-1024
    */
   public static int fastFloor(double pValue) {
      return (int)(pValue + 1024.0D) - 1024;
   }

   /**
    * Returns the greatest integer less than or equal to the double argument
    */
   public static int floor(double pValue) {
      int i = (int)pValue;
      return pValue < (double)i ? i - 1 : i;
   }

   /**
    * Long version of floor()
    */
   public static long lfloor(double pValue) {
      long i = (long)pValue;
      return pValue < (double)i ? i - 1L : i;
   }

   public static int absFloor(double p_144940_) {
      return (int)(p_144940_ >= 0.0D ? p_144940_ : -p_144940_ + 1.0D);
   }

   public static float abs(float pValue) {
      return Math.abs(pValue);
   }

   /**
    * Returns the unsigned value of an int.
    */
   public static int abs(int pValue) {
      return Math.abs(pValue);
   }

   public static int ceil(float pValue) {
      int i = (int)pValue;
      return pValue > (float)i ? i + 1 : i;
   }

   public static int ceil(double pValue) {
      int i = (int)pValue;
      return pValue > (double)i ? i + 1 : i;
   }

   /**
    * Returns the given value if between the lower and the upper bound. If the value is less than the lower bound,
    * returns the lower bound. If the value is greater than the upper bound, returns the upper bound.
    * @param pValue The value that is clamped.
    * @param pMin The lower bound for the clamp.
    * @param pMax The upper bound for the clamp.
    */
   public static byte clamp(byte pValue, byte pMin, byte pMax) {
      if (pValue < pMin) {
         return pMin;
      } else {
         return pValue > pMax ? pMax : pValue;
      }
   }

   /**
    * Returns the given value if between the lower and the upper bound. If the value is less than the lower bound,
    * returns the lower bound. If the value is greater than the upper bound, returns the upper bound.
    * @param pValue The value that is clamped.
    * @param pMin The lower bound for the clamp.
    * @param pMax The upper bound for the clamp.
    */
   public static int clamp(int pValue, int pMin, int pMax) {
      if (pValue < pMin) {
         return pMin;
      } else {
         return pValue > pMax ? pMax : pValue;
      }
   }

   /**
    * Returns the given value if between the lower and the upper bound. If the value is less than the lower bound,
    * returns the lower bound. If the value is greater than the upper bound, returns the upper bound.
    * @param pValue The value that is clamped.
    * @param pMin The lower bound for the clamp.
    * @param pMax The upper bound for the clamp.
    */
   public static long clamp(long pValue, long pMin, long pMax) {
      if (pValue < pMin) {
         return pMin;
      } else {
         return pValue > pMax ? pMax : pValue;
      }
   }

   /**
    * Returns the given value if between the lower and the upper bound. If the value is less than the lower bound,
    * returns the lower bound. If the value is greater than the upper bound, returns the upper bound.
    * @param pValue The value that is clamped.
    * @param pMin The lower bound for the clamp.
    * @param pMax The upper bound for the clamp.
    */
   public static float clamp(float pValue, float pMin, float pMax) {
      if (pValue < pMin) {
         return pMin;
      } else {
         return pValue > pMax ? pMax : pValue;
      }
   }

   /**
    * Returns the given value if between the lower and the upper bound. If the value is less than the lower bound,
    * returns the lower bound. If the value is greater than the upper bound, returns the upper bound.
    * @param pValue The value that is clamped.
    * @param pMin The lower bound for the clamp.
    * @param pMax The upper bound for the clamp.
    */
   public static double clamp(double pValue, double pMin, double pMax) {
      if (pValue < pMin) {
         return pMin;
      } else {
         return pValue > pMax ? pMax : pValue;
      }
   }

   /**
    * Method for linear interpolation of doubles.
    * @param pStart Start value for the lerp.
    * @param pEnd End value for the lerp.
    * @param pDelta A value between 0 and 1 that indicates the percentage of the lerp. (0 will give the start value and
    * 1 will give the end value) If the value is not between 0 and 1, it is clamped.
    */
   public static double clampedLerp(double pStart, double pEnd, double pDelta) {
      if (pDelta < 0.0D) {
         return pStart;
      } else {
         return pDelta > 1.0D ? pEnd : lerp(pDelta, pStart, pEnd);
      }
   }

   /**
    * Method for linear interpolation of floats.
    * @param pStart Start value for the lerp.
    * @param pEnd End value for the lerp.
    * @param pDelta A value between 0 and 1 that indicates the percentage of the lerp. (0 will give the start value and
    * 1 will give the end value) If the value is not between 0 and 1, it is clamped.
    */
   public static float clampedLerp(float pStart, float pEnd, float pDelta) {
      if (pDelta < 0.0F) {
         return pStart;
      } else {
         return pDelta > 1.0F ? pEnd : lerp(pDelta, pStart, pEnd);
      }
   }

   /**
    * Maximum of the absolute value of two numbers.
    */
   public static double absMax(double pX, double pY) {
      if (pX < 0.0D) {
         pX = -pX;
      }

      if (pY < 0.0D) {
         pY = -pY;
      }

      return pX > pY ? pX : pY;
   }

   /**
    * Buckets an integer with specifed bucket sizes.
    */
   public static int intFloorDiv(int pX, int pY) {
      return Math.floorDiv(pX, pY);
   }

   public static int nextInt(Random pRandom, int pMinimum, int pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextInt(pMaximum - pMinimum + 1) + pMinimum;
   }

   public static float nextFloat(Random pRandom, float pMinimum, float pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextFloat() * (pMaximum - pMinimum) + pMinimum;
   }

   public static double nextDouble(Random pRandom, double pMinimum, double pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextDouble() * (pMaximum - pMinimum) + pMinimum;
   }

   public static double average(long[] pValues) {
      long i = 0L;

      for(long j : pValues) {
         i += j;
      }

      return (double)i / (double)pValues.length;
   }

   public static boolean equal(float pX, float pY) {
      return Math.abs(pY - pX) < 1.0E-5F;
   }

   public static boolean equal(double pX, double pY) {
      return Math.abs(pY - pX) < (double)1.0E-5F;
   }

   public static int positiveModulo(int pX, int pY) {
      return Math.floorMod(pX, pY);
   }

   public static float positiveModulo(float pNumerator, float pDenominator) {
      return (pNumerator % pDenominator + pDenominator) % pDenominator;
   }

   public static double positiveModulo(double pNumerator, double pDenominator) {
      return (pNumerator % pDenominator + pDenominator) % pDenominator;
   }

   /**
    * Adjust the angle so that his value is in range [-180180[
    */
   public static int wrapDegrees(int pAngle) {
      int i = pAngle % 360;
      if (i >= 180) {
         i -= 360;
      }

      if (i < -180) {
         i += 360;
      }

      return i;
   }

   /**
    * the angle is reduced to an angle between -180 and +180 by mod, and a 360 check
    */
   public static float wrapDegrees(float pValue) {
      float f = pValue % 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   /**
    * the angle is reduced to an angle between -180 and +180 by mod, and a 360 check
    */
   public static double wrapDegrees(double pValue) {
      double d0 = pValue % 360.0D;
      if (d0 >= 180.0D) {
         d0 -= 360.0D;
      }

      if (d0 < -180.0D) {
         d0 += 360.0D;
      }

      return d0;
   }

   public static float degreesDifference(float p_14119_, float p_14120_) {
      return wrapDegrees(p_14120_ - p_14119_);
   }

   public static float degreesDifferenceAbs(float p_14146_, float p_14147_) {
      return abs(degreesDifference(p_14146_, p_14147_));
   }

   public static float rotateIfNecessary(float p_14095_, float p_14096_, float p_14097_) {
      float f = degreesDifference(p_14095_, p_14096_);
      float f1 = clamp(f, -p_14097_, p_14097_);
      return p_14096_ - f1;
   }

   public static float approach(float p_14122_, float p_14123_, float p_14124_) {
      p_14124_ = abs(p_14124_);
      return p_14122_ < p_14123_ ? clamp(p_14122_ + p_14124_, p_14122_, p_14123_) : clamp(p_14122_ - p_14124_, p_14123_, p_14122_);
   }

   public static float approachDegrees(float p_14149_, float p_14150_, float p_14151_) {
      float f = degreesDifference(p_14149_, p_14150_);
      return approach(p_14149_, p_14149_ + f, p_14151_);
   }

   /**
    * parses the string as integer or returns the second parameter if it fails
    */
   public static int getInt(String pValue, int pDefaultValue) {
      return NumberUtils.toInt(pValue, pDefaultValue);
   }

   /**
    * parses the string as integer or returns the second parameter if it fails. this value is capped to par2
    */
   public static int getInt(String pValue, int pDefaultValue, int pMax) {
      return Math.max(pMax, getInt(pValue, pDefaultValue));
   }

   public static double getDouble(String pValue, double pDefaultValue) {
      try {
         return Double.parseDouble(pValue);
      } catch (Throwable throwable) {
         return pDefaultValue;
      }
   }

   public static double getDouble(String pValue, double pDefaultValue, double pMax) {
      return Math.max(pMax, getDouble(pValue, pDefaultValue));
   }

   /**
    * Returns the input value rounded up to the next highest power of two.
    */
   public static int smallestEncompassingPowerOfTwo(int pValue) {
      int i = pValue - 1;
      i |= i >> 1;
      i |= i >> 2;
      i |= i >> 4;
      i |= i >> 8;
      i |= i >> 16;
      return i + 1;
   }

   /**
    * Is the given value a power of two?  (1, 2, 4, 8, 16, ...)
    */
   public static boolean isPowerOfTwo(int pValue) {
      return pValue != 0 && (pValue & pValue - 1) == 0;
   }

   /**
    * Uses a B(2, 5) De Bruijn sequence and a lookup table to efficiently calculate the log-base-two of the given value.
    * Optimized for cases where the input value is a power-of-two. If the input value is not a power-of-two, then
    * subtract 1 from the return value.
    */
   public static int ceillog2(int pValue) {
      pValue = isPowerOfTwo(pValue) ? pValue : smallestEncompassingPowerOfTwo(pValue);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)pValue * 125613361L >> 27) & 31];
   }

   /**
    * Efficiently calculates the floor of the base-2 log of an integer value.  This is effectively the index of the
    * highest bit that is set.  For example, if the number in binary is 0...100101, this will return 5.
    */
   public static int log2(int pValue) {
      return ceillog2(pValue) - (isPowerOfTwo(pValue) ? 0 : 1);
   }

   /**
    * Makes an integer color from the given red, green, and blue float values
    */
   public static int color(float pR, float pG, float pB) {
      return color(floor(pR * 255.0F), floor(pG * 255.0F), floor(pB * 255.0F));
   }

   /**
    * Makes a single int color with the given red, green, and blue values.
    */
   public static int color(int pR, int pG, int pB) {
      int $$3 = (pR << 8) + pG;
      return ($$3 << 8) + pB;
   }

   public static int colorMultiply(int p_144933_, int p_144934_) {
      int i = (p_144933_ & 16711680) >> 16;
      int j = (p_144934_ & 16711680) >> 16;
      int k = (p_144933_ & '\uff00') >> 8;
      int l = (p_144934_ & '\uff00') >> 8;
      int i1 = (p_144933_ & 255) >> 0;
      int j1 = (p_144934_ & 255) >> 0;
      int k1 = (int)((float)i * (float)j / 255.0F);
      int l1 = (int)((float)k * (float)l / 255.0F);
      int i2 = (int)((float)i1 * (float)j1 / 255.0F);
      return p_144933_ & -16777216 | k1 << 16 | l1 << 8 | i2;
   }

   public static int colorMultiply(int p_144882_, float p_144883_, float p_144884_, float p_144885_) {
      int i = (p_144882_ & 16711680) >> 16;
      int j = (p_144882_ & '\uff00') >> 8;
      int k = (p_144882_ & 255) >> 0;
      int l = (int)((float)i * p_144883_);
      int i1 = (int)((float)j * p_144884_);
      int j1 = (int)((float)k * p_144885_);
      return p_144882_ & -16777216 | l << 16 | i1 << 8 | j1;
   }

   public static float frac(float pNumber) {
      return pNumber - (float)floor(pNumber);
   }

   /**
    * Gets the decimal portion of the given double. For instance, {@code frac(5.5)} returns {@code .5}.
    */
   public static double frac(double pNumber) {
      return pNumber - (double)lfloor(pNumber);
   }

   public static Vec3 catmullRomSplinePos(Vec3 p_144893_, Vec3 p_144894_, Vec3 p_144895_, Vec3 p_144896_, double p_144897_) {
      double d0 = ((-p_144897_ + 2.0D) * p_144897_ - 1.0D) * p_144897_ * 0.5D;
      double d1 = ((3.0D * p_144897_ - 5.0D) * p_144897_ * p_144897_ + 2.0D) * 0.5D;
      double d2 = ((-3.0D * p_144897_ + 4.0D) * p_144897_ + 1.0D) * p_144897_ * 0.5D;
      double d3 = (p_144897_ - 1.0D) * p_144897_ * p_144897_ * 0.5D;
      return new Vec3(p_144893_.x * d0 + p_144894_.x * d1 + p_144895_.x * d2 + p_144896_.x * d3, p_144893_.y * d0 + p_144894_.y * d1 + p_144895_.y * d2 + p_144896_.y * d3, p_144893_.z * d0 + p_144894_.z * d1 + p_144895_.z * d2 + p_144896_.z * d3);
   }

   public static long getSeed(Vec3i pPos) {
      return getSeed(pPos.getX(), pPos.getY(), pPos.getZ());
   }

   public static long getSeed(int pX, int pY, int pZ) {
      long i = (long)(pX * 3129871) ^ (long)pZ * 116129781L ^ (long)pY;
      i = i * i * 42317861L + i * 11L;
      return i >> 16;
   }

   public static UUID createInsecureUUID(Random pRand) {
      long i = pRand.nextLong() & -61441L | 16384L;
      long j = pRand.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
      return new UUID(i, j);
   }

   /**
    * Generates a random UUID using the shared random
    */
   public static UUID createInsecureUUID() {
      return createInsecureUUID(RANDOM);
   }

   public static double inverseLerp(double p_14113_, double p_14114_, double p_14115_) {
      return (p_14113_ - p_14114_) / (p_14115_ - p_14114_);
   }

   public static float inverseLerp(float p_184656_, float p_184657_, float p_184658_) {
      return (p_184656_ - p_184657_) / (p_184658_ - p_184657_);
   }

   public static boolean rayIntersectsAABB(Vec3 p_144889_, Vec3 p_144890_, AABB p_144891_) {
      double d0 = (p_144891_.minX + p_144891_.maxX) * 0.5D;
      double d1 = (p_144891_.maxX - p_144891_.minX) * 0.5D;
      double d2 = p_144889_.x - d0;
      if (Math.abs(d2) > d1 && d2 * p_144890_.x >= 0.0D) {
         return false;
      } else {
         double d3 = (p_144891_.minY + p_144891_.maxY) * 0.5D;
         double d4 = (p_144891_.maxY - p_144891_.minY) * 0.5D;
         double d5 = p_144889_.y - d3;
         if (Math.abs(d5) > d4 && d5 * p_144890_.y >= 0.0D) {
            return false;
         } else {
            double d6 = (p_144891_.minZ + p_144891_.maxZ) * 0.5D;
            double d7 = (p_144891_.maxZ - p_144891_.minZ) * 0.5D;
            double d8 = p_144889_.z - d6;
            if (Math.abs(d8) > d7 && d8 * p_144890_.z >= 0.0D) {
               return false;
            } else {
               double d9 = Math.abs(p_144890_.x);
               double d10 = Math.abs(p_144890_.y);
               double d11 = Math.abs(p_144890_.z);
               double d12 = p_144890_.y * d8 - p_144890_.z * d5;
               if (Math.abs(d12) > d4 * d11 + d7 * d10) {
                  return false;
               } else {
                  d12 = p_144890_.z * d2 - p_144890_.x * d8;
                  if (Math.abs(d12) > d1 * d11 + d7 * d9) {
                     return false;
                  } else {
                     d12 = p_144890_.x * d5 - p_144890_.y * d2;
                     return Math.abs(d12) < d1 * d10 + d4 * d9;
                  }
               }
            }
         }
      }
   }

   public static double atan2(double p_14137_, double p_14138_) {
      double d0 = p_14138_ * p_14138_ + p_14137_ * p_14137_;
      if (Double.isNaN(d0)) {
         return Double.NaN;
      } else {
         boolean flag = p_14137_ < 0.0D;
         if (flag) {
            p_14137_ = -p_14137_;
         }

         boolean flag1 = p_14138_ < 0.0D;
         if (flag1) {
            p_14138_ = -p_14138_;
         }

         boolean flag2 = p_14137_ > p_14138_;
         if (flag2) {
            double d1 = p_14138_;
            p_14138_ = p_14137_;
            p_14137_ = d1;
         }

         double d9 = fastInvSqrt(d0);
         p_14138_ *= d9;
         p_14137_ *= d9;
         double d2 = FRAC_BIAS + p_14137_;
         int i = (int)Double.doubleToRawLongBits(d2);
         double d3 = ASIN_TAB[i];
         double d4 = COS_TAB[i];
         double d5 = d2 - FRAC_BIAS;
         double d6 = p_14137_ * d4 - p_14138_ * d5;
         double d7 = (6.0D + d6 * d6) * d6 * 0.16666666666666666D;
         double d8 = d3 + d7;
         if (flag2) {
            d8 = (Math.PI / 2D) - d8;
         }

         if (flag1) {
            d8 = Math.PI - d8;
         }

         if (flag) {
            d8 = -d8;
         }

         return d8;
      }
   }

   public static float fastInvSqrt(float pNumber) {
      float f = 0.5F * pNumber;
      int i = Float.floatToIntBits(pNumber);
      i = 1597463007 - (i >> 1);
      pNumber = Float.intBitsToFloat(i);
      return pNumber * (1.5F - f * pNumber * pNumber);
   }

   /**
    * Computes 1/sqrt(n) using <a href="https://en.wikipedia.org/wiki/Fast_inverse_square_root">the fast inverse square
    * root</a> with a constant of 0x5FE6EB50C7B537AA.
    */
   public static double fastInvSqrt(double pNumber) {
      double d0 = 0.5D * pNumber;
      long i = Double.doubleToRawLongBits(pNumber);
      i = 6910469410427058090L - (i >> 1);
      pNumber = Double.longBitsToDouble(i);
      return pNumber * (1.5D - d0 * pNumber * pNumber);
   }

   public static float fastInvCubeRoot(float pNumber) {
      int i = Float.floatToIntBits(pNumber);
      i = 1419967116 - i / 3;
      float f = Float.intBitsToFloat(i);
      f = 0.6666667F * f + 1.0F / (3.0F * f * f * pNumber);
      return 0.6666667F * f + 1.0F / (3.0F * f * f * pNumber);
   }

   public static int hsvToRgb(float pHue, float pSaturation, float pValue) {
      int i = (int)(pHue * 6.0F) % 6;
      float f = pHue * 6.0F - (float)i;
      float f1 = pValue * (1.0F - pSaturation);
      float f2 = pValue * (1.0F - f * pSaturation);
      float f3 = pValue * (1.0F - (1.0F - f) * pSaturation);
      float f4;
      float f5;
      float f6;
      switch(i) {
      case 0:
         f4 = pValue;
         f5 = f3;
         f6 = f1;
         break;
      case 1:
         f4 = f2;
         f5 = pValue;
         f6 = f1;
         break;
      case 2:
         f4 = f1;
         f5 = pValue;
         f6 = f3;
         break;
      case 3:
         f4 = f1;
         f5 = f2;
         f6 = pValue;
         break;
      case 4:
         f4 = f3;
         f5 = f1;
         f6 = pValue;
         break;
      case 5:
         f4 = pValue;
         f5 = f1;
         f6 = f2;
         break;
      default:
         throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + pHue + ", " + pSaturation + ", " + pValue);
      }

      int j = clamp((int)(f4 * 255.0F), 0, 255);
      int k = clamp((int)(f5 * 255.0F), 0, 255);
      int l = clamp((int)(f6 * 255.0F), 0, 255);
      return j << 16 | k << 8 | l;
   }

   public static int murmurHash3Mixer(int p_14184_) {
      p_14184_ ^= p_14184_ >>> 16;
      p_14184_ *= -2048144789;
      p_14184_ ^= p_14184_ >>> 13;
      p_14184_ *= -1028477387;
      return p_14184_ ^ p_14184_ >>> 16;
   }

   public static long murmurHash3Mixer(long p_144887_) {
      p_144887_ ^= p_144887_ >>> 33;
      p_144887_ *= -49064778989728563L;
      p_144887_ ^= p_144887_ >>> 33;
      p_144887_ *= -4265267296055464877L;
      return p_144887_ ^ p_144887_ >>> 33;
   }

   public static double[] cumulativeSum(double... p_144913_) {
      float f = 0.0F;

      for(double d0 : p_144913_) {
         f = (float)((double)f + d0);
      }

      for(int i = 0; i < p_144913_.length; ++i) {
         p_144913_[i] /= (double)f;
      }

      for(int j = 0; j < p_144913_.length; ++j) {
         p_144913_[j] += j == 0 ? 0.0D : p_144913_[j - 1];
      }

      return p_144913_;
   }

   public static int getRandomForDistributionIntegral(Random p_144910_, double[] p_144911_) {
      double d0 = p_144910_.nextDouble();

      for(int i = 0; i < p_144911_.length; ++i) {
         if (d0 < p_144911_[i]) {
            return i;
         }
      }

      return p_144911_.length;
   }

   public static double[] binNormalDistribution(double p_144867_, double p_144868_, double p_144869_, int p_144870_, int p_144871_) {
      double[] adouble = new double[p_144871_ - p_144870_ + 1];
      int i = 0;

      for(int j = p_144870_; j <= p_144871_; ++j) {
         adouble[i] = Math.max(0.0D, p_144867_ * StrictMath.exp(-((double)j - p_144869_) * ((double)j - p_144869_) / (2.0D * p_144868_ * p_144868_)));
         ++i;
      }

      return adouble;
   }

   public static double[] binBiModalNormalDistribution(double p_144858_, double p_144859_, double p_144860_, double p_144861_, double p_144862_, double p_144863_, int p_144864_, int p_144865_) {
      double[] adouble = new double[p_144865_ - p_144864_ + 1];
      int i = 0;

      for(int j = p_144864_; j <= p_144865_; ++j) {
         adouble[i] = Math.max(0.0D, p_144858_ * StrictMath.exp(-((double)j - p_144860_) * ((double)j - p_144860_) / (2.0D * p_144859_ * p_144859_)) + p_144861_ * StrictMath.exp(-((double)j - p_144863_) * ((double)j - p_144863_) / (2.0D * p_144862_ * p_144862_)));
         ++i;
      }

      return adouble;
   }

   public static double[] binLogDistribution(double p_144873_, double p_144874_, int p_144875_, int p_144876_) {
      double[] adouble = new double[p_144876_ - p_144875_ + 1];
      int i = 0;

      for(int j = p_144875_; j <= p_144876_; ++j) {
         adouble[i] = Math.max(p_144873_ * StrictMath.log((double)j) + p_144874_, 0.0D);
         ++i;
      }

      return adouble;
   }

   public static int binarySearch(int pMin, int pMax, IntPredicate pIsTargetBeforeOrAt) {
      int i = pMax - pMin;

      while(i > 0) {
         int j = i / 2;
         int k = pMin + j;
         if (pIsTargetBeforeOrAt.test(k)) {
            i = j;
         } else {
            pMin = k + 1;
            i -= j + 1;
         }
      }

      return pMin;
   }

   /**
    * Method for linear interpolation of floats
    * @param pDelta A value usually between 0 and 1 that indicates the percentage of the lerp. (0 will give the start
    * value and 1 will give the end value)
    * @param pStart Start value for the lerp
    * @param pEnd End value for the lerp
    */
   public static float lerp(float pDelta, float pStart, float pEnd) {
      return pStart + pDelta * (pEnd - pStart);
   }

   /**
    * Method for linear interpolation of doubles
    * @param pDelta A value usually between 0 and 1 that indicates the percentage of the lerp. (0 will give the start
    * value and 1 will give the end value)
    * @param pStart Start value for the lerp
    * @param pEnd End value for the lerp
    */
   public static double lerp(double pDelta, double pStart, double pEnd) {
      return pStart + pDelta * (pEnd - pStart);
   }

   public static double lerp2(double p_14013_, double p_14014_, double p_14015_, double p_14016_, double p_14017_, double p_14018_) {
      return lerp(p_14014_, lerp(p_14013_, p_14015_, p_14016_), lerp(p_14013_, p_14017_, p_14018_));
   }

   public static double lerp3(double p_14020_, double p_14021_, double p_14022_, double p_14023_, double p_14024_, double p_14025_, double p_14026_, double p_14027_, double p_14028_, double p_14029_, double p_14030_) {
      return lerp(p_14022_, lerp2(p_14020_, p_14021_, p_14023_, p_14024_, p_14025_, p_14026_), lerp2(p_14020_, p_14021_, p_14027_, p_14028_, p_14029_, p_14030_));
   }

   public static double smoothstep(double p_14198_) {
      return p_14198_ * p_14198_ * p_14198_ * (p_14198_ * (p_14198_ * 6.0D - 15.0D) + 10.0D);
   }

   public static double smoothstepDerivative(double p_144947_) {
      return 30.0D * p_144947_ * p_144947_ * (p_144947_ - 1.0D) * (p_144947_ - 1.0D);
   }

   public static int sign(double pX) {
      if (pX == 0.0D) {
         return 0;
      } else {
         return pX > 0.0D ? 1 : -1;
      }
   }

   public static float rotLerp(float p_14190_, float p_14191_, float p_14192_) {
      return p_14191_ + p_14190_ * wrapDegrees(p_14192_ - p_14191_);
   }

   public static float diffuseLight(float p_144949_, float p_144950_, float p_144951_) {
      return Math.min(p_144949_ * p_144949_ * 0.6F + p_144950_ * p_144950_ * ((3.0F + p_144950_) / 4.0F) + p_144951_ * p_144951_ * 0.8F, 1.0F);
   }

   /** @deprecated */
   @Deprecated
   public static float rotlerp(float p_14202_, float p_14203_, float p_14204_) {
      float f;
      for(f = p_14203_ - p_14202_; f < -180.0F; f += 360.0F) {
      }

      while(f >= 180.0F) {
         f -= 360.0F;
      }

      return p_14202_ + p_14204_ * f;
   }

   /** @deprecated */
   @Deprecated
   public static float rotWrap(double p_14210_) {
      while(p_14210_ >= 180.0D) {
         p_14210_ -= 360.0D;
      }

      while(p_14210_ < -180.0D) {
         p_14210_ += 360.0D;
      }

      return (float)p_14210_;
   }

   public static float triangleWave(float p_14157_, float p_14158_) {
      return (Math.abs(p_14157_ % p_14158_ - p_14158_ * 0.5F) - p_14158_ * 0.25F) / (p_14158_ * 0.25F);
   }

   public static float square(float pValue) {
      return pValue * pValue;
   }

   public static double square(double p_144953_) {
      return p_144953_ * p_144953_;
   }

   public static int square(int p_144945_) {
      return p_144945_ * p_144945_;
   }

   public static long square(long p_184644_) {
      return p_184644_ * p_184644_;
   }

   public static double clampedMap(double p_144852_, double p_144853_, double p_144854_, double p_144855_, double p_144856_) {
      return clampedLerp(p_144855_, p_144856_, inverseLerp(p_144852_, p_144853_, p_144854_));
   }

   public static float clampedMap(float p_184632_, float p_184633_, float p_184634_, float p_184635_, float p_184636_) {
      return clampedLerp(p_184635_, p_184636_, inverseLerp(p_184632_, p_184633_, p_184634_));
   }

   public static double map(double p_144915_, double p_144916_, double p_144917_, double p_144918_, double p_144919_) {
      return lerp(inverseLerp(p_144915_, p_144916_, p_144917_), p_144918_, p_144919_);
   }

   public static float map(float p_184638_, float p_184639_, float p_184640_, float p_184641_, float p_184642_) {
      return lerp(inverseLerp(p_184638_, p_184639_, p_184640_), p_184641_, p_184642_);
   }

   public static double wobble(double p_144955_) {
      return p_144955_ + (2.0D * (new Random((long)floor(p_144955_ * 3000.0D))).nextDouble() - 1.0D) * 1.0E-7D / 2.0D;
   }

   public static int roundToward(int p_144942_, int p_144943_) {
      return positiveCeilDiv(p_144942_, p_144943_) * p_144943_;
   }

   public static int positiveCeilDiv(int p_184653_, int p_184654_) {
      return -Math.floorDiv(-p_184653_, p_184654_);
   }

   public static int randomBetweenInclusive(Random p_144929_, int p_144930_, int p_144931_) {
      return p_144929_.nextInt(p_144931_ - p_144930_ + 1) + p_144930_;
   }

   public static float randomBetween(Random p_144925_, float p_144926_, float p_144927_) {
      return p_144925_.nextFloat() * (p_144927_ - p_144926_) + p_144926_;
   }

   public static float normal(Random p_144936_, float p_144937_, float p_144938_) {
      return p_144937_ + (float)p_144936_.nextGaussian() * p_144938_;
   }

   public static double length(double p_184646_, double p_184647_) {
      return Math.sqrt(p_184646_ * p_184646_ + p_184647_ * p_184647_);
   }

   public static double length(double p_184649_, double p_184650_, double p_184651_) {
      return Math.sqrt(p_184649_ * p_184649_ + p_184650_ * p_184650_ + p_184651_ * p_184651_);
   }

   public static int quantize(double p_184629_, int p_184630_) {
      return floor(p_184629_ / (double)p_184630_) * p_184630_;
   }

   static {
      for(int i = 0; i < 257; ++i) {
         double d0 = (double)i / 256.0D;
         double d1 = Math.asin(d0);
         COS_TAB[i] = Math.cos(d1);
         ASIN_TAB[i] = d1;
      }

   }
}