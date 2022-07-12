package com.purplik.hat.model.raremodels;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class CaptainsHatModel extends HumanoidModel<LivingEntity> {

    public CaptainsHatModel(ModelPart modelPart) {
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

        PartDefinition hat = partDefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition capHat = hat.addOrReplaceChild("capHat", CubeListBuilder.create().texOffs(0, 13).addBox(-4.0F, 0.625F, 4.5F, 8.0F, 0.0F, 2.0F, new CubeDeformation(0.25F))
                .texOffs(0, 7).addBox(-4.0F, -1.375F, -4.5F, 8.0F, 2.0F, 4.0F, new CubeDeformation(0.25F))
                .texOffs(0, 0).addBox(-4.0F, -2.375F, 0.0F, 8.0F, 3.0F, 4.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, -6.625F, 0.0F, -3.1416F, 0.0F, 3.1416F));

        return LayerDefinition.create(meshDefinition, 32, 32);
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