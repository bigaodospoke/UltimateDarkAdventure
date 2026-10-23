package com.uda.client.model;

import com.uda.entity.AbyssalDragon;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Dragão Abissal (geometria em assets/uda/geo/abyssal_dragon.json). Asas batendo, pescoço e cauda
 * ondulando, pernas recolhidas em voo, boca aberta no sopro e asas fechadas no mergulho.
 */
public class AbyssalDragonModel extends HierarchicalModel<AbyssalDragon> {
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] neck = new ModelPart[3];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] tail = new ModelPart[6];
    private final ModelPart wingLeft, wingLeftTip, wingRight, wingRightTip;
    private final ModelPart[] thighs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private float awaken;
    private float attackTime;
    private boolean transforming;

    public AbyssalDragonModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        ModelPart parent = this.body;
        for (int i = 0; i < 3; i++) {
            this.neck[i] = parent.getChild("neck" + (i + 1));
            parent = this.neck[i];
        }
        this.head = parent.getChild("head");
        this.jaw = this.head.getChild("jaw");
        parent = this.body;
        for (int i = 0; i < 6; i++) {
            this.tail[i] = parent.getChild("tail" + (i + 1));
            parent = this.tail[i];
        }
        this.wingLeft = this.body.getChild("wing_left");
        this.wingLeftTip = this.wingLeft.getChild("wing_left_tip");
        this.wingRight = this.body.getChild("wing_right");
        this.wingRightTip = this.wingRight.getChild("wing_right_tip");
        String[] legs = {"front_leg_left", "front_leg_right", "hind_leg_left", "hind_leg_right"};
        for (int i = 0; i < 4; i++) {
            this.thighs[i] = this.body.getChild(legs[i]);
            this.shins[i] = this.thighs[i].getChild(legs[i] + "_shin");
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(AbyssalDragon dragon, float limbSwing, float limbSwingAmount, float partialTick) {
        this.awaken = dragon.getAwakenProgress(partialTick);
        this.attackTime = dragon.getAttackTicks() + partialTick;
        this.transforming = dragon.isTransforming();
    }

    @Override
    public void setupAnim(AbyssalDragon dragon, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float t = ageInTicks;
        int attack = dragon.getAttack();
        float flapSpeed = 0.17F + this.awaken * 0.07F;
        float flap = Mth.sin(t * flapSpeed);

        // asas: batem; no mergulho ficam recolhidas para trás; ao despertar abrem e ficam esticadas
        float wing, tip, sweep = 0.0F;
        if (this.transforming) {
            wing = -0.45F + Mth.sin(t * 0.6F) * 0.05F;
            tip = -0.2F;
        } else if (attack == AbyssalDragon.DIVE && this.attackTime > 25) {
            wing = -0.1F;
            tip = 1.1F;
            sweep = 0.8F;
        } else if (dragon.deathTime > 0) {
            wing = 0.7F;
            tip = 0.6F;
        } else {
            wing = flap * 0.6F;
            tip = Mth.sin(t * flapSpeed - 0.8F) * 0.5F + 0.1F;
        }
        this.wingLeft.zRot = wing;
        this.wingLeftTip.zRot = tip;
        this.wingLeft.yRot = sweep;
        this.wingRight.zRot = -wing;
        this.wingRightTip.zRot = -tip;
        this.wingRight.yRot = -sweep;
        // o corpo sobe quando as asas descem
        this.body.y += Mth.sin(t * flapSpeed + Mth.PI) * 1.2F;

        // pescoço e cabeça
        float headUp = this.transforming ? -0.35F : 0.0F;
        for (int i = 0; i < 3; i++) {
            this.neck[i].xRot = headUp + Mth.sin(t * 0.08F + i * 0.6F) * 0.06F;
            this.neck[i].yRot = Mth.sin(t * 0.05F + i * 0.8F) * 0.08F;
        }
        float jawOpen = 0.08F + Mth.sin(t * 0.05F) * 0.04F;
        if (attack == AbyssalDragon.BREATH) {
            float open = Mth.clamp(this.attackTime / AbyssalDragon.BREATH_WINDUP, 0.0F, 1.0F);
            jawOpen = 0.75F * open;
            this.head.xRot = 0.25F * open;
        } else if (this.transforming || attack == AbyssalDragon.EXPLOSIONS && this.attackTime < 30) {
            jawOpen = 0.9F;
        } else if (attack == AbyssalDragon.ROAR && this.attackTime < 45) {
            float roar = Mth.clamp(this.attackTime / 15.0F, 0.0F, 1.0F);
            jawOpen = 1.05F * roar;
            this.head.xRot = -0.45F * roar;
        }
        this.jaw.xRot = jawOpen;

        // cauda ondulando
        for (int i = 0; i < 6; i++) {
            this.tail[i].yRot = Mth.sin(t * 0.12F - i * 0.6F) * 0.13F;
            this.tail[i].xRot = 0.04F + Mth.sin(t * 0.1F - i * 0.5F) * 0.06F;
        }
        // pernas recolhidas para trás
        for (int i = 0; i < 4; i++) {
            this.thighs[i].xRot = 1.0F + Mth.sin(t * flapSpeed + i) * 0.05F;
            this.shins[i].xRot = 0.7F;
        }
    }
}
