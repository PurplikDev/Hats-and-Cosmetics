package net.minecraft.world.inventory;

import java.util.List;
import java.util.Random;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;

public class EnchantmentMenu extends AbstractContainerMenu {
   private final Container enchantSlots = new SimpleContainer(2) {
      /**
       * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think
       * it hasn't changed and skip it.
       */
      public void setChanged() {
         super.setChanged();
         EnchantmentMenu.this.slotsChanged(this);
      }
   };
   private final ContainerLevelAccess access;
   private final Random random = new Random();
   private final DataSlot enchantmentSeed = DataSlot.standalone();
   public final int[] costs = new int[3];
   public final int[] enchantClue = new int[]{-1, -1, -1};
   public final int[] levelClue = new int[]{-1, -1, -1};

   public EnchantmentMenu(int p_39454_, Inventory p_39455_) {
      this(p_39454_, p_39455_, ContainerLevelAccess.NULL);
   }

   public EnchantmentMenu(int p_39457_, Inventory p_39458_, ContainerLevelAccess p_39459_) {
      super(MenuType.ENCHANTMENT, p_39457_);
      this.access = p_39459_;
      this.addSlot(new Slot(this.enchantSlots, 0, 15, 47) {
         /**
          * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
          */
         public boolean mayPlace(ItemStack p_39508_) {
            return true;
         }

         /**
          * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
          * case of armor slots)
          */
         public int getMaxStackSize() {
            return 1;
         }
      });
      this.addSlot(new Slot(this.enchantSlots, 1, 35, 47) {
         /**
          * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
          */
         public boolean mayPlace(ItemStack p_39517_) {
            return p_39517_.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS);
         }
      });

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(p_39458_, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for(int k = 0; k < 9; ++k) {
         this.addSlot(new Slot(p_39458_, k, 8 + k * 18, 142));
      }

      this.addDataSlot(DataSlot.shared(this.costs, 0));
      this.addDataSlot(DataSlot.shared(this.costs, 1));
      this.addDataSlot(DataSlot.shared(this.costs, 2));
      this.addDataSlot(this.enchantmentSeed).set(p_39458_.player.getEnchantmentSeed());
      this.addDataSlot(DataSlot.shared(this.enchantClue, 0));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 1));
      this.addDataSlot(DataSlot.shared(this.enchantClue, 2));
      this.addDataSlot(DataSlot.shared(this.levelClue, 0));
      this.addDataSlot(DataSlot.shared(this.levelClue, 1));
      this.addDataSlot(DataSlot.shared(this.levelClue, 2));
   }

   private float getPower(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos) {
      return world.getBlockState(pos).getEnchantPowerBonus(world, pos);
   }

   /**
    * Callback for when the crafting matrix is changed.
    */
   public void slotsChanged(Container pInventory) {
      if (pInventory == this.enchantSlots) {
         ItemStack itemstack = pInventory.getItem(0);
         if (!itemstack.isEmpty() && itemstack.isEnchantable()) {
            this.access.execute((p_39485_, p_39486_) -> {
               int power = 0;

               for(int k = -1; k <= 1; ++k) {
                  for(int l = -1; l <= 1; ++l) {
                     if ((k != 0 || l != 0) && p_39485_.isEmptyBlock(p_39486_.offset(l, 0, k)) && p_39485_.isEmptyBlock(p_39486_.offset(l, 1, k))) {
                        power += getPower(p_39485_, p_39486_.offset(l * 2, 0, k * 2));
                        power += getPower(p_39485_, p_39486_.offset(l * 2, 1, k * 2));

                        if (l != 0 && k != 0) {
                           power += getPower(p_39485_, p_39486_.offset(l * 2, 0, k));
                           power += getPower(p_39485_, p_39486_.offset(l * 2, 1, k));
                           power += getPower(p_39485_, p_39486_.offset(l, 0, k * 2));
                           power += getPower(p_39485_, p_39486_.offset(l, 1, k * 2));
                        }
                     }
                  }
               }

               this.random.setSeed((long)this.enchantmentSeed.get());

               for(int i1 = 0; i1 < 3; ++i1) {
                  this.costs[i1] = EnchantmentHelper.getEnchantmentCost(this.random, i1, (int)power, itemstack);
                  this.enchantClue[i1] = -1;
                  this.levelClue[i1] = -1;
                  if (this.costs[i1] < i1 + 1) {
                     this.costs[i1] = 0;
                  }
                  this.costs[i1] = net.minecraftforge.event.ForgeEventFactory.onEnchantmentLevelSet(p_39485_, p_39486_, i1, (int)power, itemstack, costs[i1]);
               }

               for(int j1 = 0; j1 < 3; ++j1) {
                  if (this.costs[j1] > 0) {
                     List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, j1, this.costs[j1]);
                     if (list != null && !list.isEmpty()) {
                        EnchantmentInstance enchantmentinstance = list.get(this.random.nextInt(list.size()));
                        this.enchantClue[j1] = Registry.ENCHANTMENT.getId(enchantmentinstance.enchantment);
                        this.levelClue[j1] = enchantmentinstance.level;
                     }
                  }
               }

               this.broadcastChanges();
            });
         } else {
            for(int i = 0; i < 3; ++i) {
               this.costs[i] = 0;
               this.enchantClue[i] = -1;
               this.levelClue[i] = -1;
            }
         }
      }

   }

   /**
    * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
    */
   public boolean clickMenuButton(Player pPlayer, int pId) {
      ItemStack itemstack = this.enchantSlots.getItem(0);
      ItemStack itemstack1 = this.enchantSlots.getItem(1);
      int i = pId + 1;
      if ((itemstack1.isEmpty() || itemstack1.getCount() < i) && !pPlayer.getAbilities().instabuild) {
         return false;
      } else if (this.costs[pId] <= 0 || itemstack.isEmpty() || (pPlayer.experienceLevel < i || pPlayer.experienceLevel < this.costs[pId]) && !pPlayer.getAbilities().instabuild) {
         return false;
      } else {
         this.access.execute((p_39481_, p_39482_) -> {
            ItemStack itemstack2 = itemstack;
            List<EnchantmentInstance> list = this.getEnchantmentList(itemstack, pId, this.costs[pId]);
            if (!list.isEmpty()) {
               pPlayer.onEnchantmentPerformed(itemstack, i);
               boolean flag = itemstack.is(Items.BOOK);
               if (flag) {
                  itemstack2 = new ItemStack(Items.ENCHANTED_BOOK);
                  CompoundTag compoundtag = itemstack.getTag();
                  if (compoundtag != null) {
                     itemstack2.setTag(compoundtag.copy());
                  }

                  this.enchantSlots.setItem(0, itemstack2);
               }

               for(int j = 0; j < list.size(); ++j) {
                  EnchantmentInstance enchantmentinstance = list.get(j);
                  if (flag) {
                     EnchantedBookItem.addEnchantment(itemstack2, enchantmentinstance);
                  } else {
                     itemstack2.enchant(enchantmentinstance.enchantment, enchantmentinstance.level);
                  }
               }

               if (!pPlayer.getAbilities().instabuild) {
                  itemstack1.shrink(i);
                  if (itemstack1.isEmpty()) {
                     this.enchantSlots.setItem(1, ItemStack.EMPTY);
                  }
               }

               pPlayer.awardStat(Stats.ENCHANT_ITEM);
               if (pPlayer instanceof ServerPlayer) {
                  CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer)pPlayer, itemstack2, i);
               }

               this.enchantSlots.setChanged();
               this.enchantmentSeed.set(pPlayer.getEnchantmentSeed());
               this.slotsChanged(this.enchantSlots);
               p_39481_.playSound((Player)null, p_39482_, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, p_39481_.random.nextFloat() * 0.1F + 0.9F);
            }

         });
         return true;
      }
   }

   private List<EnchantmentInstance> getEnchantmentList(ItemStack pStack, int pEnchantSlot, int pLevel) {
      this.random.setSeed((long)(this.enchantmentSeed.get() + pEnchantSlot));
      List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, pStack, pLevel, false);
      if (pStack.is(Items.BOOK) && list.size() > 1) {
         list.remove(this.random.nextInt(list.size()));
      }

      return list;
   }

   public int getGoldCount() {
      ItemStack itemstack = this.enchantSlots.getItem(1);
      return itemstack.isEmpty() ? 0 : itemstack.getCount();
   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed.get();
   }

   /**
    * Called when the container is closed.
    */
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.access.execute((p_39469_, p_39470_) -> {
         this.clearContainer(pPlayer, this.enchantSlots);
      });
   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return stillValid(this.access, pPlayer, Blocks.ENCHANTING_TABLE);
   }

   /**
    * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
    * inventory and the other inventory(s).
    */
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(pIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (pIndex == 0) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (pIndex == 1) {
            if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
               return ItemStack.EMPTY;
            }
         } else if (itemstack1.is(net.minecraftforge.common.Tags.Items.ENCHANTING_FUELS)) {
            if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (this.slots.get(0).hasItem() || !this.slots.get(0).mayPlace(itemstack1)) {
               return ItemStack.EMPTY;
            }

            ItemStack itemstack2 = itemstack1.copy();
            itemstack2.setCount(1);
            itemstack1.shrink(1);
            this.slots.get(0).set(itemstack2);
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(pPlayer, itemstack1);
      }

      return itemstack;
   }
}
