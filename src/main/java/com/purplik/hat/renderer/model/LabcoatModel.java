package com.purplik.hat.renderer.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class LabcoatModel extends HumanoidModel<LivingEntity> {

    public LabcoatModel(ModelPart modelPart) {
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

        PartDefinition rightLeg = partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(28, 28).addBox(-2F, 0F, -2F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));
        PartDefinition leftLeg = partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 0).addBox(-2F, 0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));

        PartDefinition leftArm = partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 16).addBox(-1F, -2F, -2F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));
        PartDefinition rightArm = partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 16).addBox(-3F, -2F, -2F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));

        PartDefinition body = partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, 0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));


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