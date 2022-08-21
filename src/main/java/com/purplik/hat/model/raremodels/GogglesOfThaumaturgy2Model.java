package com.purplik.hat.model.raremodels;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class GogglesOfThaumaturgy2Model extends HumanoidModel<LivingEntity> {

    public GogglesOfThaumaturgy2Model(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {
        CubeDeformation cubeDeformation = new CubeDeformation(0.4F);
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("head", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("hat", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("body", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_arm", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_arm", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("right_leg", new CubeListBuilder(), PartPose.ZERO);
        partdefinition.addOrReplaceChild("left_leg", new CubeListBuilder(), PartPose.ZERO);

        PartDefinition mainPart = partdefinition.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -6.5F, -4.0F, 9.0F, 4.0F, 8.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 14).addBox(3.0F, -4.5F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(1, 0).addBox(0.0F, -4.5F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(12, 12).addBox(-4.0F, -4.5F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(6, 12).addBox(-3.0F, -5.5F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 12).addBox(-3.0F, -2.5F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 5).addBox(1.0F, -2.5F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 3).addBox(1.0F, -5.5F, -5.0F, 2.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 17).addBox(-3.0F, -4.5F, -4.75F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 17).addBox(1.0F, -4.5F, -4.75F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-1.0F, -4.5F, -5.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 48, 48);
    }

    @Override
    @Nonnull
    protected Iterable<ModelPart> headParts() {
        return ImmutableList.of(this.head);
    }

    @Override
    @Nonnull
    protected Iterable<ModelPart> bodyParts() {
        return ImmutableList
                .of(this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg, this.hat);
    }
}