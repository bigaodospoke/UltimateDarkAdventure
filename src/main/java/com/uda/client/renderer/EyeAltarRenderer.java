package com.uda.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.uda.block.EyeAltarBlockEntity;
import com.uda.client.aura.efeito.Sprite;
import com.uda.client.aura.efeito.Sprites;
import com.uda.client.aura.render.TiposRender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Altar do Olho: o item flutua e gira em cima do altar. No ritual ele sobe, gira cada vez mais rápido,
 * treme e racha; o Selo do Olho se acende no chão, uma coluna de luz sobe e, no fim, um olho se abre sobre o item.
 */
public class EyeAltarRenderer implements BlockEntityRenderer<EyeAltarBlockEntity> {
    public EyeAltarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EyeAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        ItemStack item = altar.getItem();
        if (item.isEmpty() || altar.getLevel() == null) {
            return;
        }
        float time = altar.getLevel().getGameTime() + partialTick;
        int ritual = altar.getRitualTicks();
        float t = ritual < 0 ? -1.0F : Math.min(ritual + partialTick, EyeAltarBlockEntity.RITUAL_TICKS);
        float progress = t < 0 ? 0.0F : t / EyeAltarBlockEntity.RITUAL_TICKS;
        float height = 1.3F + 0.06F * Mth.sin(time * 0.1F) + Math.min(Math.max(t, 0), 40.0F) / 40.0F * 0.45F;

        // o item
        poseStack.pushPose();
        poseStack.translate(0.5F, height, 0.5F);
        if (t >= EyeAltarBlockEntity.CRACK_START) {
            float shake = 0.07F * (t - EyeAltarBlockEntity.CRACK_START) / (EyeAltarBlockEntity.RITUAL_TICKS - EyeAltarBlockEntity.CRACK_START);
            poseStack.translate(Mth.sin(time * 13.1F) * shake, Mth.sin(time * 17.3F) * shake * 0.5F, Mth.cos(time * 11.7F) * shake);
        }
        float spin = t < 0 ? time * 2.0F : time * 2.0F + t * t * 0.12F;
        poseStack.mulPose(Axis.YP.rotationDegrees(spin));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(item, ItemDisplayContext.FIXED, t >= 0 ? LightTexture.FULL_BRIGHT : light,
                OverlayTexture.NO_OVERLAY, poseStack, buffer, altar.getLevel(), (int) altar.getBlockPos().asLong());
        poseStack.popPose();

        if (t < 0) {
            return;
        }
        VertexConsumer glow = buffer.getBuffer(TiposRender.BRILHO);
        float pulse = 0.7F + 0.3F * Mth.sin(time * (0.2F + progress * 0.6F));
        // selo no chão, crescendo e girando
        AltarEffects.disc(glow, poseStack, 0.5F, 0.02F, 0.5F, 0.6F + progress * 1.6F, time * 0.03F, Sprites.SELO_OLHO,
                AltarEffects.color(0.75F * Math.min(1.0F, progress * 3), 0xC45CFF));
        AltarEffects.disc(glow, poseStack, 0.5F, 0.885F, 0.5F, 0.55F, -time * 0.08F, Sprites.VORTICE, AltarEffects.color(0.6F * pulse, 0xE28BFF));
        // anéis que descem em volta do item
        for (int k = 0; k < 2; k++) {
            float phase = (time * 0.04F + k * 0.5F) % 1.0F;
            AltarEffects.disc(glow, poseStack, 0.5F, height + 0.6F - phase * 1.2F, 0.5F, 0.85F - phase * 0.35F, time * 0.1F, Sprites.ANEL,
                    AltarEffects.color(0.7F * progress * Mth.sin(phase * Mth.PI), 0xB050FF));
        }
        // coluna de luz e halo
        AltarEffects.column(glow, poseStack, 0.5F, 0.9F, 0.5F, 1.0F + progress * 4.0F, 0.12F + progress * 0.25F, time * 0.05F, Sprites.FEIXE,
                AltarEffects.color(0.55F * progress, 0xC45CFF));
        AltarEffects.billboard(glow, poseStack, 0.5F, height, 0.5F, 0.6F + progress * 0.6F, Sprites.BRILHO, AltarEffects.color(0.35F + 0.4F * progress * pulse, 0x9B30FF));
        // clarão final
        float flash = (t - (EyeAltarBlockEntity.RITUAL_TICKS - 8)) / 8.0F;
        if (flash > 0) {
            AltarEffects.billboard(glow, poseStack, 0.5F, height, 0.5F, 1.0F + flash * 2.5F, Sprites.BRILHO, AltarEffects.color(flash, 0xFBE6FF));
        }
        // o olho se abre por cima do item (a pupila escura por último: pedir outro buffer encerra o de brilho)
        if (t > EyeAltarBlockEntity.CRACK_START) {
            float appear = Mth.clamp((t - EyeAltarBlockEntity.CRACK_START) / 15.0F, 0.0F, 1.0F);
            Sprite eye = t < EyeAltarBlockEntity.EYE_OPEN - 6 ? Sprites.OLHO[2] : t < EyeAltarBlockEntity.EYE_OPEN ? Sprites.OLHO[1] : Sprites.OLHO[0];
            AltarEffects.billboard(glow, poseStack, 0.5F, height + 0.95F, 0.5F, 0.45F, eye, AltarEffects.color(appear, 0xE88CFF));
            if (t >= EyeAltarBlockEntity.EYE_OPEN) {
                AltarEffects.billboard(buffer.getBuffer(TiposRender.ESCURO), poseStack, 0.5F, height + 0.95F, 0.5F, 0.45F, Sprites.PUPILA,
                        AltarEffects.color(0.9F, 0x000000));
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(EyeAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).inflate(2.5, 0, 2.5).expandTowards(0, 6, 0);
    }
}
