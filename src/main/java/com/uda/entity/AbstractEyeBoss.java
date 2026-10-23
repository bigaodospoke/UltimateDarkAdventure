package com.uda.entity;

import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Base dos bosses do Olho: barra de chefe, voo, ataques sincronizados com o cliente e as três fases.
 * <ul>
 *   <li>Adormecida: olho fechado, ataques básicos.</li>
 *   <li>Desperta (50% da vida): invulnerável durante a transformação, onda de choque, recupera um pouco de vida,
 *       ataques novos e a aura.</li>
 *   <li>Enfurecida (20% da vida): barra vermelha, ataca bem mais rápido e ganha o ataque final de cada boss.</li>
 * </ul>
 */
public abstract class AbstractEyeBoss extends AbstractEyeMonster {
    private static final EntityDataAccessor<Byte> DATA_ATTACK = SynchedEntityData.defineId(AbstractEyeBoss.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(AbstractEyeBoss.class, EntityDataSerializers.BOOLEAN);
    public static final int NO_ATTACK = 0;
    private static final int DEATH_DURATION = 60;

    protected final ServerBossEvent bossEvent;
    private int attackTicks;
    protected int attackCooldown = 60;

    protected AbstractEyeBoss(EntityType<? extends AbstractEyeBoss> type, Level level) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(true);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.xpReward = 500;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ATTACK, (byte) NO_ATTACK);
        builder.define(DATA_ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SkyTargetGoal<>(this, Player.class));
        this.targetSelector.addGoal(3, new SkyTargetGoal<>(this, IronGolem.class));
    }

    /** O alvo padrão só procura 4 blocos acima/abaixo; um boss voando precisa procurar numa esfera. */
    private static class SkyTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
        SkyTargetGoal(AbstractEyeBoss boss, Class<T> type) {
            super(boss, type, 10, false, false, null);
        }

        @Override
        protected AABB getTargetSearchArea(double distance) {
            return this.mob.getBoundingBox().inflate(distance, distance, distance);
        }
    }

    @Override
    protected int awakenDuration() {
        return 40;
    }

    // ------------------------------------------------------------------ ataques

    public int getAttack() {
        return this.entityData.get(DATA_ATTACK);
    }

    /** Ticks desde o começo do ataque atual (contado igual no cliente e no servidor). */
    public int getAttackTicks() {
        return this.attackTicks;
    }

    protected void setAttack(int attack) {
        this.entityData.set(DATA_ATTACK, (byte) attack);
        this.attackTicks = 0;
    }

    protected void endAttack(int cooldown) {
        this.setAttack(NO_ATTACK);
        this.attackCooldown = this.isEnraged() ? cooldown / 2 : this.isAwakened() ? cooldown * 2 / 3 : cooldown;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_ATTACK.equals(key)) {
            this.attackTicks = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.attackTicks++;
    }

    // ------------------------------------------------------------------ fases e barra de chefe

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.isAwakened() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.setAwakened(true);
        } else if (this.isAwakened() && !this.isTransforming() && !this.isEnraged() && this.getHealth() <= this.getMaxHealth() * 0.2F
                && this.level() instanceof ServerLevel level) {
            this.entityData.set(DATA_ENRAGED, true);
            this.onEnrage(level);
        }
    }

    @Override
    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    /** Entrou na fase enfurecida: barra vermelha, rugido e uma onda que empurra todo mundo. */
    protected void onEnrage(ServerLevel level) {
        this.setAttack(NO_ATTACK);
        this.attackCooldown = 30;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.bossEvent.setName(Component.translatable("boss.uda.enraged", this.getDisplayName()));
        this.playSound(ModSounds.BOSS_ENRAGE.get(), 5.0F, 0.6F);
        this.burstParticles(level, 200);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(16.0))) {
            Vec3 away = player.position().subtract(this.position()).normalize();
            player.push(away.x * 2.0, 0.7, away.z * 2.0);
            player.hurtMarked = true;
        }
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 32; i++) {
                float angle = i * Mth.TWO_PI / 32;
                double r = 4 + ring * 3;
                level.sendParticles(new DustParticleOptions(NEON_PURPLE, 3.0F), this.getX() + Mth.cos(angle) * r, this.getY(0.5),
                        this.getZ() + Mth.sin(angle) * r, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    @Override
    protected void onAwaken(ServerLevel level) {
        this.burstParticles(level, 160);
        this.setAttack(NO_ATTACK);
        this.attackCooldown = 50;
        this.heal(this.getMaxHealth() * 0.1F);
        this.bossEvent.setName(Component.translatable("boss.uda.awakened", this.getDisplayName()));
        // onda de choque: empurra quem estiver perto e apaga a visão por um instante
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(14.0))) {
            Vec3 away = player.position().subtract(this.position()).normalize();
            player.push(away.x * 1.6, 0.6, away.z * 1.6);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80), this);
        }
        for (int i = 0; i < 24; i++) {
            float angle = i * Mth.TWO_PI / 24;
            level.sendParticles(new DustParticleOptions(NEON_PURPLE, 2.5F), this.getX() + Mth.cos(angle) * 6, this.getY(0.5),
                    this.getZ() + Mth.sin(angle) * 6, 2, 0.3, 0.3, 0.3, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isTransforming() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_ENRAGED, tag.getBoolean("Enraged"));
        if (this.isEnraged()) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setName(Component.translatable("boss.uda.enraged", this.getDisplayName()));
        } else {
            this.bossEvent.setName(this.isAwakened()
                    ? Component.translatable("boss.uda.awakened", this.getDisplayName()) : this.getDisplayName());
        }
    }

    // ------------------------------------------------------------------ voo

    /** Acelera em direção a um ponto (o movimento é todo controlado por velocidade, sem pathfinding). */
    protected void flyTowards(Vec3 goal, double speed, double acceleration) {
        Vec3 delta = goal.subtract(this.position());
        double distance = delta.length();
        Vec3 wanted = distance < 0.3 ? Vec3.ZERO : delta.scale(Math.min(speed, distance * 0.2) / distance);
        this.setDeltaMovement(this.getDeltaMovement().lerp(wanted, acceleration));
    }

    /** Vira o corpo (e inclina) para uma direção. */
    protected void face(Vec3 direction, float maxTurn) {
        if (direction.lengthSqr() < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxTurn));
        this.setXRot(Mth.approachDegrees(this.getXRot(), Mth.clamp(pitch, -60.0F, 60.0F), maxTurn * 0.5F));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        // o corpo sempre acompanha a direção do voo (como o Phantom)
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                AbstractEyeBoss.this.yHeadRot = AbstractEyeBoss.this.yBodyRot = AbstractEyeBoss.this.getYRot();
            }
        };
    }

    // ------------------------------------------------------------------ chefe: voa, não cai, não some, não é empurrado

    @Override
    public boolean isNoGravity() {
        return true; // o NBT do /summon zeraria o setNoGravity do construtor
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WITHER) && !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(8.0);
    }

    /** Morte lenta: o corpo treme e solta energia por 3 segundos antes de sumir. */
    @Override
    protected void tickDeath() {
        this.deathTime++;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(0.0, -0.01, 0.0));
        if (this.level() instanceof ServerLevel level) {
            if (this.deathTime % 4 == 0) {
                level.sendParticles(ParticleTypes.EXPLOSION, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 1, 0, 0, 0, 0);
                level.sendParticles(new DustParticleOptions(NEON_PURPLE, 2.0F), this.getX(), this.getY(0.5), this.getZ(), 12,
                        this.getBbWidth() * 0.4, this.getBbHeight() * 0.4, this.getBbWidth() * 0.4, 0.05);
            }
            if (this.deathTime >= DEATH_DURATION && !this.isRemoved()) {
                this.burstParticles(level, 200);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5), this.getZ(), 1, 0, 0, 0, 0);
                this.remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
    }
}
