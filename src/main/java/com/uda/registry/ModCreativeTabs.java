package com.uda.registry;

import java.util.List;

import com.uda.UltimateDarkAdventure;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltimateDarkAdventure.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.uda"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.EYE_SWORD.get()))
                    .displayItems((parameters, output) -> {
                        // na ordem da progressão
                        for (var item : List.of(ModItems.EYE_ORE, ModItems.DEEPSLATE_EYE_ORE, ModItems.RAW_EYE_FRAGMENT, ModItems.EYE_INGOT,
                                ModItems.EYE_CRYSTAL, ModItems.EYE_CORE, ModItems.WATCHER_EYE, ModItems.EYE_ALTAR, ModItems.EYE_SWORD,
                                ModItems.EYE_AXE, ModItems.EYE_PICKAXE, ModItems.EYE_SHOVEL, ModItems.EYE_BOW, ModItems.EYE_SHIELD,
                                ModItems.EYE_HELMET, ModItems.EYE_CHESTPLATE, ModItems.EYE_LEGGINGS, ModItems.EYE_BOOTS, ModItems.ABYSSAL_FRAGMENT,
                                ModItems.AGAMOTO_EYE, ModItems.EYE_GRIMOIRE, ModItems.AWAKENED_EYE_CORE, ModItems.ABYSSAL_ALTAR,
                                ModItems.ABYSSAL_SCALE, ModItems.ABYSSAL_HEART, ModItems.VOID_ALTAR, ModItems.VOID_ESSENCE,
                                ModItems.HOLLOW_WATCHER_SPAWN_EGG, ModItems.BLIND_EXECUTIONER_SPAWN_EGG, ModItems.ABYSSAL_DRAGON_SPAWN_EGG,
                                ModItems.BLACK_EYE_SPAWN_EGG)) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
