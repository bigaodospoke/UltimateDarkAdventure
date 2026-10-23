package com.uda.item;

import java.util.List;

import com.uda.entity.EyeArrow;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Arco Ocular (também o arco do Hollow Watcher). Desperto na mão do jogador: as flechas viram
 * Flechas Oculares, que procuram o inimigo mais perto da mira.
 */
public class OcularBowItem extends BowItem implements OcularItem {
    public OcularBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public String powerKey() {
        return "eye_bow";
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack ammo, ItemStack weapon) {
        if (arrow instanceof EyeArrow || !Ocular.isAwakened(weapon) || !(arrow.getOwner() instanceof Player player)) {
            return super.customArrow(arrow, ammo, weapon);
        }
        EyeArrow eye = new EyeArrow(player.level(), player, ammo, weapon, Ocular.findTarget(player, Ocular.isSupreme(weapon) ? 64.0 : 48.0));
        eye.pickup = arrow.pickup;
        eye.setCritArrow(arrow.isCritArrow());
        boolean supreme = Ocular.isSupreme(weapon);
        eye.setSupreme(supreme);
        eye.setBaseDamage(arrow.getBaseDamage() + (supreme ? 3.0 : 1.0));
        return eye;
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
