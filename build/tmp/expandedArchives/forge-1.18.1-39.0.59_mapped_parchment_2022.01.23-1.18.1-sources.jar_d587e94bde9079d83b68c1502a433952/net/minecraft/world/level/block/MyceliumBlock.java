package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MyceliumBlock extends SpreadingSnowyDirtBlock {
   public MyceliumBlock(BlockBehaviour.Properties p_54898_) {
      super(p_54898_);
   }

   /**
    * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
    */
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, Random pRand) {
      super.animateTick(pState, pLevel, pPos, pRand);
      if (pRand.nextInt(10) == 0) {
         pLevel.addParticle(ParticleTypes.MYCELIUM, (double)pPos.getX() + pRand.nextDouble(), (double)pPos.getY() + 1.1D, (double)pPos.getZ() + pRand.nextDouble(), 0.0D, 0.0D, 0.0D);
      }

   }
}