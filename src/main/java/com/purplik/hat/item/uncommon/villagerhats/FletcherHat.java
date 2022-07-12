package com.purplik.hat.item.uncommon.villagerhats;

import com.purplik.hat.HatTab;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FletcherHat extends Item {

    public FletcherHat(Properties pProperties) {
        super(pProperties.tab(HatTab.COSMETICS_TAB).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("tooltip.hat.fletcher_hat.tooltip"));
        pTooltipComponents.add(Component.translatable("tooltip.hat.uncommon.tooltip"));
    }
}