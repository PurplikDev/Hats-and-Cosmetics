package net.minecraft.client.searchtree;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReloadableIdSearchTree<T> implements MutableSearchTree<T> {
   protected SuffixArray<T> namespaceTree = new SuffixArray<>();
   protected SuffixArray<T> pathTree = new SuffixArray<>();
   private final Function<T, Stream<ResourceLocation>> idGetter;
   private final List<T> contents = Lists.newArrayList();
   private final Object2IntMap<T> orderT = new Object2IntOpenHashMap<>();

   public ReloadableIdSearchTree(Function<T, Stream<ResourceLocation>> pIdGetter) {
      this.idGetter = pIdGetter;
   }

   /**
    * Recalculates the contents of this search tree, reapplying {@link #nameFunc} and {@link #idFunc}. Should be called
    * whenever resources are reloaded (e.g. language changes).
    */
   public void refresh() {
      this.namespaceTree = new SuffixArray<>();
      this.pathTree = new SuffixArray<>();

      for(T t : this.contents) {
         this.index(t);
      }

      this.namespaceTree.generate();
      this.pathTree.generate();
   }

   public void add(T pObject) {
      this.orderT.put(pObject, this.contents.size());
      this.contents.add(pObject);
      this.index(pObject);
   }

   public void clear() {
      this.contents.clear();
      this.orderT.clear();
   }

   /**
    * Directly puts the given item into {@link #byId} and {@link #byName}, applying {@link #nameFunc} and {@link
    * idFunc}.
    */
   protected void index(T pElement) {
      this.idGetter.apply(pElement).forEach((p_119885_) -> {
         this.namespaceTree.add(pElement, p_119885_.getNamespace().toLowerCase(Locale.ROOT));
         this.pathTree.add(pElement, p_119885_.getPath().toLowerCase(Locale.ROOT));
      });
   }

   /**
    * Compares two elements. Returns {@code 1} if the first element has more entries, {@code 0} if they have the same
    * number of entries, and {@code -1} if the second element has more enties.
    */
   protected int comparePosition(T p_119881_, T p_119882_) {
      return Integer.compare(this.orderT.getInt(p_119881_), this.orderT.getInt(p_119882_));
   }

   /**
    * Searches this search tree for the given text.
    * <p>
    * If the query does not contain a <code>:</code>, then only {@link #byName} is searched" if it does contain a colon,
    * both {@link #byName} and {@link #byId} are searched and the results are merged using a {@link MergingIterator}.
    * @return A list of all matching items in this search tree.
    */
   public List<T> search(String pSearchText) {
      int i = pSearchText.indexOf(58);
      if (i == -1) {
         return this.pathTree.search(pSearchText);
      } else {
         List<T> list = this.namespaceTree.search(pSearchText.substring(0, i).trim());
         String s = pSearchText.substring(i + 1).trim();
         List<T> list1 = this.pathTree.search(s);
         return Lists.newArrayList(new ReloadableIdSearchTree.IntersectionIterator<>(list.iterator(), list1.iterator(), this::comparePosition));
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected static class IntersectionIterator<T> extends AbstractIterator<T> {
      private final PeekingIterator<T> firstIterator;
      private final PeekingIterator<T> secondIterator;
      private final Comparator<T> orderT;

      public IntersectionIterator(Iterator<T> pFirstIterator, Iterator<T> pSecondIterator, Comparator<T> pOrderT) {
         this.firstIterator = Iterators.peekingIterator(pFirstIterator);
         this.secondIterator = Iterators.peekingIterator(pSecondIterator);
         this.orderT = pOrderT;
      }

      protected T computeNext() {
         while(this.firstIterator.hasNext() && this.secondIterator.hasNext()) {
            int i = this.orderT.compare(this.firstIterator.peek(), this.secondIterator.peek());
            if (i == 0) {
               this.secondIterator.next();
               return this.firstIterator.next();
            }

            if (i < 0) {
               this.firstIterator.next();
            } else {
               this.secondIterator.next();
            }
         }

         return this.endOfData();
      }
   }
}