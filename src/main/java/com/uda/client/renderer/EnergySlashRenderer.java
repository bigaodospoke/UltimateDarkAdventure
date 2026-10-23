package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.TiposRender;
import com.uda.entity.EnergySlash;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Meia-lua de energia: três faixas brilhantes (halo roxo, roxo neon e miolo branco) curvadas para a frente. */
public class EnergySlashRenderer extends EntityRenderer<EnergySlash> {
    private static final int SEGMENTS = 16;

    public EnergySlashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EnergySlash slash, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = (slash.tickCount + partialTick) / EnergySlash.LIFETIME;
        float fade = Mth.clamp((1.0F - age) * 2.5F, 0.0F, 1.0F) * Mth.clamp(age * 8.0F, 0.0F, 1.0F);
        float radius = (slash.isSupreme() ? 1.6F : 1.25F) * (0.8F + age * 0.6F);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, slash.yRotO, slash.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, slash.xRotO, slash.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(28.0F)); // corte na diagonal: visível também de trás
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        PoseStack.Pose pose = poseStack.last();
        arc(glow, pose, radius, 0.42F, color(0.45F * fade, 0x8A2BE2));
        arc(glow, pose, radius, 0.24F, color(0.9F * fade, slash.isSupreme() ? 0xE28BFF : 0xC45CFF));
        arc(glow, pose, radius, 0.07F, color(fade, 0xFFFFFF));
        poseStack.popPose();
        super.render(slash, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static int color(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | rgb;
    }

    /** Arco horizontal de -75° a 75°, abaulado para a frente (+z), mais fino nas pontas. */
    private static void arc(VertexConsumer consumer, PoseStack.Pose pose, float radius, float width, int color) {
        Sprite s = Sprites.FEIXE;
        float limit = 75.0F * Mth.DEG_TO_RAD;
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = -limit + 2 * limit * i / SEGMENTS, a1 = -limit + 2 * limit * (i + 1) / SEGMENTS;
            float w0 = width * (float) Math.pow(Mth.cos(a0), 1.5), w1 = width * (float) Math.pow(Mth.cos(a1), 1.5);
            float x0 = Mth.sin(a0) * radius, z0 = Mth.cos(a0) * radius * 0.45F;
            float x1 = Mth.sin(a1) * radius, z1 = Mth.cos(a1) * radius * 0.45F;
            float u0 = s.u0() + (s.u1() - s.u0()) * i / SEGMENTS, u1 = s.u0() + (s.u1() - s.u0()) * (i + 1) / SEGMENTS;
            vertex(consumer, pose, x0, -w0, z0, u0, s.v0(), color);
            vertex(consumer, pose, x0, w0, z0, u0, s.v1(), color);
            vertex(consumer, pose, x1, w1, z1, u1, s.v1(), color);
            vertex(consumer, pose, x1, -w1, z1, u1, s.v0(), color);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergySlash slash) {
        return TiposRender.ATLAS;
    }
}
