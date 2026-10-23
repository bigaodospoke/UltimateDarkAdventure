package com.uda.client.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.uda.UltimateDarkAdventure;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Interface temática: um pacote de recursos embutido no mod ("Ultimate Dark Adventure" na tela de pacotes)
 * com as texturas da interface recoloridas (gerado por tools/interface/gerar_interface.mjs), mais os
 * desenhos feitos por código em {@link InterfaceEvents}, que só aparecem com o pacote ligado.
 */
public final class InterfaceTheme {
    public static final String PACK_ID = "mod/" + UltimateDarkAdventure.MODID + ":resourcepacks/uda_interface";
    private static volatile boolean active;

    private InterfaceTheme() {
    }

    /** O pacote da interface está ligado (atualizado a cada recarga de recursos). */
    public static boolean isActive() {
        return active;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InterfaceTheme::addPack);
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> refresh()));
        enableOnFirstRun();
    }

    private static void addPack(AddPackFindersEvent event) {
        event.addPackFinders(UltimateDarkAdventure.id("resourcepacks/uda_interface"), PackType.CLIENT_RESOURCES,
                Component.literal("Ultimate Dark Adventure"), PackSource.BUILT_IN, false, Pack.Position.TOP);
    }

    private static void refresh() {
        active = Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(PACK_ID);
    }

    /**
     * Pacotes de mod nascem desligados. Na primeira vez que o mod roda, o nosso entra na lista de pacotes
     * escolhidos (as opções do jogo já foram lidas quando os mods são construídos). Depois disso a escolha é
     * do jogador: se ele desligar, continua desligado.
     */
    private static void enableOnFirstRun() {
        Path marker = FMLPaths.CONFIGDIR.get().resolve(UltimateDarkAdventure.MODID).resolve("interface_ligada.txt");
        if (Files.exists(marker)) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return;
        }
        if (!options.resourcePacks.contains(PACK_ID)) {
            options.resourcePacks.add(PACK_ID);
            options.save();
        }
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "A interface do Ultimate Dark Adventure já foi ligada uma vez.\n"
                    + "Para desligar, use Opções > Pacotes de recursos.\n");
        } catch (IOException e) {
            UltimateDarkAdventure.LOGGER.warn("Não consegui gravar {}", marker, e);
        }
    }
}
