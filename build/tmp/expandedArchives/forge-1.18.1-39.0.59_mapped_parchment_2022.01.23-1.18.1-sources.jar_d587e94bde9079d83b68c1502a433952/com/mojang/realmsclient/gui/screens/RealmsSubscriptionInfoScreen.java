package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.Subscription;
import com.mojang.realmsclient.exception.RealmsServiceException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsSubscriptionInfoScreen extends RealmsScreen {
   static final Logger LOGGER = LogManager.getLogger();
   private static final Component SUBSCRIPTION_TITLE = new TranslatableComponent("mco.configure.world.subscription.title");
   private static final Component SUBSCRIPTION_START_LABEL = new TranslatableComponent("mco.configure.world.subscription.start");
   private static final Component TIME_LEFT_LABEL = new TranslatableComponent("mco.configure.world.subscription.timeleft");
   private static final Component DAYS_LEFT_LABEL = new TranslatableComponent("mco.configure.world.subscription.recurring.daysleft");
   private static final Component SUBSCRIPTION_EXPIRED_TEXT = new TranslatableComponent("mco.configure.world.subscription.expired");
   private static final Component SUBSCRIPTION_LESS_THAN_A_DAY_TEXT = new TranslatableComponent("mco.configure.world.subscription.less_than_a_day");
   private static final Component MONTH_SUFFIX = new TranslatableComponent("mco.configure.world.subscription.month");
   private static final Component MONTHS_SUFFIX = new TranslatableComponent("mco.configure.world.subscription.months");
   private static final Component DAY_SUFFIX = new TranslatableComponent("mco.configure.world.subscription.day");
   private static final Component DAYS_SUFFIX = new TranslatableComponent("mco.configure.world.subscription.days");
   private static final Component UNKNOWN = new TranslatableComponent("mco.configure.world.subscription.unknown");
   private final Screen lastScreen;
   final RealmsServer serverData;
   final Screen mainScreen;
   private Component daysLeft = UNKNOWN;
   private Component startDate = UNKNOWN;
   @Nullable
   private Subscription.SubscriptionType type;
   private static final String PURCHASE_LINK = "https://aka.ms/ExtendJavaRealms";

   public RealmsSubscriptionInfoScreen(Screen pLastScreen, RealmsServer pServerData, Screen pMainScreen) {
      super(NarratorChatListener.NO_TITLE);
      this.lastScreen = pLastScreen;
      this.serverData = pServerData;
      this.mainScreen = pMainScreen;
   }

   public void init() {
      this.getSubscription(this.serverData.id);
      this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
      this.addRenderableWidget(new Button(this.width / 2 - 100, row(6), 200, 20, new TranslatableComponent("mco.configure.world.subscription.extend"), (p_90010_) -> {
         String s = "https://aka.ms/ExtendJavaRealms?subscriptionId=" + this.serverData.remoteSubscriptionId + "&profileId=" + this.minecraft.getUser().getUuid();
         this.minecraft.keyboardHandler.setClipboard(s);
         Util.getPlatform().openUri(s);
      }));
      this.addRenderableWidget(new Button(this.width / 2 - 100, row(12), 200, 20, CommonComponents.GUI_BACK, (p_90006_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
      if (this.serverData.expired) {
         this.addRenderableWidget(new Button(this.width / 2 - 100, row(10), 200, 20, new TranslatableComponent("mco.configure.world.delete.button"), (p_89999_) -> {
            Component component = new TranslatableComponent("mco.configure.world.delete.question.line1");
            Component component1 = new TranslatableComponent("mco.configure.world.delete.question.line2");
            this.minecraft.setScreen(new RealmsLongConfirmationScreen(this::deleteRealm, RealmsLongConfirmationScreen.Type.Warning, component, component1, true));
         }));
      }

   }

   public Component getNarrationMessage() {
      return CommonComponents.joinLines(SUBSCRIPTION_TITLE, SUBSCRIPTION_START_LABEL, this.startDate, TIME_LEFT_LABEL, this.daysLeft);
   }

   private void deleteRealm(boolean p_90012_) {
      if (p_90012_) {
         (new Thread("Realms-delete-realm") {
            public void run() {
               try {
                  RealmsClient realmsclient = RealmsClient.create();
                  realmsclient.deleteWorld(RealmsSubscriptionInfoScreen.this.serverData.id);
               } catch (RealmsServiceException realmsserviceexception) {
                  RealmsSubscriptionInfoScreen.LOGGER.error("Couldn't delete world");
                  RealmsSubscriptionInfoScreen.LOGGER.error(realmsserviceexception);
               }

               RealmsSubscriptionInfoScreen.this.minecraft.execute(() -> {
                  RealmsSubscriptionInfoScreen.this.minecraft.setScreen(RealmsSubscriptionInfoScreen.this.mainScreen);
               });
            }
         }).start();
      }

      this.minecraft.setScreen(this);
   }

   private void getSubscription(long pServerId) {
      RealmsClient realmsclient = RealmsClient.create();

      try {
         Subscription subscription = realmsclient.subscriptionFor(pServerId);
         this.daysLeft = this.daysLeftPresentation(subscription.daysLeft);
         this.startDate = localPresentation(subscription.startDate);
         this.type = subscription.type;
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't get subscription");
         this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this.lastScreen));
      }

   }

   private static Component localPresentation(long p_182539_) {
      Calendar calendar = new GregorianCalendar(TimeZone.getDefault());
      calendar.setTimeInMillis(p_182539_);
      return new TextComponent(DateFormat.getDateTimeInstance().format(calendar.getTime()));
   }

   public void removed() {
      this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      int i = this.width / 2 - 100;
      drawCenteredString(pPoseStack, this.font, SUBSCRIPTION_TITLE, this.width / 2, 17, 16777215);
      this.font.draw(pPoseStack, SUBSCRIPTION_START_LABEL, (float)i, (float)row(0), 10526880);
      this.font.draw(pPoseStack, this.startDate, (float)i, (float)row(1), 16777215);
      if (this.type == Subscription.SubscriptionType.NORMAL) {
         this.font.draw(pPoseStack, TIME_LEFT_LABEL, (float)i, (float)row(3), 10526880);
      } else if (this.type == Subscription.SubscriptionType.RECURRING) {
         this.font.draw(pPoseStack, DAYS_LEFT_LABEL, (float)i, (float)row(3), 10526880);
      }

      this.font.draw(pPoseStack, this.daysLeft, (float)i, (float)row(4), 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   private Component daysLeftPresentation(int p_89984_) {
      if (p_89984_ < 0 && this.serverData.expired) {
         return SUBSCRIPTION_EXPIRED_TEXT;
      } else if (p_89984_ <= 1) {
         return SUBSCRIPTION_LESS_THAN_A_DAY_TEXT;
      } else {
         int i = p_89984_ / 30;
         int j = p_89984_ % 30;
         MutableComponent mutablecomponent = new TextComponent("");
         if (i > 0) {
            mutablecomponent.append(Integer.toString(i)).append(" ");
            if (i == 1) {
               mutablecomponent.append(MONTH_SUFFIX);
            } else {
               mutablecomponent.append(MONTHS_SUFFIX);
            }
         }

         if (j > 0) {
            if (i > 0) {
               mutablecomponent.append(", ");
            }

            mutablecomponent.append(Integer.toString(j)).append(" ");
            if (j == 1) {
               mutablecomponent.append(DAY_SUFFIX);
            } else {
               mutablecomponent.append(DAYS_SUFFIX);
            }
         }

         return mutablecomponent;
      }
   }
}