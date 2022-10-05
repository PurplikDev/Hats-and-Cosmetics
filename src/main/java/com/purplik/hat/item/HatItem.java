package com.purplik.hat.item;

import com.purplik.hat.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HatItem extends Item {

    private String itemName;

    public HatItem(Properties properties, String itemName) {
        super(properties.tab(Registry.COSMETICS_TAB).stacksTo(1).rarity(Rarity.EPIC));
        this.itemName = itemName;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable Level level, List<Component> components, TooltipFlag tooltipFlag) {
        components.add(Component.translatable("tooltip.hat."+ this.itemName + ".tooltip"));
    }
}
