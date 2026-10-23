package com.uda.client.ui;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.uda.UltimateDarkAdventure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Os desenhos da interface temática que não dá para fazer só com texturas. Tudo só com o pacote ligado. */
public final class InterfaceEvents {
    private static final ResourceLocation LOGO = UltimateDarkAdventure.id("textures/gui/interface/logo.png");
    private static final ResourceLocation LOADING = UltimateDarkAdventure.id("textures/gui/interface/loading.png");
    private static final ResourceLocation CONTAINER_EYE = UltimateDarkAdventure.id("textures/gui/interface/container_eye.png");
    private static final ResourceLocation BOSS_BAR = UltimateDarkAdventure.id("textures/gui/interface/boss_bar.png");
    private static final int LOGO_W = 512, LOGO_H = 228;
    private static final int LOADING_W = 768, LOADING_H = 432;
    /** Lilás dos títulos (o vanilla usa #404040, que some no fundo escuro). */
    private static final int LABEL = 0xD9C7EF;
    private static final int VANILLA_LABEL = 4210752;

    private enum Labels { NONE, STANDARD, TITLE_ONLY, CREATIVE, MERCHANT }

    private static final Map<Class<?>, Labels> LABEL_MODES = new HashMap<>();
    private static boolean smoothTextures;

    private InterfaceEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::tooltipColor);
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::containerForeground);
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::screenInit);
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::screenRenderPre);
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::screenRenderPost);
        NeoForge.EVENT_BUS.addListener(InterfaceEvents::bossBar);
    }

    // ------------------------------------------------------------------ tooltip

    private static void tooltipColor(RenderTooltipEvent.Color event) {
        if (!InterfaceTheme.isActive()) return;
        event.setBackground(0xF00D0A14);
        event.setBorderStart(0xF09B30FF);
        event.setBorderEnd(0xF03A1060);
    }

    // ------------------------------------------------------------------ containers: títulos legíveis e o olho em cima

    private static void containerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!InterfaceTheme.isActive()) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        switch (labelMode(screen)) {
            case STANDARD -> {
                g.drawString(font, screen.getTitle(), screen.titleLabelX, screen.titleLabelY, LABEL, false);
                g.drawString(font, screen.playerInventoryTitle, screen.inventoryLabelX, screen.inventoryLabelY, LABEL, false);
            }
            case TITLE_ONLY -> g.drawString(font, screen.getTitle(), screen.titleLabelX, screen.titleLabelY, LABEL, false);
            case CREATIVE -> {
                CreativeModeTab tab = CreativeModeInventoryScreen.selectedTab;
                // só nas abas com o fundo vanilla (que foi recolorido); abas de outros mods têm fundo próprio
                if (tab.showTitle() && tab.getLabelColor() == VANILLA_LABEL && "minecraft".equals(tab.getBackgroundTexture().getNamespace())) {
                    g.drawString(font, tab.getDisplayName(), 8, 6, LABEL, false);
                }
            }
            case MERCHANT -> merchantLabels(g, font, (MerchantScreen) screen);
            default -> {
            }
        }
        if (!(screen instanceof CreativeModeInventoryScreen) && screen.getClass().getName().startsWith("net.minecraft.")) {
            blend();
            g.blit(CONTAINER_EYE, screen.getXSize() / 2 - 20, -9, 0, 0, 40, 13, 40, 13);
        }
    }

    /** Como o vanilla desenha os títulos desta tela (redesenhamos por cima, no mesmo lugar). */
    private static Labels labelMode(AbstractContainerScreen<?> screen) {
        return LABEL_MODES.computeIfAbsent(screen.getClass(), c -> {
            if (!c.getName().startsWith("net.minecraft.") || screen instanceof BeaconScreen) return Labels.NONE;
            if (screen instanceof CreativeModeInventoryScreen) return Labels.CREATIVE;
            if (screen instanceof InventoryScreen) return Labels.TITLE_ONLY;
            if (screen instanceof MerchantScreen) return Labels.MERCHANT;
            if (screen instanceof AnvilScreen) return Labels.STANDARD; // chama o padrão e desenha o custo
            return declaresRenderLabels(c) ? Labels.NONE : Labels.STANDARD;
        });
    }

    /** true se alguma classe abaixo de AbstractContainerScreen reescreve renderLabels. */
    private static boolean declaresRenderLabels(Class<?> c) {
        for (Class<?> k = c; k != null && k != AbstractContainerScreen.class; k = k.getSuperclass()) {
            for (Method m : k.getDeclaredMethods()) {
                if (m.getName().equals("renderLabels") && m.getParameterCount() == 3) return true;
            }
        }
        return false;
    }

    private static void merchantLabels(GuiGraphics g, Font font, MerchantScreen screen) {
        int level = screen.getMenu().getTraderLevel();
        Component title = level > 0 && level <= 5 && screen.getMenu().showProgressBar()
                ? Component.translatable("merchant.title", screen.getTitle(), Component.translatable("merchant.level." + level))
                : screen.getTitle();
        g.drawString(font, title, 49 + screen.getXSize() / 2 - font.width(title) / 2, 6, LABEL, false);
        g.drawString(font, screen.playerInventoryTitle, screen.inventoryLabelX, screen.inventoryLabelY, LABEL, false);
        Component trades = Component.translatable("merchant.trades");
        g.drawString(font, trades, 5 - font.width(trades) / 2 + 48, 6, LABEL, false);
    }

    // ------------------------------------------------------------------ menu de pausa com o logo

    private static void screenInit(ScreenEvent.Init.Post event) {
        if (!InterfaceTheme.isActive() || !(event.getScreen() instanceof PauseScreen pause) || !pause.showsPauseMenu()) return;
        int maxBottom = 0;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof StringWidget text && text.getMessage().equals(pause.getTitle())) {
                text.visible = false; // o logo toma o lugar do título "Menu do jogo"
            } else if (listener instanceof AbstractWidget w && isCentered(w, pause)) {
                maxBottom = Math.max(maxBottom, w.getY() + w.getHeight());
            }
        }
        // desce os botões para abrir espaço para o logo, sem passar do fim da tela
        int shift = Mth.clamp(pause.height - 8 - maxBottom, 0, 44);
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget w && !(w instanceof StringWidget) && isCentered(w, pause)) {
                w.setY(w.getY() + shift);
            }
        }
    }

    private static boolean isCentered(AbstractWidget w, Screen screen) {
        return Math.abs(w.getX() + w.getWidth() / 2 - screen.width / 2) < 80;
    }

    private static void screenRenderPost(ScreenEvent.Render.Post event) {
        if (!InterfaceTheme.isActive()) return;
        Screen screen = event.getScreen();
        GuiGraphics g = event.getGuiGraphics();
        if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
            int top = Integer.MAX_VALUE;
            for (GuiEventListener listener : pause.children()) {
                if (listener instanceof AbstractWidget w && w.visible && !(w instanceof StringWidget) && isCentered(w, pause)) {
                    top = Math.min(top, w.getY());
                }
            }
            int h = Math.min(top - 10, 100);
            if (top != Integer.MAX_VALUE && h >= 28) {
                int w = h * LOGO_W / LOGO_H;
                smooth();
                blend();
                g.blit(LOGO, (pause.width - w) / 2, Math.max(2, (top - h) / 2 - 2), w, h, 0, 0, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
            }
        } else if (screen instanceof AdvancementsScreen advancements) {
            int left = (advancements.width - 252) / 2, top = (advancements.height - 140) / 2;
            Component title = advancements.selectedTab != null ? advancements.selectedTab.getTitle() : Component.translatable("gui.advancements");
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            g.drawString(Minecraft.getInstance().font, title, left + 8, top + 6, LABEL, false);
            g.pose().popPose();
        }
    }

    // ------------------------------------------------------------------ telas de carregamento

    private static void screenRenderPre(ScreenEvent.Render.Pre event) {
        if (!InterfaceTheme.isActive()) return;
        Screen screen = event.getScreen();
        boolean known = screen instanceof LevelLoadingScreen || screen instanceof ReceivingLevelScreen
                || screen instanceof GenericMessageScreen || screen instanceof ProgressScreen;
        if (!known) return;
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        int width = screen.width, height = screen.height;
        // a arte cobre a tela toda (corta as sobras, sem distorcer)
        float scale = Math.max((float) width / LOADING_W, (float) height / LOADING_H);
        int w = Mth.ceil(LOADING_W * scale), h = Mth.ceil(LOADING_H * scale);
        smooth();
        blend();
        g.blit(LOADING, (width - w) / 2, (height - h) / 2, w, h, 0, 0, LOADING_W, LOADING_H, LOADING_W, LOADING_H);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xD0000000);

        Font font = Minecraft.getInstance().font;
        int progress = -1;
        Component text;
        if (screen instanceof LevelLoadingScreen loading) {
            progress = Mth.clamp(loading.progressListener.getProgress(), 0, 100);
            text = Component.translatable("uda.loading.world");
        } else {
            text = screen.getTitle().getString().isBlank() ? Component.translatable("uda.loading") : screen.getTitle();
        }
        int barW = Math.min(220, width - 40), barX = (width - barW) / 2, barY = height - 34;
        g.drawCenteredString(font, text, width / 2, barY - 14, LABEL);
        g.fill(barX - 2, barY - 2, barX + barW + 2, barY + 7, 0xFF05030A);
        g.fill(barX - 1, barY - 1, barX + barW + 1, barY + 6, 0xFF6A2AA8);
        g.fill(barX, barY, barX + barW, barY + 5, 0xFF0B0812);
        if (progress >= 0) {
            int fill = barW * progress / 100;
            g.fillGradient(barX, barY, barX + fill, barY + 5, 0xFFE28BFF, 0xFF6A1FB0);
            g.drawCenteredString(font, progress + "%", width / 2, barY + 10, 0xB49AD6);
        } else {
            // sem progresso conhecido: um brilho que corre pela barra
            float t = (System.currentTimeMillis() % 1600L) / 1600.0F;
            int seg = barW / 4, x = barX + (int) ((barW + seg) * t) - seg;
            g.fillGradient(Math.max(barX, x), barY, Math.min(barX + barW, x + seg), barY + 5, 0xFFE28BFF, 0xFF6A1FB0);
        }
    }

    // ------------------------------------------------------------------ barra dos bosses do mod

    private static void bossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (!InterfaceTheme.isActive()) return;
        LerpingBossEvent boss = event.getBossEvent();
        if (!(boss.getName().getContents() instanceof TranslatableContents tc)
                || !(tc.getKey().startsWith("entity." + UltimateDarkAdventure.MODID + ".") || tc.getKey().startsWith("boss." + UltimateDarkAdventure.MODID + "."))) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics g = event.getGuiGraphics();
        // a moldura (13 px de altura) começa onde a barra vanilla começaria; o nome fica no lugar do vanilla
        int x = event.getX(), y = event.getY() + 4;
        blend();
        g.blit(BOSS_BAR, x - 20, y - 4, 0, 0, 222, 13, 256, 32);
        g.blit(BOSS_BAR, x, y, 0, 28, 182, 5, 256, 32);
        int fill = Mth.clamp((int) (boss.getProgress() * 183.0F), 0, 182);
        g.blit(BOSS_BAR, x, y, 0, boss.getColor() == BossEvent.BossBarColor.RED ? 22 : 16, fill, 5, 256, 32);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, boss.getName(), g.guiWidth() / 2 - font.width(boss.getName()) / 2, event.getY() - 9, 0xF0D8FF);
        event.setIncrement(event.getIncrement() + 5);
    }

    // ------------------------------------------------------------------

    private static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** Logo e arte de carregamento são imagens grandes reduzidas: filtro suave para não serrilhar. */
    private static void smooth() {
        if (smoothTextures) return;
        smoothTextures = true;
        Minecraft.getInstance().getTextureManager().getTexture(LOGO).setFilter(true, false);
        Minecraft.getInstance().getTextureManager().getTexture(LOADING).setFilter(true, false);
    }
}
