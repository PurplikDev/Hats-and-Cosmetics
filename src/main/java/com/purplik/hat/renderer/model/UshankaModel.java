package com.purplik.hat.renderer.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class UshankaModel extends HumanoidModel<LivingEntity> {

    public UshankaModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {
        CubeDeformation cubeDeformation = new CubeDeformation(0.4F);
        MeshDefinition meshDefinition = HumanoidModel.createMesh(cubeDeformation, 0.0F);
        PartDefinition partDefinition = meshDefinition.getRoot();

        partDefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        partDefinition.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 0, 0, 0, cubeDeformation), PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));

        PartDefinition mainPart = partDefinition.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(0, 54).addBox(-4.5F, -8.5F, -4.5F, 9.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                .texOffs(0, 54).addBox(-4.5F, -7.5F, -4.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 43).addBox(-4.5F, -7.5F, -2.5F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(0, 39).addBox(-3.5F, -7.5F, -4.5F, 7.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(18, 43).addBox(3.5F, -7.5F, -2.5F, 1.0F, 3.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(27, 55).addBox(3.5F, -7.5F, -4.5F, 1.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(18, 38).addBox(-3.5F, -7.5F, 3.5F, 7.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(-3.0F, -9.0F, -4.0F, 6.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(16, 35).addBox(-4.0F, -9.0F, -5.0F, 8.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 34).addBox(-5.0F, -8.0F, -5.0F, 10.0F, 2.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(43, 39).addBox(-5.0F, -7.0F, -4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 39).addBox(4.0F, -7.0F, -4.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 45).addBox(4.0F, -6.0F, -3.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(43, 45).addBox(-5.0F, -6.0F, -3.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(43, 51).addBox(-5.0F, -6.0F, -2.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(36, 51).addBox(4.0F, -6.0F, -2.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 38).addBox(-5.0F, -6.0F, -1.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(49, 38).addBox(4.0F, -6.0F, -1.0F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(54, 45).addBox(-5.0F, -6.0F, 0.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 45).addBox(4.0F, -6.0F, 0.0F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(48, 25).addBox(4.0F, -6.0F, 1.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(48, 17).addBox(-5.0F, -6.0F, 1.0F, 1.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(44, 12).addBox(-4.0F, -6.0F, 4.0F, 8.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0F, 0.0F));
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