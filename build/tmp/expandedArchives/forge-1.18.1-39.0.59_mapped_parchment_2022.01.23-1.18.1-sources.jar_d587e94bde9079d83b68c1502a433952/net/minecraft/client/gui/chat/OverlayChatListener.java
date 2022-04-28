package net.minecraft.client.gui.chat;

import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayChatListener implements ChatListener {
   private final Minecraft minecraft;

   public OverlayChatListener(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   /**
    * Called whenever this listener receives a chat message, if this listener is registered to the given type in {@link
    * net.minecraft.client.gui.GuiIngame#chatListeners chatListeners}
    */
   public void handle(ChatType pChatType, Component pMessage, UUID pSender) {
      this.minecraft.gui.setOverlayMessage(pMessage, false);
   }
}