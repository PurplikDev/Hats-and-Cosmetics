package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class ZombieHorse extends AbstractHorse {
   public ZombieHorse(EntityType<? extends ZombieHorse> p_30994_, Level p_30995_) {
      super(p_30994_, p_30995_);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return createBaseHorseAttributes().add(Attributes.MAX_HEALTH, 15.0D).add(Attributes.MOVEMENT_SPEED, (double)0.2F);
   }

   protected void randomizeAttributes() {
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(this.generateRandomJumpStrength());
   }

   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   protected SoundEvent getAmbientSound() {
      super.getAmbientSound();
      return SoundEvents.ZOMBIE_HORSE_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      super.getDeathSound();
      return SoundEvents.ZOMBIE_HORSE_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      super.getHurtSound(pDamageSource);
      return SoundEvents.ZOMBIE_HORSE_HURT;
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel p_149561_, AgeableMob p_149562_) {
      return EntityType.ZOMBIE_HORSE.create(p_149561_);
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (!this.isTamed()) {
         return InteractionResult.PASS;
      } else if (this.isBaby()) {
         return super.mobInteract(pPlayer, pHand);
      } else if (pPlayer.isSecondaryUseActive()) {
         this.openInventory(pPlayer);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      } else if (this.isVehicle()) {
         return super.mobInteract(pPlayer, pHand);
      } else {
         if (!itemstack.isEmpty()) {
            if (itemstack.is(Items.SADDLE) && !this.isSaddled()) {
               this.openInventory(pPlayer);
               return InteractionResult.sidedSuccess(this.level.isClientSide);
            }

            InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
            if (interactionresult.consumesAction()) {
               return interactionresult;
            }
         }

         this.doPlayerRide(pPlayer);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      }
   }

   protected void addBehaviourGoals() {
   }
}