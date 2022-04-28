package net.minecraft.client;

import com.mojang.authlib.GameProfile;
import com.mojang.util.UUIDTypeAdapter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class User {
   private final String name;
   private final String uuid;
   private final String accessToken;
   private final Optional<String> xuid;
   private final Optional<String> clientId;
   private final User.Type type;
   /** Forge: Cache of the local session's GameProfile properties. */
   private com.mojang.authlib.properties.PropertyMap properties;

   public User(String pName, String pUuid, String pAccessToken, Optional<String> pXuid, Optional<String> pClientId, User.Type pType) {
      if (pName == null || pName.isEmpty()) {
         pName = "MissingName";
         pUuid = pAccessToken = "NotValid";
         org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(getClass().getName());
         logger.warn("=========================================================");
         logger.warn("WARNING!! the username was not set for this session, typically");
         logger.warn("this means you installed Forge incorrectly. We have set your");
         logger.warn("name to \"MissingName\" and your session to nothing. Please");
         logger.warn("check your installation and post a console log from the launcher");
         logger.warn("when asking for help!");
         logger.warn("=========================================================");
      }
      this.name = pName;
      this.uuid = pUuid;
      this.accessToken = pAccessToken;
      this.xuid = pXuid;
      this.clientId = pClientId;
      this.type = pType;
   }

   public String getSessionId() {
      return "token:" + this.accessToken + ":" + this.uuid;
   }

   public String getUuid() {
      return this.uuid;
   }

   public String getName() {
      return this.name;
   }

   public String getAccessToken() {
      return this.accessToken;
   }

   public Optional<String> getClientId() {
      return this.clientId;
   }

   public Optional<String> getXuid() {
      return this.xuid;
   }

   public GameProfile getGameProfile() {
      try {
         UUID uuid = UUIDTypeAdapter.fromString(this.getUuid());
         GameProfile ret = new GameProfile(uuid, this.getName());    //Forge: Adds cached GameProfile properties to returned GameProfile.
         if (properties != null) ret.getProperties().putAll(properties); // Helps to cut down on calls to the session service,
         return ret;                                                     // which helps to fix MC-52974.
      } catch (IllegalArgumentException illegalargumentexception) {
         return new GameProfile((UUID)null, this.getName());
      }
   }

   //For internal use only. Modders should never need to use this.
   public void setProperties(com.mojang.authlib.properties.PropertyMap properties) {
       if (this.properties == null)
           this.properties = properties;
   }

   public boolean hasCachedProperties() {
       return properties != null;
   }

   public User.Type getType() {
      return this.type;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      LEGACY("legacy"),
      MOJANG("mojang"),
      MSA("msa");

      private static final Map<String, User.Type> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap((p_92560_) -> {
         return p_92560_.name;
      }, Function.identity()));
      private final String name;

      private Type(String p_92558_) {
         this.name = p_92558_;
      }

      @Nullable
      public static User.Type byName(String pTypeName) {
         return BY_NAME.get(pTypeName.toLowerCase(Locale.ROOT));
      }

      public String getName() {
         return this.name;
      }
   }
}
