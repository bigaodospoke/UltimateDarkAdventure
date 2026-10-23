package com.uda.client.renderer;

import com.uda.UltimateDarkAdventure;
import com.uda.entity.Awakenable;

import net.minecraft.resources.ResourceLocation;

/**
 * As três texturas de um mob do Olho (e as versões de brilho), escolhidas pelo progresso do despertar:
 * fechado -> fechado com energia vazando -> aberto. Durante uma piscada volta para "fechado com energia".
 */
public record EyeTextures(ResourceLocation dormant, ResourceLocation blink, ResourceLocation awakened,
        ResourceLocation dormantGlow, ResourceLocation blinkGlow, ResourceLocation awakenedGlow) {

    public static EyeTextures of(String mob) {
        String base = "textures/entity/" + mob + "/" + mob + "_";
        return new EyeTextures(
                UltimateDarkAdventure.id(base + "dormant.png"),
                UltimateDarkAdventure.id(base + "blink.png"),
                UltimateDarkAdventure.id(base + "awakened.png"),
                UltimateDarkAdventure.id(base + "dormant_glow.png"),
                UltimateDarkAdventure.id(base + "blink_glow.png"),
                UltimateDarkAdventure.id(base + "awakened_glow.png"));
    }

    private int stage(Awakenable entity) {
        float progress = entity.getAwakenProgress(1.0F);
        if (progress < 0.35F) {
            return 0;
        }
        return progress < 0.7F || entity.isBlinking() ? 1 : 2;
    }

    public ResourceLocation base(Awakenable entity) {
        return switch (this.stage(entity)) {
            case 0 -> this.dormant;
            case 1 -> this.blink;
            default -> this.awakened;
        };
    }

    public ResourceLocation glow(Awakenable entity) {
        return switch (this.stage(entity)) {
            case 0 -> this.dormantGlow;
            case 1 -> this.blinkGlow;
            default -> this.awakenedGlow;
        };
    }
}
