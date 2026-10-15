package com.nexussmp.nexuscore.core.registry;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Os sons do mod.
 *
 * <p>Os arquivos das backrooms vem do SP-Backrooms Revamped (LGPL-3.0); credito e licenca estao em
 * {@code assets/nexuscore/sounds/backrooms/}. Quais arquivos cada evento toca esta no
 * {@code sounds.json} — trocar um som nao mexe em codigo.
 */
public final class NexusSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, NexusCore.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_AMBIENCE = register("backrooms.skinwalker.ambience");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_STEP = register("backrooms.skinwalker.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_SNIFF = register("backrooms.skinwalker.sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_NOTICE = register("backrooms.skinwalker.notice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_CHASE = register("backrooms.skinwalker.chase");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_REVEAL = register("backrooms.skinwalker.reveal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_BONE_CRACK = register("backrooms.skinwalker.bone_crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_JUMPSCARE = register("backrooms.skinwalker.jumpscare");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKINWALKER_RELEASE = register("backrooms.skinwalker.release");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_BLINK = register("backrooms.light.blink");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_OUT = register("backrooms.light.out");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_ON = register("backrooms.light.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIGHT_HUM = register("backrooms.light.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLITCH = register("backrooms.glitch");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENCE = register("backrooms.ambience");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(NexusCore.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private NexusSounds() {}
}
