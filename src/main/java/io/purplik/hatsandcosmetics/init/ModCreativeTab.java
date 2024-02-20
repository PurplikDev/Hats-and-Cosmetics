package io.purplik.hatsandcosmetics.init;

import io.purplik.hatsandcosmetics.HatsAndCosmetics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HatsAndCosmetics.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HAT_TAB = CREATIVE_MODE_TABS
            .register("hatsandcosmetics_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hatsandcosmetics"))
            .icon(() -> Items.STICK.getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.TEST_ITEM.get());
            }).build());
}
