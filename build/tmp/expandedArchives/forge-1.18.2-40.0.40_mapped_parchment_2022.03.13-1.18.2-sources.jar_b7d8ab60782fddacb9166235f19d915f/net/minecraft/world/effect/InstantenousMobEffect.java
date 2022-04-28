package net.minecraft.world.effect;

public class InstantenousMobEffect extends MobEffect {
   public InstantenousMobEffect(MobEffectCategory p_19440_, int p_19441_) {
      super(p_19440_, p_19441_);
   }

   /**
    * Returns true if the potion has an instant effect instead of a continuous one (eg Harming)
    */
   public boolean isInstantenous() {
      return true;
   }

   /**
    * checks if Potion effect is ready to be applied this tick.
    */
   public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
      return pDuration >= 1;
   }
}