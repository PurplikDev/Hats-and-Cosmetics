package com.purplik.hat.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class RatModel extends HumanoidModel<LivingEntity> {

    public RatModel(ModelPart modelPart) {
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

        PartDefinition rat = partDefinition.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition leftBottomLeg_r1 = rat.addOrReplaceChild("leftBottomLeg_r1", CubeListBuilder.create().texOffs(16, 10).addBox(-2.1392F, 1.8498F, 0.4619F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -0.0025F, 1.0495F, -0.2822F));

        PartDefinition rightBottomLeg_r1 = rat.addOrReplaceChild("rightBottomLeg_r1", CubeListBuilder.create().texOffs(16, 10).addBox(-2.5272F, 1.6693F, -2.4571F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -0.5976F, 1.0413F, -0.4253F));

        PartDefinition tail_r1 = rat.addOrReplaceChild("tail_r1", CubeListBuilder.create().texOffs(0, 17).addBox(-6.2056F, -2.1501F, -1.059F, 4.0F, 1.0F, 1.0F, new CubeDeformation(-0.25F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -1.1579F, 0.1694F, -1.734F));

        PartDefinition rightTopLeg_r1 = rat.addOrReplaceChild("rightTopLeg_r1", CubeListBuilder.create().texOffs(16, 10).addBox(0.7445F, -0.1436F, -3.1976F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -0.913F, 0.1186F, -1.5152F));

        PartDefinition leftTopLeg_r1 = rat.addOrReplaceChild("leftTopLeg_r1", CubeListBuilder.create().texOffs(16, 10).addBox(0.7445F, -0.1374F, 1.1697F, 2.0F, 3.0F, 2.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -1.3493F, 0.1186F, -1.5152F));

        PartDefinition rightEar_r1 = rat.addOrReplaceChild("rightEar_r1", CubeListBuilder.create().texOffs(0, 0).addBox(1.6887F, -4.7637F, -2.3977F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -1.0333F, 1.2942F, -1.1289F));

        PartDefinition leftEar_r1 = rat.addOrReplaceChild("leftEar_r1", CubeListBuilder.create().texOffs(8, 17).addBox(1.6801F, -4.7637F, 0.3704F, 1.0F, 2.0F, 2.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -0.3385F, 0.7864F, -0.357F));

        PartDefinition body_r1 = rat.addOrReplaceChild("body_r1", CubeListBuilder.create().texOffs(0, 0).addBox(-3.7298F, -2.9312F, -3.0143F, 7.0F, 4.0F, 6.0F, new CubeDeformation(-0.75F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -1.0769F, 0.4703F, -1.3315F));

        PartDefinition head_r1 = rat.addOrReplaceChild("head_r1", CubeListBuilder.create().texOffs(0, 10).addBox(0.3175F, -3.7637F, -2.0143F, 4.0F, 3.0F, 4.0F, new CubeDeformation(-0.5F)), PartPose.offsetAndRotation(-1.9537F, -0.8518F, 2.0143F, -0.5067F, 1.0663F, -0.5651F));

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