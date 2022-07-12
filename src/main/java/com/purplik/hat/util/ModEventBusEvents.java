/*
package com.purplik.hat.util;

import com.mojang.serialization.Codec;
import com.purplik.hat.Hat;
import com.purplik.hat.loot.LegendaryHatLoot;
import com.purplik.hat.loot.RareHatLoot;
import com.purplik.hat.loot.UncommonHatLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = Hat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerModifierSerilizer(@Nonnull final RegisterEvent event)  {

        event.register(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {

            helper.register(new ResourceLocation(Hat.MOD_ID, "legendary_hat_loot" ), new LegendaryHatLoot.Serializer());
            helper.register(new ResourceLocation(Hat.MOD_ID, "rare_hat_loot" ), new RareHatLoot.Serializer());
            helper.register(new ResourceLocation(Hat.MOD_ID, "uncommon_hat_loot" ), new UncommonHatLoot.Serializer());
        });

    }
}
*/