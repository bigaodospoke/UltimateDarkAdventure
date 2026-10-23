package com.uda.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Escudo Ocular. Desperto: o olho devolve parte do golpe em quem atacou (ModEvents). */
public class OcularShieldItem extends ShieldItem implements OcularItem {
    public OcularShieldItem(Properties properties) {
        super(properties);
    }

    @Override
    public String powerKey() {
        return "eye_shield";
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.AMETHYST_SHARD);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Ocular.inventoryTick(stack, level, entity, selected);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Ocular.appendTooltip(stack, this, tooltip);
    }
}
