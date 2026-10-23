package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uda.client.ModModelLayers;
import com.uda.client.model.HollowWatcherModel;
import com.uda.entity.HollowWatcher;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class HollowWatcherRenderer extends HumanoidMobRenderer<HollowWatcher, HollowWatcherModel> {
    private static final EyeTextures TEXTURES = EyeTextures.of("hollow_watcher");

    public HollowWatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new HollowWatcherModel(context.bakeLayer(ModModelLayers.HOLLOW_WATCHER)), 0.6F);
        this.addLayer(new EyeGlowLayer<>(this, TEXTURES::glow));
    }

    @Override
    public ResourceLocation getTextureLocation(HollowWatcher entity) {
        return TEXTURES.base(entity);
    }

    @Override
    protected void scale(HollowWatcher entity, PoseStack poseStack, float partialTick) {
        // Alto como o Wither Skeleton.
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    protected boolean isShaking(HollowWatcher entity) {
        return entity.isTransforming() || super.isShaking(entity);
    }
}
