package com.hollowlight.world.labyrinth;

import com.hollowlight.Hollowlight;
import com.hollowlight.config.ModConfig;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Cache and query layer for {@link LabyrinthLayout}s.
 *
 * <p>Chunk generation runs on worker threads, so layouts are cached in a concurrent map keyed by
 * (layer, seed) and built exactly once per world session. Every chunk of a layer therefore sees
 * the byte-identical layout, which is what makes a maze stable while the player walks it.</p>
 */
public final class LabyrinthLayouts {

    private static final String[] LAYER_NAMES = {"layer_1", "layer_2", "layer_3", "layer_4", "layer_5"};
    private static final Map<Long, LabyrinthLayout> CACHE = new ConcurrentHashMap<>();

    private LabyrinthLayouts() {
    }

    public static RegistryKey<World> layerKey(int layer) {
        return LabyrinthMapData.layerKey(layer);
    }

    /** 1..5 for the labyrinth layers, 0 for the overworld, -1 for anything else. */
    public static int layerOfWorld(World world) {
        RegistryKey<World> key = world.getRegistryKey();
        for (int index = 0; index < LAYER_NAMES.length; index++) {
            if (key == RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,
                    Identifier.of(Hollowlight.MOD_ID, LAYER_NAMES[index]))) {
                return index + 1;
            }
        }
        return key == World.OVERWORLD ? 0 : -1;
    }

    public static LabyrinthLayout layout(int layer, long seed) {
        ModConfig config = ModConfig.get();
        long cacheKey = (seed * 31L + layer) * 1_000_003L + config.labyrinthWidth;
        return CACHE.computeIfAbsent(cacheKey, ignored -> LabyrinthLayout.create(layer, seed,
                config.labyrinthWidth, config.labyrinthHeight, config.braidChance,
                config.labyrinthFloorY, config.ceilingScale));
    }

    public static LabyrinthLayout layout(World world) {
        int layer = layerOfWorld(world);
        if (layer < 1) {
            return null;
        }
        return layout(layer, world.getSeed());
    }

    /** Fog-of-war friendly landing point: the middle of the landing room, one block above floor. */
    public static BlockPos landingPos(World world, int layer) {
        LabyrinthLayout layout = layout(layer, world.getSeed());
        return new BlockPos(layout.landingCellX() * LabyrinthLayout.PITCH + 3,
                layout.floorY(), layout.landingCellZ() * LabyrinthLayout.PITCH + 3);
    }

    /** The lore carved on a wall block, or null if this block is not an inscription. */
    public static String inscriptionAt(World world, BlockPos pos) {
        LabyrinthLayout layout = layout(world);
        if (layout == null) {
            return null;
        }
        return layout.inscriptionAt(LabyrinthLayout.key(pos.getX(), pos.getY(), pos.getZ()));
    }

    public static BlockPos shrineExit(ServerWorld overworld) {
        return com.hollowlight.world.structure.EntranceBuilder.exitPoint(overworld);
    }

    /** Story bookkeeping for crossing a threshold. */
    public static void markGateUsed(ServerPlayerEntity player, int link) {
        if (link >= 1) {
            StoryManager.grant(player, "entered_layer_" + link);
        } else {
            StoryManager.grant(player, StoryFlags.ENTERED_SHRINE);
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
