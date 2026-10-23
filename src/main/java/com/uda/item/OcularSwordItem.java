package com.uda.item;

import java.util.List;

import com.uda.entity.EnergySlash;
import com.uda.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Espada Ocular. Aberta: +3 de dano, golpes marcam e definham, e o botão direito solta um Corte de Energia.
 * Suprema: +5 de dano e três cortes em leque.
 */
public class OcularSwordItem extends SwordItem implements OcularItem {
    public OcularSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public String powerKey() {
        return "eye_sword";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Ocular.isAwakened(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        boolean supreme = Ocular.isSupreme(stack);
        if (!level.isClientSide) {
            int count = supreme ? 3 : 1;
            for (int i = 0; i < count; i++) {
                EnergySlash slash = new EnergySlash(level, player, supreme ? 10.0F : 7.0F, supreme);
                slash.shootFromRotation(player, player.getXRot(), player.getYRot() + (i - (count - 1) / 2.0F) * 16.0F, 0.0F, 1.5F, 0.0F);
                level.addFreshEntity(slash);
            }
            level.playSound(null, player, ModSounds.WEAPON_SLASH.get(), SoundSource.PLAYERS, 1.0F, supreme ? 0.8F : 1.0F);
        }
        Ocular.spend(player, stack, 2, supreme ? 14 : 22);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (Ocular.isAwakened(stack)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, Ocular.isSupreme(stack) ? 1 : 0), attacker);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
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
