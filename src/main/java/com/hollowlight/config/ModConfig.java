package com.hollowlight.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.hollowlight.Hollowlight;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Configuration stored as {@code config/hollowlight.json}.
 *
 * <p>Every option here is meant to be safe to change mid-playthrough except the labyrinth
 * dimensions (layout is seeded from the world seed and the layer index, so changing sizes only
 * affects newly generated chunks — the mod warns about that explicitly).</p>
 */
public final class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;

    // ------------------------------------------------------------------- sanity ----------
    /** Master switch for the psychological system. Off means the world is still dark. */
    public boolean enableSanitySystem = true;
    /** Multiplier on all sanity loss (0.0 - 3.0). Default 1.0. */
    public float sanityDrainMultiplier = 1.0F;
    /** Multiplier on all sanity gain (0.0 - 3.0). Default 1.0. */
    public float sanityRecoveryMultiplier = 1.0F;
    /** Whether the HUD eye indicator is drawn. */
    public boolean showSanityHud = true;

    // ------------------------------------------------------------------- horror ----------
    /** Master switch for scripted horror events (apparitions, whispers, light theft). */
    public boolean enableHorrorEvents = true;
    /** Average delay between ambient horror events, in seconds. */
    public int horrorEventIntervalSeconds = 95;
    /** How many blocks away an apparition may appear. */
    public int apparitionDistance = 18;

    // --------------------------------------------------------------- labyrinth -----------
    /** Cells across of each labyrinth layer. Changing this mid-world only affects new chunks. */
    public int labyrinthWidth = 40;
    /** Cells deep of each labyrinth layer. */
    public int labyrinthHeight = 40;
    /** Base Y of the walkable floor inside each layer. */
    public int labyrinthFloorY = 44;
    /** Fraction of dead ends that are opened to create loops (0.0 - 1.0). */
    public float braidChance = 0.45F;
    /** Extra ceiling height multiplier for cathedral-scale rooms. */
    public float ceilingScale = 1.0F;
    /** Multiplier on hostile entity spawn attempts inside the labyrinth. */
    public float entitySpawnMultiplier = 1.0F;
    /** Whether the mod's own ambience tracks play on top of vanilla music. */
    public boolean enableMusic = true;
    /** Whether the room-tone beds (wind, calm rooms, the Lamplighter's theme) are scheduled. */
    public boolean enableAmbience = true;
    /** Multiplier on all mod sound volumes. */
    public float soundVolume = 1.0F;
    /** Whether safe rooms restore sanity passively. */
    public boolean safeRoomsRestoreSanity = true;

    public static ModConfig get() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hollowlight.json");
    }

    public void load() {
        Path path = file();
        if (!Files.exists(path)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                instance = loaded.sanitised();
            }
        } catch (IOException | JsonSyntaxException exception) {
            Hollowlight.LOGGER.error("Could not read hollowlight.json, using defaults", exception);
        }
    }

    public void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException exception) {
            Hollowlight.LOGGER.error("Could not write hollowlight.json", exception);
        }
    }

    private ModConfig sanitised() {
        sanityDrainMultiplier = clamp(sanityDrainMultiplier, 0.0F, 3.0F);
        sanityRecoveryMultiplier = clamp(sanityRecoveryMultiplier, 0.0F, 3.0F);
        braidChance = clamp(braidChance, 0.0F, 0.9F);
        ceilingScale = clamp(ceilingScale, 0.5F, 2.0F);
        entitySpawnMultiplier = clamp(entitySpawnMultiplier, 0.0F, 4.0F);
        soundVolume = clamp(soundVolume, 0.0F, 2.0F);
        horrorEventIntervalSeconds = Math.max(20, horrorEventIntervalSeconds);
        labyrinthWidth = Math.max(16, Math.min(96, labyrinthWidth));
        labyrinthHeight = Math.max(16, Math.min(96, labyrinthHeight));
        labyrinthFloorY = Math.max(12, Math.min(96, labyrinthFloorY));
        return this;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
