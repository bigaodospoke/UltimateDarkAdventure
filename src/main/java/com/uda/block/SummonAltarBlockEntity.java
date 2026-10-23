package com.uda.block;

import java.util.function.Supplier;

import com.uda.entity.AbstractEyeBoss;
import com.uda.entity.AbstractEyeMonster;
import com.uda.registry.ModBlockEntities;
import com.uda.registry.ModEntities;
import com.uda.registry.ModSounds;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Ritual de invocação dos bosses (Altar Abissal → Dragão Abissal, Altar do Vazio → O Olho Negro).
 * A energia corre dos cantos do altar para o centro, sobe numa coluna e, no fim, o boss aparece num raio.
 */
public class SummonAltarBlockEntity extends BlockEntity {
    public static final int RITUAL_TICKS = 120;
    private static final int[] BEATS = {0, 30, 55, 75, 90, 100, 108, 114};

    private int ritualTicks = -1;

    public SummonAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUMMON_ALTAR.get(), pos, state);
    }

    public int getRitualTicks() {
        return this.ritualTicks;
    }

    public boolean isVoid() {
        return this.getBlockState().getBlock() instanceof VoidAltarBlock;
    }

    private Supplier<? extends EntityType<? extends AbstractEyeBoss>> bossType() {
        return this.isVoid() ? ModEntities.BLACK_EYE : ModEntities.ABYSSAL_DRAGON;
    }

    /** Já existe um boss deste altar por perto (não invoca outro). */
    public boolean bossNearby() {
        return this.level != null && !this.level.getEntities(this.bossType().get(), new AABB(this.worldPosition).inflate(128.0),
                AbstractEyeBoss::isAlive).isEmpty();
    }

    public void start() {
        if (this.level == null || this.ritualTicks >= 0) {
            return;
        }
        this.ritualTicks = 0;
        this.level.playSound(null, this.worldPosition, ModSounds.SUMMON_START.get(), SoundSource.HOSTILE, 3.0F, this.isVoid() ? 0.5F : 0.7F);
        this.sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SummonAltarBlockEntity altar) {
        if (altar.ritualTicks < 0) {
            return;
        }
        int t = altar.ritualTicks++;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 center = Vec3.atBottomCenterOf(pos).add(0.0, 1.2, 0.0);
        float k = t / (float) RITUAL_TICKS;
        if (t % 2 == 0) {
            // quatro correntes de energia dos cantos para o centro
            for (int c = 0; c < 4; c++) {
                float a = c * Mth.HALF_PI + Mth.PI / 4 + t * 0.03F;
                double x = center.x + Mth.cos(a) * 3.5, z = center.z + Mth.sin(a) * 3.5;
                server.sendParticles(ParticleTypes.REVERSE_PORTAL, x, center.y, z, 0, center.x - x, 0.0, center.z - z, 0.09);
            }
            // a coluna sobe com o ritual
            double top = center.y + k * 14.0;
            server.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 2.0F), center.x, center.y + (top - center.y) * level.random.nextDouble(),
                    center.z, 3, 0.15, 0.4, 0.15, 0.0);
        }
        for (int beat : BEATS) {
            if (t == beat) {
                server.playSound(null, pos, ModSounds.ALTAR_HEARTBEAT.get(), SoundSource.HOSTILE, 2.5F, 0.5F + k * 0.4F);
            }
        }
        if (t >= RITUAL_TICKS) {
            altar.ritualTicks = -1;
            altar.summon(server, pos, state);
            altar.sync();
        }
    }

    private void summon(ServerLevel level, BlockPos pos, BlockState state) {
        AbstractEyeBoss boss = this.bossType().get().create(level);
        if (boss == null) {
            return;
        }
        Vec3 at = Vec3.atBottomCenterOf(pos).add(0.0, this.isVoid() ? 6.0 : 9.0, 0.0);
        boss.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
        level.addFreshEntity(boss);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.sendParticles(new DustParticleOptions(AbstractEyeMonster.NEON_PURPLE, 3.0F), at.x, at.y, at.z, 120, 2.0, 2.0, 2.0, 0.1);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, pos, ModSounds.SUMMON_COMPLETE.get(), SoundSource.HOSTILE, 5.0F, this.isVoid() ? 0.6F : 0.8F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(48.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, boss);
        }
        if (state.getBlock() instanceof AbyssalAltarBlock) {
            level.setBlock(pos, state.setValue(AbyssalAltarBlock.FRAGMENTS, 0).setValue(AbyssalAltarBlock.ACTIVE, false), 3);
        } else if (state.getBlock() instanceof VoidAltarBlock) {
            level.setBlock(pos, state.setValue(VoidAltarBlock.ACTIVE, false), 3);
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Ritual", this.ritualTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.ritualTicks = tag.contains("Ritual") ? tag.getInt("Ritual") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
