package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface StringRepresentable {
   String getSerializedName();

   static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(Supplier<E[]> pElementSupplier, Function<String, E> pNamingFunction) {
      E[] ae = pElementSupplier.get();
      return ExtraCodecs.orCompressed(ExtraCodecs.stringResolverCodec((p_184753_) -> {
         return p_184753_.getSerializedName();
      }, pNamingFunction), ExtraCodecs.idResolverCodec((p_184748_) -> {
         return p_184748_.ordinal();
      }, (p_184751_) -> {
         return (E)(p_184751_ >= 0 && p_184751_ < ae.length ? ae[p_184751_] : null);
      }, -1));
   }

   static Keyable keys(final StringRepresentable[] pSerializables) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> p_184758_) {
            return Arrays.stream(pSerializables).map(StringRepresentable::getSerializedName).map(p_184758_::createString);
         }
      };
   }
}