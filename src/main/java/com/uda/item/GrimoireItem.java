package com.uda.item;

import java.util.List;

import com.uda.entity.EyeOrb;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Grimório do Olho: lança Orbes Oculares. Fechado: um orbe reto.
 * Aberto: três orbes em leque que perseguem o inimigo mais perto da mira.
 */
public class GrimoireItem extends Item implements OcularItem {
    private static final int DORMANT_COOLDOWN = 24;
    private static final int AWAKENED_COOLDOWN = 16;

    public GrimoireItem(Properties properties) {
        super(properties);
    }

    @Override
    public String powerKey() {
        return "eye_grimoire";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean awakened = Ocular.isAwakened(stack);
        boolean supreme = Ocular.isSupreme(stack);
        if (!level.isClientSide) {
            LivingEntity target = awakened ? Ocular.findTarget(player, 32.0) : null;
            int count = supreme ? 5 : awakened ? 3 : 1;
            for (int i = 0; i < count; i++) {
                float spread = (i - (count - 1) / 2.0F) * 12.0F;
                EyeOrb orb = supreme ? new EyeOrb(level, player, target, 9.0F, 1.2F) : new EyeOrb(level, player, awakened, target);
                orb.setExplosive(supreme);
                orb.shootFromRotation(player, player.getXRot(), player.getYRot() + spread, 0.0F, awakened ? 1.0F : 1.3F, 1.0F);
                level.addFreshEntity(orb);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS,
                    0.8F, awakened ? 0.8F : 1.4F);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, awakened ? AWAKENED_COOLDOWN : DORMANT_COOLDOWN);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
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
