package com.uda.registry;

import com.mojang.serialization.Codec;
import com.uda.UltimateDarkAdventure;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, UltimateDarkAdventure.MODID);

    /** Olho da arma aberto agora (tecla "Despertar a arma"). Ausente = fechado. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AWAKENED =
            DATA_COMPONENTS.registerComponentType("awakened", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Forma liberada no Altar do Olho: 0 selada, 1 desperta, 2 suprema. Ausente = selada. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TIER =
            DATA_COMPONENTS.registerComponentType("tier", builder -> builder.persistent(ExtraCodecs.intRange(0, 2)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }
}
