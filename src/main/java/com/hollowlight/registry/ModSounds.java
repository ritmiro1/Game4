package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;

/**
 * Every sound the mod plays. The list is intentionally long and quiet: ambience carries
 * the horror, stingers only punctuate it. All files live in
 * {@code assets/hollowlight/sounds/} and are declared in {@code sounds.json}.
 */
public final class ModSounds {

    private static final List<SoundEvent> REGISTERED = new ArrayList<>();

    // ---- layer ambience -------------------------------------------------------------------
    public static final SoundEvent AMBIENCE_HALLS = create("ambience.halls");
    public static final SoundEvent AMBIENCE_DROWNED = create("ambience.drowned");
    public static final SoundEvent AMBIENCE_LIVING = create("ambience.living");
    public static final SoundEvent AMBIENCE_MEMORIAL = create("ambience.memorial");
    public static final SoundEvent AMBIENCE_HEART = create("ambience.heart");
    public static final SoundEvent AMBIENCE_SAFE = create("ambience.safe");

    // ---- environmental ---------------------------------------------------------------------
    public static final SoundEvent DRIP = create("ambient.drip");
    public static final SoundEvent RUMBLE = create("ambient.rumble");
    public static final SoundEvent WIND_CORRIDOR = create("ambient.wind");
    public static final SoundEvent STONE_SETTLE = create("ambient.stone_settle");
    public static final SoundEvent WHISPER = create("ambient.whisper");
    public static final SoundEvent WHISPER_CLOSE = create("ambient.whisper_close");
    public static final SoundEvent BREATH = create("ambient.breath");
    public static final SoundEvent HEARTBEAT = create("ambient.heartbeat");
    public static final SoundEvent DISTANT_BELL = create("ambient.bell");

    // ---- entities --------------------------------------------------------------------------
    public static final SoundEvent HOLLOWED_IDLE = create("entity.hollowed.idle");
    public static final SoundEvent HOLLOWED_HURT = create("entity.hollowed.hurt");
    public static final SoundEvent HOLLOWED_DEATH = create("entity.hollowed.death");
    public static final SoundEvent HOLLOWED_STEP = create("entity.hollowed.step");
    public static final SoundEvent PAPERFACE_WATCH = create("entity.paperface.watch");
    public static final SoundEvent PAPERFACE_ATTACK = create("entity.paperface.attack");
    public static final SoundEvent STITCHER_AMBIENT = create("entity.stitcher.ambient");
    public static final SoundEvent STITCHER_ATTACK = create("entity.stitcher.attack");
    public static final SoundEvent WEEPING_CRY = create("entity.weeping.cry");
    public static final SoundEvent WEEPING_SOFT = create("entity.weeping.soft");
    public static final SoundEvent CHOIR_SING = create("entity.choir.sing");
    public static final SoundEvent MOTH_FLUTTER = create("entity.moth.flutter");
    public static final SoundEvent ECHO_VOICE = create("entity.echo.voice");
    public static final SoundEvent LAMPKEEPER_ROAR = create("entity.lampkeeper.roar");
    public static final SoundEvent LAMPKEEPER_PHASE = create("entity.lampkeeper.phase");
    public static final SoundEvent LAMPKEEPER_DEATH = create("entity.lampkeeper.death");

    // ---- interaction -----------------------------------------------------------------------
    public static final SoundEvent GATE_OPEN = create("block.gate_open");
    public static final SoundEvent GATE_LOCKED = create("block.gate_locked");
    public static final SoundEvent ALTAR_ACTIVATE = create("block.altar_activate");
    public static final SoundEvent PAGE_TURN = create("item.page_turn");
    public static final SoundEvent KEY_INSERT = create("item.key_insert");
    public static final SoundEvent LANTERN_LIGHT = create("item.lantern_light");
    public static final SoundEvent LANTERN_EMPTY = create("item.lantern_empty");
    public static final SoundEvent LORE_DISCOVER = create("item.lore_discover");
    public static final SoundEvent TRAP_TRIGGER = create("block.trap_trigger");
    public static final SoundEvent MEMORY_RETURN = create("item.memory_return");
    public static final SoundEvent STINGER_SOFT = create("horror.stinger_soft");
    public static final SoundEvent STINGER_DEEP = create("horror.stinger_deep");

    // ---- music -----------------------------------------------------------------------------
    public static final SoundEvent MUSIC_HALLS = create("music.halls");
    public static final SoundEvent MUSIC_DROWNED = create("music.drowned");
    public static final SoundEvent MUSIC_LIVING = create("music.living");
    public static final SoundEvent MUSIC_MEMORIAL = create("music.memorial");
    public static final SoundEvent MUSIC_HEART = create("music.heart");
    public static final SoundEvent MUSIC_BOSS = create("music.boss");
    public static final SoundEvent MUSIC_ENDING = create("music.ending");

    private ModSounds() {
    }

    private static SoundEvent create(String path) {
        SoundEvent event = SoundEvent.of(Hollowlight.id(path));
        REGISTERED.add(event);
        return event;
    }

    /** Registers every event created above. Called once from the mod initialiser. */
    public static void bootstrap() {
        for (SoundEvent event : REGISTERED) {
            Registry.register(Registries.SOUND_EVENT, event.id(), event);
        }
        Hollowlight.LOGGER.debug("Registered {} sound events", REGISTERED.size());
    }
}
