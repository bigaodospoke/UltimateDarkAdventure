package com.uda.client.renderer;

import com.uda.UltimateDarkAdventure;
import com.uda.entity.EyeArrow;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class EyeArrowRenderer extends ArrowRenderer<EyeArrow> {
    private static final ResourceLocation TEXTURE = UltimateDarkAdventure.id("textures/entity/projectiles/eye_arrow.png");

    public EyeArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(EyeArrow entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(EyeArrow entity, BlockPos pos) {
        return 15; // a flecha brilha no escuro
    }
}
