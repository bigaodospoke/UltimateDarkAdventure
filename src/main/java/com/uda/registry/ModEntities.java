package com.uda.registry;

import com.uda.UltimateDarkAdventure;
import com.uda.entity.AbyssalDragon;
import com.uda.entity.BlackEye;
import com.uda.entity.BlindExecutioner;
import com.uda.entity.EyeArrow;
import com.uda.entity.EnergySlash;
import com.uda.entity.EyeBomb;
import com.uda.entity.EyeOrb;
import com.uda.entity.HollowWatcher;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, UltimateDarkAdventure.MODID);

    // Esqueleto alto como o Wither Skeleton (o modelo é renderizado em escala 1.2).
    public static final DeferredHolder<EntityType<?>, EntityType<HollowWatcher>> HOLLOW_WATCHER =
            ENTITY_TYPES.register("hollow_watcher", () -> EntityType.Builder.of(HollowWatcher::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F)
                    .eyeHeight(2.1F)
                    .ridingOffset(-0.875F)
                    .clientTrackingRange(8)
                    .build("hollow_watcher"));

    // Illager de cabeça alta; a forma ascendida cresce via atributo de escala.
    public static final DeferredHolder<EntityType<?>, EntityType<BlindExecutioner>> BLIND_EXECUTIONER =
            ENTITY_TYPES.register("blind_executioner", () -> EntityType.Builder.of(BlindExecutioner::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.1F)
                    .eyeHeight(1.8F)
                    .ridingOffset(-0.6F)
                    .clientTrackingRange(8)
                    .build("blind_executioner"));

    public static final DeferredHolder<EntityType<?>, EntityType<EyeArrow>> EYE_ARROW =
            ENTITY_TYPES.register("eye_arrow", () -> EntityType.Builder.<EyeArrow>of(EyeArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.13F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("eye_arrow"));

    public static final DeferredHolder<EntityType<?>, EntityType<EyeOrb>> EYE_ORB =
            ENTITY_TYPES.register("eye_orb", () -> EntityType.Builder.<EyeOrb>of(EyeOrb::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(4)
                    .updateInterval(2)
                    .build("eye_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<EyeBomb>> EYE_BOMB =
            ENTITY_TYPES.register("eye_bomb", () -> EntityType.Builder.<EyeBomb>of(EyeBomb::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .build("eye_bomb"));

    public static final DeferredHolder<EntityType<?>, EntityType<EnergySlash>> ENERGY_SLASH =
            ENTITY_TYPES.register("energy_slash", () -> EntityType.Builder.<EnergySlash>of(EnergySlash::new, MobCategory.MISC)
                    .sized(1.2F, 0.5F)
                    .clientTrackingRange(6)
                    .updateInterval(1)
                    .build("energy_slash"));

    // Bosses: invocados nos altares do Santuário Abissal e da Catedral do Vazio
    public static final DeferredHolder<EntityType<?>, EntityType<AbyssalDragon>> ABYSSAL_DRAGON =
            ENTITY_TYPES.register("abyssal_dragon", () -> EntityType.Builder.of(AbyssalDragon::new, MobCategory.MONSTER)
                    .sized(4.0F, 3.0F)
                    .eyeHeight(1.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("abyssal_dragon"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlackEye>> BLACK_EYE =
            ENTITY_TYPES.register("black_eye", () -> EntityType.Builder.of(BlackEye::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F)
                    .eyeHeight(1.5F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("black_eye"));

    private ModEntities() {
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOW_WATCHER.get(), HollowWatcher.createAttributes().build());
        event.put(BLIND_EXECUTIONER.get(), BlindExecutioner.createAttributes().build());
        event.put(ABYSSAL_DRAGON.get(), AbyssalDragon.createAttributes().build());
        event.put(BLACK_EYE.get(), BlackEye.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // Hollow Watcher: só nas cavernas profundas (abaixo de y = 0) quando nasce sozinho
        event.register(HOLLOW_WATCHER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> (spawnType != MobSpawnType.NATURAL || pos.getY() < 0)
                        && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(BLIND_EXECUTIONER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
