package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteOpenHashSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TextComponentTagVisitor implements TagVisitor {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int INLINE_LIST_THRESHOLD = 8;
   private static final ByteCollection INLINE_ELEMENT_TYPES = new ByteOpenHashSet(Arrays.asList((byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6));
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_KEY = ChatFormatting.AQUA;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_STRING = ChatFormatting.GREEN;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER = ChatFormatting.GOLD;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER_TYPE = ChatFormatting.RED;
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private final String indentation;
   private final int depth;
   private Component result = TextComponent.EMPTY;

   public TextComponentTagVisitor(String pIndentation, int pDepth) {
      this.indentation = pIndentation;
      this.depth = pDepth;
   }

   public Component visit(Tag pTag) {
      pTag.accept(this);
      return this.result;
   }

   public void visitString(StringTag pTag) {
      String s = StringTag.quoteAndEscape(pTag.getAsString());
      String s1 = s.substring(0, 1);
      Component component = (new TextComponent(s.substring(1, s.length() - 1))).withStyle(SYNTAX_HIGHLIGHTING_STRING);
      this.result = (new TextComponent(s1)).append(component).append(s1);
   }

   public void visitByte(ByteTag pTag) {
      Component component = (new TextComponent("b")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      this.result = (new TextComponent(String.valueOf((Object)pTag.getAsNumber()))).append(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitShort(ShortTag pTag) {
      Component component = (new TextComponent("s")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      this.result = (new TextComponent(String.valueOf((Object)pTag.getAsNumber()))).append(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitInt(IntTag pTag) {
      this.result = (new TextComponent(String.valueOf((Object)pTag.getAsNumber()))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitLong(LongTag pTag) {
      Component component = (new TextComponent("L")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      this.result = (new TextComponent(String.valueOf((Object)pTag.getAsNumber()))).append(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitFloat(FloatTag pTag) {
      Component component = (new TextComponent("f")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      this.result = (new TextComponent(String.valueOf(pTag.getAsFloat()))).append(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitDouble(DoubleTag pTag) {
      Component component = (new TextComponent("d")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      this.result = (new TextComponent(String.valueOf(pTag.getAsDouble()))).append(component).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
   }

   public void visitByteArray(ByteArrayTag pTag) {
      Component component = (new TextComponent("B")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      MutableComponent mutablecomponent = (new TextComponent("[")).append(component).append(";");
      byte[] abyte = pTag.getAsByteArray();

      for(int i = 0; i < abyte.length; ++i) {
         MutableComponent mutablecomponent1 = (new TextComponent(String.valueOf((int)abyte[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         mutablecomponent.append(" ").append(mutablecomponent1).append(component);
         if (i != abyte.length - 1) {
            mutablecomponent.append(ELEMENT_SEPARATOR);
         }
      }

      mutablecomponent.append("]");
      this.result = mutablecomponent;
   }

   public void visitIntArray(IntArrayTag pTag) {
      Component component = (new TextComponent("I")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      MutableComponent mutablecomponent = (new TextComponent("[")).append(component).append(";");
      int[] aint = pTag.getAsIntArray();

      for(int i = 0; i < aint.length; ++i) {
         mutablecomponent.append(" ").append((new TextComponent(String.valueOf(aint[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
         if (i != aint.length - 1) {
            mutablecomponent.append(ELEMENT_SEPARATOR);
         }
      }

      mutablecomponent.append("]");
      this.result = mutablecomponent;
   }

   public void visitLongArray(LongArrayTag pTag) {
      Component component = (new TextComponent("L")).withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
      MutableComponent mutablecomponent = (new TextComponent("[")).append(component).append(";");
      long[] along = pTag.getAsLongArray();

      for(int i = 0; i < along.length; ++i) {
         Component component1 = (new TextComponent(String.valueOf(along[i]))).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         mutablecomponent.append(" ").append(component1).append(component);
         if (i != along.length - 1) {
            mutablecomponent.append(ELEMENT_SEPARATOR);
         }
      }

      mutablecomponent.append("]");
      this.result = mutablecomponent;
   }

   public void visitList(ListTag pTag) {
      if (pTag.isEmpty()) {
         this.result = new TextComponent("[]");
      } else if (INLINE_ELEMENT_TYPES.contains(pTag.getElementType()) && pTag.size() <= 8) {
         String s = ELEMENT_SEPARATOR + " ";
         MutableComponent mutablecomponent2 = new TextComponent("[");

         for(int j = 0; j < pTag.size(); ++j) {
            if (j != 0) {
               mutablecomponent2.append(s);
            }

            mutablecomponent2.append((new TextComponentTagVisitor(this.indentation, this.depth)).visit(pTag.get(j)));
         }

         mutablecomponent2.append("]");
         this.result = mutablecomponent2;
      } else {
         MutableComponent mutablecomponent = new TextComponent("[");
         if (!this.indentation.isEmpty()) {
            mutablecomponent.append("\n");
         }

         for(int i = 0; i < pTag.size(); ++i) {
            MutableComponent mutablecomponent1 = new TextComponent(Strings.repeat(this.indentation, this.depth + 1));
            mutablecomponent1.append((new TextComponentTagVisitor(this.indentation, this.depth + 1)).visit(pTag.get(i)));
            if (i != pTag.size() - 1) {
               mutablecomponent1.append(ELEMENT_SEPARATOR).append(this.indentation.isEmpty() ? " " : "\n");
            }

            mutablecomponent.append(mutablecomponent1);
         }

         if (!this.indentation.isEmpty()) {
            mutablecomponent.append("\n").append(Strings.repeat(this.indentation, this.depth));
         }

         mutablecomponent.append("]");
         this.result = mutablecomponent;
      }
   }

   public void visitCompound(CompoundTag pTag) {
      if (pTag.isEmpty()) {
         this.result = new TextComponent("{}");
      } else {
         MutableComponent mutablecomponent = new TextComponent("{");
         Collection<String> collection = pTag.getAllKeys();
         if (LOGGER.isDebugEnabled()) {
            List<String> list = Lists.newArrayList(pTag.getAllKeys());
            Collections.sort(list);
            collection = list;
         }

         if (!this.indentation.isEmpty()) {
            mutablecomponent.append("\n");
         }

         MutableComponent mutablecomponent1;
         for(Iterator<String> iterator = collection.iterator(); iterator.hasNext(); mutablecomponent.append(mutablecomponent1)) {
            String s = iterator.next();
            mutablecomponent1 = (new TextComponent(Strings.repeat(this.indentation, this.depth + 1))).append(handleEscapePretty(s)).append(NAME_VALUE_SEPARATOR).append(" ").append((new TextComponentTagVisitor(this.indentation, this.depth + 1)).visit(pTag.get(s)));
            if (iterator.hasNext()) {
               mutablecomponent1.append(ELEMENT_SEPARATOR).append(this.indentation.isEmpty() ? " " : "\n");
            }
         }

         if (!this.indentation.isEmpty()) {
            mutablecomponent.append("\n").append(Strings.repeat(this.indentation, this.depth));
         }

         mutablecomponent.append("}");
         this.result = mutablecomponent;
      }
   }

   protected static Component handleEscapePretty(String pText) {
      if (SIMPLE_VALUE.matcher(pText).matches()) {
         return (new TextComponent(pText)).withStyle(SYNTAX_HIGHLIGHTING_KEY);
      } else {
         String s = StringTag.quoteAndEscape(pText);
         String s1 = s.substring(0, 1);
         Component component = (new TextComponent(s.substring(1, s.length() - 1))).withStyle(SYNTAX_HIGHLIGHTING_KEY);
         return (new TextComponent(s1)).append(component).append(s1);
      }
   }

   public void visitEnd(EndTag pTag) {
      this.result = TextComponent.EMPTY;
   }
}