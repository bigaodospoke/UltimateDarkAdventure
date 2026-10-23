package com.uda.registry;

import com.uda.UltimateDarkAdventure;
import com.uda.block.EyeAltarBlockEntity;
import com.uda.block.SummonAltarBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, UltimateDarkAdventure.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EyeAltarBlockEntity>> EYE_ALTAR =
            BLOCK_ENTITY_TYPES.register("eye_altar", () -> BlockEntityType.Builder.of(EyeAltarBlockEntity::new, ModBlocks.EYE_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SummonAltarBlockEntity>> SUMMON_ALTAR =
            BLOCK_ENTITY_TYPES.register("summon_altar", () -> BlockEntityType.Builder.of(SummonAltarBlockEntity::new,
                    ModBlocks.ABYSSAL_ALTAR.get(), ModBlocks.VOID_ALTAR.get()).build(null));

    private ModBlockEntities() {
    }
}
