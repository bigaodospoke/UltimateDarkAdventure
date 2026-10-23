package com.uda.entity;

import javax.annotation.Nullable;

import com.uda.UltimateDarkAdventure;
import com.uda.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Hollow Watcher — esqueleto negro com um núcleo ocular no peito, das cavernas profundas.
 * Ao perder metade da vida o olho se abre: atira mais rápido e mais preciso, e suas flechas viram
 * Flechas Oculares (energizadas), que perseguem o alvo e o marcam.
 * <p>
 * Vigia Ancestral: 1 em cada 10 que nascem sozinhos é maior e mais forte e sempre deixa o Olho do Vigia,
 * que libera o Altar do Olho (os primeiros despertares).
 */
public class HollowWatcher extends AbstractEyeMonster implements RangedAttackMob {
    private static final ResourceLocation AWAKENED_SIGHT = UltimateDarkAdventure.id("hollow_watcher_awakened_sight");
    private static final int DORMANT_ATTACK_INTERVAL = 40;
    private static final int AWAKENED_ATTACK_INTERVAL = 20;
    private static final EntityDataAccessor<Boolean> DATA_ANCIENT = SynchedEntityData.defineId(HollowWatcher.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation ANCIENT_HEALTH = UltimateDarkAdventure.id("ancient_watcher_health");
    private static final ResourceLocation ANCIENT_SCALE = UltimateDarkAdventure.id("ancient_watcher_scale");
    private static final ResourceLocation ANCIENT_ARMOR = UltimateDarkAdventure.id("ancient_watcher_armor");
    private static final float ANCIENT_CHANCE = 0.1F;

    private RangedBowAttackGoal<HollowWatcher> bowGoal;

    public HollowWatcher(EntityType<? extends HollowWatcher> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        // Chamado pelo construtor de Mob, antes dos campos desta classe serem inicializados.
        this.bowGoal = new RangedBowAttackGoal<>(this, 1.0, DORMANT_ATTACK_INTERVAL, 16.0F);
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(4, this.bowGoal);
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            public boolean canUse() {
                return !HollowWatcher.this.isHoldingBow() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !HollowWatcher.this.isHoldingBow() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    private boolean isHoldingBow() {
        return this.isHolding(stack -> stack.getItem() instanceof BowItem);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ANCIENT, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isAwakened() && this.isAlive() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.setAwakened(true);
        }
        return hurt;
    }

    // ------------------------------------------------------------------ Vigia Ancestral

    public boolean isAncient() {
        return this.entityData.get(DATA_ANCIENT);
    }

    public void setAncient(boolean ancient) {
        this.entityData.set(DATA_ANCIENT, ancient);
        this.setModifier(Attributes.MAX_HEALTH, ANCIENT_HEALTH, ancient, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.setModifier(Attributes.SCALE, ANCIENT_SCALE, ancient, 0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        this.setModifier(Attributes.ARMOR, ANCIENT_ARMOR, ancient, 4.0, AttributeModifier.Operation.ADD_VALUE);
        if (ancient) {
            this.setPersistenceRequired();
        }
    }

    private void setModifier(Holder<Attribute> attribute, ResourceLocation id, boolean active, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            if (active) {
                instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
            }
        }
    }

    @Override
    protected Component getTypeName() {
        return this.isAncient() ? Component.translatable("entity.uda.ancient_watcher") : super.getTypeName();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.isAncient()) {
            this.spawnAtLocation(ModItems.WATCHER_EYE.get());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Ancient", this.isAncient());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Ancient")) {
            this.entityData.set(DATA_ANCIENT, true);
        }
    }

    @Override
    protected void applyAwakenedModifiers(boolean awakened) {
        if (this.bowGoal != null) {
            this.bowGoal.setMinAttackInterval(awakened ? AWAKENED_ATTACK_INTERVAL : DORMANT_ATTACK_INTERVAL);
        }
        AttributeInstance followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) {
            followRange.removeModifier(AWAKENED_SIGHT);
            if (awakened) {
                followRange.addTransientModifier(new AttributeModifier(AWAKENED_SIGHT, 12.0, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    @Override
    protected void onAwaken(ServerLevel level) {
        super.onAwaken(level);
        this.playSound(SoundEvents.ENDER_EYE_DEATH, 1.2F, 0.5F);
        this.playSound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.8F, 0.7F);
    }

    @Override
    protected void onFallAsleep(ServerLevel level) {
        this.playSound(SoundEvents.ENDER_EYE_LAUNCH, 0.6F, 0.5F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack ammo = new ItemStack(Items.ARROW);
        AbstractArrow arrow;
        if (this.isAwakened()) {
            EyeArrow eyeArrow = new EyeArrow(this.level(), this, ammo, weapon, target);
            eyeArrow.setBaseDamageFromMob(velocity);
            eyeArrow.setBaseDamage(eyeArrow.getBaseDamage() + 1.0);
            arrow = eyeArrow;
        } else {
            arrow = ProjectileUtil.getMobArrow(this, ammo, velocity, weapon);
        }
        if (weapon.getItem() instanceof BowItem bow) {
            arrow = bow.customArrow(arrow, ammo, weapon);
        }
        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0 / 3.0) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // O olho aberto enxerga tudo: muito mais preciso.
        float inaccuracy = this.isAwakened() ? 2.0F : 14 - this.level().getDifficulty().getId() * 4;
        arrow.shoot(dx, dy + horizontal * 0.2F, dz, 1.6F, inaccuracy);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EYE_BOW.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData) {
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        if (spawnType == MobSpawnType.NATURAL && level.getRandom().nextFloat() < ANCIENT_CHANCE) {
            this.setAncient(true);
            this.setHealth(this.getMaxHealth());
        }
        return spawnGroupData;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15F, 1.0F);
    }
}
