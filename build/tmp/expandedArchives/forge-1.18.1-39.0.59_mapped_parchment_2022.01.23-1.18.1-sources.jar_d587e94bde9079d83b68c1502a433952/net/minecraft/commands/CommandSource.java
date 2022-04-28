package net.minecraft.commands;

import java.util.UUID;
import net.minecraft.network.chat.Component;

public interface CommandSource {
   /** A {@code CommandSource} that ignores all messages. */
   CommandSource NULL = new CommandSource() {
      /**
       * Send a chat message to the CommandSender
       */
      public void sendMessage(Component p_80172_, UUID p_80173_) {
      }

      public boolean acceptsSuccess() {
         return false;
      }

      public boolean acceptsFailure() {
         return false;
      }

      public boolean shouldInformAdmins() {
         return false;
      }
   };

   /**
    * Send a chat message to the CommandSender
    */
   void sendMessage(Component pComponent, UUID pSenderUUID);

   boolean acceptsSuccess();

   boolean acceptsFailure();

   boolean shouldInformAdmins();

   default boolean alwaysAccepts() {
      return false;
   }
}