package com.purplik.hat.item.uncommon;

import com.purplik.hat.HatTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EngineersHat extends Item {

    public EngineersHat(Properties pProperties) {
        super(pProperties.tab(HatTab.COSMETICS_TAB).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.hat.engineers_hat.tooltip"));
        pTooltipComponents.add(Component.translatable("tooltip.hat.uncommon.tooltip"));
    }

}
