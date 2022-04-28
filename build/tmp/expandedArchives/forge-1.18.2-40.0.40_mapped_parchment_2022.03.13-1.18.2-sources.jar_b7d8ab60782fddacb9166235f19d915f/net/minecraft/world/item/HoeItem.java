package net.minecraft.world.item;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class HoeItem extends DiggerItem {
   protected static final Map<Block, Pair<Predicate<UseOnContext>, Consumer<UseOnContext>>> TILLABLES = Maps.newHashMap(ImmutableMap.of(Blocks.GRASS_BLOCK, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.DIRT_PATH, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.DIRT, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.FARMLAND.defaultBlockState())), Blocks.COARSE_DIRT, Pair.of(HoeItem::onlyIfAirAbove, changeIntoState(Blocks.DIRT.defaultBlockState())), Blocks.ROOTED_DIRT, Pair.of((p_150861_) -> {
      return true;
   }, changeIntoStateAndDropItem(Blocks.DIRT.defaultBlockState(), Items.HANGING_ROOTS))));

   public HoeItem(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Item.Properties pProperties) {
      super((float)pAttackDamageModifier, pAttackSpeedModifier, pTier, BlockTags.MINEABLE_WITH_HOE, pProperties);
   }

   /**
    * Called when this item is used when targetting a Block
    */
   public InteractionResult useOn(UseOnContext pContext) {
      int hook = net.minecraftforge.event.ForgeEventFactory.onHoeUse(pContext);
      if (hook != 0) return hook > 0 ? InteractionResult.SUCCESS : InteractionResult.FAIL;
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> pair = TILLABLES.get(level.getBlockState(blockpos).getBlock());
      if (pair == null) {
         return InteractionResult.PASS;
      } else {
         Predicate<UseOnContext> predicate = pair.getFirst();
         Consumer<UseOnContext> consumer = pair.getSecond();
         if (predicate.test(pContext)) {
            Player player = pContext.getPlayer();
            level.playSound(player, blockpos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (!level.isClientSide) {
               consumer.accept(pContext);
               if (player != null) {
                  pContext.getItemInHand().hurtAndBreak(1, player, (p_150845_) -> {
                     p_150845_.broadcastBreakEvent(pContext.getHand());
                  });
               }
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public static Consumer<UseOnContext> changeIntoState(BlockState pState) {
      return (p_150848_) -> {
         p_150848_.getLevel().setBlock(p_150848_.getClickedPos(), pState, 11);
      };
   }

   public static Consumer<UseOnContext> changeIntoStateAndDropItem(BlockState pState, ItemLike pItemToDrop) {
      return (p_150855_) -> {
         p_150855_.getLevel().setBlock(p_150855_.getClickedPos(), pState, 11);
         Block.popResourceFromFace(p_150855_.getLevel(), p_150855_.getClickedPos(), p_150855_.getClickedFace(), new ItemStack(pItemToDrop));
      };
   }

   public static boolean onlyIfAirAbove(UseOnContext p_150857_) {
      return p_150857_.getClickedFace() != Direction.DOWN && p_150857_.getLevel().getBlockState(p_150857_.getClickedPos().above()).isAir();
   }

   @Override
   public boolean canPerformAction(ItemStack stack, net.minecraftforge.common.ToolAction toolAction) {
      return net.minecraftforge.common.ToolActions.DEFAULT_HOE_ACTIONS.contains(toolAction);
   }
}
