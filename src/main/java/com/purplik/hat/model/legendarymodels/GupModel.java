package com.purplik.hat.model.legendarymodels;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class GupModel extends HumanoidModel<LivingEntity> {

    public GupModel(ModelPart modelPart) {
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

        PartDefinition rightArm = partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -4.0F, -1.5F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 5).addBox(-2.5F, -5.0F, 0.0F, 3.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshDefinition, 16, 16);
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