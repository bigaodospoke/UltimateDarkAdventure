package com.uda.entity;

import javax.annotation.Nullable;

import com.uda.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/** Orbe Ocular lançado pelo Grimório: voa reto (sem gravidade); desperto, persegue o alvo. */
public class EyeOrb extends Projectile implements GlowingEye {
    private static final EntityDataAccessor<Boolean> DATA_AWAKENED = SynchedEntityData.defineId(EyeOrb.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(EyeOrb.class, EntityDataSerializers.FLOAT);
    private static final int LIFETIME = 70;
    private static final double HOMING_STRENGTH = 0.12;

    @Nullable
    private LivingEntity target;
    private float damage = -1.0F;
    /** Orbe do grimório supremo: explode ao acertar (sem quebrar blocos). */
    private boolean explosive;

    public EyeOrb(EntityType<? extends EyeOrb> type, Level level) {
        super(type, level);
    }

    public EyeOrb(Level level, LivingEntity owner, boolean awakened, @Nullable LivingEntity target) {
        this(ModEntities.EYE_ORB.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        this.entityData.set(DATA_AWAKENED, awakened);
        this.target = target;
    }

    /** Orbe dos bosses: dano e tamanho próprios (o alvo nulo deixa o orbe reto). */
    public EyeOrb(Level level, LivingEntity owner, @Nullable LivingEntity target, float damage, float size) {
        this(level, owner, true, target);
        this.damage = damage;
        this.entityData.set(DATA_SIZE, size);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AWAKENED, false);
        builder.define(DATA_SIZE, 1.0F);
    }

    public void setExplosive(boolean explosive) {
        this.explosive = explosive;
    }

    @Override
    public float eyeSize() {
        return this.entityData.get(DATA_SIZE) * (this.isAwakened() ? 0.42F : 0.34F);
    }

    @Override
    public boolean eyeAwakened() {
        return this.isAwakened();
    }

    public boolean isAwakened() {
        return this.entityData.get(DATA_AWAKENED);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > LIFETIME) {
            this.burst();
            this.discard();
            return;
        }
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit)) {
            this.hitTargetOrDeflectSelf(hit);
        }
        if (this.isRemoved()) {
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        if (!this.level().isClientSide && this.target != null && this.target.isAlive() && this.tickCount > 3) {
            Vec3 toTarget = this.target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(motion.length());
            motion = motion.lerp(toTarget, HOMING_STRENGTH);
            this.setDeltaMovement(motion);
            this.hasImpulse = true;
        }
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
        if (this.level().isClientSide) {
            // portal reverso brilha no escuro (a poeira colorida fica escura de noite)
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                    this.getX() - motion.x * 0.8, this.getY() + 0.1 - motion.y * 0.8, this.getZ() - motion.z * 0.8, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        // orbes dos mobs do Olho não acertam outros mobs do Olho
        return super.canHitEntity(entity) && entity != this.getOwner()
                && !(this.getOwner() instanceof AbstractEyeMonster && entity instanceof AbstractEyeMonster);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity hitEntity = result.getEntity();
        float damage = this.damage > 0 ? this.damage : this.isAwakened() ? 6.0F : 5.0F;
        hitEntity.hurt(this.damageSources().indirectMagic(this, this.getOwner()), damage);
        if (hitEntity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), this.getEffectSource());
            if (this.isAwakened()) {
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 40), this.getEffectSource());
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (this.explosive) {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.6F, Level.ExplosionInteraction.NONE);
            }
            this.burst();
            this.discard();
        }
    }

    private void burst() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.4F), this.getX(), this.getY() + 0.1, this.getZ(),
                    16, 0.25, 0.25, 0.25, 0.02);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.1, 0.1, 0.1, 0.1);
            this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 0.8F, 0.6F);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakened", this.isAwakened());
        tag.putFloat("Damage", this.damage);
        tag.putFloat("Size", this.entityData.get(DATA_SIZE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_AWAKENED, tag.getBoolean("Awakened"));
        this.damage = tag.contains("Damage") ? tag.getFloat("Damage") : -1.0F;
        this.entityData.set(DATA_SIZE, tag.contains("Size") ? tag.getFloat("Size") : 1.0F);
    }
}
