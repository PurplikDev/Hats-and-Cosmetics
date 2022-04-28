package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class PotionItem extends Item {
   private static final int DRINK_DURATION = 32;

   public PotionItem(Item.Properties p_42979_) {
      super(p_42979_);
   }

   public ItemStack getDefaultInstance() {
      return PotionUtils.setPotion(super.getDefaultInstance(), Potions.WATER);
   }

   /**
    * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using
    * the Item before the action is complete.
    */
   public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
      Player player = pEntityLiving instanceof Player ? (Player)pEntityLiving : null;
      if (player instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)player, pStack);
      }

      if (!pLevel.isClientSide) {
         for(MobEffectInstance mobeffectinstance : PotionUtils.getMobEffects(pStack)) {
            if (mobeffectinstance.getEffect().isInstantenous()) {
               mobeffectinstance.getEffect().applyInstantenousEffect(player, player, pEntityLiving, mobeffectinstance.getAmplifier(), 1.0D);
            } else {
               pEntityLiving.addEffect(new MobEffectInstance(mobeffectinstance));
            }
         }
      }

      if (player != null) {
         player.awardStat(Stats.ITEM_USED.get(this));
         if (!player.getAbilities().instabuild) {
            pStack.shrink(1);
         }
      }

      if (player == null || !player.getAbilities().instabuild) {
         if (pStack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
         }

         if (player != null) {
            player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
         }
      }

      pLevel.gameEvent(pEntityLiving, GameEvent.DRINKING_FINISH, pEntityLiving.eyeBlockPosition());
      return pStack;
   }

   /**
    * How long it takes to use or consume an item
    */
   public int getUseDuration(ItemStack pStack) {
      return 32;
   }

   /**
    * returns the action that specifies what animation to play when the items is being used
    */
   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.DRINK;
   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
    * {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      return ItemUtils.startUsingInstantly(pLevel, pPlayer, pHand);
   }

   /**
    * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
    * different names based on their damage or NBT.
    */
   public String getDescriptionId(ItemStack pStack) {
      return PotionUtils.getPotion(pStack).getName(this.getDescriptionId() + ".effect.");
   }

   /**
    * allows items to add custom lines of information to the mouseover description
    */
   public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
      PotionUtils.addPotionTooltip(pStack, pTooltip, 1.0F);
   }

   /**
    * Returns true if this item has an enchantment glint. By default, this returns <code>stack.isItemEnchanted()</code>,
    * but other items can override it (for instance, written books always return true).
    * 
    * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get
    * the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
    */
   public boolean isFoil(ItemStack pStack) {
      return super.isFoil(pStack) || !PotionUtils.getMobEffects(pStack).isEmpty();
   }

   /**
    * returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
    */
   public void fillItemCategory(CreativeModeTab pGroup, NonNullList<ItemStack> pItems) {
      if (this.allowdedIn(pGroup)) {
         for(Potion potion : Registry.POTION) {
            if (potion != Potions.EMPTY) {
               pItems.add(PotionUtils.setPotion(new ItemStack(this), potion));
            }
         }
      }

   }
}