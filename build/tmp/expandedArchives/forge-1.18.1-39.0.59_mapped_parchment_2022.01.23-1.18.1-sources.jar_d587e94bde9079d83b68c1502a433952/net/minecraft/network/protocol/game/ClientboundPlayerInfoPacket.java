package net.minecraft.network.protocol.game;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

public class ClientboundPlayerInfoPacket implements Packet<ClientGamePacketListener> {
   private final ClientboundPlayerInfoPacket.Action action;
   private final List<ClientboundPlayerInfoPacket.PlayerUpdate> entries;

   public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action pAction, ServerPlayer... pPlayers) {
      this.action = pAction;
      this.entries = Lists.newArrayListWithCapacity(pPlayers.length);

      for(ServerPlayer serverplayer : pPlayers) {
         this.entries.add(new ClientboundPlayerInfoPacket.PlayerUpdate(serverplayer.getGameProfile(), serverplayer.latency, serverplayer.gameMode.getGameModeForPlayer(), serverplayer.getTabListDisplayName()));
      }

   }

   public ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action pAction, Collection<ServerPlayer> pPlayers) {
      this.action = pAction;
      this.entries = Lists.newArrayListWithCapacity(pPlayers.size());

      for(ServerPlayer serverplayer : pPlayers) {
         this.entries.add(new ClientboundPlayerInfoPacket.PlayerUpdate(serverplayer.getGameProfile(), serverplayer.latency, serverplayer.gameMode.getGameModeForPlayer(), serverplayer.getTabListDisplayName()));
      }

   }

   public ClientboundPlayerInfoPacket(FriendlyByteBuf pBuffer) {
      this.action = pBuffer.readEnum(ClientboundPlayerInfoPacket.Action.class);
      this.entries = pBuffer.readList(this.action::read);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeEnum(this.action);
      pBuffer.writeCollection(this.entries, this.action::write);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handlePlayerInfo(this);
   }

   public List<ClientboundPlayerInfoPacket.PlayerUpdate> getEntries() {
      return this.entries;
   }

   public ClientboundPlayerInfoPacket.Action getAction() {
      return this.action;
   }

   @Nullable
   static Component readDisplayName(FriendlyByteBuf pBuffer) {
      return pBuffer.readBoolean() ? pBuffer.readComponent() : null;
   }

   static void writeDisplayName(FriendlyByteBuf pBuffer, @Nullable Component pDisplayName) {
      if (pDisplayName == null) {
         pBuffer.writeBoolean(false);
      } else {
         pBuffer.writeBoolean(true);
         pBuffer.writeComponent(pDisplayName);
      }

   }

   public String toString() {
      return MoreObjects.toStringHelper(this).add("action", this.action).add("entries", this.entries).toString();
   }

   public static enum Action {
      ADD_PLAYER {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179101_) {
            GameProfile gameprofile = new GameProfile(p_179101_.readUUID(), p_179101_.readUtf(16));
            PropertyMap propertymap = gameprofile.getProperties();
            p_179101_.readWithCount((p_179099_) -> {
               String s = p_179099_.readUtf();
               String s1 = p_179099_.readUtf();
               if (p_179099_.readBoolean()) {
                  String s2 = p_179099_.readUtf();
                  propertymap.put(s, new Property(s, s1, s2));
               } else {
                  propertymap.put(s, new Property(s, s1));
               }

            });
            GameType gametype = GameType.byId(p_179101_.readVarInt());
            int i = p_179101_.readVarInt();
            Component component = ClientboundPlayerInfoPacket.readDisplayName(p_179101_);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, i, gametype, component);
         }

         protected void write(FriendlyByteBuf p_179106_, ClientboundPlayerInfoPacket.PlayerUpdate p_179107_) {
            p_179106_.writeUUID(p_179107_.getProfile().getId());
            p_179106_.writeUtf(p_179107_.getProfile().getName());
            p_179106_.writeCollection(p_179107_.getProfile().getProperties().values(), (p_179103_, p_179104_) -> {
               p_179103_.writeUtf(p_179104_.getName());
               p_179103_.writeUtf(p_179104_.getValue());
               if (p_179104_.hasSignature()) {
                  p_179103_.writeBoolean(true);
                  p_179103_.writeUtf(p_179104_.getSignature());
               } else {
                  p_179103_.writeBoolean(false);
               }

            });
            p_179106_.writeVarInt(p_179107_.getGameMode().getId());
            p_179106_.writeVarInt(p_179107_.getLatency());
            ClientboundPlayerInfoPacket.writeDisplayName(p_179106_, p_179107_.getDisplayName());
         }
      },
      UPDATE_GAME_MODE {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179112_) {
            GameProfile gameprofile = new GameProfile(p_179112_.readUUID(), (String)null);
            GameType gametype = GameType.byId(p_179112_.readVarInt());
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, gametype, (Component)null);
         }

         protected void write(FriendlyByteBuf p_179114_, ClientboundPlayerInfoPacket.PlayerUpdate p_179115_) {
            p_179114_.writeUUID(p_179115_.getProfile().getId());
            p_179114_.writeVarInt(p_179115_.getGameMode().getId());
         }
      },
      UPDATE_LATENCY {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179120_) {
            GameProfile gameprofile = new GameProfile(p_179120_.readUUID(), (String)null);
            int i = p_179120_.readVarInt();
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, i, (GameType)null, (Component)null);
         }

         protected void write(FriendlyByteBuf p_179122_, ClientboundPlayerInfoPacket.PlayerUpdate p_179123_) {
            p_179122_.writeUUID(p_179123_.getProfile().getId());
            p_179122_.writeVarInt(p_179123_.getLatency());
         }
      },
      UPDATE_DISPLAY_NAME {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179128_) {
            GameProfile gameprofile = new GameProfile(p_179128_.readUUID(), (String)null);
            Component component = ClientboundPlayerInfoPacket.readDisplayName(p_179128_);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, (GameType)null, component);
         }

         protected void write(FriendlyByteBuf p_179130_, ClientboundPlayerInfoPacket.PlayerUpdate p_179131_) {
            p_179130_.writeUUID(p_179131_.getProfile().getId());
            ClientboundPlayerInfoPacket.writeDisplayName(p_179130_, p_179131_.getDisplayName());
         }
      },
      REMOVE_PLAYER {
         protected ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf p_179136_) {
            GameProfile gameprofile = new GameProfile(p_179136_.readUUID(), (String)null);
            return new ClientboundPlayerInfoPacket.PlayerUpdate(gameprofile, 0, (GameType)null, (Component)null);
         }

         protected void write(FriendlyByteBuf p_179138_, ClientboundPlayerInfoPacket.PlayerUpdate p_179139_) {
            p_179138_.writeUUID(p_179139_.getProfile().getId());
         }
      };

      protected abstract ClientboundPlayerInfoPacket.PlayerUpdate read(FriendlyByteBuf pBuffer);

      protected abstract void write(FriendlyByteBuf pBuffer, ClientboundPlayerInfoPacket.PlayerUpdate pUpdateData);
   }

   public static class PlayerUpdate {
      private final int latency;
      private final GameType gameMode;
      private final GameProfile profile;
      @Nullable
      private final Component displayName;

      public PlayerUpdate(GameProfile pProfile, int pLatency, @Nullable GameType pGameMode, @Nullable Component pDisplayName) {
         this.profile = pProfile;
         this.latency = pLatency;
         this.gameMode = pGameMode;
         this.displayName = pDisplayName;
      }

      public GameProfile getProfile() {
         return this.profile;
      }

      public int getLatency() {
         return this.latency;
      }

      public GameType getGameMode() {
         return this.gameMode;
      }

      @Nullable
      public Component getDisplayName() {
         return this.displayName;
      }

      public String toString() {
         return MoreObjects.toStringHelper(this).add("latency", this.latency).add("gameMode", this.gameMode).add("profile", this.profile).add("displayName", this.displayName == null ? null : Component.Serializer.toJson(this.displayName)).toString();
      }
   }
}