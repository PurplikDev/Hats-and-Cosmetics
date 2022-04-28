package net.minecraft.world.item.trading;

import java.util.ArrayList;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public class MerchantOffers extends ArrayList<MerchantOffer> {
   public MerchantOffers() {
   }

   public MerchantOffers(CompoundTag pCompoundTag) {
      ListTag listtag = pCompoundTag.getList("Recipes", 10);

      for(int i = 0; i < listtag.size(); ++i) {
         this.add(new MerchantOffer(listtag.getCompound(i)));
      }

   }

   @Nullable
   public MerchantOffer getRecipeFor(ItemStack pStackA, ItemStack pStackB, int pIndex) {
      if (pIndex > 0 && pIndex < this.size()) {
         MerchantOffer merchantoffer1 = this.get(pIndex);
         return merchantoffer1.satisfiedBy(pStackA, pStackB) ? merchantoffer1 : null;
      } else {
         for(int i = 0; i < this.size(); ++i) {
            MerchantOffer merchantoffer = this.get(i);
            if (merchantoffer.satisfiedBy(pStackA, pStackB)) {
               return merchantoffer;
            }
         }

         return null;
      }
   }

   public void writeToStream(FriendlyByteBuf pBuffer) {
      pBuffer.writeByte((byte)(this.size() & 255));

      for(int i = 0; i < this.size(); ++i) {
         MerchantOffer merchantoffer = this.get(i);
         pBuffer.writeItem(merchantoffer.getBaseCostA());
         pBuffer.writeItem(merchantoffer.getResult());
         ItemStack itemstack = merchantoffer.getCostB();
         pBuffer.writeBoolean(!itemstack.isEmpty());
         if (!itemstack.isEmpty()) {
            pBuffer.writeItem(itemstack);
         }

         pBuffer.writeBoolean(merchantoffer.isOutOfStock());
         pBuffer.writeInt(merchantoffer.getUses());
         pBuffer.writeInt(merchantoffer.getMaxUses());
         pBuffer.writeInt(merchantoffer.getXp());
         pBuffer.writeInt(merchantoffer.getSpecialPriceDiff());
         pBuffer.writeFloat(merchantoffer.getPriceMultiplier());
         pBuffer.writeInt(merchantoffer.getDemand());
      }

   }

   public static MerchantOffers createFromStream(FriendlyByteBuf pBuffer) {
      MerchantOffers merchantoffers = new MerchantOffers();
      int i = pBuffer.readByte() & 255;

      for(int j = 0; j < i; ++j) {
         ItemStack itemstack = pBuffer.readItem();
         ItemStack itemstack1 = pBuffer.readItem();
         ItemStack itemstack2 = ItemStack.EMPTY;
         if (pBuffer.readBoolean()) {
            itemstack2 = pBuffer.readItem();
         }

         boolean flag = pBuffer.readBoolean();
         int k = pBuffer.readInt();
         int l = pBuffer.readInt();
         int i1 = pBuffer.readInt();
         int j1 = pBuffer.readInt();
         float f = pBuffer.readFloat();
         int k1 = pBuffer.readInt();
         MerchantOffer merchantoffer = new MerchantOffer(itemstack, itemstack2, itemstack1, k, l, i1, f, k1);
         if (flag) {
            merchantoffer.setToOutOfStock();
         }

         merchantoffer.setSpecialPriceDiff(j1);
         merchantoffers.add(merchantoffer);
      }

      return merchantoffers;
   }

   public CompoundTag createTag() {
      CompoundTag compoundtag = new CompoundTag();
      ListTag listtag = new ListTag();

      for(int i = 0; i < this.size(); ++i) {
         MerchantOffer merchantoffer = this.get(i);
         listtag.add(merchantoffer.createTag());
      }

      compoundtag.put("Recipes", listtag);
      return compoundtag;
   }
}