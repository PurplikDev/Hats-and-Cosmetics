package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance>, net.minecraftforge.common.extensions.IForgeMobEffectInstance {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final MobEffect effect;
   private int duration;
   private int amplifier;
   private boolean ambient;
   /** True if potion effect duration is at maximum, false otherwise. */
   private boolean noCounter;
   private boolean visible;
   private boolean showIcon;
   /** A hidden effect which is not shown to the player. */
   @Nullable
   private MobEffectInstance hiddenEffect;

   public MobEffectInstance(MobEffect pEffect) {
      this(pEffect, 0, 0);
   }

   public MobEffectInstance(MobEffect pEffect, int pDuration) {
      this(pEffect, pDuration, 0);
   }

   public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier) {
      this(pEffect, pDuration, pAmplifier, false, true);
   }

   public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible) {
      this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pVisible);
   }

   public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon) {
      this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pShowIcon, (MobEffectInstance)null);
   }

   public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon, @Nullable MobEffectInstance pHiddenEffect) {
      this.effect = pEffect;
      this.duration = pDuration;
      this.amplifier = pAmplifier;
      this.ambient = pAmbient;
      this.visible = pVisible;
      this.showIcon = pShowIcon;
      this.hiddenEffect = pHiddenEffect;
   }

   public MobEffectInstance(MobEffectInstance pOther) {
      this.effect = pOther.effect;
      this.setDetailsFrom(pOther);
   }

   void setDetailsFrom(MobEffectInstance pEffectInstance) {
      this.duration = pEffectInstance.duration;
      this.amplifier = pEffectInstance.amplifier;
      this.ambient = pEffectInstance.ambient;
      this.visible = pEffectInstance.visible;
      this.showIcon = pEffectInstance.showIcon;
      this.curativeItems = pEffectInstance.curativeItems == null ? null : new java.util.ArrayList<net.minecraft.world.item.ItemStack>(pEffectInstance.curativeItems);
   }

   public boolean update(MobEffectInstance pOther) {
      if (this.effect != pOther.effect) {
         LOGGER.warn("This method should only be called for matching effects!");
      }

      boolean flag = false;
      if (pOther.amplifier > this.amplifier) {
         if (pOther.duration < this.duration) {
            MobEffectInstance mobeffectinstance = this.hiddenEffect;
            this.hiddenEffect = new MobEffectInstance(this);
            this.hiddenEffect.hiddenEffect = mobeffectinstance;
         }

         this.amplifier = pOther.amplifier;
         this.duration = pOther.duration;
         flag = true;
      } else if (pOther.duration > this.duration) {
         if (pOther.amplifier == this.amplifier) {
            this.duration = pOther.duration;
            flag = true;
         } else if (this.hiddenEffect == null) {
            this.hiddenEffect = new MobEffectInstance(pOther);
         } else {
            this.hiddenEffect.update(pOther);
         }
      }

      if (!pOther.ambient && this.ambient || flag) {
         this.ambient = pOther.ambient;
         flag = true;
      }

      if (pOther.visible != this.visible) {
         this.visible = pOther.visible;
         flag = true;
      }

      if (pOther.showIcon != this.showIcon) {
         this.showIcon = pOther.showIcon;
         flag = true;
      }

      return flag;
   }

   public MobEffect getEffect() {
      return this.effect == null ? null : this.effect.delegate.get();
   }

   public int getDuration() {
      return this.duration;
   }

   public int getAmplifier() {
      return this.amplifier;
   }

   /**
    * Gets whether this potion effect originated from a beacon
    */
   public boolean isAmbient() {
      return this.ambient;
   }

   /**
    * Gets whether this potion effect will show ambient particles or not.
    */
   public boolean isVisible() {
      return this.visible;
   }

   public boolean showIcon() {
      return this.showIcon;
   }

   public boolean tick(LivingEntity p_19553_, Runnable p_19554_) {
      if (this.duration > 0) {
         if (this.effect.isDurationEffectTick(this.duration, this.amplifier)) {
            this.applyEffect(p_19553_);
         }

         this.tickDownDuration();
         if (this.duration == 0 && this.hiddenEffect != null) {
            this.setDetailsFrom(this.hiddenEffect);
            this.hiddenEffect = this.hiddenEffect.hiddenEffect;
            p_19554_.run();
         }
      }

      return this.duration > 0;
   }

   private int tickDownDuration() {
      if (this.hiddenEffect != null) {
         this.hiddenEffect.tickDownDuration();
      }

      return --this.duration;
   }

   public void applyEffect(LivingEntity pEntity) {
      if (this.duration > 0) {
         this.effect.applyEffectTick(pEntity, this.amplifier);
      }

   }

   public String getDescriptionId() {
      return this.effect.getDescriptionId();
   }

   public String toString() {
      String s;
      if (this.amplifier > 0) {
         s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.duration;
      } else {
         s = this.getDescriptionId() + ", Duration: " + this.duration;
      }

      if (!this.visible) {
         s = s + ", Particles: false";
      }

      if (!this.showIcon) {
         s = s + ", Show Icon: false";
      }

      return s;
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof MobEffectInstance)) {
         return false;
      } else {
         MobEffectInstance mobeffectinstance = (MobEffectInstance)pOther;
         return this.duration == mobeffectinstance.duration && this.amplifier == mobeffectinstance.amplifier && this.ambient == mobeffectinstance.ambient && this.effect.equals(mobeffectinstance.effect);
      }
   }

   public int hashCode() {
      int i = this.effect.hashCode();
      i = 31 * i + this.duration;
      i = 31 * i + this.amplifier;
      return 31 * i + (this.ambient ? 1 : 0);
   }

   /**
    * Write a custom potion effect to a potion item's NBT data.
    */
   public CompoundTag save(CompoundTag pNbt) {
      pNbt.putByte("Id", (byte)MobEffect.getId(this.getEffect()));
      net.minecraftforge.common.ForgeHooks.saveMobEffect(pNbt, "forge:id", this.getEffect());
      this.writeDetailsTo(pNbt);
      return pNbt;
   }

   private void writeDetailsTo(CompoundTag pNbt) {
      pNbt.putByte("Amplifier", (byte)this.getAmplifier());
      pNbt.putInt("Duration", this.getDuration());
      pNbt.putBoolean("Ambient", this.isAmbient());
      pNbt.putBoolean("ShowParticles", this.isVisible());
      pNbt.putBoolean("ShowIcon", this.showIcon());
      if (this.hiddenEffect != null) {
         CompoundTag compoundtag = new CompoundTag();
         this.hiddenEffect.save(compoundtag);
         pNbt.put("HiddenEffect", compoundtag);
      }
      writeCurativeItems(pNbt);

   }

   /**
    * Read a custom potion effect from a potion item's NBT data.
    */
   @Nullable
   public static MobEffectInstance load(CompoundTag pNbt) {
      int i = pNbt.getByte("Id") & 0xFF;
      MobEffect mobeffect = MobEffect.byId(i);
      mobeffect = net.minecraftforge.common.ForgeHooks.loadMobEffect(pNbt, "forge:id", mobeffect);
      return mobeffect == null ? null : loadSpecifiedEffect(mobeffect, pNbt);
   }

   private static MobEffectInstance loadSpecifiedEffect(MobEffect pEffect, CompoundTag pNbt) {
      int i = pNbt.getByte("Amplifier");
      int j = pNbt.getInt("Duration");
      boolean flag = pNbt.getBoolean("Ambient");
      boolean flag1 = true;
      if (pNbt.contains("ShowParticles", 1)) {
         flag1 = pNbt.getBoolean("ShowParticles");
      }

      boolean flag2 = flag1;
      if (pNbt.contains("ShowIcon", 1)) {
         flag2 = pNbt.getBoolean("ShowIcon");
      }

      MobEffectInstance mobeffectinstance = null;
      if (pNbt.contains("HiddenEffect", 10)) {
         mobeffectinstance = loadSpecifiedEffect(pEffect, pNbt.getCompound("HiddenEffect"));
      }

      return readCurativeItems(new MobEffectInstance(pEffect, j, i < 0 ? 0 : i, flag, flag1, flag2, mobeffectinstance), pNbt);
   }

   /**
    * Toggle the isPotionDurationMax field.
    */
   public void setNoCounter(boolean pMaxDuration) {
      this.noCounter = pMaxDuration;
   }

   /**
    * Get the value of the isPotionDurationMax field.
    */
   public boolean isNoCounter() {
      return this.noCounter;
   }

   public int compareTo(MobEffectInstance p_19566_) {
      int i = 32147;
      return (this.getDuration() <= 32147 || p_19566_.getDuration() <= 32147) && (!this.isAmbient() || !p_19566_.isAmbient()) ? ComparisonChain.start().compare(this.isAmbient(), p_19566_.isAmbient()).compare(this.getDuration(), p_19566_.getDuration()).compare(this.getEffect().getSortOrder(this), p_19566_.getEffect().getSortOrder(this)).result() : ComparisonChain.start().compare(this.isAmbient(), p_19566_.isAmbient()).compare(this.getEffect().getSortOrder(this), p_19566_.getEffect().getSortOrder(this)).result();
   }

   //======================= FORGE START ===========================
   private java.util.List<net.minecraft.world.item.ItemStack> curativeItems;

   @Override
   public java.util.List<net.minecraft.world.item.ItemStack> getCurativeItems() {
      if (this.curativeItems == null) //Lazy load this so that we don't create a circular dep on Items.
         this.curativeItems = getEffect().getCurativeItems();
      return this.curativeItems;
   }
   @Override
   public void setCurativeItems(java.util.List<net.minecraft.world.item.ItemStack> curativeItems) {
      this.curativeItems = curativeItems;
   }
   private static MobEffectInstance readCurativeItems(MobEffectInstance effect, CompoundTag nbt) {
      if (nbt.contains("CurativeItems", net.minecraft.nbt.Tag.TAG_LIST)) {
         java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
         net.minecraft.nbt.ListTag list = nbt.getList("CurativeItems", net.minecraft.nbt.Tag.TAG_COMPOUND);
         for (int i = 0; i < list.size(); i++) {
            items.add(net.minecraft.world.item.ItemStack.of(list.getCompound(i)));
         }
         effect.setCurativeItems(items);
      }

      return effect;
   }
}
