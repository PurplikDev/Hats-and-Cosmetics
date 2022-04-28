package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

public class ComponentUtils {
   public static final String DEFAULT_SEPARATOR_TEXT = ", ";
   public static final Component DEFAULT_SEPARATOR = (new TextComponent(", ")).withStyle(ChatFormatting.GRAY);
   public static final Component DEFAULT_NO_STYLE_SEPARATOR = new TextComponent(", ");

   /**
    * Merge the component's styles with the given Style.
    */
   public static MutableComponent mergeStyles(MutableComponent pComponent, Style pStyle) {
      if (pStyle.isEmpty()) {
         return pComponent;
      } else {
         Style style = pComponent.getStyle();
         if (style.isEmpty()) {
            return pComponent.setStyle(pStyle);
         } else {
            return style.equals(pStyle) ? pComponent : pComponent.setStyle(style.applyTo(pStyle));
         }
      }
   }

   public static Optional<MutableComponent> updateForEntity(@Nullable CommandSourceStack pCommandSourceStack, Optional<Component> pOptionalComponent, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      return pOptionalComponent.isPresent() ? Optional.of(updateForEntity(pCommandSourceStack, pOptionalComponent.get(), pEntity, pRecursionDepth)) : Optional.empty();
   }

   public static MutableComponent updateForEntity(@Nullable CommandSourceStack pCommandSourceStack, Component pComponent, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      if (pRecursionDepth > 100) {
         return pComponent.copy();
      } else {
         MutableComponent mutablecomponent = pComponent instanceof ContextAwareComponent ? ((ContextAwareComponent)pComponent).resolve(pCommandSourceStack, pEntity, pRecursionDepth + 1) : pComponent.plainCopy();

         for(Component component : pComponent.getSiblings()) {
            mutablecomponent.append(updateForEntity(pCommandSourceStack, component, pEntity, pRecursionDepth + 1));
         }

         return mutablecomponent.withStyle(resolveStyle(pCommandSourceStack, pComponent.getStyle(), pEntity, pRecursionDepth));
      }
   }

   private static Style resolveStyle(@Nullable CommandSourceStack pCommandSourceStack, Style pStyle, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      HoverEvent hoverevent = pStyle.getHoverEvent();
      if (hoverevent != null) {
         Component component = hoverevent.getValue(HoverEvent.Action.SHOW_TEXT);
         if (component != null) {
            HoverEvent hoverevent1 = new HoverEvent(HoverEvent.Action.SHOW_TEXT, updateForEntity(pCommandSourceStack, component, pEntity, pRecursionDepth + 1));
            return pStyle.withHoverEvent(hoverevent1);
         }
      }

      return pStyle;
   }

   public static Component getDisplayName(GameProfile pProfile) {
      if (pProfile.getName() != null) {
         return new TextComponent(pProfile.getName());
      } else {
         return pProfile.getId() != null ? new TextComponent(pProfile.getId().toString()) : new TextComponent("(unknown)");
      }
   }

   public static Component formatList(Collection<String> pElements) {
      return formatAndSortList(pElements, (p_130742_) -> {
         return (new TextComponent(p_130742_)).withStyle(ChatFormatting.GREEN);
      });
   }

   public static <T extends Comparable<T>> Component formatAndSortList(Collection<T> pElements, Function<T, Component> pComponentExtractor) {
      if (pElements.isEmpty()) {
         return TextComponent.EMPTY;
      } else if (pElements.size() == 1) {
         return pComponentExtractor.apply(pElements.iterator().next());
      } else {
         List<T> list = Lists.newArrayList(pElements);
         list.sort(Comparable::compareTo);
         return formatList(list, pComponentExtractor);
      }
   }

   public static <T> Component formatList(Collection<? extends T> pElements, Function<T, Component> pComponentExtractor) {
      return formatList(pElements, DEFAULT_SEPARATOR, pComponentExtractor);
   }

   public static <T> MutableComponent formatList(Collection<? extends T> pElements, Optional<? extends Component> pOptionalSeparator, Function<T, Component> pComponentExtractor) {
      return formatList(pElements, DataFixUtils.orElse(pOptionalSeparator, DEFAULT_SEPARATOR), pComponentExtractor);
   }

   public static Component formatList(Collection<? extends Component> pElements, Component pSeparator) {
      return formatList(pElements, pSeparator, Function.identity());
   }

   public static <T> MutableComponent formatList(Collection<? extends T> pElements, Component pSeparator, Function<T, Component> pComponentExtractor) {
      if (pElements.isEmpty()) {
         return new TextComponent("");
      } else if (pElements.size() == 1) {
         return pComponentExtractor.apply(pElements.iterator().next()).copy();
      } else {
         MutableComponent mutablecomponent = new TextComponent("");
         boolean flag = true;

         for(T t : pElements) {
            if (!flag) {
               mutablecomponent.append(pSeparator);
            }

            mutablecomponent.append(pComponentExtractor.apply(t));
            flag = false;
         }

         return mutablecomponent;
      }
   }

   /**
    * Wraps the text with square brackets.
    */
   public static MutableComponent wrapInSquareBrackets(Component pToWrap) {
      return new TranslatableComponent("chat.square_brackets", pToWrap);
   }

   public static Component fromMessage(Message pMessage) {
      return (Component)(pMessage instanceof Component ? (Component)pMessage : new TextComponent(pMessage.getString()));
   }
}