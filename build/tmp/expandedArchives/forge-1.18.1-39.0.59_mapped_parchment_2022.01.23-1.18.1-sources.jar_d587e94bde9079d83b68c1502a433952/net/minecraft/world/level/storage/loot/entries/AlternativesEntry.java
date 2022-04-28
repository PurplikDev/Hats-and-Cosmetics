package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.apache.commons.lang3.ArrayUtils;

/**
 * A composite loot pool entry container that expands all its children in order until one of them succeeds.
 * This container succeeds if one of its children succeeds.
 */
public class AlternativesEntry extends CompositeEntryBase {
   AlternativesEntry(LootPoolEntryContainer[] p_79384_, LootItemCondition[] p_79385_) {
      super(p_79384_, p_79385_);
   }

   public LootPoolEntryType getType() {
      return LootPoolEntries.ALTERNATIVES;
   }

   /**
    * Compose the given children into one container.
    */
   protected ComposableEntryContainer compose(ComposableEntryContainer[] pEntries) {
      switch(pEntries.length) {
      case 0:
         return ALWAYS_FALSE;
      case 1:
         return pEntries[0];
      case 2:
         return pEntries[0].or(pEntries[1]);
      default:
         return (p_79393_, p_79394_) -> {
            for(ComposableEntryContainer composableentrycontainer : pEntries) {
               if (composableentrycontainer.expand(p_79393_, p_79394_)) {
                  return true;
               }
            }

            return false;
         };
      }
   }

   public void validate(ValidationContext pValidationContext) {
      super.validate(pValidationContext);

      for(int i = 0; i < this.children.length - 1; ++i) {
         if (ArrayUtils.isEmpty((Object[])this.children[i].conditions)) {
            pValidationContext.reportProblem("Unreachable entry!");
         }
      }

   }

   public static AlternativesEntry.Builder alternatives(LootPoolEntryContainer.Builder<?>... pChildren) {
      return new AlternativesEntry.Builder(pChildren);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
      private final List<LootPoolEntryContainer> entries = Lists.newArrayList();

      public Builder(LootPoolEntryContainer.Builder<?>... pChildren) {
         for(LootPoolEntryContainer.Builder<?> builder : pChildren) {
            this.entries.add(builder.build());
         }

      }

      protected AlternativesEntry.Builder getThis() {
         return this;
      }

      public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> pChildBuilder) {
         this.entries.add(pChildBuilder.build());
         return this;
      }

      public LootPoolEntryContainer build() {
         return new AlternativesEntry(this.entries.toArray(new LootPoolEntryContainer[0]), this.getConditions());
      }
   }
}