package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VeryBiasedToBottomHeight extends HeightProvider {
   public static final Codec<VeryBiasedToBottomHeight> CODEC = RecordCodecBuilder.create((p_162057_) -> {
      return p_162057_.group(VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter((p_162070_) -> {
         return p_162070_.minInclusive;
      }), VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter((p_162068_) -> {
         return p_162068_.maxInclusive;
      }), Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter((p_162063_) -> {
         return p_162063_.inner;
      })).apply(p_162057_, VeryBiasedToBottomHeight::new);
   });
   private static final Logger LOGGER = LogManager.getLogger();
   private final VerticalAnchor minInclusive;
   private final VerticalAnchor maxInclusive;
   private final int inner;

   private VeryBiasedToBottomHeight(VerticalAnchor p_162052_, VerticalAnchor p_162053_, int p_162054_) {
      this.minInclusive = p_162052_;
      this.maxInclusive = p_162053_;
      this.inner = p_162054_;
   }

   public static VeryBiasedToBottomHeight of(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive, int pInner) {
      return new VeryBiasedToBottomHeight(pMinInclusive, pMaxInclusive, pInner);
   }

   public int sample(Random pRandom, WorldGenerationContext pContext) {
      int i = this.minInclusive.resolveY(pContext);
      int j = this.maxInclusive.resolveY(pContext);
      if (j - i - this.inner + 1 <= 0) {
         LOGGER.warn("Empty height range: {}", (Object)this);
         return i;
      } else {
         int k = Mth.nextInt(pRandom, i + this.inner, j);
         int l = Mth.nextInt(pRandom, i, k - 1);
         return Mth.nextInt(pRandom, i, l - 1 + this.inner);
      }
   }

   public HeightProviderType<?> getType() {
      return HeightProviderType.VERY_BIASED_TO_BOTTOM;
   }

   public String toString() {
      return "biased[" + this.minInclusive + "-" + this.maxInclusive + " inner: " + this.inner + "]";
   }
}