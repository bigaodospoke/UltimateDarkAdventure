package com.uda.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Material do mod com uma linha de descrição (tooltip.uda.&lt;id&gt;) dizendo para que serve. */
public class LoreItem extends Item {
    private final boolean glint;

    public LoreItem(Properties properties, boolean glint) {
        super(properties);
        this.glint = glint;
    }

    public LoreItem(Properties properties) {
        this(properties, false);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.glint || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.uda." + BuiltInRegistries.ITEM.getKey(this).getPath()).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
