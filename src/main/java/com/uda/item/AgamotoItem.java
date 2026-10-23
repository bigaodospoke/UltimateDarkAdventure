package com.uda.item;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Olho de Agamoto — o pingente que o Blind Executioner usava.
 * Desperto: "Instante Congelado" (botão direito) quase para o tempo dos inimigos em volta.
 * Supremo: além disso, uma vez a cada 5 minutos o tempo volta quando você morreria (ModEvents).
 */
public class AgamotoItem extends Item implements OcularItem {
    public static final int REWIND_COOLDOWN = 20 * 60 * 5;
    private static final int HISTORY_EVERY = 10;
    private static final int HISTORY_SIZE = 10; // 5 segundos
    /** Posições recentes de quem carrega um Agamoto supremo (para onde o tempo volta). */
    private static final Map<UUID, ArrayDeque<Vec3>> HISTORY = new ConcurrentHashMap<>();

    public AgamotoItem(Properties properties) {
        super(properties);
    }

    @Override
    public String powerKey() {
        return "agamoto_eye";
    }

    @Override
    public boolean opensWithKey() {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return Ocular.tier(stack) >= Ocular.SUPREME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int tier = Ocular.tier(stack);
        if (tier < Ocular.AWAKENED) {
            return InteractionResultHolder.pass(stack);
        }
        if (level instanceof ServerLevel server) {
            boolean supreme = tier >= Ocular.SUPREME;
            double radius = supreme ? 12.0 : 8.0;
            int duration = supreme ? 160 : 100;
            for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                    e -> e != player && e.isAlive() && e instanceof Enemy)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 6), player);
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 4), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1), player);
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY(0.5), target.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            }
            // círculo de relógio se expandindo
            for (int i = 0; i < 48; i++) {
                float a = i * Mth.TWO_PI / 48;
                server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.3F), player.getX() + Mth.cos(a) * radius * 0.5,
                        player.getY() + 0.2, player.getZ() + Mth.sin(a) * radius * 0.5, 1, 0, 0, 0, 0);
            }
            server.playSound(null, player, ModSounds.AGAMOTO_FREEZE.get(), SoundSource.PLAYERS, 1.2F, 0.8F);
        }
        player.getCooldowns().addCooldown(this, tier >= Ocular.SUPREME ? 400 : 600);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && Ocular.tier(stack) >= Ocular.SUPREME
                && player.tickCount % HISTORY_EVERY == 0) {
            ArrayDeque<Vec3> history = HISTORY.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
            if (player.onGround()) {
                history.addLast(player.position());
                if (history.size() > HISTORY_SIZE) history.removeFirst();
            }
        }
    }

    /** Onde o jogador estava há ~5 s (ou onde está, se não houver histórico). */
    public static Vec3 rewindPosition(Player player) {
        ArrayDeque<Vec3> history = HISTORY.get(player.getUUID());
        return history == null || history.isEmpty() ? player.position() : history.peekFirst();
    }

    public static void forget(Player player) {
        HISTORY.remove(player.getUUID());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Ocular.appendTooltip(stack, this, tooltip);
    }
}
