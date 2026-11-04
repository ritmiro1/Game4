package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.world.labyrinth.gen.LabyrinthChunkGenerator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Registers the labyrinth chunk generator.
 *
 * <p>The five dimensions themselves, their dimension types and their biomes are pure data
 * ({@code data/hollowlight/dimension/...}). Only the generator needs code, because it writes
 * the maze out of a {@link com.hollowlight.world.labyrinth.LabyrinthLayout} instead of
 * sampling noise.</p>
 */
public final class ModWorldgen {

    private ModWorldgen() {
    }

    public static void bootstrap() {
        Registry.register(Registries.CHUNK_GENERATOR, Hollowlight.id("labyrinth"),
                LabyrinthChunkGenerator.CODEC);
        Hollowlight.LOGGER.debug("Registered labyrinth chunk generator");
    }
}
