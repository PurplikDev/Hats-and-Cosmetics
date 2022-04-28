package net.minecraft.client.gui.chat;

import com.mojang.text2speech.Narrator;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class NarratorChatListener implements ChatListener {
   public static final Component NO_TITLE = TextComponent.EMPTY;
   private static final Logger LOGGER = LogManager.getLogger();
   public static final NarratorChatListener INSTANCE = new NarratorChatListener();
   private final Narrator narrator = Narrator.getNarrator();

   /**
    * Called whenever this listener receives a chat message, if this listener is registered to the given type in {@link
    * net.minecraft.client.gui.GuiIngame#chatListeners chatListeners}
    */
   public void handle(ChatType pChatType, Component pMessage, UUID pSender) {
      NarratorStatus narratorstatus = getStatus();
      if (narratorstatus != NarratorStatus.OFF) {
         if (!this.narrator.active()) {
            this.logNarratedMessage(pMessage.getString());
         } else {
            if (narratorstatus == NarratorStatus.ALL || narratorstatus == NarratorStatus.CHAT && pChatType == ChatType.CHAT || narratorstatus == NarratorStatus.SYSTEM && pChatType == ChatType.SYSTEM) {
               Component component;
               if (pMessage instanceof TranslatableComponent && "chat.type.text".equals(((TranslatableComponent)pMessage).getKey())) {
                  component = new TranslatableComponent("chat.type.text.narrate", ((TranslatableComponent)pMessage).getArgs());
               } else {
                  component = pMessage;
               }

               String s = component.getString();
               this.logNarratedMessage(s);
               this.narrator.say(s, pChatType.shouldInterrupt());
            }

         }
      }
   }

   public void sayNow(Component pMessage) {
      this.sayNow(pMessage.getString());
   }

   public void sayNow(String pMessage) {
      NarratorStatus narratorstatus = getStatus();
      if (narratorstatus != NarratorStatus.OFF && narratorstatus != NarratorStatus.CHAT && !pMessage.isEmpty()) {
         this.logNarratedMessage(pMessage);
         if (this.narrator.active()) {
            this.narrator.clear();
            this.narrator.say(pMessage, true);
         }
      }

   }

   private static NarratorStatus getStatus() {
      return Minecraft.getInstance().options.narratorStatus;
   }

   private void logNarratedMessage(String pMessage) {
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         LOGGER.debug("Narrating: {}", (Object)pMessage.replaceAll("\n", "\\\\n"));
      }

   }

   public void updateNarratorStatus(NarratorStatus pStatus) {
      this.clear();
      this.narrator.say((new TranslatableComponent("options.narrator")).append(" : ").append(pStatus.getName()).getString(), true);
      ToastComponent toastcomponent = Minecraft.getInstance().getToasts();
      if (this.narrator.active()) {
         if (pStatus == NarratorStatus.OFF) {
            SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastIds.NARRATOR_TOGGLE, new TranslatableComponent("narrator.toast.disabled"), (Component)null);
         } else {
            SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastIds.NARRATOR_TOGGLE, new TranslatableComponent("narrator.toast.enabled"), pStatus.getName());
         }
      } else {
         SystemToast.addOrUpdate(toastcomponent, SystemToast.SystemToastIds.NARRATOR_TOGGLE, new TranslatableComponent("narrator.toast.disabled"), new TranslatableComponent("options.narrator.notavailable"));
      }

   }

   public boolean isActive() {
      return this.narrator.active();
   }

   public void clear() {
      if (getStatus() != NarratorStatus.OFF && this.narrator.active()) {
         this.narrator.clear();
      }
   }

   public void destroy() {
      this.narrator.destroy();
   }
}