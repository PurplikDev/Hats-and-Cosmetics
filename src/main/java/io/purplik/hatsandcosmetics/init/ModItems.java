package io.purplik.hatsandcosmetics.init;

import io.purplik.hatsandcosmetics.HatsAndCosmetics;
import io.purplik.hatsandcosmetics.common.HatItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HatsAndCosmetics.MOD_ID);

    public static final DeferredItem<HatItem> TEST_ITEM = ITEMS.register("test_item", HatItem::new);
}
