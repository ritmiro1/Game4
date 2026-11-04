package com.hollowlight;

import com.hollowlight.config.ModConfig;
import com.hollowlight.horror.HorrorDirector;
import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModEntities;
import com.hollowlight.registry.ModItemGroups;
import com.hollowlight.registry.ModItems;
import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.registry.ModScreenHandlers;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.registry.ModWorldgen;
import com.hollowlight.sanity.AmbienceDirector;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.labyrinth.LabyrinthManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of <em>Hollowlight — The Lamplighter's Descent</em>.
 *
 * <p>The mod is organised around one idea: a labyrinth that is also a memory. Everything
 * below the surface is generated from a single seed by
 * {@link com.hollowlight.world.labyrinth.gen.LabyrinthChunkGenerator}, and the story is
 * delivered through discoverable lore fragments tracked by {@link StoryManager}.</p>
 *
 * <p>Registration order matters: sounds are registered first (blocks and items play them),
 * then blocks, items, entities and the gameplay systems.</p>
 */
public final class Hollowlight implements ModInitializer {

    public static final String MOD_ID = "hollowlight";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Shorthand for a namespaced identifier in this mod. */
    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Hollowlight is waking up. Someone left a lamp burning.");

        ModConfig.get().load();

        ModWorldgen.bootstrap();
        ModSounds.bootstrap();
        ModParticles.bootstrap();
        ModBlocks.bootstrap();
        ModItems.bootstrap();
        ModEntities.bootstrap();
        ModScreenHandlers.bootstrap();
        ModItemGroups.bootstrap();
        ModPayloads.registerServerbound();

        LabyrinthManager.bootstrap();
        SanityManager.bootstrap();
        AmbienceDirector.bootstrap();
        StoryManager.bootstrap();
        HorrorDirector.bootstrap();

        LOGGER.info("Hollowlight finished loading {} blocks, {} items and {} entities.",
                ModBlocks.all().size(), ModItems.all().size(), ModEntities.all().size());
    }
}
