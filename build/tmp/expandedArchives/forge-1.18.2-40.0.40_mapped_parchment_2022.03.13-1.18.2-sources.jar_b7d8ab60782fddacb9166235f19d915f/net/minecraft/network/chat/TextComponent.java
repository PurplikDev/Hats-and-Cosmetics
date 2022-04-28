package net.minecraft.network.chat;

/**
 * A Component that represents just a fixed String.
 */
public class TextComponent extends BaseComponent {
   public static final Component EMPTY = new TextComponent("");
   private final String text;

   public TextComponent(String pText) {
      this.text = pText;
   }

   /**
    * Gets the text value of this component. This is used to access the {@link #text} property, and only should be used
    * when dealing specifically with instances of {@link TextComponent} - for other purposes, use {@link
    * #getContents()}.
    */
   public String getText() {
      return this.text;
   }

   /**
    * Gets the raw content of this component (but not its sibling components), without any formatting codes. For
    * example, this is the raw text in a {@link TextComponentString}, but it's the translated text for a {@link
    * TextComponentTranslation} and it's the score value for a {@link TextComponentScore}.
    */
   public String getContents() {
      return this.text;
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public TextComponent plainCopy() {
      return new TextComponent(this.text);
   }

   public boolean equals(Object p_131290_) {
      if (this == p_131290_) {
         return true;
      } else if (!(p_131290_ instanceof TextComponent)) {
         return false;
      } else {
         TextComponent textcomponent = (TextComponent)p_131290_;
         return this.text.equals(textcomponent.getText()) && super.equals(p_131290_);
      }
   }

   public String toString() {
      return "TextComponent{text='" + this.text + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }
}