package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput implements Predicate<ItemStack> {
   private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType((p_120986_, p_120987_) -> {
      return new TranslatableComponent("arguments.item.overstacked", p_120986_, p_120987_);
   });
   private final Item item;
   @Nullable
   private final CompoundTag tag;

   public ItemInput(Item pItem, @Nullable CompoundTag pTag) {
      this.item = pItem;
      this.tag = pTag;
   }

   public Item getItem() {
      return this.item;
   }

   public boolean test(ItemStack pStack) {
      return pStack.is(this.item) && NbtUtils.compareNbt(this.tag, pStack.getTag(), true);
   }

   public ItemStack createItemStack(int pCount, boolean pAllowOversizedStacks) throws CommandSyntaxException {
      ItemStack itemstack = new ItemStack(this.item, pCount);
      if (this.tag != null) {
         itemstack.setTag(this.tag);
      }

      if (pAllowOversizedStacks && pCount > itemstack.getMaxStackSize()) {
         throw ERROR_STACK_TOO_BIG.create(Registry.ITEM.getKey(this.item), itemstack.getMaxStackSize());
      } else {
         return itemstack;
      }
   }

   public String serialize() {
      StringBuilder stringbuilder = new StringBuilder(Registry.ITEM.getId(this.item));
      if (this.tag != null) {
         stringbuilder.append((Object)this.tag);
      }

      return stringbuilder.toString();
   }
}