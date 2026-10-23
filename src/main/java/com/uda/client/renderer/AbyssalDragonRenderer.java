package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.uda.client.ModModelLayers;
import com.uda.client.model.AbyssalDragonModel;
import com.uda.entity.AbyssalDragon;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AbyssalDragonRenderer extends MobRenderer<AbyssalDragon, AbyssalDragonModel> {
    private static final EyeTextures TEXTURES = EyeTextures.of("abyssal_dragon");
    private static final float SCALE = 1.6F;

    public AbyssalDragonRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalDragonModel(context.bakeLayer(ModModelLayers.ABYSSAL_DRAGON)), 3.0F);
        this.addLayer(new EyeGlowLayer<>(this, TEXTURES::glow));
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalDragon dragon) {
        return TEXTURES.base(dragon);
    }

    @Override
    protected void scale(AbyssalDragon dragon, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected boolean isShaking(AbyssalDragon dragon) {
        return dragon.isTransforming() || super.isShaking(dragon);
    }

    @Override
    protected void setupRotations(AbyssalDragon dragon, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(dragon, poseStack, bob, yBodyRot, partialTick, scale);
        // inclina o corpo inteiro conforme sobe/mergulha
        float center = dragon.getBbHeight() * 0.5F;
        poseStack.translate(0.0F, center, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-dragon.getViewXRot(partialTick)));
        poseStack.translate(0.0F, -center, 0.0F);
    }

    @Override
    protected float getFlipDegrees(AbyssalDragon dragon) {
        return 0.0F; // não tomba de lado ao morrer
    }
}
