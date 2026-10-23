package com.uda.entity;

import java.util.HashSet;
import java.util.Set;

import com.uda.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Corte de Energia da Espada Ocular: meia-lua roxa que voa reto, atravessa os inimigos
 * (acerta cada um uma vez) e se desfaz ao bater num bloco.
 */
public class EnergySlash extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_SUPREME = SynchedEntityData.defineId(EnergySlash.class, EntityDataSerializers.BOOLEAN);
    public static final int LIFETIME = 14;

    private final Set<Integer> hit = new HashSet<>();
    private float damage = 7.0F;

    public EnergySlash(EntityType<? extends EnergySlash> type, Level level) {
        super(type, level);
    }

    public EnergySlash(Level level, LivingEntity owner, float damage, boolean supreme) {
        this(ModEntities.ENERGY_SLASH.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.35, owner.getZ());
        this.damage = damage;
        this.entityData.set(DATA_SUPREME, supreme);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SUPREME, false);
    }

    public boolean isSupreme() {
        return this.entityData.get(DATA_SUPREME);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        Vec3 from = this.position();
        Vec3 to = from.add(motion);
        if (this.level() instanceof ServerLevel level) {
            HitResult block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().expandTowards(motion).inflate(0.6),
                    this::canSlash)) {
                this.hit.add(victim.getId());
                if (victim.hurt(this.damageSources().indirectMagic(this, this.getOwner()), this.damage)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80), this.getOwner());
                    if (this.isSupreme()) {
                        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), this.getOwner());
                    }
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0, 0, 0, 0);
                }
            }
            if (block.getType() == HitResult.Type.BLOCK) {
                Vec3 at = block.getLocation();
                level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 1.4F), at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.02);
                this.discard();
                return;
            }
        } else if (this.random.nextFloat() < 0.6F) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
        this.setPos(to);
        this.setDeltaMovement(motion.scale(0.97));
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    private boolean canSlash(LivingEntity entity) {
        if (entity == this.getOwner() || !entity.isAlive() || this.hit.contains(entity.getId())) {
            return false;
        }
        if (entity instanceof OwnableEntity pet && pet.getOwner() == this.getOwner()) {
            return false;
        }
        return this.getOwner() == null || !this.getOwner().isAlliedTo(entity);
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.putBoolean("Supreme", this.isSupreme());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.entityData.set(DATA_SUPREME, tag.getBoolean("Supreme"));
    }
}
