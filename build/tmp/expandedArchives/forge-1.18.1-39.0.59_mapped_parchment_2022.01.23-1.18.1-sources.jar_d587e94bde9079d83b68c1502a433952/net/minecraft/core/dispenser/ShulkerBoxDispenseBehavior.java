package net.minecraft.core.dispenser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.DispenserBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShulkerBoxDispenseBehavior extends OptionalDispenseItemBehavior {
   private static final Logger LOGGER = LogManager.getLogger();

   /**
    * Dispense the specified stack, play the dispense sound and spawn particles.
    */
   protected ItemStack execute(BlockSource pSource, ItemStack pStack) {
      this.setSuccess(false);
      Item item = pStack.getItem();
      if (item instanceof BlockItem) {
         Direction direction = pSource.getBlockState().getValue(DispenserBlock.FACING);
         BlockPos blockpos = pSource.getPos().relative(direction);
         Direction direction1 = pSource.getLevel().isEmptyBlock(blockpos.below()) ? direction : Direction.UP;

         try {
            this.setSuccess(((BlockItem)item).place(new DirectionalPlaceContext(pSource.getLevel(), blockpos, direction, pStack, direction1)).consumesAction());
         } catch (Exception exception) {
            LOGGER.error("Error trying to place shulker box at {}", blockpos, exception);
         }
      }

      return pStack;
   }
}