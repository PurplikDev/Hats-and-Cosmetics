package net.minecraft.resources;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Registry;

/**
 * A codec that provides a registry, by key to the consumer, assuming the ops used is a {@code
 * net.minecraft.resources.RegistryReadOps}.
 * No data is encoded or decoded, rather, the registry is looked up in the ops's {@code registryAccess}.
 * This provides a read-only view of a registry, and can reference registries owned by the registry access or not.
 */
public final class RegistryLookupCodec<E> extends MapCodec<Registry<E>> {
   private final ResourceKey<? extends Registry<E>> registryKey;

   public static <E> RegistryLookupCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey) {
      return new RegistryLookupCodec<>(pRegistryKey);
   }

   private RegistryLookupCodec(ResourceKey<? extends Registry<E>> pRegistryKey) {
      this.registryKey = pRegistryKey;
   }

   public <T> RecordBuilder<T> encode(Registry<E> p_135619_, DynamicOps<T> p_135620_, RecordBuilder<T> p_135621_) {
      return p_135621_;
   }

   public <T> DataResult<Registry<E>> decode(DynamicOps<T> pOps, MapLike<T> pInput) {
      return pOps instanceof RegistryReadOps ? ((RegistryReadOps)pOps).registry(this.registryKey) : DataResult.error("Not a registry ops");
   }

   public String toString() {
      return "RegistryLookupCodec[" + this.registryKey + "]";
   }

   public <T> Stream<T> keys(DynamicOps<T> pOps) {
      return Stream.empty();
   }
}