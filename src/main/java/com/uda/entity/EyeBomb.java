package com.uda.entity;

import com.uda.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

/** Bomba Ocular da "chuva de olhos" do Dragão Abissal: cai e explode (sem quebrar blocos). */
public class EyeBomb extends Projectile implements GlowingEye {
    private static final double GRAVITY = 0.04;
    private static final int LIFETIME = 200;

    public EyeBomb(EntityType<? extends EyeBomb> type, Level level) {
        super(type, level);
    }

    public EyeBomb(Level level, LivingEntity owner, Vec3 position, Vec3 velocity) {
        this(ModEntities.EYE_BOMB.get(), level);
        this.setOwner(owner);
        this.setPos(position);
        this.setDeltaMovement(velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > LIFETIME) {
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
        Vec3 motion = this.getDeltaMovement().add(0.0, -GRAVITY, 0.0);
        this.setDeltaMovement(motion);
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY() + 0.2, this.getZ(), 0.0, 0.02, 0.0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !(entity instanceof AbstractEyeMonster);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            level.explode(this, this.getX(), this.getY(), this.getZ(), 2.2F, Level.ExplosionInteraction.NONE);
            level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 2.0F), this.getX(), this.getY() + 0.3, this.getZ(),
                    24, 0.6, 0.4, 0.6, 0.05);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.3, 0.3, 0.3, 0.15);
            this.discard();
        }
    }

    @Override
    public float eyeSize() {
        return 0.55F;
    }

    @Override
    public boolean eyeAwakened() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
