package com.purplik.hat;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class HatTab {
    public static final CreativeModeTab COSMETICS_TAB = new CreativeModeTab("cosmetics_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Registry.LAB_COAT.get());
        }
    };
}
