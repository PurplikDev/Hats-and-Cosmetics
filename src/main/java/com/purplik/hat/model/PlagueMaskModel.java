package com.purplik.hat.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class PlagueMaskModel extends HumanoidModel<LivingEntity> {

    public PlagueMaskModel(ModelPart modelPart) {
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

        PartDefinition plague_mask = partDefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, -4.0F, -8.25F, 3.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition beakSmall_r1 = plague_mask.addOrReplaceChild("beakSmall_r1", CubeListBuilder.create().texOffs(14, 16).addBox(-1.0F, -1.0F, -2.75F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -2.0F, -8.25F, 0.1309F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
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