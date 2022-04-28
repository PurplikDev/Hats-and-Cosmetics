package net.minecraft.network.protocol.game;

import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagCollection;

public class ClientboundUpdateTagsPacket implements Packet<ClientGamePacketListener> {
   private final Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> tags;

   public ClientboundUpdateTagsPacket(Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> pTags) {
      this.tags = pTags;
   }

   public ClientboundUpdateTagsPacket(FriendlyByteBuf pBuffer) {
      this.tags = pBuffer.readMap((p_179484_) -> {
         return ResourceKey.createRegistryKey(p_179484_.readResourceLocation());
      }, TagCollection.NetworkPayload::read);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeMap(this.tags, (p_179480_, p_179481_) -> {
         p_179480_.writeResourceLocation(p_179481_.location());
      }, (p_179477_, p_179478_) -> {
         p_179478_.write(p_179477_);
      });
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleUpdateTags(this);
   }

   public Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> getTags() {
      return this.tags;
   }
}