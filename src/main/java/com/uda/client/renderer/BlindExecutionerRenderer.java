package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uda.client.ModModelLayers;
import com.uda.client.model.BlindExecutionerModel;
import com.uda.entity.BlindExecutioner;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class BlindExecutionerRenderer extends HumanoidMobRenderer<BlindExecutioner, BlindExecutionerModel> {
    private static final EyeTextures TEXTURES = EyeTextures.of("blind_executioner");

    public BlindExecutionerRenderer(EntityRendererProvider.Context context) {
        super(context, new BlindExecutionerModel(context.bakeLayer(ModModelLayers.BLIND_EXECUTIONER)), 0.55F);
        this.addLayer(new EyeGlowLayer<>(this, TEXTURES::glow));
    }

    @Override
    public ResourceLocation getTextureLocation(BlindExecutioner entity) {
        return TEXTURES.base(entity);
    }

    @Override
    protected boolean isShaking(BlindExecutioner entity) {
        return entity.isTransforming() || super.isShaking(entity);
    }

    @Override
    protected void setupRotations(BlindExecutioner entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        float whirl = entity.getWhirlProgress(partialTick);
        if (whirl > 0.0F) {
            // Duas voltas completas com os machados abertos.
            poseStack.mulPose(Axis.YP.rotationDegrees(whirl * 720.0F));
        }
    }
}
