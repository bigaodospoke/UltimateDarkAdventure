package com.uda.client.renderer;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uda.entity.Awakenable;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Desenha olhos e rachaduras em brilho total (como os olhos do Enderman), pulsando.
 * Dormente: brilho fraco e lento ("energia selada"). Desperto: pulsação forte.
 */
public class EyeGlowLayer<T extends LivingEntity & Awakenable, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private final Function<T, ResourceLocation> texture;

    public EyeGlowLayer(RenderLayerParent<T, M> parent, Function<T, ResourceLocation> texture) {
        super(parent);
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity, float limbSwing, float limbSwingAmount,
            float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        float awaken = entity.getAwakenProgress(partialTick);
        float dormant = 0.3F + 0.15F * Mth.sin(ageInTicks * 0.05F);
        float awakened = entity.isEnraged() ? 0.85F + 0.15F * Mth.sin(ageInTicks * 0.5F) : 0.8F + 0.2F * Mth.sin(ageInTicks * 0.18F);
        int level = (int) (Mth.clamp(Mth.lerp(awaken, dormant, awakened), 0.0F, 1.0F) * 255.0F);
        int color = FastColor.ARGB32.color(255, level, level, level);
        this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(this.texture.apply(entity))),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}
