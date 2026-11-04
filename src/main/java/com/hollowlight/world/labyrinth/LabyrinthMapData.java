package com.hollowlight.world.labyrinth;

import com.hollowlight.story.StoryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Which cells of which layer a player has actually walked, and — the small mechanic that makes
 * the lens honest — which cells they are standing in right now.
 */
public final class LabyrinthMapData {

    private static final String[] LAYER_WORLDS = {
            "layer_1", "layer_2", "layer_3", "layer_4", "layer_5"
    };

    private LabyrinthMapData() {
    }

    public static RegistryKey<World> layerKey(int layer) {
        return RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD,
                Identifier.of(com.hollowlight.Hollowlight.MOD_ID, LAYER_WORLDS[Math.max(0, Math.min(4, layer - 1))]));
    }

    /** 0 = the overworld, 1..5 = the layers, -1 = some other dimension. */
    public static int currentLayer(ServerPlayerEntity player) {
        RegistryKey<World> key = player.getWorld().getRegistryKey();
        for (int index = 0; index < LAYER_WORLDS.length; index++) {
            if (key == layerKey(index + 1)) {
                return index + 1;
            }
        }
        return player.getWorld().getRegistryKey() == World.OVERWORLD ? 0 : -1;
    }

    public static void rememberCurrentCell(ServerPlayerEntity player) {
        int layer = currentLayer(player);
        if (layer < 1) {
            return;
        }
        BlockPos pos = player.getBlockPos();
        int cellX = Math.floorDiv(pos.getX(), 8);
        int cellZ = Math.floorDiv(pos.getZ(), 8);
        String cell = layer + ":" + cellX + ":" + cellZ;
        var story = StoryManager.story(player);
        if (!story.visitedCells().contains(cell)) {
            story.visitedCells().add(cell);
            // Reveal the neighbours as well: you can see the walls you walk past.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    story.visitedCells().add(layer + ":" + (cellX + dx) + ":" + (cellZ + dz));
                }
            }
        }
    }

    public static boolean hasVisited(ServerPlayerEntity player, int layer, int cellX, int cellZ) {
        return StoryManager.story(player).visitedCells().contains(layer + ":" + cellX + ":" + cellZ);
    }
}
