package com.uda.entity;

import org.joml.Vector3f;

import com.uda.registry.ModEntities;
import com.uda.registry.ModSounds;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * O Olho Negro — um olho gigante que flutua, cercado de anéis cheios de olhos, correntes e seis asas.
 * Paira em volta do alvo sem tirar o olho dele e alterna: orbes oculares que perseguem, e o raio ocular
 * (carrega mirando, depois dispara e varre devagar até o alvo). Desperto: as pálpebras se abrem,
 * as correntes flutuam e ele ganha a barragem de olhos e as correntes que puxam quem foge.
 */
public class BlackEye extends AbstractEyeBoss {
    public static final int ORBS = 1;
    public static final int BEAM_CHARGE = 2;
    public static final int BEAM = 3;
    public static final int BARRAGE = 4;
    public static final int CHAINS = 5;
    public static final int SPIN_BEAMS = 6;
    public static final int SPIN_CHARGE = 24;
    public static final int SPIN_TICKS = 110;
    public static final double SPIN_RANGE = 26.0;
    public static final int BEAM_CHARGE_TICKS = 30;
    private static final int BEAM_TICKS = 50;
    private static final double BEAM_RANGE = 32.0;

    private static final EntityDataAccessor<Vector3f> DATA_BEAM_END = SynchedEntityData.defineId(BlackEye.class, EntityDataSerializers.VECTOR3);

    private double orbitAngle;
    private Vec3 home;

    public BlackEye(EntityType<? extends BlackEye> type, Level level) {
        super(type, level);
        this.xpReward = 700;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BEAM_END, new Vector3f());
    }

    /** Centro do olho (de onde saem o raio e os orbes). */
    public Vec3 eyeCenter() {
        return this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
    }

    public Vec3 getBeamEnd() {
        return new Vec3(this.entityData.get(DATA_BEAM_END));
    }

    private void setBeamEnd(Vec3 end) {
        this.entityData.set(DATA_BEAM_END, end.toVector3f());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.home == null) {
            this.home = this.position();
        }
        if (this.isTransforming()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.setAttack(NO_ATTACK);
            this.flyTowards(this.home, 0.2, 0.05);
            return;
        }
        // paira em volta do alvo, sempre olhando para ele
        this.orbitAngle += this.isAwakened() ? 0.018 : 0.012;
        Vec3 goal = target.position().add(Math.cos(this.orbitAngle) * 11.0, 6.0 + Math.sin(this.orbitAngle * 3.0), Math.sin(this.orbitAngle) * 11.0);
        boolean anchored = this.getAttack() == BEAM || this.getAttack() == SPIN_BEAMS;
        this.flyTowards(goal, anchored ? 0.05 : 0.35, 0.05);
        this.face(target.getEyePosition().subtract(this.eyeCenter()), 10.0F);

        switch (this.getAttack()) {
            case ORBS -> this.orbs(target);
            case BEAM_CHARGE -> this.beamCharge(target);
            case BEAM -> this.beam(target);
            case BARRAGE -> this.barrage(target);
            case CHAINS -> this.chains(target);
            case SPIN_BEAMS -> this.spinBeams();
            default -> {
                if (--this.attackCooldown <= 0) {
                    this.chooseAttack(target);
                }
            }
        }
    }

    private void chooseAttack(LivingEntity target) {
        if (this.isEnraged() && this.random.nextFloat() < 0.35F) {
            this.setAttack(SPIN_BEAMS);
            return;
        }
        if (this.isAwakened() && target.distanceTo(this) > 14.0 && this.random.nextFloat() < 0.6F) {
            this.setAttack(CHAINS);
            return;
        }
        int[] options = this.isAwakened() ? new int[] {ORBS, BEAM_CHARGE, BARRAGE} : new int[] {ORBS, BEAM_CHARGE};
        this.setAttack(options[this.random.nextInt(options.length)]);
    }

    // ------------------------------------------------------------------ orbes oculares

    private void orbs(LivingEntity target) {
        int ticks = this.getAttackTicks();
        int count = this.isAwakened() ? 5 : 3;
        int every = this.isAwakened() ? 7 : 10;
        if (ticks % every == 0 && ticks / every < count) {
            Vec3 offset = new Vec3(this.random.nextGaussian(), this.random.nextGaussian() * 0.6, this.random.nextGaussian()).normalize().scale(2.6);
            Vec3 from = this.eyeCenter().add(offset);
            EyeOrb orb = new EyeOrb(this.level(), this, target, 7.0F, 1.8F);
            orb.setPos(from);
            Vec3 dir = target.getEyePosition().subtract(from).normalize();
            orb.shoot(dir.x, dir.y, dir.z, 0.55F, 4.0F);
            this.level().addFreshEntity(orb);
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
        }
        if (ticks > every * count + 15) {
            this.endAttack(60);
        }
    }

    // ------------------------------------------------------------------ raio ocular

    private void beamCharge(LivingEntity target) {
        int ticks = this.getAttackTicks();
        this.setBeamEnd(target.getEyePosition());
        if (ticks == 1) {
            this.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.5F);
        }
        if (ticks >= BEAM_CHARGE_TICKS) {
            this.setAttack(BEAM);
            this.playSound(SoundEvents.GUARDIAN_ATTACK, 3.0F, 0.5F);
        }
    }

    private void beam(LivingEntity target) {
        int ticks = this.getAttackTicks();
        Vec3 eye = this.eyeCenter();
        // o ponto de mira varre devagar até o alvo
        Vec3 aim = this.getBeamEnd();
        Vec3 toTarget = target.getEyePosition().subtract(aim);
        double step = this.isAwakened() ? 0.3 : 0.18;
        aim = toTarget.length() < step ? target.getEyePosition() : aim.add(toTarget.normalize().scale(step));
        Vec3 dir = aim.subtract(eye).normalize();
        BlockHitResult hit = this.level().clip(new ClipContext(eye, eye.add(dir.scale(BEAM_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : eye.add(dir.scale(BEAM_RANGE));
        // guarda a mira (não o ponto de impacto) para continuar varrendo dali
        this.setBeamEnd(aim);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(NEON_PURPLE, 1.8F), end.x, end.y, end.z, 4, 0.2, 0.2, 0.2, 0.0);
            if (ticks % 4 == 0) {
                float damage = this.isAwakened() ? 8.0F : 6.0F;
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5),
                        e -> e != this && e.isAlive() && !(e instanceof AbstractEyeMonster))) {
                    Vec3 rel = victim.getBoundingBox().getCenter().subtract(eye);
                    double along = Mth.clamp(rel.dot(dir), 0.0, end.distanceTo(eye));
                    if (rel.subtract(dir.scale(along)).length() < 1.0 + victim.getBbWidth() * 0.5
                            && victim.hurt(this.damageSources().indirectMagic(this, this), damage)) {
                        victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), this);
                    }
                }
            }
        }
        if (ticks > BEAM_TICKS + (this.isAwakened() ? 20 : 0)) {
            this.endAttack(70);
        }
    }

    // ------------------------------------------------------------------ barragem de olhos (desperto)

    private void barrage(LivingEntity target) {
        int ticks = this.getAttackTicks();
        if (ticks >= 10 && ticks <= 46 && (ticks - 10) % 12 == 0) {
            Vec3 toTarget = target.getEyePosition().subtract(this.eyeCenter()).normalize();
            float base = (float) Math.atan2(toTarget.z, toTarget.x) + ticks * 0.13F;
            for (int i = 0; i < 12; i++) {
                float angle = base + i * Mth.TWO_PI / 12;
                Vec3 dir = new Vec3(Math.cos(angle), toTarget.y * 0.6 - 0.1, Math.sin(angle)).normalize();
                EyeOrb orb = new EyeOrb(this.level(), this, null, 4.0F, 0.9F);
                orb.setPos(this.eyeCenter().add(dir.scale(2.4)));
                orb.shoot(dir.x, dir.y, dir.z, 0.5F, 0.0F);
                this.level().addFreshEntity(orb);
            }
            this.playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 2.5F, 0.5F);
        }
        if (ticks > 60) {
            this.endAttack(60);
        }
    }

    // ------------------------------------------------------------------ correntes (desperto)

    private void chains(LivingEntity target) {
        int ticks = this.getAttackTicks();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = this.eyeCenter();
        Vec3 to = target.getBoundingBox().getCenter();
        if (ticks <= 16) {
            // a corrente aparece como um rastro de elos indo até o alvo
            int links = (int) (from.distanceTo(to) * 2);
            for (int i = 0; i < links; i++) {
                Vec3 p = from.lerp(to, (double) i / links);
                level.sendParticles(new DustParticleOptions(i % 2 == 0 ? NEON_PURPLE : DARK_PURPLE, 1.0F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }
        if (ticks == 8) {
            Vec3 pull = from.subtract(to).normalize();
            target.setDeltaMovement(pull.scale(1.7).add(0.0, 0.5, 0.0));
            target.hurtMarked = true;
            target.hurt(this.damageSources().mobAttack(this), 3.0F);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
            this.playSound(SoundEvents.CHAIN_BREAK, 3.0F, 0.5F);
        }
        if (ticks > 30) {
            this.endAttack(40);
        }
    }

    // ------------------------------------------------------------------ olhar final (enfurecido)

    /** Direção do raio k (0..3) no tick t do Olhar Final: quatro raios inclinados para baixo, girando. */
    public static Vec3 spinDirection(float ticks, int k) {
        float angle = (ticks - SPIN_CHARGE) * 0.045F + k * Mth.HALF_PI;
        float pitch = 32.0F * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(angle) * Mth.cos(pitch), -Mth.sin(pitch), Mth.sin(angle) * Mth.cos(pitch));
    }

    /** Carrega e então gira quatro raios em volta de si, varrendo o chão. */
    private void spinBeams() {
        int ticks = this.getAttackTicks();
        if (ticks == 1) {
            this.playSound(ModSounds.BOSS_ENRAGE.get(), 4.0F, 1.2F);
        }
        if (ticks == SPIN_CHARGE) {
            this.playSound(SoundEvents.GUARDIAN_ATTACK, 4.0F, 0.4F);
        }
        if (ticks >= SPIN_CHARGE && ticks % 4 == 0 && this.level() instanceof ServerLevel level) {
            Vec3 eye = this.eyeCenter();
            for (int k = 0; k < 4; k++) {
                Vec3 dir = spinDirection(ticks, k);
                BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(SPIN_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                Vec3 end = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : eye.add(dir.scale(SPIN_RANGE));
                level.sendParticles(new DustParticleOptions(NEON_PURPLE, 2.0F), end.x, end.y + 0.2, end.z, 3, 0.3, 0.1, 0.3, 0.0);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5),
                        e -> e != this && e.isAlive() && !(e instanceof AbstractEyeMonster))) {
                    Vec3 rel = victim.getBoundingBox().getCenter().subtract(eye);
                    double along = Mth.clamp(rel.dot(dir), 0.0, end.distanceTo(eye));
                    if (rel.subtract(dir.scale(along)).length() < 1.2 + victim.getBbWidth() * 0.5) {
                        victim.hurt(this.damageSources().indirectMagic(this, this), 7.0F);
                    }
                }
            }
        }
        if (ticks > SPIN_CHARGE + SPIN_TICKS) {
            this.endAttack(80);
        }
    }

    // ------------------------------------------------------------------ despertar

    @Override
    protected void onAwaken(ServerLevel level) {
        super.onAwaken(level);
        this.playSound(SoundEvents.WITHER_SPAWN, 3.0F, 0.6F);
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.5F);
        // dois Hollow Watchers despertos vêm proteger o Olho
        for (int i = 0; i < 2; i++) {
            HollowWatcher watcher = ModEntities.HOLLOW_WATCHER.get().create(level);
            if (watcher == null) continue;
            double angle = i * Math.PI + this.random.nextDouble();
            int x = Mth.floor(this.getX() + Math.cos(angle) * 4);
            int z = Mth.floor(this.getZ() + Math.sin(angle) * 4);
            Vec3 at = new Vec3(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
            watcher.moveTo(at.x, at.y, at.z, this.getYRot(), 0.0F);
            watcher.finalizeSpawn(level, level.getCurrentDifficultyAt(watcher.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            watcher.setTarget(this.getTarget());
            watcher.setAwakened(true);
            level.addFreshEntity(watcher);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 40, 0.4, 1.0, 0.4, 0.1);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // o raio pode ir longe
        return this.getAttack() == BEAM || this.getAttack() == BEAM_CHARGE || this.getAttack() == SPIN_BEAMS
                ? super.getBoundingBoxForCulling().inflate(BEAM_RANGE)
                : super.getBoundingBoxForCulling();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
