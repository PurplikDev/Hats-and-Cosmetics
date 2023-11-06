package com.purplik.hat.renderer;

import com.purplik.hat.Hat;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class CosmeticLayerDefinitions {

    private static final ModelLayerLocation registerLayer(String hatName) {
        return new ModelLayerLocation(new ResourceLocation(Hat.MOD_ID, hatName), "main");
    }

    public static final ModelLayerLocation TOPHAT = registerLayer("tophat");
    public static final ModelLayerLocation USHANKA = registerLayer("ushanka");
    public static final ModelLayerLocation GUP = registerLayer("gup");
    public static final ModelLayerLocation LABCOAT = registerLayer("labcoat");
    public static final ModelLayerLocation LAB_GOGGLES = registerLayer("labgoggles");
    public static final ModelLayerLocation GOGGLES_OF_THAUMATURGY = registerLayer("goggles_of_thaumaturgy");
    public static final ModelLayerLocation GOGGLES_OF_THAUMATURGY_2 = registerLayer("goggles_of_thaumaturgy_2");
    public static final ModelLayerLocation BANDITS_HAT = registerLayer("bandits_hat");
    public static final ModelLayerLocation CAPTAINS_HAT = registerLayer("captains_hat");
    public static final ModelLayerLocation VILLAGER_HAT = registerLayer("villager_hat");
    public static final ModelLayerLocation ENGINEERS_HAT = registerLayer("engineers_hat");
    public static final ModelLayerLocation MONOCLE = registerLayer("monocle");
    public static final ModelLayerLocation RAT = registerLayer("rat");
    public static final ModelLayerLocation PLAGUE_MASK = registerLayer("plague_mask");
    public static final ModelLayerLocation PLAGUE_HAT = registerLayer("plague_hat");
    public static final ModelLayerLocation SHADES = registerLayer("shades");
    public static final ModelLayerLocation POOF_BEANIE = registerLayer("poof_beanie");
    public static final ModelLayerLocation SCARF = registerLayer("scarf");
}
