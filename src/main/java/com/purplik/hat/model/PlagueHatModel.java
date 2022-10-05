package com.purplik.hat.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class PlagueHatModel extends HumanoidModel<LivingEntity> {

    public PlagueHatModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {
        CubeDeformation cubeDeformation = new CubeDeformation(0.4F);
        MeshDefinition meshdefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partDefinition = meshdefinition.getRoot();

        partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));

        PartDefinition plague_hat = partDefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -6.25F, -7.0F, 14.0F, 0.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 15).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 2.0F, 9.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
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