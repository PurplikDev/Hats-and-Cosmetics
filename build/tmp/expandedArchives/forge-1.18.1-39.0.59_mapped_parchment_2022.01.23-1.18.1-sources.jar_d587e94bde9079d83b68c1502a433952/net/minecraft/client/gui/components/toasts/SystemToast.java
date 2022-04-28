package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
   private static final long DISPLAY_TIME = 5000L;
   private static final int MAX_LINE_SIZE = 200;
   private final SystemToast.SystemToastIds id;
   private Component title;
   private List<FormattedCharSequence> messageLines;
   private long lastChanged;
   private boolean changed;
   private final int width;

   public SystemToast(SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      this(pId, pTitle, nullToEmpty(pMessage), 160);
   }

   public static SystemToast multiline(Minecraft pMinecraft, SystemToast.SystemToastIds pId, Component pTitle, Component pMessage) {
      Font font = pMinecraft.font;
      List<FormattedCharSequence> list = font.split(pMessage, 200);
      int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
      return new SystemToast(pId, pTitle, list, i + 30);
   }

   private SystemToast(SystemToast.SystemToastIds pId, Component pTitle, List<FormattedCharSequence> pMessageLines, int pWidth) {
      this.id = pId;
      this.title = pTitle;
      this.messageLines = pMessageLines;
      this.width = pWidth;
   }

   private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component pMessage) {
      return pMessage == null ? ImmutableList.of() : ImmutableList.of(pMessage.getVisualOrderText());
   }

   public int width() {
      return this.width;
   }

   /**
    * 
    * @param pTimeSinceLastVisible time in milliseconds
    */
   public Toast.Visibility render(PoseStack pPoseStack, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
      if (this.changed) {
         this.lastChanged = pTimeSinceLastVisible;
         this.changed = false;
      }

      RenderSystem.setShaderTexture(0, TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      int i = this.width();
      int j = 12;
      if (i == 160 && this.messageLines.size() <= 1) {
         pToastComponent.blit(pPoseStack, 0, 0, 0, 64, i, this.height());
      } else {
         int k = this.height() + Math.max(0, this.messageLines.size() - 1) * 12;
         int l = 28;
         int i1 = Math.min(4, k - 28);
         this.renderBackgroundRow(pPoseStack, pToastComponent, i, 0, 0, 28);

         for(int j1 = 28; j1 < k - i1; j1 += 10) {
            this.renderBackgroundRow(pPoseStack, pToastComponent, i, 16, j1, Math.min(16, k - j1 - i1));
         }

         this.renderBackgroundRow(pPoseStack, pToastComponent, i, 32 - i1, k - i1, i1);
      }

      if (this.messageLines == null) {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 18.0F, 12.0F, -256);
      } else {
         pToastComponent.getMinecraft().font.draw(pPoseStack, this.title, 18.0F, 7.0F, -256);

         for(int k1 = 0; k1 < this.messageLines.size(); ++k1) {
            pToastComponent.getMinecraft().font.draw(pPoseStack, this.messageLines.get(k1), 18.0F, (float)(18 + k1 * 12), -1);
         }
      }

      return pTimeSinceLastVisible - this.lastChanged < 5000L ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
   }

   private void renderBackgroundRow(PoseStack p_94837_, ToastComponent p_94838_, int p_94839_, int p_94840_, int p_94841_, int p_94842_) {
      int i = p_94840_ == 0 ? 20 : 5;
      int j = Math.min(60, p_94839_ - i);
      p_94838_.blit(p_94837_, 0, p_94841_, 0, 64 + p_94840_, i, p_94842_);

      for(int k = i; k < p_94839_ - j; k += 64) {
         p_94838_.blit(p_94837_, k, p_94841_, 32, 64 + p_94840_, Math.min(64, p_94839_ - k - j), p_94842_);
      }

      p_94838_.blit(p_94837_, p_94839_ - j, p_94841_, 160 - j, 64 + p_94840_, j, p_94842_);
   }

   public void reset(Component pTitle, @Nullable Component pMessage) {
      this.title = pTitle;
      this.messageLines = nullToEmpty(pMessage);
      this.changed = true;
   }

   public SystemToast.SystemToastIds getToken() {
      return this.id;
   }

   public static void add(ToastComponent pToastComponent, SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      pToastComponent.addToast(new SystemToast(pId, pTitle, pMessage));
   }

   public static void addOrUpdate(ToastComponent pToastComponent, SystemToast.SystemToastIds pId, Component pTitle, @Nullable Component pMessage) {
      SystemToast systemtoast = pToastComponent.getToast(SystemToast.class, pId);
      if (systemtoast == null) {
         add(pToastComponent, pId, pTitle, pMessage);
      } else {
         systemtoast.reset(pTitle, pMessage);
      }

   }

   public static void onWorldAccessFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE, new TranslatableComponent("selectWorld.access_failure"), new TextComponent(pMessage));
   }

   public static void onWorldDeleteFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE, new TranslatableComponent("selectWorld.delete_failure"), new TextComponent(pMessage));
   }

   public static void onPackCopyFailure(Minecraft pMinecraft, String pMessage) {
      add(pMinecraft.getToasts(), SystemToast.SystemToastIds.PACK_COPY_FAILURE, new TranslatableComponent("pack.copyFailure"), new TextComponent(pMessage));
   }

   @OnlyIn(Dist.CLIENT)
   public static enum SystemToastIds {
      TUTORIAL_HINT,
      NARRATOR_TOGGLE,
      WORLD_BACKUP,
      WORLD_GEN_SETTINGS_TRANSFER,
      PACK_LOAD_FAILURE,
      WORLD_ACCESS_FAILURE,
      PACK_COPY_FAILURE;
   }
}