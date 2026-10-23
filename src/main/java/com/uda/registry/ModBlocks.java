package com.uda.registry;

import com.uda.UltimateDarkAdventure;
import com.uda.block.AbyssalAltarBlock;
import com.uda.block.EyeAltarBlock;
import com.uda.block.VoidAltarBlock;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(UltimateDarkAdventure.MODID);

    // Minério do Olho: brilha de leve no escuro (os olhos piscam na textura)
    public static final DeferredBlock<DropExperienceBlock> EYE_ORE = BLOCKS.register("eye_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).lightLevel(state -> 3)));

    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_EYE_ORE = BLOCKS.register("deepslate_eye_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE).lightLevel(state -> 3)));

    public static final DeferredBlock<EyeAltarBlock> EYE_ALTAR = BLOCKS.register("eye_altar",
            () -> new EyeAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_TILES).noOcclusion()
                    .lightLevel(state -> state.getValue(EyeAltarBlock.ACTIVE) ? 13 : 5)));

    // Altares de invocação: só existem nas estruturas e não quebram
    public static final DeferredBlock<AbyssalAltarBlock> ABYSSAL_ALTAR = BLOCKS.register("abyssal_altar",
            () -> new AbyssalAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.DEEPSLATE_TILES).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 5 + state.getValue(AbyssalAltarBlock.FRAGMENTS) * 2)));

    public static final DeferredBlock<VoidAltarBlock> VOID_ALTAR = BLOCKS.register("void_altar",
            () -> new VoidAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F)
                    .noLootTable().sound(SoundType.DEEPSLATE_TILES).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> state.getValue(VoidAltarBlock.ACTIVE) ? 15 : 7)));

    private ModBlocks() {
    }
}
