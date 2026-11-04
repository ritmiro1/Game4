package com.hollowlight.world.labyrinth.gen;

import com.hollowlight.world.labyrinth.LabyrinthLayout;
import com.hollowlight.world.labyrinth.LabyrinthPalette;
import net.minecraft.block.BlockState;

/**
 * A write-only window onto the chunk currently being generated. All coordinates are absolute
 * world coordinates; writes outside the chunk are silently dropped. Room builders are pure
 * functions of their {@link com.hollowlight.world.labyrinth.PlacedRoom}, so it does not matter
 * that a room spanning four chunks is invoked once per chunk with different clipping.
 */
public interface BlockCanvas {

    void set(int x, int y, int z, BlockState state);

    default void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    /** Hollow box: walls, floor and ceiling but open inside. */
    default void shell(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    if (edge) {
                        set(x, y, z, state);
                    }
                }
            }
        }
    }

    LabyrinthPalette palette();

    LabyrinthLayout layout();

    int floorY();
}
