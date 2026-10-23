package com.uda.client.renderer;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uda.client.ModModelLayers;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.TiposRender;
import com.uda.client.model.BlackEyeModel;
import com.uda.entity.BlackEye;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** O Olho Negro e o raio ocular (fino e tremendo na carga, grosso e pulsando quando dispara). */
public class BlackEyeRenderer extends MobRenderer<BlackEye, BlackEyeModel> {
    private static final EyeTextures TEXTURES = EyeTextures.of("black_eye");
    private static final float SCALE = 2.0F;

    public BlackEyeRenderer(EntityRendererProvider.Context context) {
        super(context, new BlackEyeModel(context.bakeLayer(ModModelLayers.BLACK_EYE)), 2.0F);
        this.addLayer(new EyeGlowLayer<>(this, TEXTURES::glow));
    }

    @Override
    public ResourceLocation getTextureLocation(BlackEye eye) {
        return TEXTURES.base(eye);
    }

    @Override
    protected void scale(BlackEye eye, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected boolean isShaking(BlackEye eye) {
        return eye.isTransforming() || super.isShaking(eye);
    }

    @Override
    protected float getFlipDegrees(BlackEye eye) {
        return 0.0F;
    }

    @Override
    public void render(BlackEye eye, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(eye, entityYaw, partialTick, poseStack, buffer, packedLight);
        int attack = eye.getAttack();
        if (attack == BlackEye.SPIN_BEAMS) {
            this.renderSpin(eye, partialTick, poseStack, buffer);
            return;
        }
        if (attack != BlackEye.BEAM_CHARGE && attack != BlackEye.BEAM) {
            return;
        }
        // tudo relativo à posição (interpolada) do Olho, que é a origem do poseStack aqui
        Vec3 origin = new Vec3(Mth.lerp(partialTick, eye.xo, eye.getX()), Mth.lerp(partialTick, eye.yo, eye.getY()),
                Mth.lerp(partialTick, eye.zo, eye.getZ()));
        Vec3 start = new Vec3(0.0, eye.getBbHeight() * 0.5, 0.0);
        Vec3 end = eye.getBeamEnd().subtract(origin);
        Vec3 dir = end.subtract(start).normalize();
        start = start.add(dir.scale(1.8)); // sai da frente do olho
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        float t = eye.tickCount + partialTick;
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        PoseStack.Pose pose = poseStack.last();
        if (attack == BlackEye.BEAM_CHARGE) {
            float charge = Mth.clamp((eye.getAttackTicks() + partialTick) / BlackEye.BEAM_CHARGE_TICKS, 0.0F, 1.0F);
            float flicker = 0.5F + 0.5F * Mth.sin(t * 2.3F);
            beam(glow, pose, start, end, camera, 0.06F + charge * 0.06F, color(0.4F + 0.4F * flicker * charge, 0xB050FF));
        } else {
            float pulse = 0.85F + 0.15F * Mth.sin(t * 1.7F);
            beam(glow, pose, start, end, camera, 0.9F * pulse, color(0.55F, 0x8A2BE2));
            beam(glow, pose, start, end, camera, 0.45F * pulse, color(0.9F, 0xC45CFF));
            beam(glow, pose, start, end, camera, 0.16F, color(1.0F, 0xFFFFFF));
            billboard(glow, pose, end, camera, 1.4F * pulse, Sprites.BRILHO, color(0.9F, 0xC45CFF));
            billboard(glow, pose, start, camera, 1.2F * pulse, Sprites.BRILHO, color(0.8F, 0xE28BFF));
        }
    }

    /** Olhar Final: quatro raios girando (finos e tremendo enquanto carregam). */
    private void renderSpin(BlackEye eye, float partialTick, PoseStack poseStack, MultiBufferSource buffer) {
        Vec3 origin = new Vec3(Mth.lerp(partialTick, eye.xo, eye.getX()), Mth.lerp(partialTick, eye.yo, eye.getY()),
                Mth.lerp(partialTick, eye.zo, eye.getZ()));
        Vec3 center = new Vec3(0.0, eye.getBbHeight() * 0.5, 0.0);
        Vec3 camera = this.entityRenderDispatcher.camera.getPosition().subtract(origin);
        float ticks = eye.getAttackTicks() + partialTick;
        boolean charging = ticks < BlackEye.SPIN_CHARGE;
        float t = eye.tickCount + partialTick;
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        PoseStack.Pose pose = poseStack.last();
        for (int k = 0; k < 4; k++) {
            Vec3 dir = BlackEye.spinDirection(Math.max(ticks, BlackEye.SPIN_CHARGE), k);
            Vec3 worldStart = origin.add(center).add(dir.scale(1.8));
            BlockHitResult hit = eye.level().clip(new ClipContext(worldStart, worldStart.add(dir.scale(BlackEye.SPIN_RANGE)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, eye));
            Vec3 start = worldStart.subtract(origin);
            Vec3 end = hit.getLocation().subtract(origin);
            if (charging) {
                float flicker = 0.5F + 0.5F * Mth.sin(t * 2.3F + k);
                beam(glow, pose, start, end, camera, 0.08F, color(0.3F + 0.4F * flicker * ticks / BlackEye.SPIN_CHARGE, 0xB050FF));
            } else {
                float pulse = 0.85F + 0.15F * Mth.sin(t * 1.7F + k);
                beam(glow, pose, start, end, camera, 0.8F * pulse, color(0.5F, 0x8A2BE2));
                beam(glow, pose, start, end, camera, 0.4F * pulse, color(0.9F, 0xC45CFF));
                beam(glow, pose, start, end, camera, 0.14F, color(1.0F, 0xFFFFFF));
                billboard(glow, pose, end, camera, 1.2F * pulse, Sprites.BRILHO, color(0.85F, 0xC45CFF));
            }
        }
    }

    private static int color(float alpha, int rgb) {
        return ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }

    /** Faixa de A até B sempre virada para a câmera. */
    private static void beam(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 camera, float width, int color) {
        Vec3 mid = a.add(b).scale(0.5);
        Vec3 side = b.subtract(a).cross(camera.subtract(mid)).normalize().scale(width * 0.5);
        Sprite s = Sprites.FEIXE;
        vertex(consumer, pose, a.subtract(side), s.u0(), s.v0(), color);
        vertex(consumer, pose, a.add(side), s.u0(), s.v1(), color);
        vertex(consumer, pose, b.add(side), s.u1(), s.v1(), color);
        vertex(consumer, pose, b.subtract(side), s.u1(), s.v0(), color);
    }

    private static void billboard(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, Vec3 camera, float half, Sprite s, int color) {
        Vec3 toCamera = camera.subtract(at).normalize();
        Vec3 right = toCamera.cross(new Vec3(0, 1, 0)).normalize().scale(half);
        Vec3 up = right.cross(toCamera).normalize().scale(half);
        vertex(consumer, pose, at.subtract(right).subtract(up), s.u0(), s.v1(), color);
        vertex(consumer, pose, at.add(right).subtract(up), s.u1(), s.v1(), color);
        vertex(consumer, pose, at.add(right).add(up), s.u1(), s.v0(), color);
        vertex(consumer, pose, at.subtract(right).add(up), s.u0(), s.v0(), color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p, float u, float v, int color) {
        Vector3f pos = new Vector3f((float) p.x, (float) p.y, (float) p.z);
        consumer.addVertex(pose, pos.x, pos.y, pos.z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
