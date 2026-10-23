package com.uda.item;

import java.util.List;

import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machado Ocular (também o do Blind Executioner). Aberto: +3 de dano, corta mais rápido e o botão direito
 * faz o Giro do Carrasco (acerta tudo em volta e definha). Supremo: giro maior e mais forte.
 */
public class OcularAxeItem extends AxeItem implements OcularItem {
    public OcularAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public String powerKey() {
        return "eye_axe";
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!Ocular.isAwakened(stack)) {
            return InteractionResultHolder.pass(stack);
        }
        boolean supreme = Ocular.isSupreme(stack);
        if (level instanceof ServerLevel server) {
            double radius = supreme ? 4.5 : 3.5;
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * (supreme ? 1.0F : 0.8F);
            for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius, 1.0, radius),
                    e -> e != player && e.isAlive() && !player.isAlliedTo(e) && !(e instanceof OwnableEntity pet && pet.getOwner() == player))) {
                if (victim.distanceToSqr(player) > radius * radius) continue;
                if (victim.hurt(player.damageSources().playerAttack(player), damage)) {
                    victim.knockback(0.7, player.getX() - victim.getX(), player.getZ() - victim.getZ());
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, supreme ? 1 : 0), player);
                }
            }
            for (int i = 0; i < 12; i++) {
                float a = i * Mth.TWO_PI / 12;
                double x = player.getX() + Mth.cos(a) * radius * 0.8, z = player.getZ() + Mth.sin(a) * radius * 0.8;
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, x, player.getY(0.5), z, 1, 0, 0, 0, 0);
                server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.4F), x, player.getY(0.4), z, 2, 0.2, 0.2, 0.2, 0);
            }
            server.playSound(null, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 0.6F);
            server.playSound(null, player, ModSounds.WEAPON_SLASH.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
        }
        Ocular.spend(player, stack, 3, supreme ? 40 : 60);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return Ocular.isAwakened(stack) && speed > 1.0F ? speed * (Ocular.isSupreme(stack) ? 2.0F : 1.6F) : speed;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (Ocular.isAwakened(stack)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), attacker);
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
