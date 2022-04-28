package net.minecraft.network.syncher;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Handles encoding and decoding of data for {@link SynchedEntityData}.
 * Note that mods cannot add new serializers, because this is not a managed registry and the serializer ID is limited to
 * 16.
 */
public interface EntityDataSerializer<T> {
   void write(FriendlyByteBuf pBuffer, T pValue);

   T read(FriendlyByteBuf pBuffer);

   default EntityDataAccessor<T> createAccessor(int pId) {
      return new EntityDataAccessor<>(pId, this);
   }

   T copy(T pValue);
}