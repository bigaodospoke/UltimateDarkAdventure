package com.uda.client.model;

import com.uda.entity.BlindExecutioner;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Illager de manto longo e capuz, com venda nos olhos, gola alta e o pingente
 * (Olho de Agamoto) no peito. A venda some quando ele ascende.
 * Layout da textura: tools/texturas/gerar_texturas.mjs (blindExecutioner).
 */
public class BlindExecutionerModel extends HumanoidModel<BlindExecutioner> {
    private final ModelPart blindfold;
    private float awaken;
    private float whirl;
    private boolean transforming;

    public BlindExecutionerModel(ModelPart root) {
        super(root);
        this.blindfold = this.head.getChild("blindfold");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 10, 8), PartPose.ZERO);
        head.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2, 4, 2), PartPose.offset(0.0F, -2.0F, 0.0F));
        head.addOrReplaceChild("blindfold",
                CubeListBuilder.create().texOffs(64, 0).addBox(-4.0F, -6.0F, -4.0F, 8, 2, 8, new CubeDeformation(0.25F)), PartPose.ZERO);
        // capuz do manto
        root.addOrReplaceChild("hat",
                CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -10.0F, -4.0F, 8, 12, 8, new CubeDeformation(0.45F)), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8, 12, 6)
                .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8, 20, 6, new CubeDeformation(0.5F)) // manto
                .texOffs(64, 10).addBox(-5.0F, -1.0F, -4.0F, 10, 3, 8), // gola
                PartPose.ZERO);
        body.addOrReplaceChild("pendant",
                CubeListBuilder.create().texOffs(100, 0).addBox(-2.5F, 2.5F, -4.5F, 5, 5, 1), PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 46).addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 46).mirror().addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(BlindExecutioner entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.awaken = entity.getAwakenProgress(partialTick);
        this.whirl = entity.getWhirlProgress(partialTick);
        this.transforming = entity.isTransforming();
        this.blindfold.visible = this.awaken < 0.5F;
    }

    @Override
    public void setupAnim(BlindExecutioner entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // o HumanoidModel nunca zera o zRot da cabeça, e o modelo é compartilhado entre os mobs
        this.head.zRot = 0.0F;

        if (this.transforming) {
            // Ascensão: braços abertos para o alto, cabeça para trás.
            float rise = Mth.sin(Math.min(this.awaken * 2.0F, 1.0F) * Mth.HALF_PI);
            this.rightArm.xRot = -2.6F * rise;
            this.leftArm.xRot = -2.6F * rise;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.rightArm.zRot = 0.45F * rise;
            this.leftArm.zRot = -0.45F * rise;
            this.head.xRot = -0.6F * rise;
            this.head.yRot = 0.0F;
        } else if (this.whirl > 0.0F) {
            // Giro do Carrasco: braços esticados para os lados (o corpo gira no renderizador).
            this.rightArm.xRot = -0.2F;
            this.leftArm.xRot = -0.2F;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.rightArm.zRot = Mth.HALF_PI;
            this.leftArm.zRot = -Mth.HALF_PI;
        } else if (entity.isAggressive()) {
            // Dois machados erguidos; o braço que golpeia desce com força.
            this.rightArm.xRot = -1.7F + Mth.cos(ageInTicks * 0.09F) * 0.1F;
            this.leftArm.xRot = -1.7F + Mth.cos(ageInTicks * 0.09F + 1.5F) * 0.1F;
            this.rightArm.yRot = -0.2F;
            this.leftArm.yRot = 0.2F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            if (this.attackTime > 0.0F) {
                float swing = Mth.sin(this.attackTime * Mth.PI);
                float swing2 = Mth.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * Mth.PI);
                ModelPart arm = this.getArm(this.attackArm(entity));
                arm.xRot += swing * 2.2F - swing2 * 0.4F;
            }
            AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);
        } else if (!entity.isAwakened()) {
            // Cego e dormente: inclina a cabeça como quem escuta.
            this.head.zRot = Mth.sin(ageInTicks * 0.05F) * 0.15F;
        }
        this.hat.copyFrom(this.head);
    }

    private HumanoidArm attackArm(BlindExecutioner entity) {
        HumanoidArm main = entity.getMainArm();
        return entity.swingingArm == InteractionHand.MAIN_HAND ? main : main.getOpposite();
    }
}
