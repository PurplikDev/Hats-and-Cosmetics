package net.minecraft.network.chat;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Component which displays a KeyBinding's display name.
 */
public class KeybindComponent extends BaseComponent {
   private static Function<String, Supplier<Component>> keyResolver = (p_130928_) -> {
      return () -> {
         return new TextComponent(p_130928_);
      };
   };
   private final String name;
   private Supplier<Component> nameResolver;

   public KeybindComponent(String pName) {
      this.name = pName;
   }

   public static void setKeyResolver(Function<String, Supplier<Component>> pKeyResolver) {
      keyResolver = pKeyResolver;
   }

   private Component getNestedComponent() {
      if (this.nameResolver == null) {
         this.nameResolver = keyResolver.apply(this.name);
      }

      return this.nameResolver.get();
   }

   public <T> Optional<T> visitSelf(FormattedText.ContentConsumer<T> pConsumer) {
      return this.getNestedComponent().visit(pConsumer);
   }

   public <T> Optional<T> visitSelf(FormattedText.StyledContentConsumer<T> pConsumer, Style pStyle) {
      return this.getNestedComponent().visit(pConsumer, pStyle);
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public KeybindComponent plainCopy() {
      return new KeybindComponent(this.name);
   }

   public boolean equals(Object p_130932_) {
      if (this == p_130932_) {
         return true;
      } else if (!(p_130932_ instanceof KeybindComponent)) {
         return false;
      } else {
         KeybindComponent keybindcomponent = (KeybindComponent)p_130932_;
         return this.name.equals(keybindcomponent.name) && super.equals(p_130932_);
      }
   }

   public String toString() {
      return "KeybindComponent{keybind='" + this.name + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }

   public String getName() {
      return this.name;
   }
}