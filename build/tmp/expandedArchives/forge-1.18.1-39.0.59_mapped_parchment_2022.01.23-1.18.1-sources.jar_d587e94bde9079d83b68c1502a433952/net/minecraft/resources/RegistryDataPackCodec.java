package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;

public final class RegistryDataPackCodec<E> implements Codec<MappedRegistry<E>> {
   private final Codec<MappedRegistry<E>> directCodec;
   private final ResourceKey<? extends Registry<E>> registryKey;
   private final Codec<E> elementCodec;

   public static <E> RegistryDataPackCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Lifecycle pLifecycle, Codec<E> pElementCodec) {
      return new RegistryDataPackCodec<>(pRegistryKey, pLifecycle, pElementCodec);
   }

   private RegistryDataPackCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Lifecycle pLifecycle, Codec<E> pElementCodec) {
      this.directCodec = MappedRegistry.directCodec(pRegistryKey, pLifecycle, pElementCodec);
      this.registryKey = pRegistryKey;
      this.elementCodec = pElementCodec;
   }

   public <T> DataResult<T> encode(MappedRegistry<E> pInput, DynamicOps<T> pOps, T pPrefix) {
      return this.directCodec.encode(pInput, pOps, pPrefix);
   }

   public <T> DataResult<Pair<MappedRegistry<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
      DataResult<Pair<MappedRegistry<E>, T>> dataresult = this.directCodec.decode(pOps, pInput);
      return pOps instanceof RegistryReadOps ? dataresult.flatMap((p_135553_) -> {
         return ((RegistryReadOps)pOps).decodeElements(p_135553_.getFirst(), this.registryKey, this.elementCodec).map((p_179848_) -> {
            return Pair.of(p_179848_, (T)p_135553_.getSecond());
         });
      }) : dataresult;
   }

   public String toString() {
      return "RegistryDataPackCodec[" + this.directCodec + " " + this.registryKey + " " + this.elementCodec + "]";
   }
}