package com.uda.entity;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Base dos monstros do Olho: guarda a forma (dormente/desperta), sincroniza com o cliente
 * e cuida da animação de abrir o olho, das piscadas e das partículas de energia.
 */
public abstract class AbstractEyeMonster extends Monster implements Awakenable {
    private static final EntityDataAccessor<Boolean> DATA_AWAKENED =
            SynchedEntityData.defineId(AbstractEyeMonster.class, EntityDataSerializers.BOOLEAN);

    public static final Vector3f NEON_PURPLE = new Vector3f(0.69F, 0.19F, 1.0F);
    public static final Vector3f DARK_PURPLE = new Vector3f(0.25F, 0.05F, 0.4F);

    private float awakenProgress;
    private float prevAwakenProgress;
    private int blinkTicks;
    private int nextBlinkIn = 80;

    protected AbstractEyeMonster(EntityType<? extends AbstractEyeMonster> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKENED, false);
    }

    @Override
    public boolean isAwakened() {
        return this.entityData.get(DATA_AWAKENED);
    }

    public void setAwakened(boolean awakened) {
        if (awakened == this.isAwakened()) {
            return;
        }
        this.entityData.set(DATA_AWAKENED, awakened);
        this.applyAwakenedModifiers(awakened);
        if (this.level() instanceof ServerLevel serverLevel) {
            if (awakened) {
                this.onAwaken(serverLevel);
            } else {
                this.onFallAsleep(serverLevel);
            }
        }
    }

    /** Duração, em ticks, da animação de abrir (ou fechar) o olho. */
    protected int awakenDuration() {
        return 20;
    }

    /** Aplica ou remove os bônus da forma desperta. Também é chamado ao carregar do disco. */
    protected void applyAwakenedModifiers(boolean awakened) {
    }

    protected void onAwaken(ServerLevel level) {
        this.burstParticles(level, 40);
    }

    protected void onFallAsleep(ServerLevel level) {
    }

    /** Verdadeiro enquanto o olho ainda está se abrindo. */
    public boolean isTransforming() {
        return this.isAwakened() && this.awakenProgress < 1.0F;
    }

    @Override
    public float getAwakenProgress(float partialTick) {
        return Mth.lerp(partialTick, this.prevAwakenProgress, this.awakenProgress);
    }

    @Override
    public boolean isBlinking() {
        return this.blinkTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();
        this.prevAwakenProgress = this.awakenProgress;
        this.awakenProgress = Mth.approach(this.awakenProgress, this.isAwakened() ? 1.0F : 0.0F, 1.0F / this.awakenDuration());
        if (this.level().isClientSide) {
            this.tickBlink();
            this.spawnAmbientParticles();
        }
    }

    private void tickBlink() {
        if (this.blinkTicks > 0) {
            this.blinkTicks--;
        } else if (this.awakenProgress >= 1.0F && --this.nextBlinkIn <= 0) {
            this.blinkTicks = 4;
            this.nextBlinkIn = 60 + this.random.nextInt(140);
        }
    }

    /** Partículas contínuas (só no cliente): energia selada quando dormente, vazando quando desperto. */
    protected void spawnAmbientParticles() {
        if (this.isInvisible()) {
            return;
        }
        if (this.isAwakened()) {
            // a aura (cliente) faz o grosso do efeito; aqui só um pouco de poeira roxa
            if (this.random.nextFloat() < 0.2F) {
                this.level().addParticle(new DustParticleOptions(NEON_PURPLE, 0.8F),
                        this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.02, 0.0);
            }
            if (this.random.nextFloat() < 0.05F) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL,
                        this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0);
            }
        } else if (this.random.nextFloat() < 0.04F) {
            this.level().addParticle(new DustParticleOptions(DARK_PURPLE, 0.6F),
                    this.getRandomX(0.4), this.getY(0.6), this.getRandomZ(0.4), 0.0, 0.01, 0.0);
        }
    }

    protected void burstParticles(ServerLevel level, int count) {
        level.sendParticles(new DustParticleOptions(NEON_PURPLE, 1.6F), this.getX(), this.getY(0.6), this.getZ(),
                count, this.getBbWidth() * 0.6, this.getBbHeight() * 0.35, this.getBbWidth() * 0.6, 0.02);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.5), this.getZ(),
                count / 2, 0.3, 0.6, 0.3, 0.08);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        // Ao aparecer para o jogador, já surge na forma certa (sem animar a abertura do olho).
        if (DATA_AWAKENED.equals(key) && this.tickCount == 0) {
            this.awakenProgress = this.prevAwakenProgress = this.isAwakened() ? 1.0F : 0.0F;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakened", this.isAwakened());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean awakened = tag.getBoolean("Awakened");
        this.entityData.set(DATA_AWAKENED, awakened);
        this.applyAwakenedModifiers(awakened);
        this.awakenProgress = this.prevAwakenProgress = awakened ? 1.0F : 0.0F;
    }
}
