package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A component which shows the display names of entities selected by an {@link EntitySelector}.
 */
public class SelectorComponent extends BaseComponent implements ContextAwareComponent {
   private static final Logger LOGGER = LogManager.getLogger();
   /** The selector used to find the matching entities of this text component */
   private final String pattern;
   @Nullable
   private final EntitySelector selector;
   protected final Optional<Component> separator;

   public SelectorComponent(String pPattern, Optional<Component> pSeparator) {
      this.pattern = pPattern;
      this.separator = pSeparator;
      EntitySelector entityselector = null;

      try {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(pPattern));
         entityselector = entityselectorparser.parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
         LOGGER.warn("Invalid selector component: {}: {}", pPattern, commandsyntaxexception.getMessage());
      }

      this.selector = entityselector;
   }

   /**
    * Gets the selector of this component, in plain text.
    */
   public String getPattern() {
      return this.pattern;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   public Optional<Component> getSeparator() {
      return this.separator;
   }

   public MutableComponent resolve(@Nullable CommandSourceStack pCommandSourceStack, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      if (pCommandSourceStack != null && this.selector != null) {
         Optional<? extends Component> optional = ComponentUtils.updateForEntity(pCommandSourceStack, this.separator, pEntity, pRecursionDepth);
         return ComponentUtils.formatList(this.selector.findEntities(pCommandSourceStack), optional, Entity::getDisplayName);
      } else {
         return new TextComponent("");
      }
   }

   /**
    * Gets the raw content of this component (but not its sibling components), without any formatting codes. For
    * example, this is the raw text in a {@link TextComponentString}, but it's the translated text for a {@link
    * TextComponentTranslation} and it's the score value for a {@link TextComponentScore}.
    */
   public String getContents() {
      return this.pattern;
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public SelectorComponent plainCopy() {
      return new SelectorComponent(this.pattern, this.separator);
   }

   public boolean equals(Object p_131094_) {
      if (this == p_131094_) {
         return true;
      } else if (!(p_131094_ instanceof SelectorComponent)) {
         return false;
      } else {
         SelectorComponent selectorcomponent = (SelectorComponent)p_131094_;
         return this.pattern.equals(selectorcomponent.pattern) && super.equals(p_131094_);
      }
   }

   public String toString() {
      return "SelectorComponent{pattern='" + this.pattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }
}