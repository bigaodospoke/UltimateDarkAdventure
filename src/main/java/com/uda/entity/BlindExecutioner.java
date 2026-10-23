package com.uda.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.uda.UltimateDarkAdventure;
import com.uda.registry.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Blind Executioner — pillager amaldiçoado das Ruínas Amaldiçoadas, de manto de mago, venda nos olhos e dois machados.
 * <p>
 * Dormente ele é cego: não precisa de linha de visão, mas só percebe quem faz barulho
 * (quem anda agachado longe dele passa despercebido). Ao perder 40% da vida ele ascende:
 * a venda cai, um olho roxo se abre no rosto, ele cresce e passa a girar os machados.
 */
public class BlindExecutioner extends AbstractEyeMonster {
    private static final ResourceLocation ASCENDED_SCALE = UltimateDarkAdventure.id("blind_executioner_ascended_scale");
    private static final ResourceLocation ASCENDED_SPEED = UltimateDarkAdventure.id("blind_executioner_ascended_speed");
    private static final ResourceLocation ASCENDED_DAMAGE = UltimateDarkAdventure.id("blind_executioner_ascended_damage");
    private static final ResourceLocation ASCENDED_SIGHT = UltimateDarkAdventure.id("blind_executioner_ascended_sight");
    private static final ResourceLocation ASCENDED_KNOCKBACK = UltimateDarkAdventure.id("blind_executioner_ascended_knockback");

    private static final byte EVENT_WHIRL = 60;
    public static final int WHIRL_DURATION = 16;
    private static final int WHIRL_HIT_TICK = WHIRL_DURATION / 2;
    private static final int WHIRL_COOLDOWN = 110;
    /** Distância (ao quadrado) em que até quem está agachado é ouvido. */
    private static final double SNEAK_HEARING_DISTANCE_SQR = 3.0 * 3.0;

    private int whirlTicks;
    private int whirlCooldown = 60;
    private int sneakLostTicks;
    private boolean offhandNext;

    public BlindExecutioner(EntityType<? extends BlindExecutioner> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AscensionGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        // mustSee = false: ele não enxerga, ele ouve (inclusive através de paredes)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::canSense));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 10, false, false, this::canSense));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, 10, false, false, this::canSense));
    }

    /** Dormente: quem anda agachado só é percebido bem de perto. */
    private boolean canSense(LivingEntity entity) {
        return this.isAwakened() || !entity.isShiftKeyDown() || this.distanceToSqr(entity) <= SNEAK_HEARING_DISTANCE_SQR;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.whirlTicks > 0) {
            this.whirlTicks--;
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();

        // Cego: perde o rastro de quem se afasta agachado.
        if (!this.isAwakened() && target != null && target.isShiftKeyDown() && this.distanceToSqr(target) > 36.0) {
            if (++this.sneakLostTicks > 60) {
                this.setTarget(null);
                this.sneakLostTicks = 0;
            }
        } else {
            this.sneakLostTicks = 0;
        }

        if (this.whirlCooldown > 0) {
            this.whirlCooldown--;
        }
        if (this.isAwakened() && !this.isTransforming() && this.whirlCooldown == 0 && this.whirlTicks == 0
                && target != null && this.distanceToSqr(target) < 16.0) {
            this.startWhirl();
        }
        if (this.whirlTicks == WHIRL_HIT_TICK) {
            this.whirlHit();
        }
    }

    // ------------------------------------------------------------------ ascensão

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Invulnerável enquanto o olho se abre.
        if (this.isTransforming() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isAwakened() && this.isAlive() && this.getHealth() <= this.getMaxHealth() * 0.6F) {
            this.setAwakened(true);
        }
        return hurt;
    }

    @Override
    protected int awakenDuration() {
        return 30;
    }

    @Override
    protected void applyAwakenedModifiers(boolean awakened) {
        this.setModifier(Attributes.SCALE, ASCENDED_SCALE, awakened, 0.25, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.setModifier(Attributes.MOVEMENT_SPEED, ASCENDED_SPEED, awakened, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.setModifier(Attributes.ATTACK_DAMAGE, ASCENDED_DAMAGE, awakened, 2.0, AttributeModifier.Operation.ADD_VALUE);
        this.setModifier(Attributes.FOLLOW_RANGE, ASCENDED_SIGHT, awakened, 16.0, AttributeModifier.Operation.ADD_VALUE);
        this.setModifier(Attributes.KNOCKBACK_RESISTANCE, ASCENDED_KNOCKBACK, awakened, 0.4, AttributeModifier.Operation.ADD_VALUE);
    }

    private void setModifier(Holder<Attribute> attribute, ResourceLocation id,
            boolean active, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (active) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    @Override
    protected void onAwaken(ServerLevel level) {
        this.burstParticles(level, 80);
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5F, 0.6F);
        this.playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
        this.heal(10.0F);
        this.getNavigation().stop();
        // A venda cai: quem estiver perto sente o Olho se abrir.
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8.0))) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60), this);
        }
    }

    /** Fica parado (e invulnerável) enquanto a venda cai e o olho se abre. */
    private class AscensionGoal extends Goal {
        AscensionGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return BlindExecutioner.this.isTransforming();
        }

        @Override
        public void start() {
            BlindExecutioner.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlindExecutioner.this.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ giro dos machados

    private void startWhirl() {
        this.whirlTicks = WHIRL_DURATION;
        this.whirlCooldown = WHIRL_COOLDOWN;
        this.level().broadcastEntityEvent(this, EVENT_WHIRL);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.6F);
    }

    private void whirlHit() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F;
        DamageSource source = this.damageSources().mobAttack(this);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0, 0.5, 3.0),
                e -> e != this && e.isAlive() && !(e instanceof Enemy) && !this.isAlliedTo(e))) {
            if (victim.hurt(source, damage)) {
                victim.knockback(0.9, this.getX() - victim.getX(), this.getZ() - victim.getZ());
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), this);
            }
        }
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        for (int i = 0; i < 12; i++) {
            float angle = i * Mth.TWO_PI / 12;
            double x = this.getX() + Mth.cos(angle) * 2.5;
            double z = this.getZ() + Mth.sin(angle) * 2.5;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, this.getY(0.5), z, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(new DustParticleOptions(NEON_PURPLE, 1.4F), x, this.getY(0.4), z, 3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_WHIRL) {
            this.whirlTicks = WHIRL_DURATION;
        } else {
            super.handleEntityEvent(id);
        }
    }

    /** 0..1 durante o giro, 0 fora dele. */
    public float getWhirlProgress(float partialTick) {
        if (this.whirlTicks <= 0) {
            return 0.0F;
        }
        return Mth.clamp((WHIRL_DURATION - this.whirlTicks + partialTick) / WHIRL_DURATION, 0.0F, 1.0F);
    }

    // ------------------------------------------------------------------ combate

    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        // Alterna os dois machados a cada golpe.
        super.swing(this.offhandNext ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, updateSelf);
        this.offhandNext = !this.offhandNext;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && this.isAwakened() && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60), this);
        }
        return hit;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EYE_AXE.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.EYE_AXE.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.04F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.04F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return spawnGroupData;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isAwakened() ? 0.6F : 0.75F);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_DEATH;
    }
}
