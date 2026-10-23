package com.uda.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.uda.UltimateDarkAdventure;
import com.uda.item.Ocular;
import com.uda.item.OcularItem;

import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Olho ao lado da hotbar mostrando o estado da arma do Olho na mão: selada, fechada (pisca de vez em quando),
 * aberta ou suprema (pulsando), com a recarga do poder escurecendo o olho de cima para baixo.
 */
public final class EyeHudLayer implements LayeredDraw.Layer {
    public static final ResourceLocation ID = UltimateDarkAdventure.id("eye_hud");
    private static final ResourceLocation TEXTURE = UltimateDarkAdventure.id("textures/gui/interface/eye_hud.png");
    private static final int SIZE = 22, FRAMES = 7;

    @Override
    public void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !(mc.getCameraEntity() instanceof Player player) || mc.gameMode == null || mc.player != player
                || mc.player.isSpectator()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof OcularItem)) {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof OcularItem)) return;
        }
        OcularItem item = (OcularItem) stack.getItem();
        long tick = player.tickCount;
        int frame;
        if (Ocular.tier(stack) == Ocular.SEALED) {
            frame = 0;
        } else if (Ocular.isSupreme(stack)) {
            frame = 5 + (int) (tick / 6 % 2);
        } else if (Ocular.isAwakened(stack) || !item.opensWithKey()) {
            frame = 3 + (int) (tick / 6 % 2);
        } else {
            frame = tick % 90 < 3 ? 2 : 1;
        }

        int x = g.guiWidth() / 2 + 91 + 6;
        // abre espaço para o que o vanilla desenha à direita da hotbar
        if (player.getMainArm() == HumanoidArm.LEFT && !player.getOffhandItem().isEmpty()) x += 29;
        if (mc.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) x += 20;
        int y = g.guiHeight() - SIZE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEXTURE, x, y, frame * SIZE, 0, SIZE, SIZE, SIZE * FRAMES, SIZE);
        float cooldown = player.getCooldowns().getCooldownPercent(stack.getItem(), delta.getGameTimeDeltaPartialTick(true));
        if (cooldown > 0.0F) {
            int h = Math.round((SIZE - 6) * cooldown);
            g.fill(x + 3, y + 3 + (SIZE - 6 - h), x + SIZE - 3, y + SIZE - 3, 0xA0100020);
        }
        RenderSystem.disableBlend();
    }
}
