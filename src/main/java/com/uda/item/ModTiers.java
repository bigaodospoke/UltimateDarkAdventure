package com.uda.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

public final class ModTiers {
    // Material provisório: o reparo passa a usar o Cristal Roxo Ocular quando o minério existir.
    public static final Tier OCULAR = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
            1800, 8.0F, 3.5F, 18, () -> Ingredient.of(Items.AMETHYST_SHARD));

    private ModTiers() {
    }
}
