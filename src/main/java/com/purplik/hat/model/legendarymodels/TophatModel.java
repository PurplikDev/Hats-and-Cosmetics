package com.purplik.hat.model.legendarymodels;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class TophatModel extends HumanoidModel<LivingEntity> {

    public TophatModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {
        CubeDeformation cubeDeformation = new CubeDeformation(0.4F);
        MeshDefinition meshDefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild("head", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("hat", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("body", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("right_arm", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("left_arm", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("right_leg", new CubeListBuilder(), PartPose.ZERO);
        partDefinition.addOrReplaceChild("left_leg", new CubeListBuilder(), PartPose.ZERO);

        PartDefinition mainPart = partDefinition.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -7.0F, -5.0F, 10.0F, 1.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(0, 11).addBox(-4.0F, -15.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));

        PartDefinition cube_r1 = mainPart.addOrReplaceChild("cube_r1", CubeListBuilder.create()
                .texOffs(22, 17).addBox(-0.615F, -0.646F, -5.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.5F, -6.5F, 0.0F, 0.0F, 0.0F, -0.2618F));

        PartDefinition cube_r2 = mainPart.addOrReplaceChild("cube_r2", CubeListBuilder.create()
                .texOffs(0, 27).addBox(-0.385F, -0.646F, -5.0F, 1.0F, 1.0F, 10.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.5F, -6.5F, 0.0F, 0.0F, 0.0F, 0.2618F));


        return LayerDefinition.create(meshDefinition, 64, 64);
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