package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;

/**
 * A codec that wraps a single element, or "file", within a registry. Possibly allows inline definitions, and always
 * falls back to the element codec (and thus writing the registry element inline) if it fails to decode from the
 * registry.
 */
public final class RegistryFileCodec<E> implements Codec<Supplier<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<E> elementCodec;
   private final boolean allowInline;

   /**
    * Creates a codec for a single registry element, which is held as an un-resolved {@code Supplier<E>}. Both inline
    * definitions of the object, and references to an existing registry element id are allowed.
    * @param pRegistryKey The registry which elements may belong to.
    * @param pElementCodec The codec used to decode either inline definitions, or elements before entering them into the
    * registry.
    */
   public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      return create(pRegistryKey, pElementCodec, true);
   }

   /**
    * Creates a codec of registry elements, represented as un-resolved {@code Supplier<E>}s. This list can consist of
    * either:
    * <ol>
    * <li>A list of registry element ids, which are resolved by the registry ops and element codec into registered
    * elements.</li>
    * <li>A list of inline definitions of registry elements, which are not registered.</li>
    * </ol>
    * Due to a deficiency of {@link com.mojang.serialization.codecs.EitherCodec}, when the first fails to resolve a
    * single element, this will instead fallback to trying to interpret the list as a list of inline definitions. And
    * will <strong>not report the earlier error</strong>, instead reporting that the list consists of elements that are
    * "Not a JSON object".
    * @param pRegistryKey The registry which elements may belong to.
    * @param pElementCodec The codec used to decode either inline definitions, or elements before entering them into the
    * registry.
    */
   public static <E> Codec<List<Supplier<E>>> homogeneousList(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      return Codec.either(create(pRegistryKey, pElementCodec, false).listOf(), pElementCodec.<Supplier<E>>xmap((p_135604_) -> {
         return () -> {
            return p_135604_;
         };
      }, Supplier::get).listOf()).xmap((p_135578_) -> {
         return p_135578_.map((p_179856_) -> {
            return p_179856_;
         }, (p_179852_) -> {
            return p_179852_;
         });
      }, Either::left);
   }

   private static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
      return new RegistryFileCodec<>(pRegistryKey, pElementCodec, pAllowInline);
   }

   private RegistryFileCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
      this.registryKey = pRegistryKey;
      this.elementCodec = pElementCodec;
      this.allowInline = pAllowInline;
   }

   public <T> DataResult<T> encode(Supplier<E> pInput, DynamicOps<T> pOps, T pPrefix) {
      return pOps instanceof RegistryWriteOps ? ((RegistryWriteOps)pOps).encode(pInput.get(), pPrefix, this.registryKey, this.elementCodec) : this.elementCodec.encode(pInput.get(), pOps, pPrefix);
   }

   public <T> DataResult<Pair<Supplier<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
      return pOps instanceof RegistryReadOps ? ((RegistryReadOps)pOps).decodeElement(pInput, this.registryKey, this.elementCodec, this.allowInline) : this.elementCodec.decode(pOps, pInput).map((p_135580_) -> {
         return p_135580_.mapFirst((p_179850_) -> {
            return () -> {
               return p_179850_;
            };
         });
      });
   }

   public String toString() {
      return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
   }
}