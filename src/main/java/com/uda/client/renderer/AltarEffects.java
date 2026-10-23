package com.uda.client.renderer;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uda.client.aura.efeito.Sprite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** Pincel dos efeitos dos altares (desenhados com luz somada, usando o atlas da aura). */
final class AltarEffects {
    private AltarEffects() {
    }

    static int color(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    /** Disco deitado no plano XZ, centrado em (x, y, z), girado por angle. */
    static void disc(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z, float radius, float angle, Sprite s, int color) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotation(angle));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -radius, 0, -radius, s.u0(), s.v0(), color);
        vertex(consumer, pose, -radius, 0, radius, s.u0(), s.v1(), color);
        vertex(consumer, pose, radius, 0, radius, s.u1(), s.v1(), color);
        vertex(consumer, pose, radius, 0, -radius, s.u1(), s.v0(), color);
        poseStack.popPose();
    }

    /** Quadrado virado para a câmera. */
    static void billboard(VertexConsumer consumer, PoseStack poseStack, float x, float y, float z, float half, Sprite s, int color) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        Quaternionf camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        poseStack.mulPose(camera);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -half, -half, 0, s.u0(), s.v1(), color);
        vertex(consumer, pose, half, -half, 0, s.u1(), s.v1(), color);
        vertex(consumer, pose, half, half, 0, s.u1(), s.v0(), color);
        vertex(consumer, pose, -half, half, 0, s.u0(), s.v0(), color);
        poseStack.popPose();
    }

    /** Coluna vertical de luz (dois planos cruzados). */
    static void column(VertexConsumer consumer, PoseStack poseStack, float x, float y0, float z, float height, float width, float angle, Sprite s,
            int color) {
        poseStack.pushPose();
        poseStack.translate(x, y0, z);
        poseStack.mulPose(Axis.YP.rotation(angle));
        PoseStack.Pose pose = poseStack.last();
        for (int k = 0; k < 2; k++) {
            float cx = k == 0 ? width : 0, cz = k == 0 ? 0 : width;
            // o FEIXE brilha no meio do v: o v atravessa a largura da coluna
            vertex(consumer, pose, -cx, 0, -cz, s.u0(), s.v0(), color);
            vertex(consumer, pose, cx, 0, cz, s.u0(), s.v1(), color);
            vertex(consumer, pose, cx, height, cz, s.u1(), s.v1(), color & 0x00FFFFFF);
            vertex(consumer, pose, -cx, height, -cz, s.u1(), s.v0(), color & 0x00FFFFFF);
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color) {
        consumer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
