package net.minecraft.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;

/**
 * A {@link DelegatingOps} which uses a backing {@link RegistryAccess} to write elements.
 */
public class RegistryWriteOps<T> extends DelegatingOps<T> {
   private final RegistryAccess registryAccess;

   public static <T> RegistryWriteOps<T> create(DynamicOps<T> pDelegate, RegistryAccess pRegistryAccess) {
      return new RegistryWriteOps<>(pDelegate, pRegistryAccess);
   }

   private RegistryWriteOps(DynamicOps<T> pDelegate, RegistryAccess pRegistryAccess) {
      super(pDelegate);
      this.registryAccess = pRegistryAccess;
   }

   /**
    * Encodes an element of a given registry.
    * Since the registry key needs to be provided in order to know what registry to encode the element into, this cannot
    * override the {@code encode} method in {@link DelegatingOps}.
    * Instead, callers that use this ops (such as {@link net.minecraft.resources.RegistryFileCodec}) are forced to
    * {@code instanceof} check if the ops is a {@code RegistryWriteOps} and call the specialized {@code encode} method
    * instead.
    * @param pElement The object to encode, optionally, an element in a registry
    * @param pRegistryKey The registry in which the element may be found.
    * @param pElementCodec A direct codec to serialize an element. If the registry key does not exist in the held {@link
    * #registryAccess}, or the element does not exist in the registry, this will be used as the fallback encoder.
    */
   protected <E> DataResult<T> encode(E pElement, T pPrefix, ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      Optional<? extends Registry<E>> optional = this.registryAccess.ownedRegistry(pRegistryKey);
      if (optional.isPresent()) {
         Registry<E> registry = optional.get();
         Optional<ResourceKey<E>> optional1 = registry.getResourceKey(pElement);
         if (optional1.isPresent()) {
            ResourceKey<E> resourcekey = optional1.get();
            return ResourceLocation.CODEC.encode(resourcekey.location(), this.delegate, pPrefix);
         }
      }

      return pElementCodec.encode(pElement, this, pPrefix);
   }
}