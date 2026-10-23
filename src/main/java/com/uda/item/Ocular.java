package com.uda.item;

import java.util.List;

import javax.annotation.Nullable;

import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModDataComponents;
import com.uda.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Regras comuns do equipamento do Olho.
 * <p>
 * Forma (componente uda:tier): Selada (0) → Desperta (1) → Suprema (2), liberadas no Altar do Olho.
 * As armas, depois de despertas, abrem/fecham o olho com a tecla "Despertar a arma": abertas usam o poder,
 * mas se alimentam da própria durabilidade (1 ponto a cada 2 s na mão) e se fecham antes de quebrar.
 * Armadura e amuletos ficam sempre ativos depois de despertos.
 */
public final class Ocular {
    public static final int SEALED = 0;
    public static final int AWAKENED = 1;
    public static final int SUPREME = 2;

    private static final int DRAIN_INTERVAL = 40;
    /** Abaixo desta fração de durabilidade o olho não abre (e fecha sozinho). */
    private static final float MIN_DURABILITY = 0.1F;

    private Ocular() {
    }

    public static int tier(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TIER.get(), SEALED);
    }

    public static ItemStack withTier(ItemStack stack, int tier) {
        ItemStack copy = stack.copy();
        copy.set(ModDataComponents.TIER.get(), tier);
        return copy;
    }

    /** O poder está ativo agora: armas com o olho aberto; armadura e amuletos despertos. */
    public static boolean isAwakened(ItemStack stack) {
        if (!(stack.getItem() instanceof OcularItem item) || tier(stack) < AWAKENED) {
            return false;
        }
        return !item.opensWithKey() || stack.getOrDefault(ModDataComponents.AWAKENED.get(), false);
    }

    public static boolean isSupreme(ItemStack stack) {
        return isAwakened(stack) && tier(stack) >= SUPREME;
    }

    private static void setOpen(ItemStack stack, boolean open) {
        if (open) {
            stack.set(ModDataComponents.AWAKENED.get(), true);
        } else {
            stack.remove(ModDataComponents.AWAKENED.get());
        }
    }

    private static float durabilityLeft(ItemStack stack) {
        return stack.isDamageableItem() ? 1.0F - (float) stack.getDamageValue() / stack.getMaxDamage() : 1.0F;
    }

    /** Abre ou fecha o olho da arma (lado do servidor). */
    public static void toggle(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof OcularItem item) || !item.opensWithKey()) {
            return;
        }
        if (tier(stack) < AWAKENED) {
            player.displayClientMessage(Component.translatable("message.uda.sealed").withStyle(ChatFormatting.DARK_PURPLE), true);
            player.playNotifySound(SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8F, 0.6F);
            return;
        }
        boolean open = !stack.getOrDefault(ModDataComponents.AWAKENED.get(), false);
        if (open && durabilityLeft(stack) <= MIN_DURABILITY) {
            player.displayClientMessage(Component.translatable("message.uda.too_weak").withStyle(ChatFormatting.DARK_PURPLE), true);
            return;
        }
        setOpen(stack, open);
        ServerLevel level = player.serverLevel();
        if (open) {
            level.playSound(null, player, ModSounds.EYE_OPEN.get(), SoundSource.PLAYERS, 1.0F, tier(stack) >= SUPREME ? 0.8F : 1.1F);
            level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.2F),
                    player.getX(), player.getY(0.6), player.getZ(), 30, 0.4, 0.5, 0.4, 0.02);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
        } else {
            level.playSound(null, player, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
        player.displayClientMessage(Component.translatable(open ? "message.uda.awakened" : "message.uda.dormant")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
    }

    /** Chamado no inventoryTick das armas: partículas quando aberta na mão e o consumo de durabilidade. */
    public static void inventoryTick(ItemStack stack, Level level, Entity entity, boolean selected) {
        if (!isAwakened(stack) || !(entity instanceof LivingEntity holder)) {
            return;
        }
        if (!selected && holder.getOffhandItem() != stack) {
            return;
        }
        if (level.isClientSide) {
            if (level.random.nextFloat() < (isSupreme(stack) ? 0.25F : 0.12F)) {
                level.addParticle(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 0.7F),
                        holder.getRandomX(0.8), holder.getY(0.35 + level.random.nextDouble() * 0.4), holder.getRandomZ(0.8), 0.0, 0.02, 0.0);
            }
            return;
        }
        if (holder.tickCount % DRAIN_INTERVAL != 0 || !stack.isDamageableItem()
                || holder instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        if (durabilityLeft(stack) <= MIN_DURABILITY) {
            setOpen(stack, false);
            level.playSound(null, holder, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.5F);
            return;
        }
        stack.hurtAndBreak(1, (ServerLevel) level, holder, item -> {
        });
    }

    public static void appendTooltip(ItemStack stack, OcularItem item, List<Component> tooltip) {
        int tier = tier(stack);
        ChatFormatting tierColor = tier == SUPREME ? ChatFormatting.LIGHT_PURPLE : tier == AWAKENED ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GRAY;
        Component form = Component.translatable("tooltip.uda.tier." + tier).withStyle(tierColor);
        if (item.opensWithKey() && tier >= AWAKENED) {
            boolean open = stack.getOrDefault(ModDataComponents.AWAKENED.get(), false);
            form = form.copy().append(Component.translatable(open ? "tooltip.uda.state.open" : "tooltip.uda.state.closed").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(form);
        tooltip.add(Component.translatable("tooltip.uda." + item.powerKey() + ".power")
                .withStyle(tier >= AWAKENED ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.uda." + item.powerKey() + ".supreme")
                .withStyle(tier >= SUPREME ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_GRAY));
        if (tier == SEALED) {
            tooltip.add(Component.translatable("tooltip.uda.needs_altar").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else if (item.opensWithKey()) {
            tooltip.add(Component.translatable("tooltip.uda.toggle_hint", Component.keybind("key.uda.awaken")).withStyle(ChatFormatting.GRAY));
        }
    }

    /** O olho tudo vê: o inimigo mais alinhado com a mira, dentro do alcance e à vista. */
    @Nullable
    public static LivingEntity findTarget(LivingEntity shooter, double range) {
        Vec3 eye = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        LivingEntity best = null;
        double bestDot = Math.cos(Math.toRadians(25));
        AABB area = shooter.getBoundingBox().expandTowards(look.scale(range)).inflate(range * 0.4);
        for (LivingEntity candidate : shooter.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != shooter && e.isAlive() && e instanceof Enemy && shooter.hasLineOfSight(e))) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double dist = to.length();
            if (dist > range) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = candidate;
            }
        }
        return best;
    }

    /** Usa o poder ativo de uma arma: gasta durabilidade e põe em espera (as duas formas mais caras no supremo). */
    public static void spend(Player player, ItemStack stack, int durability, int cooldown) {
        player.getCooldowns().addCooldown(stack.getItem(), cooldown);
        if (!player.level().isClientSide && !player.getAbilities().instabuild) {
            stack.hurtAndBreak(durability, (ServerLevel) player.level(), player, item -> {
            });
        }
    }
}
