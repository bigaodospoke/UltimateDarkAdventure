package com.uda.entity;

import javax.annotation.Nullable;

import com.uda.registry.ModEntities;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Flecha Ocular — disparada pelo Hollow Watcher desperto.
 * Curva levemente em direção ao alvo e marca quem atinge (brilho + fraqueza).
 */
public class EyeArrow extends AbstractArrow {
    private static final int HOMING_TICKS = 30;
    private static final double HOMING_STRENGTH = 0.08;

    @Nullable
    private LivingEntity homingTarget;
    /** Flecha do arco supremo: persegue por mais tempo, com mais força, e definha. */
    private boolean supreme;

    public EyeArrow(EntityType<? extends EyeArrow> type, Level level) {
        super(type, level);
    }

    public EyeArrow(Level level, LivingEntity owner, ItemStack ammo, @Nullable ItemStack weapon, @Nullable LivingEntity target) {
        super(ModEntities.EYE_ARROW.get(), owner, level, ammo, weapon);
        this.homingTarget = target;
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (!this.inGround) {
                this.level().addParticle(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 0.9F),
                        this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                if (this.random.nextFloat() < 0.3F) {
                    this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
                }
            }
        } else if (!this.inGround && this.homingTarget != null && this.tickCount < (this.supreme ? HOMING_TICKS * 2 : HOMING_TICKS)
                && this.homingTarget.isAlive()) {
            Vec3 motion = this.getDeltaMovement();
            Vec3 toTarget = this.homingTarget.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(motion.length());
            this.setDeltaMovement(motion.lerp(toTarget, this.supreme ? HOMING_STRENGTH * 2 : HOMING_STRENGTH));
            this.hasImpulse = true;
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), this.getEffectSource());
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60), this.getEffectSource());
        if (this.supreme) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1), this.getEffectSource());
        }
    }

    public void setSupreme(boolean supreme) {
        this.supreme = supreme;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}
