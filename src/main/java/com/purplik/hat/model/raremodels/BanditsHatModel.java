package com.purplik.hat.model.raremodels;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class BanditsHatModel extends HumanoidModel<LivingEntity> {

    public BanditsHatModel(ModelPart modelPart) {
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

        PartDefinition part_1 = hat.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(26, 0).addBox(-7.5691F, 2.3981F, -6.0F, 2.0F, 0.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.625F, 0.0F, -3.1416F, 0.0F, 2.8798F));

        PartDefinition part_2 = hat.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(0, 30).addBox(5.5691F, 2.3981F, -6.0F, 2.0F, 0.0F, 12.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -6.625F, 0.0F, 3.1416F, 0.0F, -2.8798F));

        PartDefinition flat_part = hat.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, 0.875F, -7.0F, 12.0F, 0.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(24, 22).addBox(-4.0F, -2.375F, -4.0F, 8.0F, 3.0F, 8.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, -6.625F, 0.0F, -3.1416F, 0.0F, 3.1416F));

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