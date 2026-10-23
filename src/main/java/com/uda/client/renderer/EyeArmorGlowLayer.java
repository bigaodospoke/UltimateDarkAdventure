package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uda.UltimateDarkAdventure;
import com.uda.item.Ocular;
import com.uda.item.OcularArmorItem;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Olhos e linhas da Armadura Ocular desperta brilhando no escuro (pulsando), por cima da armadura normal. */
public class EyeArmorGlowLayer<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final ResourceLocation[][] TEXTURES = new ResourceLocation[3][2];

    static {
        String[] forms = {"sealed", "awakened", "supreme"};
        for (int tier = 1; tier < 3; tier++) {
            for (int layer = 1; layer <= 2; layer++) {
                TEXTURES[tier][layer - 1] = UltimateDarkAdventure.id("textures/models/armor/ocular_" + forms[tier] + "_layer_" + layer + "_glow.png");
            }
        }
    }

    private final A inner;
    private final A outer;

    public EyeArmorGlowLayer(RenderLayerParent<T, M> parent, A inner, A outer) {
        super(parent);
        this.inner = inner;
        this.outer = outer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            int tier = Ocular.tier(stack);
            if (!(stack.getItem() instanceof OcularArmorItem) || tier < Ocular.AWAKENED) {
                continue;
            }
            boolean legs = slot == EquipmentSlot.LEGS;
            A model = legs ? this.inner : this.outer;
            this.getParentModel().copyPropertiesTo(model);
            model.setAllVisible(false);
            switch (slot) {
                case HEAD -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                case LEGS -> {
                    model.body.visible = true;
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                default -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
            }
            float speed = tier >= Ocular.SUPREME ? 0.25F : 0.12F;
            int level = (int) ((0.75F + 0.25F * Mth.sin(ageInTicks * speed + slot.ordinal())) * 255.0F);
            int color = FastColor.ARGB32.color(255, level, level, level);
            model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(TEXTURES[tier][legs ? 1 : 0])), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, color);
        }
    }
}
