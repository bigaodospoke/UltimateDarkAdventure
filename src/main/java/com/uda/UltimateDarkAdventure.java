package com.uda;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.uda.network.AwakenItemPayload;
import com.uda.registry.ModArmorMaterials;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModBlocks;
import com.uda.registry.ModCreativeTabs;
import com.uda.registry.ModDataComponents;
import com.uda.registry.ModEntities;
import com.uda.registry.ModItems;
import com.uda.registry.ModSounds;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Ultimate Dark Adventure — "O Olho tudo vê".
 * Todo o conteúdo do mod tem uma forma Fechada (dormente) e uma forma Aberta (desperta).
 */
@Mod(UltimateDarkAdventure.MODID)
public class UltimateDarkAdventure {
    public static final String MODID = "uda";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimateDarkAdventure(IEventBus modEventBus) {
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
        modEventBus.addListener(AwakenItemPayload::register);
        ModEvents.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
