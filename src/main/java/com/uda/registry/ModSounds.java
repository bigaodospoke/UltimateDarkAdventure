package com.uda.registry;

import com.uda.UltimateDarkAdventure;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sons do mod. Os arquivos de áudio são do próprio Minecraft, combinados e reafinados em
 * assets/uda/sounds.json (o mod não traz áudio próprio).
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, UltimateDarkAdventure.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> EYE_OPEN = register("eye.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_PLACE = register("altar.place");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_RITUAL = register("altar.ritual");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_HEARTBEAT = register("altar.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALTAR_AWAKEN = register("altar.awaken");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUMMON_START = register("summon.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUMMON_COMPLETE = register("summon.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOSS_ENRAGE = register("boss.enrage");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SLASH = register("weapon.slash");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SCAN = register("weapon.scan");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_BARRIER = register("weapon.barrier");
    public static final DeferredHolder<SoundEvent, SoundEvent> AGAMOTO_FREEZE = register("agamoto.freeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> AGAMOTO_REWIND = register("agamoto.rewind");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(UltimateDarkAdventure.id(name)));
    }

    private ModSounds() {
    }
}
