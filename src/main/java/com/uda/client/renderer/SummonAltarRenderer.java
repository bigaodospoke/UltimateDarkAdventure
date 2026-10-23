package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.uda.block.SummonAltarBlockEntity;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.TiposRender;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Invocação nos altares: o selo se abre no chão e uma coluna de energia sobe até onde o boss vai aparecer. */
public class SummonAltarRenderer implements BlockEntityRenderer<SummonAltarBlockEntity> {
    public SummonAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SummonAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        int ritual = altar.getRitualTicks();
        if (ritual < 0 || altar.getLevel() == null) {
            return;
        }
        float time = altar.getLevel().getGameTime() + partialTick;
        float progress = Math.min(ritual + partialTick, SummonAltarBlockEntity.RITUAL_TICKS) / SummonAltarBlockEntity.RITUAL_TICKS;
        float pulse = 0.7F + 0.3F * Mth.sin(time * (0.15F + progress * 0.5F));
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        float top = altar.isVoid() ? 1.02F : 0.78F;
        AltarEffects.disc(glow, poseStack, 0.5F, 0.03F, 0.5F, 1.0F + progress * 4.0F, time * 0.02F, Sprites.SELO_OLHO,
                AltarEffects.color(0.8F * Math.min(1.0F, progress * 2), 0xC45CFF));
        AltarEffects.disc(glow, poseStack, 0.5F, 0.04F, 0.5F, 0.8F + progress * 3.0F, -time * 0.03F, Sprites.CIRCULO_RUNAS,
                AltarEffects.color(0.5F * progress, 0x8A2BE2));
        AltarEffects.disc(glow, poseStack, 0.5F, top, 0.5F, 0.6F, -time * 0.1F, Sprites.VORTICE, AltarEffects.color(0.8F * pulse, 0xE28BFF));
        float height = (altar.isVoid() ? 6.0F : 9.0F) * progress;
        AltarEffects.column(glow, poseStack, 0.5F, top, 0.5F, height, 0.25F + progress * 0.6F, time * 0.04F, Sprites.FEIXE,
                AltarEffects.color(0.7F * pulse, 0xC45CFF));
        AltarEffects.billboard(glow, poseStack, 0.5F, top + height, 0.5F, 0.5F + progress * 2.0F, Sprites.BRILHO,
                AltarEffects.color(0.6F * pulse, 0x9B30FF));
        if (progress > 0.6F) {
            float open = (progress - 0.6F) / 0.4F;
            AltarEffects.billboard(glow, poseStack, 0.5F, top + height, 0.5F, 0.6F + open * 0.8F,
                    open < 0.5F ? Sprites.OLHO[2] : open < 0.8F ? Sprites.OLHO[1] : Sprites.OLHO[0], AltarEffects.color(open, 0xE88CFF));
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SummonAltarBlockEntity altar) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(SummonAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).inflate(5.0, 0, 5.0).expandTowards(0, 12, 0);
    }
}
