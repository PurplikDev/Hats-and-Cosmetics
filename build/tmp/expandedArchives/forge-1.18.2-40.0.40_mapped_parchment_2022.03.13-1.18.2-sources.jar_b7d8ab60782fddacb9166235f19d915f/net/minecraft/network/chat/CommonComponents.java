package net.minecraft.network.chat;

import java.util.Arrays;
import java.util.Collection;

public class CommonComponents {
   public static final Component OPTION_ON = new TranslatableComponent("options.on");
   public static final Component OPTION_OFF = new TranslatableComponent("options.off");
   public static final Component GUI_DONE = new TranslatableComponent("gui.done");
   public static final Component GUI_CANCEL = new TranslatableComponent("gui.cancel");
   public static final Component GUI_YES = new TranslatableComponent("gui.yes");
   public static final Component GUI_NO = new TranslatableComponent("gui.no");
   public static final Component GUI_PROCEED = new TranslatableComponent("gui.proceed");
   public static final Component GUI_BACK = new TranslatableComponent("gui.back");
   public static final Component CONNECT_FAILED = new TranslatableComponent("connect.failed");
   public static final Component NEW_LINE = new TextComponent("\n");
   public static final Component NARRATION_SEPARATOR = new TextComponent(". ");

   public static Component optionStatus(boolean pIsEnabled) {
      return pIsEnabled ? OPTION_ON : OPTION_OFF;
   }

   public static MutableComponent optionStatus(Component pMessage, boolean pComposed) {
      return new TranslatableComponent(pComposed ? "options.on.composed" : "options.off.composed", pMessage);
   }

   public static MutableComponent optionNameValue(Component pCaption, Component pValueMessage) {
      return new TranslatableComponent("options.generic_value", pCaption, pValueMessage);
   }

   public static MutableComponent joinForNarration(Component pFirstComponent, Component pSecondComponent) {
      return (new TextComponent("")).append(pFirstComponent).append(NARRATION_SEPARATOR).append(pSecondComponent);
   }

   public static Component joinLines(Component... pLines) {
      return joinLines(Arrays.asList(pLines));
   }

   public static Component joinLines(Collection<? extends Component> pLines) {
      return ComponentUtils.formatList(pLines, NEW_LINE);
   }
}