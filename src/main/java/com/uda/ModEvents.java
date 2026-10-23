package com.uda;

import com.uda.entity.AbstractEyeMonster;
import com.uda.item.AgamotoItem;
import com.uda.item.Ocular;
import com.uda.item.OcularArmorItem;
import com.uda.item.OcularAxeItem;
import com.uda.item.OcularShieldItem;
import com.uda.item.OcularSwordItem;
import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Eventos de jogo do equipamento desperto. */
public final class ModEvents {
    private static final ResourceLocation AWAKENED_DAMAGE = UltimateDarkAdventure.id("awakened_weapon_damage");

    private ModEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ModEvents::awakenedWeaponDamage);
        NeoForge.EVENT_BUS.addListener(ModEvents::awakenedShieldReflect);
        NeoForge.EVENT_BUS.addListener(ModEvents::playerTick);
        NeoForge.EVENT_BUS.addListener(ModEvents::agamotoRewind);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> AgamotoItem.forget(event.getEntity()));
    }

    /** Espada e machado abertos batem mais forte (supremos ainda mais). */
    private static void awakenedWeaponDamage(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if ((stack.getItem() instanceof OcularSwordItem || stack.getItem() instanceof OcularAxeItem) && Ocular.isAwakened(stack)) {
            event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(AWAKENED_DAMAGE, Ocular.isSupreme(stack) ? 5.0 : 3.0,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
    }

    /** Escudo aberto: o olho devolve parte do golpe de quem bateu de perto. */
    private static void awakenedShieldReflect(LivingShieldBlockEvent event) {
        LivingEntity blocker = event.getEntity();
        ItemStack shield = blocker.getUseItem();
        if (!event.getBlocked() || !(shield.getItem() instanceof OcularShieldItem) || !Ocular.isAwakened(shield)
                || event.getDamageSource().is(DamageTypes.THORNS)
                || !(event.getDamageSource().getDirectEntity() instanceof LivingEntity attacker) || attacker == blocker
                || !(blocker.level() instanceof ServerLevel level)) {
            return;
        }
        attacker.hurt(blocker.damageSources().thorns(blocker), Ocular.isSupreme(shield) ? 6.0F : 3.0F);
        attacker.knockback(0.6, blocker.getX() - attacker.getX(), blocker.getZ() - attacker.getZ());
        attacker.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60), blocker);
        level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.3F),
                attacker.getX(), attacker.getY(0.5), attacker.getZ(), 14, 0.3, 0.4, 0.3, 0.02);
        level.playSound(null, blocker.getX(), blocker.getY(), blocker.getZ(), SoundEvents.ENDER_EYE_DEATH, blocker.getSoundSource(), 0.7F, 1.4F);
    }

    private static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        barrier(player);
        if (player.tickCount % 20 == 0) {
            armorSet(player);
        }
    }

    /**
     * Barreira Ocular: bloqueando com o escudo aberto, os projéteis que chegam perto são devolvidos.
     * Desperto: só os que vêm pela frente. Supremo: de qualquer lado e num raio maior.
     */
    private static void barrier(ServerPlayer player) {
        ItemStack shield = player.getUseItem();
        if (!player.isBlocking() || !(shield.getItem() instanceof OcularShieldItem) || !Ocular.isAwakened(shield)) {
            return;
        }
        boolean supreme = Ocular.isSupreme(shield);
        double radius = supreme ? 5.0 : 3.5;
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(radius),
                p -> p.getOwner() != player && p.isAlive())) {
            Vec3 motion = projectile.getDeltaMovement();
            Vec3 toPlayer = player.getBoundingBox().getCenter().subtract(projectile.position());
            boolean incoming = motion.lengthSqr() > 0.01 && motion.dot(toPlayer) > 0;
            boolean inFront = toPlayer.normalize().dot(look) < -0.2;
            if (!incoming || !supreme && !inFront || projectile instanceof AbstractArrow arrow && arrow.pickup != AbstractArrow.Pickup.DISALLOWED
                    && motion.lengthSqr() < 0.05) {
                continue;
            }
            projectile.setDeltaMovement(motion.scale(-1.2));
            projectile.setOwner(player);
            projectile.hasImpulse = true;
            level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.5F), projectile.getX(), projectile.getY(), projectile.getZ(),
                    10, 0.15, 0.15, 0.15, 0.02);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), ModSounds.WEAPON_BARRIER.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        }
        if (player.tickCount % 4 == 0) {
            for (int i = 0; i < 8; i++) {
                float a = i * Mth.TWO_PI / 8 + player.tickCount * 0.1F;
                level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 0.8F), player.getX() + Mth.cos(a) * 1.2, player.getY(0.6),
                        player.getZ() + Mth.sin(a) * 1.2, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Conjunto Ocular (4 peças despertas): Visão do Olho — visão noturna e inimigos num raio de 16 blocos marcados.
     * Supremo: também Resistência I e imunidade a definhamento e escuridão.
     */
    private static void armorSet(ServerPlayer player) {
        if (OcularArmorItem.countWorn(player, Ocular.AWAKENED) < 4) {
            return;
        }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        for (LivingEntity enemy : player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(16.0),
                e -> e instanceof Enemy && e.isAlive())) {
            enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, true, false), player);
        }
        if (OcularArmorItem.countWorn(player, Ocular.SUPREME) == 4) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.DARKNESS);
        }
    }

    /** Olho de Agamoto supremo: uma vez a cada 5 minutos, em vez de morrer, o tempo volta ~5 segundos. */
    private static void agamotoRewind(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }
        ItemStack agamoto = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AgamotoItem && Ocular.tier(stack) >= Ocular.SUPREME) {
                agamoto = stack;
                break;
            }
        }
        if (agamoto.isEmpty() && player.getOffhandItem().getItem() instanceof AgamotoItem && Ocular.tier(player.getOffhandItem()) >= Ocular.SUPREME) {
            agamoto = player.getOffhandItem();
        }
        if (agamoto.isEmpty() || player.getCooldowns().isOnCooldown(agamoto.getItem())) {
            return;
        }
        event.setCanceled(true);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(0.5), player.getZ(), 80, 0.4, 0.8, 0.4, 0.3);
        Vec3 back = AgamotoItem.rewindPosition(player);
        player.teleportTo(back.x, back.y, back.z);
        player.setHealth(player.getMaxHealth() * 0.5F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
        player.getCooldowns().addCooldown(agamoto.getItem(), AgamotoItem.REWIND_COOLDOWN);
        level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 2.0F), back.x, back.y + 1, back.z, 60, 0.5, 0.8, 0.5, 0.05);
        level.playSound(null, back.x, back.y, back.z, ModSounds.AGAMOTO_REWIND.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
    }
}
