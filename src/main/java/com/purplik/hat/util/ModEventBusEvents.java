package com.purplik.hat.util;

import com.purplik.hat.Hat;
import com.purplik.hat.loot.LegendaryHatLoot;
import com.purplik.hat.loot.RareHatLoot;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber(modid = Hat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {
    @SubscribeEvent
    public static void registerModifierSerilizer(@Nonnull final RegistryEvent.Register<GlobalLootModifierSerializer<?>> event)  {
        event.getRegistry().registerAll( new LegendaryHatLoot.Serializer().setRegistryName( new ResourceLocation(Hat.MOD_ID, "legendary_hat_loot" )));
        event.getRegistry().registerAll( new RareHatLoot.Serializer().setRegistryName( new ResourceLocation(Hat.MOD_ID, "rare_hat_loot" )));
    }
}
