package io.purplik.hatsandcosmetics.common;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class HatItem extends Item {
    public HatItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    }
}
