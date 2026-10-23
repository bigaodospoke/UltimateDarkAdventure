package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.TiposRender;
import com.uda.entity.GlowingEye;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** Olho brilhante virado para a câmera, com halo roxo e pupila em fenda (orbes e bombas oculares). */
public class GlowingEyeRenderer<T extends Entity & GlowingEye> extends EntityRenderer<T> {
    public GlowingEyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(T orb, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float t = orb.tickCount + partialTick;
        float size = orb.eyeSize() * (1.0F + 0.08F * Mth.sin(t * 0.6F));
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.12F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        quad(glow, pose, size * 2.4F, Sprites.BRILHO, 0x908A2BE2);
        quad(glow, pose, size * 1.3F, Sprites.ANEL, 0xC0C45CFF);
        quad(glow, pose, size, Sprites.OLHO[0], 0xFFE88CFF);
        quad(buffer.getBuffer(TiposRender.ESCURO), pose, size, Sprites.PUPILA, 0xE6000000);
        poseStack.popPose();
        super.render(orb, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float half, Sprite sprite, int color) {
        vertex(consumer, pose, -half, -half, sprite.u0(), sprite.v1(), color);
        vertex(consumer, pose, half, -half, sprite.u1(), sprite.v1(), color);
        vertex(consumer, pose, half, half, sprite.u1(), sprite.v0(), color);
        vertex(consumer, pose, -half, half, sprite.u0(), sprite.v0(), color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int color) {
        consumer.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T orb) {
        return TiposRender.ATLAS;
    }
}
