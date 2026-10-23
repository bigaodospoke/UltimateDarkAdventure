package com.uda.entity;

import java.util.ArrayList;
import java.util.List;

import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dragão Abissal — dragão negro de asas rasgadas, coberto de olhos.
 * Circula o alvo voando e alterna: sopro roxo (cone que deixa poças de fogo roxo), investida
 * com mordida e chuva de olhos (bombas que caem em volta do alvo). Desperto: mais rápido,
 * mais bombas e as explosões oculares (marcas no chão que estouram depois de um instante).
 */
public class AbyssalDragon extends AbstractEyeBoss {
    public static final int BREATH = 1;
    public static final int DIVE = 2;
    public static final int RAIN = 3;
    public static final int EXPLOSIONS = 4;
    public static final int ROAR = 5;
    public static final int BREATH_WINDUP = 20;
    private static final int BREATH_LENGTH = 70;
    private static final double BREATH_RANGE = 16.0;

    private double circleAngle;
    private Vec3 breathDirection = Vec3.ZERO;
    private Vec3 diveTarget;
    private Vec3 home;
    private final List<Mark> marks = new ArrayList<>();

    /** Marca de explosão ocular: o anel no chão encolhe até estourar. */
    private static final class Mark {
        final Vec3 pos;
        int ticks;

        Mark(Vec3 pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    public AbyssalDragon(EntityType<? extends AbyssalDragon> type, Level level) {
        super(type, level);
        this.xpReward = 600;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    private double speed() {
        return this.isAwakened() ? 1.35 : 1.0;
    }

    /** Boca do dragão: à frente do corpo, na direção em que ele olha. */
    public Vec3 mouthPosition() {
        Vec3 forward = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
        return this.position().add(0.0, this.getBbHeight() * 0.5, 0.0).add(forward.scale(6.0));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.home == null) {
            this.home = this.position();
        }
        this.tickMarks();
        if (this.isTransforming()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.setAttack(NO_ATTACK);
            this.circle(this.home.add(0, -8, 0), 0.6);
            return;
        }
        switch (this.getAttack()) {
            case BREATH -> this.breath(target);
            case DIVE -> this.dive(target);
            case RAIN -> this.rain(target);
            case EXPLOSIONS -> this.explosions(target);
            case ROAR -> this.roar(target);
            default -> {
                this.circle(target.position(), this.speed());
                if (--this.attackCooldown <= 0) {
                    this.chooseAttack();
                }
            }
        }
    }

    private void circle(Vec3 center, double speed) {
        this.circleAngle += 0.03 * speed;
        Vec3 goal = center.add(Math.cos(this.circleAngle) * 18.0, 11.0 + Math.sin(this.circleAngle * 2.0) * 2.0, Math.sin(this.circleAngle) * 18.0);
        this.flyTowards(goal, 0.85 * speed, 0.08);
        this.face(this.getDeltaMovement(), 6.0F);
    }

    private void chooseAttack() {
        int options = this.isEnraged() ? 5 : this.isAwakened() ? 4 : 3;
        int pick = 1 + this.random.nextInt(options);
        this.diveTarget = null;
        this.setAttack(pick);
    }

    // ------------------------------------------------------------------ sopro roxo

    private void breath(LivingEntity target) {
        int ticks = this.getAttackTicks();
        Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1).normalize();
        this.flyTowards(target.position().add(away.scale(10.0)).add(0.0, 5.0, 0.0), 0.6 * this.speed(), 0.1);
        Vec3 toTarget = target.getEyePosition().subtract(this.mouthPosition()).normalize();
        this.face(toTarget, 8.0F);
        if (ticks == 1) {
            this.breathDirection = toTarget;
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.8F);
        }
        if (ticks < BREATH_WINDUP) {
            return;
        }
        // o sopro acompanha o alvo devagar (dá para fugir dele)
        this.breathDirection = this.breathDirection.lerp(toTarget, 0.06).normalize();
        Vec3 mouth = this.mouthPosition();
        if (ticks % 4 == 0) {
            float damage = this.isAwakened() ? 7.0F : 5.0F;
            for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().expandTowards(this.breathDirection.scale(BREATH_RANGE + 6)).inflate(4.0),
                    e -> e != this && e.isAlive() && !(e instanceof AbstractEyeMonster))) {
                Vec3 rel = victim.getBoundingBox().getCenter().subtract(mouth);
                double along = rel.dot(this.breathDirection);
                if (along < 0 || along > BREATH_RANGE) continue;
                double off = rel.subtract(this.breathDirection.scale(along)).length();
                if (off < 1.2 + along * 0.22 && victim.hurt(this.damageSources().indirectMagic(this, this), damage)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), this);
                }
            }
        }
        if (ticks % 12 == 0) {
            // onde o sopro bate no chão fica uma poça de fogo roxo
            BlockHitResult hit = this.level().clip(new ClipContext(mouth, mouth.add(this.breathDirection.scale(BREATH_RANGE + 8)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                AreaEffectCloud cloud = new AreaEffectCloud(this.level(), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
                cloud.setOwner(this);
                cloud.setParticle(ParticleTypes.DRAGON_BREATH);
                cloud.setRadius(2.5F);
                cloud.setDuration(100);
                cloud.setRadiusPerTick(-0.01F);
                cloud.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                this.level().addFreshEntity(cloud);
            }
        }
        if (ticks % 10 == 0) {
            this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 0.6F);
        }
        if (ticks > BREATH_WINDUP + BREATH_LENGTH + (this.isAwakened() ? 30 : 0)) {
            this.endAttack(70);
        }
    }

    public Vec3 getBreathDirection() {
        return this.breathDirection;
    }

    // ------------------------------------------------------------------ investida

    private void dive(LivingEntity target) {
        int ticks = this.getAttackTicks();
        if (ticks < 25) {
            // sobe bem alto antes de mergulhar
            this.flyTowards(target.position().add(0.0, 16.0, 0.0), 0.9 * this.speed(), 0.1);
            this.face(target.position().subtract(this.position()), 6.0F);
            return;
        }
        if (this.diveTarget == null) {
            this.diveTarget = target.position().add(0.0, 1.0, 0.0);
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 1.1F);
        }
        this.flyTowards(this.diveTarget, 1.7 * this.speed(), 0.25);
        this.face(this.getDeltaMovement(), 12.0F);
        if (this.getBoundingBox().inflate(1.5).intersects(target.getBoundingBox())) {
            if (target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                Vec3 push = target.position().subtract(this.position()).normalize();
                target.push(push.x * 2.0, 0.8, push.z * 2.0);
                target.hurtMarked = true;
            }
            this.endAttack(60);
        } else if (this.position().distanceTo(this.diveTarget) < 2.5 || ticks > 80) {
            this.endAttack(60);
        }
    }

    // ------------------------------------------------------------------ chuva de olhos

    private void rain(LivingEntity target) {
        int ticks = this.getAttackTicks();
        this.flyTowards(target.position().add(0.0, 17.0, 0.0), 0.8 * this.speed(), 0.1);
        this.face(target.position().subtract(this.position()).multiply(1, 0, 1), 4.0F);
        int every = this.isAwakened() ? 2 : 3;
        if (ticks > 20 && ticks < 90 && ticks % every == 0) {
            Vec3 from = this.position().add(this.random.nextGaussian() * 1.5, -0.5, this.random.nextGaussian() * 1.5);
            Vec3 spot = target.position().add(this.random.nextGaussian() * 6.0, 0.0, this.random.nextGaussian() * 6.0);
            double fall = 30.0;
            Vec3 velocity = new Vec3((spot.x - from.x) / fall, (spot.y - from.y + 0.5 * 0.04 * fall * fall) / fall, (spot.z - from.z) / fall);
            this.level().addFreshEntity(new EyeBomb(this.level(), this, from, velocity));
            if (ticks % 12 == 0) {
                this.playSound(SoundEvents.ENDER_EYE_DEATH, 2.0F, 0.5F);
            }
        }
        if (ticks > 100) {
            this.endAttack(70);
        }
    }

    // ------------------------------------------------------------------ explosões oculares (desperto)

    private void explosions(LivingEntity target) {
        int ticks = this.getAttackTicks();
        this.flyTowards(target.position().add(this.position().subtract(target.position()).multiply(1, 0, 1).normalize().scale(12.0)).add(0, 8, 0),
                0.5, 0.1);
        this.face(target.position().subtract(this.position()), 6.0F);
        if (ticks == 10) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.6F);
            this.marks.add(new Mark(target.position(), 34));
            for (int i = 0; i < 6; i++) {
                float angle = i * Mth.TWO_PI / 6 + this.random.nextFloat();
                double dist = 3.0 + this.random.nextDouble() * 4.0;
                this.marks.add(new Mark(target.position().add(Math.cos(angle) * dist, 0.0, Math.sin(angle) * dist), 30 + i * 4));
            }
        }
        if (ticks > 70) {
            this.endAttack(60);
        }
    }

    // ------------------------------------------------------------------ rugido abissal (enfurecido)

    /** Para no ar, ruge puxando quem estiver perto e solta um anel de bombas oculares em todas as direções. */
    private void roar(LivingEntity target) {
        int ticks = this.getAttackTicks();
        this.flyTowards(target.position().add(0.0, 12.0, 0.0), 0.3, 0.1);
        this.face(target.position().subtract(this.position()), 6.0F);
        if (ticks == 18 && this.level() instanceof ServerLevel level) {
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.45F);
            this.playSound(ModSounds.BOSS_ENRAGE.get(), 4.0F, 0.8F);
            for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(24.0))) {
                Vec3 pull = this.position().subtract(player.position()).multiply(1, 0, 1).normalize();
                player.push(pull.x * 1.4, 0.5, pull.z * 1.4);
                player.hurtMarked = true;
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40), this);
            }
            for (int i = 0; i < 14; i++) {
                float angle = i * Mth.TWO_PI / 14;
                Vec3 velocity = new Vec3(Mth.cos(angle) * 0.55, 0.35, Mth.sin(angle) * 0.55);
                this.level().addFreshEntity(new EyeBomb(this.level(), this, this.position().add(0, 1, 0), velocity));
            }
        }
        if (ticks > 55) {
            this.endAttack(50);
        }
    }

    private void tickMarks() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Mark mark : this.marks) {
            mark.ticks--;
            if (mark.ticks % 2 == 0) {
                double radius = 0.6 + mark.ticks * 0.07;
                for (int i = 0; i < 10; i++) {
                    float angle = i * Mth.TWO_PI / 10 + mark.ticks * 0.2F;
                    level.sendParticles(new DustParticleOptions(NEON_PURPLE, 1.3F), mark.pos.x + Math.cos(angle) * radius, mark.pos.y + 0.15,
                            mark.pos.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
                }
            }
            if (mark.ticks <= 0) {
                level.explode(this, mark.pos.x, mark.pos.y + 0.5, mark.pos.z, 2.6F, Level.ExplosionInteraction.NONE);
                level.sendParticles(new DustParticleOptions(NEON_PURPLE, 2.5F), mark.pos.x, mark.pos.y + 0.8, mark.pos.z, 30, 0.8, 0.8, 0.8, 0.05);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, mark.pos.x, mark.pos.y + 0.8, mark.pos.z, 25, 0.4, 0.6, 0.4, 0.2);
            }
        }
        this.marks.removeIf(mark -> mark.ticks <= 0);
    }

    // ------------------------------------------------------------------ cliente: partículas do sopro e bater de asas

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            return;
        }
        if (this.getAttack() == BREATH && this.getAttackTicks() > BREATH_WINDUP) {
            Vec3 mouth = this.mouthPosition();
            Vec3 dir = Vec3.directionFromRotation(this.getXRot(), this.getYRot());
            for (int i = 0; i < 6; i++) {
                Vec3 v = dir.scale(0.8 + this.random.nextDouble() * 0.6).add(this.random.nextGaussian() * 0.12, this.random.nextGaussian() * 0.12,
                        this.random.nextGaussian() * 0.12);
                this.level().addParticle(ParticleTypes.DRAGON_BREATH, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
                if (i % 2 == 0) {
                    this.level().addParticle(new DustParticleOptions(NEON_PURPLE, 2.0F), mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
                }
            }
        }
        if (this.tickCount % 24 == 0 && !this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 2.5F,
                    0.7F + this.random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected void onAwaken(ServerLevel level) {
        super.onAwaken(level);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.5F);
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }
}
