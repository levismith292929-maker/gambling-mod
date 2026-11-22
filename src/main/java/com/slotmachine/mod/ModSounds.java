package com.slotmachine.mod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent SPIN_START = register("spin_start");
    public static final SoundEvent REEL_TICK = register("reel_tick");
    public static final SoundEvent REEL_STOP = register("reel_stop");
    public static final SoundEvent BIRDLY1 = register("birdly1");
    public static final SoundEvent BIRDLY2 = register("birdly2");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void registerSounds() {
        SlotMachineMod.LOGGER.info("Registering sounds for " + SlotMachineMod.MOD_ID);
    }
}
