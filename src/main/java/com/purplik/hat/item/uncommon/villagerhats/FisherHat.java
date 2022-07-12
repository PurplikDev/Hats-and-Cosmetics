package com.purplik.hat.item.uncommon.villagerhats;

import com.purplik.hat.HatTab;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FisherHat extends Item {

    public FisherHat(Properties pProperties) {
        super(pProperties.tab(HatTab.COSMETICS_TAB).stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(new TranslatableComponent("tooltip.hat.fisher_hat.tooltip"));
        pTooltipComponents.add(new TranslatableComponent("tooltip.hat.uncommon.tooltip"));
    }
}