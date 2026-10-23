package com.uda.client.model;

import com.uda.entity.BlackEye;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * O Olho Negro (geometria em assets/uda/geo/black_eye.json). O olho acompanha o alvo, as pálpebras
 * se abrem ao despertar (e piscam), os três anéis giram, as seis asas batem devagar e as correntes,
 * que ficam penduradas, sobem e flutuam quando ele desperta.
 */
public class BlackEyeModel extends HierarchicalModel<BlackEye> {
    private static final int CHAINS = 4;
    private static final int LINKS = 8;
    /** Curva de cada elo quando a corrente está enrolada no olho (raio ~12.5px com elos de 4px). */
    private static final float WRAP = 0.321F;

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart lidTop, lidBottom;
    private final ModelPart ringFront, ringSide, ringHalo;
    private final ModelPart[] wingsLeft = new ModelPart[3];
    private final ModelPart[] wingsRight = new ModelPart[3];
    private final ModelPart[][] chains = new ModelPart[CHAINS][LINKS];
    private float awaken;
    private boolean blinking;

    public BlackEyeModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.lidTop = this.core.getChild("lid_top");
        this.lidBottom = this.core.getChild("lid_bottom");
        this.ringFront = root.getChild("ring_front");
        this.ringSide = root.getChild("ring_side");
        this.ringHalo = root.getChild("ring_halo");
        for (int k = 0; k < 3; k++) {
            this.wingsLeft[k] = root.getChild("wing_left_" + k);
            this.wingsRight[k] = root.getChild("wing_right_" + k);
        }
        for (int c = 0; c < CHAINS; c++) {
            ModelPart parent = root;
            for (int k = 0; k < LINKS; k++) {
                this.chains[c][k] = parent.getChild("chain_" + c + "_" + k);
                parent = this.chains[c][k];
            }
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(BlackEye eye, float limbSwing, float limbSwingAmount, float partialTick) {
        this.awaken = eye.getAwakenProgress(partialTick);
        this.blinking = eye.isBlinking();
    }

    @Override
    public void setupAnim(BlackEye eye, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float t = ageInTicks;
        int attack = eye.getAttack();

        // o olho olha para o alvo
        this.core.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.core.xRot = headPitch * Mth.DEG_TO_RAD;
        // pálpebras: fechadas dormente, abertas desperto (fecham numa piscada)
        float open = this.awaken * (this.blinking ? 0.1F : 1.0F);
        if (attack == BlackEye.BEAM_CHARGE) {
            open = Math.max(open, 0.35F + 0.1F * Mth.sin(t * 1.3F));
        }
        this.lidTop.xRot = 1.45F * open;
        this.lidBottom.xRot = -1.45F * open;

        // anéis girando (mais rápido desperto e durante o raio)
        float spin = 0.02F + this.awaken * 0.035F + (attack == BlackEye.BEAM ? 0.05F : 0.0F);
        this.ringFront.zRot = t * spin;
        this.ringSide.xRot = t * spin * 1.3F;
        this.ringHalo.yRot = -t * spin * 0.7F;

        // asas: batem devagar (para trás e para frente) e abrem mais quando desperto
        for (int k = 0; k < 3; k++) {
            float s = Mth.sin(t * 0.07F + k * 0.9F);
            float spread = 0.35F - this.awaken * 0.25F;
            this.wingsLeft[k].yRot = -spread + s * 0.25F;
            this.wingsRight[k].yRot = spread - s * 0.25F;
        }

        // correntes: dormente, enroladas em volta do olho (selado); desperto, soltas para cima, ondulando
        for (int c = 0; c < CHAINS; c++) {
            ModelPart first = this.chains[c][0];
            first.xRot = -Mth.HALF_PI - this.awaken * 0.7F + Mth.sin(t * 0.05F + c) * 0.1F * this.awaken;
            for (int k = 1; k < LINKS; k++) {
                ModelPart link = this.chains[c][k];
                float wave = Mth.sin(t * 0.12F - k * 0.7F + c * 1.3F) * 0.3F;
                link.xRot = Mth.lerp(this.awaken, WRAP, wave);
                link.zRot = Mth.cos(t * 0.1F - k * 0.5F + c) * 0.25F * this.awaken;
            }
        }
    }
}
