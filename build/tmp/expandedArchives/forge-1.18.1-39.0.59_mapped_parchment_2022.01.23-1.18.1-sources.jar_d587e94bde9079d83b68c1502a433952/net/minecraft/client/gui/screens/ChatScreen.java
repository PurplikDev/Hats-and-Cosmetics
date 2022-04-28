package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatScreen extends Screen {
   public static final int MOUSE_SCROLL_SPEED = 7;
   private static final Component USAGE_TEXT = new TranslatableComponent("chat_screen.usage");
   private String historyBuffer = "";
   /**
    * keeps position of which chat message you will select when you press up, (does not increase for duplicated messages
    * sent immediately after each other)
    */
   private int historyPos = -1;
   /** Chat entry field */
   protected EditBox input;
   /** is the text that appears when you press the chat key and the input box appears pre-filled */
   private final String initial;
   CommandSuggestions commandSuggestions;

   public ChatScreen(String pInitial) {
      super(new TranslatableComponent("chat_screen.title"));
      this.initial = pInitial;
   }

   protected void init() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.historyPos = this.minecraft.gui.getChat().getRecentChat().size();
      this.input = new EditBox(this.font, 4, this.height - 12, this.width - 4, 12, new TranslatableComponent("chat.editBox")) {
         protected MutableComponent createNarrationMessage() {
            return super.createNarrationMessage().append(ChatScreen.this.commandSuggestions.getNarrationMessage());
         }
      };
      this.input.setMaxLength(256);
      this.input.setBordered(false);
      this.input.setValue(this.initial);
      this.input.setResponder(this::onEdited);
      this.addWidget(this.input);
      this.commandSuggestions = new CommandSuggestions(this.minecraft, this, this.input, this.font, false, false, 1, 10, true, -805306368);
      this.commandSuggestions.updateCommandInfo();
      this.setInitialFocus(this.input);
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.input.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.setChatLine(s);
      this.commandSuggestions.updateCommandInfo();
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
      this.minecraft.gui.getChat().resetChatScroll();
   }

   public void tick() {
      this.input.tick();
   }

   private void onEdited(String p_95611_) {
      String s = this.input.getValue();
      this.commandSuggestions.setAllowSuggestions(!s.equals(this.initial));
      this.commandSuggestions.updateCommandInfo();
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.commandSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (pKeyCode == 256) {
         this.minecraft.setScreen((Screen)null);
         return true;
      } else if (pKeyCode != 257 && pKeyCode != 335) {
         if (pKeyCode == 265) {
            this.moveInHistory(-1);
            return true;
         } else if (pKeyCode == 264) {
            this.moveInHistory(1);
            return true;
         } else if (pKeyCode == 266) {
            this.minecraft.gui.getChat().scrollChat((double)(this.minecraft.gui.getChat().getLinesPerPage() - 1));
            return true;
         } else if (pKeyCode == 267) {
            this.minecraft.gui.getChat().scrollChat((double)(-this.minecraft.gui.getChat().getLinesPerPage() + 1));
            return true;
         } else {
            return false;
         }
      } else {
         String s = this.input.getValue().trim();
         if (!s.isEmpty()) {
            this.sendMessage(s);
         }

         this.minecraft.setScreen((Screen)null);
         return true;
      }
   }

   public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
      if (pDelta > 1.0D) {
         pDelta = 1.0D;
      }

      if (pDelta < -1.0D) {
         pDelta = -1.0D;
      }

      if (this.commandSuggestions.mouseScrolled(pDelta)) {
         return true;
      } else {
         if (!hasShiftDown()) {
            pDelta *= 7.0D;
         }

         this.minecraft.gui.getChat().scrollChat(pDelta);
         return true;
      }
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.commandSuggestions.mouseClicked((double)((int)pMouseX), (double)((int)pMouseY), pButton)) {
         return true;
      } else {
         if (pButton == 0) {
            ChatComponent chatcomponent = this.minecraft.gui.getChat();
            if (chatcomponent.handleChatQueueClicked(pMouseX, pMouseY)) {
               return true;
            }

            Style style = chatcomponent.getClickedComponentStyleAt(pMouseX, pMouseY);
            if (style != null && this.handleComponentClicked(style)) {
               return true;
            }
         }

         return this.input.mouseClicked(pMouseX, pMouseY, pButton) ? true : super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   protected void insertText(String pText, boolean pOverwrite) {
      if (pOverwrite) {
         this.input.setValue(pText);
      } else {
         this.input.insertText(pText);
      }

   }

   /**
    * input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next
    * message from the current cursor position
    */
   public void moveInHistory(int pMsgPos) {
      int i = this.historyPos + pMsgPos;
      int j = this.minecraft.gui.getChat().getRecentChat().size();
      i = Mth.clamp(i, 0, j);
      if (i != this.historyPos) {
         if (i == j) {
            this.historyPos = j;
            this.input.setValue(this.historyBuffer);
         } else {
            if (this.historyPos == j) {
               this.historyBuffer = this.input.getValue();
            }

            this.input.setValue(this.minecraft.gui.getChat().getRecentChat().get(i));
            this.commandSuggestions.setAllowSuggestions(false);
            this.historyPos = i;
         }
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.setFocused(this.input);
      this.input.setFocus(true);
      fill(pPoseStack, 2, this.height - 14, this.width - 2, this.height - 2, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));
      this.input.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      this.commandSuggestions.render(pPoseStack, pMouseX, pMouseY);
      Style style = this.minecraft.gui.getChat().getClickedComponentStyleAt((double)pMouseX, (double)pMouseY);
      if (style != null && style.getHoverEvent() != null) {
         this.renderComponentHoverEffect(pPoseStack, style, pMouseX, pMouseY);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void setChatLine(String p_95613_) {
      this.input.setValue(p_95613_);
   }

   protected void updateNarrationState(NarrationElementOutput p_169238_) {
      p_169238_.add(NarratedElementType.TITLE, this.getTitle());
      p_169238_.add(NarratedElementType.USAGE, USAGE_TEXT);
      String s = this.input.getValue();
      if (!s.isEmpty()) {
         p_169238_.nest().add(NarratedElementType.TITLE, new TranslatableComponent("chat_screen.message", s));
      }

   }
}