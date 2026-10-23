package com.uda.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uda.entity.HollowWatcher;

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
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;

/**
 * Esqueleto de ossos finos (como o Wither Skeleton), com caixa torácica vazada,
 * um núcleo ocular no peito e ombreiras de osso com espinhos.
 * Layout da textura: tools/texturas/gerar_texturas.mjs (hollowWatcher).
 */
public class HollowWatcherModel extends HumanoidModel<HollowWatcher> {
    private final ModelPart core;
    private float awaken;

    public HollowWatcherModel(ModelPart root) {
        super(root);
        this.core = this.body.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4), PartPose.ZERO);
        body.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 32).addBox(-2.5F, -2.5F, -1.5F, 5, 5, 3), PartPose.offset(0.0F, 4.0F, -1.5F));

        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2), PartPose.offset(-5.0F, 2.0F, 0.0F));
        rightArm.addOrReplaceChild("right_pauldron", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-2.5F, -3.5F, -2.0F, 4, 3, 4)
                .texOffs(32, 32).addBox(-2.0F, -5.5F, -0.5F, 1, 2, 1), PartPose.ZERO);

        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.0F, 2, 12, 2), PartPose.offset(5.0F, 2.0F, 0.0F));
        leftArm.addOrReplaceChild("left_pauldron", CubeListBuilder.create().mirror()
                .texOffs(16, 32).addBox(-1.5F, -3.5F, -2.0F, 4, 3, 4)
                .texOffs(32, 32).addBox(1.0F, -5.5F, -0.5F, 1, 2, 1), PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.0F, 0.0F, -1.0F, 2, 12, 2), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(HollowWatcher entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.awaken = entity.getAwakenProgress(partialTick);
        this.rightArmPose = ArmPose.EMPTY;
        this.leftArmPose = ArmPose.EMPTY;
        ItemStack stack = entity.getItemInHand(InteractionHand.MAIN_HAND);
        if (stack.getItem() instanceof BowItem && entity.isAggressive()) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.rightArmPose = ArmPose.BOW_AND_ARROW;
            } else {
                this.leftArmPose = ArmPose.BOW_AND_ARROW;
            }
        }
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
    }

    @Override
    public void setupAnim(HollowWatcher entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ItemStack stack = entity.getMainHandItem();
        if (entity.isAggressive() && !(stack.getItem() instanceof BowItem)) {
            // Sem arco: ataca com os braços estendidos, como o esqueleto vanilla.
            float swing = Mth.sin(this.attackTime * Mth.PI);
            float swing2 = Mth.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * Mth.PI);
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightArm.yRot = -(0.1F - swing * 0.6F);
            this.leftArm.yRot = 0.1F - swing * 0.6F;
            this.rightArm.xRot = -Mth.HALF_PI - (swing * 1.2F - swing2 * 0.4F);
            this.leftArm.xRot = -Mth.HALF_PI - (swing * 1.2F - swing2 * 0.4F);
            AnimationUtils.bobArms(this.rightArm, this.leftArm, ageInTicks);
        }

        // Desperto: o núcleo pulsa e a cabeça treme de leve.
        float pulse = 1.0F + Mth.sin(ageInTicks * 0.25F) * 0.07F * this.awaken;
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;
        // (o HumanoidModel nunca zera o zRot da cabeça, e o modelo é compartilhado: sempre atribuir)
        this.head.zRot = Mth.sin(ageInTicks * 0.9F) * 0.04F * this.awaken;
        this.hat.copyFrom(this.head);
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack poseStack) {
        // Braços finos: o item fica 1px mais para fora, como no esqueleto vanilla.
        float offset = side == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        ModelPart arm = this.getArm(side);
        arm.x += offset;
        arm.translateAndRotate(poseStack);
        arm.x -= offset;
    }
}
