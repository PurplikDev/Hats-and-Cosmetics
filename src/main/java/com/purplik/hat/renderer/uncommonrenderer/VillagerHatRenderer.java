package com.purplik.hat.renderer.uncommonrenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.purplik.hat.model.uncommonmodels.VillagerHatTemplate;
import com.purplik.hat.renderer.CosmeticLayerDefinitions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class VillagerHatRenderer<L extends LivingEntity> implements ICurioRenderer {

    private final VillagerHatTemplate model;

    public VillagerHatRenderer(String path) {
        this.model = new VillagerHatTemplate(Minecraft.getInstance().getEntityModels().bakeLayer(CosmeticLayerDefinitions.VILLAGER_HAT));
        this.VILLAGER_TEXTURE = new ResourceLocation("minecraft:textures/entity/villager/profession/" + path + ".png");
    }

    private ResourceLocation VILLAGER_TEXTURE;

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>>
    void render(ItemStack itemStack,
                SlotContext slotContext,
                PoseStack poseStack,
                RenderLayerParent<T, M> renderLayerParent,
                MultiBufferSource multiBufferSource,
                int light,
                float limbSwing,
                float limbSwingAmount,
                float partialTicks,
                float ageInTicks,
                float netHeadYaw,
                float headPitch
                ) {
        LivingEntity entity = slotContext.entity();
        this.model.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTicks);
        this.model.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ICurioRenderer.followBodyRotations(entity, this.model);
        VertexConsumer vertexconsumer = ItemRenderer
                .getArmorFoilBuffer(multiBufferSource, RenderType.armorCutoutNoCull(VILLAGER_TEXTURE), false,
                        itemStack.hasFoil());
        this.model
                .renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F,
                        1.0F, 1.0F);
    }
}
