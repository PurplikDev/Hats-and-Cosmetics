package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public record ClientboundLoginPacket(int playerId, boolean hardcore, GameType gameType, @Nullable GameType previousGameType, Set<ResourceKey<Level>> levels, RegistryAccess.Frozen registryHolder, Holder<DimensionType> dimensionType, ResourceKey<Level> dimension, long seed, int maxPlayers, int chunkRadius, int simulationDistance, boolean reducedDebugInfo, boolean showDeathScreen, boolean isDebug, boolean isFlat) implements Packet<ClientGamePacketListener> {
   public ClientboundLoginPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readInt(), pBuffer.readBoolean(), GameType.byId(pBuffer.readByte()), GameType.byNullableId(pBuffer.readByte()), pBuffer.readCollection(Sets::newHashSetWithExpectedSize, (p_178965_) -> {
         return ResourceKey.create(Registry.DIMENSION_REGISTRY, p_178965_.readResourceLocation());
      }), pBuffer.readWithCodec(RegistryAccess.NETWORK_CODEC).freeze(), pBuffer.readWithCodec(DimensionType.CODEC), ResourceKey.create(Registry.DIMENSION_REGISTRY, pBuffer.readResourceLocation()), pBuffer.readLong(), pBuffer.readVarInt(), pBuffer.readVarInt(), pBuffer.readVarInt(), pBuffer.readBoolean(), pBuffer.readBoolean(), pBuffer.readBoolean(), pBuffer.readBoolean());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeInt(this.playerId);
      pBuffer.writeBoolean(this.hardcore);
      pBuffer.writeByte(this.gameType.getId());
      pBuffer.writeByte(GameType.getNullableId(this.previousGameType));
      pBuffer.writeCollection(this.levels, (p_178962_, p_178963_) -> {
         p_178962_.writeResourceLocation(p_178963_.location());
      });
      pBuffer.writeWithCodec(RegistryAccess.NETWORK_CODEC, this.registryHolder);
      pBuffer.writeWithCodec(DimensionType.CODEC, this.dimensionType);
      pBuffer.writeResourceLocation(this.dimension.location());
      pBuffer.writeLong(this.seed);
      pBuffer.writeVarInt(this.maxPlayers);
      pBuffer.writeVarInt(this.chunkRadius);
      pBuffer.writeVarInt(this.simulationDistance);
      pBuffer.writeBoolean(this.reducedDebugInfo);
      pBuffer.writeBoolean(this.showDeathScreen);
      pBuffer.writeBoolean(this.isDebug);
      pBuffer.writeBoolean(this.isFlat);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleLogin(this);
   }
}