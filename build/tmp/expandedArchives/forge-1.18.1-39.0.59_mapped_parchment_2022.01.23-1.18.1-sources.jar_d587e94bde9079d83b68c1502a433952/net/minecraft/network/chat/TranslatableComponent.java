package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.world.entity.Entity;

public class TranslatableComponent extends BaseComponent implements ContextAwareComponent {
   private static final Object[] NO_ARGS = new Object[0];
   private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
   private static final FormattedText TEXT_NULL = FormattedText.of("null");
   private final String key;
   private final Object[] args;
   @Nullable
   private Language decomposedWith;
   /**
    * The discrete elements that make up this component. For example, this would be ["Prefix, ", "FirstArg",
    * "SecondArg", " again ", "SecondArg", " and ", "FirstArg", " lastly ", "ThirdArg", " and also ", "FirstArg", "
    * again!"] for "translation.test.complex" (see en_us.json)
    */
   private List<FormattedText> decomposedParts = ImmutableList.of();
   private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

   public TranslatableComponent(String pKey) {
      this.key = pKey;
      this.args = NO_ARGS;
   }

   public TranslatableComponent(String pKey, Object... pArgs) {
      this.key = pKey;
      this.args = pArgs;
   }

   /**
    * Ensures that all of the children are up to date with the most recent translation mapping.
    */
   private void decompose() {
      Language language = Language.getInstance();
      if (language != this.decomposedWith) {
         this.decomposedWith = language;
         String s = language.getOrDefault(this.key);

         try {
            Builder<FormattedText> builder = ImmutableList.builder();
            this.decomposeTemplate(s, builder::add);
            this.decomposedParts = builder.build();
         } catch (TranslatableFormatException translatableformatexception) {
            this.decomposedParts = ImmutableList.of(FormattedText.of(s));
         }

      }
   }

   private void decomposeTemplate(String p_200006_, Consumer<FormattedText> p_200007_) {
      Matcher matcher = FORMAT_PATTERN.matcher(p_200006_);

      try {
         int i = 0;

         int j;
         int l;
         for(j = 0; matcher.find(j); j = l) {
            int k = matcher.start();
            l = matcher.end();
            if (k > j) {
               String s = p_200006_.substring(j, k);
               if (s.indexOf(37) != -1) {
                  throw new IllegalArgumentException();
               }

               p_200007_.accept(FormattedText.of(s));
            }

            String s4 = matcher.group(2);
            String s1 = p_200006_.substring(k, l);
            if ("%".equals(s4) && "%%".equals(s1)) {
               p_200007_.accept(TEXT_PERCENT);
            } else {
               if (!"s".equals(s4)) {
                  throw new TranslatableFormatException(this, "Unsupported format: '" + s1 + "'");
               }

               String s2 = matcher.group(1);
               int i1 = s2 != null ? Integer.parseInt(s2) - 1 : i++;
               if (i1 < this.args.length) {
                  p_200007_.accept(this.getArgument(i1));
               }
            }
         }

         if (j == 0) {
            // if we failed to match above, lets try the messageformat handler instead.
            j = net.minecraftforge.internal.TextComponentMessageFormatHandler.handle(this, p_200007_, this.args, p_200006_);
         }
         if (j < p_200006_.length()) {
            String s3 = p_200006_.substring(j);
            if (s3.indexOf(37) != -1) {
               throw new IllegalArgumentException();
            }

            p_200007_.accept(FormattedText.of(s3));
         }

      } catch (IllegalArgumentException illegalargumentexception) {
         throw new TranslatableFormatException(this, illegalargumentexception);
      }
   }

   private FormattedText getArgument(int pIndex) {
      if (pIndex >= this.args.length) {
         throw new TranslatableFormatException(this, pIndex);
      } else {
         Object object = this.args[pIndex];
         if (object instanceof Component) {
            return (Component)object;
         } else {
            return object == null ? TEXT_NULL : FormattedText.of(object.toString());
         }
      }
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public TranslatableComponent plainCopy() {
      return new TranslatableComponent(this.key, this.args);
   }

   public <T> Optional<T> visitSelf(FormattedText.StyledContentConsumer<T> pConsumer, Style pStyle) {
      this.decompose();

      for(FormattedText formattedtext : this.decomposedParts) {
         Optional<T> optional = formattedtext.visit(pConsumer, pStyle);
         if (optional.isPresent()) {
            return optional;
         }
      }

      return Optional.empty();
   }

   public <T> Optional<T> visitSelf(FormattedText.ContentConsumer<T> pConsumer) {
      this.decompose();

      for(FormattedText formattedtext : this.decomposedParts) {
         Optional<T> optional = formattedtext.visit(pConsumer);
         if (optional.isPresent()) {
            return optional;
         }
      }

      return Optional.empty();
   }

   public MutableComponent resolve(@Nullable CommandSourceStack pCommandSourceStack, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      Object[] aobject = new Object[this.args.length];

      for(int i = 0; i < aobject.length; ++i) {
         Object object = this.args[i];
         if (object instanceof Component) {
            aobject[i] = ComponentUtils.updateForEntity(pCommandSourceStack, (Component)object, pEntity, pRecursionDepth);
         } else {
            aobject[i] = object;
         }
      }

      return new TranslatableComponent(this.key, aobject);
   }

   public boolean equals(Object p_131324_) {
      if (this == p_131324_) {
         return true;
      } else if (!(p_131324_ instanceof TranslatableComponent)) {
         return false;
      } else {
         TranslatableComponent translatablecomponent = (TranslatableComponent)p_131324_;
         return Arrays.equals(this.args, translatablecomponent.args) && this.key.equals(translatablecomponent.key) && super.equals(p_131324_);
      }
   }

   public int hashCode() {
      int i = super.hashCode();
      i = 31 * i + this.key.hashCode();
      return 31 * i + Arrays.hashCode(this.args);
   }

   public String toString() {
      return "TranslatableComponent{key='" + this.key + "', args=" + Arrays.toString(this.args) + ", siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }

   /**
    * Gets the key used to translate this component.
    */
   public String getKey() {
      return this.key;
   }

   /**
    * Gets the object array that is used to translate the key.
    */
   public Object[] getArgs() {
      return this.args;
   }
}
