package com.hollowlight.world.labyrinth.gen;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.world.labyrinth.LabyrinthLayout;
import com.hollowlight.world.labyrinth.LabyrinthPalette;
import com.hollowlight.world.labyrinth.PlacedRoom;
import com.hollowlight.world.labyrinth.RoomKind;
import com.hollowlight.world.labyrinth.maze.Side;
import com.hollowlight.world.labyrinth.room.RoomFurnisher;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Turns a {@link LabyrinthLayout} into blocks, one chunk at a time.
 *
 * <p>The shell of every chunk is computed column by column from the maze graph, so the same
 * function produces the same walls no matter in what order (or on what thread) chunks are
 * generated. Rooms then stamp their furniture on top through a clipped canvas, and finally the
 * layout's functional content — lamps, chests, inscriptions, gates — is placed at the exact
 * positions the layout reserved for it.</p>
 */
public final class LabyrinthBuilder {

    private static final BlockState BEDROCK = Blocks.BEDROCK.getDefaultState();
    private static final BlockState AIR = Blocks.CAVE_AIR.getDefaultState();
    private static final BlockState WATER = Blocks.WATER.getDefaultState();
    private static final BlockState CHEST = Blocks.CHEST.getDefaultState();
    private static final int CORRIDOR_TOP = 3;

    private LabyrinthBuilder() {
    }

    /** Minimal write interface so the builder does not care whether it writes into a chunk or
     * a world: the chunk generator passes a structure-world accessor. */
    @FunctionalInterface
    public interface BlockSetter {
        void set(int x, int y, int z, BlockState state);
    }

    public static void build(BlockSetter setter, ChunkPos pos, LabyrinthLayout layout,
                             LabyrinthPalette palette) {
        Canvas canvas = new Canvas(setter, pos.getStartX(), pos.getStartZ(), palette, layout);
        int baseX = pos.getStartX();
        int baseZ = pos.getStartZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                buildColumn(canvas, layout, palette, baseX + localX, baseZ + localZ);
            }
        }

        for (PlacedRoom room : layout.rooms()) {
            if (room.maxBlockX() < baseX || room.minBlockX() > baseX + 15
                    || room.maxBlockZ() < baseZ || room.minBlockZ() > baseZ + 15) {
                continue;
            }
            RoomFurnisher.furnish(canvas, room);
        }

        placeFeatures(canvas, layout, baseX, baseZ);
    }

    private static void buildColumn(Canvas canvas, LabyrinthLayout layout, LabyrinthPalette palette,
                                    int worldX, int worldZ) {
        int cellX = Math.floorDiv(worldX, LabyrinthLayout.PITCH);
        int cellZ = Math.floorDiv(worldZ, LabyrinthLayout.PITCH);
        int localX = Math.floorMod(worldX, LabyrinthLayout.PITCH);
        int localZ = Math.floorMod(worldZ, LabyrinthLayout.PITCH);

        int floorY = layout.floorY();
        int top = floorY + 3;
        boolean xWall = localX == 0 || localX >= 6;
        boolean zWall = localZ == 0 || localZ >= 6;

        if (!xWall && !zWall) {
            buildInterior(canvas, layout, palette, worldX, worldZ, cellX, cellZ, floorY);
            return;
        }

        if (xWall && zWall) {
            int ceiling = Math.max(
                    Math.max(layout.ceiling(cellX, cellZ), layout.ceiling(cellX - 1, cellZ - 1)),
                    Math.max(layout.ceiling(cellX - 1, cellZ), layout.ceiling(cellX, cellZ - 1)));
            for (int y = 0; y <= floorY + ceiling + CORRIDOR_TOP; y++) {
                canvas.set(worldX, y, worldZ, y <= 1 ? BEDROCK : palette.wall());
            }
            return;
        }

        int neighbourX = cellX;
        int neighbourZ = cellZ;
        boolean open;
        boolean doorInRange;
        if (xWall) {
            boolean west = localX == 0;
            neighbourX = west ? cellX - 1 : cellX + 1;
            open = west
                    ? layout.grid().isOpen(cellX - 1, cellZ, Side.EAST)
                    : layout.grid().isOpen(cellX, cellZ, Side.EAST);
            doorInRange = localZ >= 2 && localZ <= 4;
        } else {
            boolean north = localZ == 0;
            neighbourZ = north ? cellZ - 1 : cellZ + 1;
            open = north
                    ? layout.grid().isOpen(cellX, cellZ - 1, Side.SOUTH)
                    : layout.grid().isOpen(cellX, cellZ, Side.SOUTH);
            doorInRange = localX >= 2 && localX <= 4;
        }

        int ceiling = Math.max(layout.ceiling(cellX, cellZ), layout.ceiling(neighbourX, neighbourZ));
        boolean doorway = open && doorInRange;

        if (doorway) {
            // A doorway: floor underneath, four blocks of clear passage, lintel above.
            for (int y = 0; y <= floorY + ceiling + CORRIDOR_TOP; y++) {
                if (y <= 1) {
                    canvas.set(worldX, y, worldZ, BEDROCK);
                } else if (y == floorY - 1) {
                    canvas.set(worldX, y, worldZ, palette.floor());
                } else if (y >= floorY && y <= top) {
                    canvas.set(worldX, y, worldZ, AIR);
                } else {
                    canvas.set(worldX, y, worldZ, palette.wall());
                }
            }
            // A carved lintel stone above every door: the architecture announces its own doors.
            canvas.set(worldX, top + 1, worldZ, palette.accent());
            return;
        }

        // Solid wall band between two cells.
        for (int y = 0; y <= floorY + ceiling + CORRIDOR_TOP; y++) {
            canvas.set(worldX, y, worldZ, y <= 1 ? BEDROCK : palette.wall());
        }
        int hash = LabyrinthLayout.hash(worldX, worldZ, layout.layer() * 29);
        if (Math.floorMod(hash, 19) == 0) {
            canvas.set(worldX, floorY + 2, worldZ, palette.accent());
        } else if (Math.floorMod(hash >> 6, 71) == 0) {
            canvas.set(worldX, floorY + 3, worldZ, palette.rib());
        }
    }

    private static void buildInterior(Canvas canvas, LabyrinthLayout layout, LabyrinthPalette palette,
                                      int worldX, int worldZ, int cellX, int cellZ, int floorY) {
        int ceiling = layout.ceiling(cellX, cellZ);
        boolean flooded = layout.flooded(cellX, cellZ);
        PlacedRoom room = layout.roomAt(cellX, cellZ);
        BlockState floor = palette.floor();
        BlockState ceilingState = palette.ceiling();
        if (room != null && (room.kind() == RoomKind.SAFE_ROOM || room.kind() == RoomKind.FINAL_CHAMBER)) {
            // Warm, fitted stonework: the only rooms in the labyrinth that were made for people.
            floor = palette.rib();
            ceilingState = palette.wall();
        }
        int highest = floorY + ceiling + CORRIDOR_TOP;
        for (int y = 0; y <= highest; y++) {
            if (y <= 1) {
                canvas.set(worldX, y, worldZ, BEDROCK);
            } else if (y < floorY - 1) {
                canvas.set(worldX, y, worldZ, palette.deep());
            } else if (y == floorY - 1) {
                canvas.set(worldX, y, worldZ, floor);
            } else if (y < floorY + ceiling) {
                canvas.set(worldX, y, worldZ, flooded && y == floorY ? WATER : AIR);
            } else {
                canvas.set(worldX, y, worldZ, y < floorY + ceiling + CORRIDOR_TOP ? ceilingState : AIR);
            }
        }

        int hash = LabyrinthLayout.hash(worldX, worldZ, layout.layer() * 13);
        if (Math.floorMod(hash, 41) == 0) {
            canvas.set(worldX, floorY - 1, worldZ, palette.accent());
        }
        if (Math.floorMod(hash >> 7, 67) == 0) {
            canvas.set(worldX, floorY + ceiling - 1, worldZ, palette.rib());
        }
        // Occasional ceiling stalactite made of the layer's own corruption.
        if (Math.floorMod(hash >> 11, 97) == 0) {
            canvas.set(worldX, floorY + ceiling - 2, worldZ, palette.accent());
        }
    }

    private static void placeFeatures(Canvas canvas, LabyrinthLayout layout, int baseX, int baseZ) {
        int maxX = baseX + 15;
        int maxZ = baseZ + 15;
        for (Map.Entry<Long, String> entry : layout.features().entrySet()) {
            long key = entry.getKey();
            int x = LabyrinthLayout.keyX(key);
            int z = LabyrinthLayout.keyZ(key);
            if (x < baseX || x > maxX || z < baseZ || z > maxZ) {
                continue;
            }
            BlockState state = canvas.palette().feature(entry.getValue());
            if (state != null) {
                canvas.set(x, LabyrinthLayout.keyY(key), z, state);
            }
        }
        for (Map.Entry<Long, Integer> entry : layout.gates().entrySet()) {
            long key = entry.getKey();
            int x = LabyrinthLayout.keyX(key);
            int z = LabyrinthLayout.keyZ(key);
            if (x < baseX || x > maxX || z < baseZ || z > maxZ) {
                continue;
            }
            canvas.set(x, LabyrinthLayout.keyY(key), z, canvas.palette().gate(entry.getValue()));
        }
        for (Long key : layout.containers().keySet()) {
            int x = LabyrinthLayout.keyX(key);
            int z = LabyrinthLayout.keyZ(key);
            if (x < baseX || x > maxX || z < baseZ || z > maxZ) {
                continue;
            }
            canvas.set(x, LabyrinthLayout.keyY(key), z, CHEST);
        }
        for (Long key : layout.inscriptions().keySet()) {
            int x = LabyrinthLayout.keyX(key);
            int z = LabyrinthLayout.keyZ(key);
            if (x < baseX || x > maxX || z < baseZ || z > maxZ) {
                continue;
            }
            canvas.set(x, LabyrinthLayout.keyY(key), z, ModBlocks.INSCRIPTION_STONE.getDefaultState());
        }
    }

    /** Chunk-scoped canvas that drops writes outside the chunk it is filling. */
    public static final class Canvas implements BlockCanvas {

        private final BlockSetter setter;
        private final int baseX;
        private final int baseZ;
        private final LabyrinthPalette palette;
        private final LabyrinthLayout layout;
        private final int floorY;
        private final java.util.Map<Long, BlockState> written = new java.util.HashMap<>();

        Canvas(BlockSetter setter, int baseX, int baseZ, LabyrinthPalette palette, LabyrinthLayout layout) {
            this.setter = setter;
            this.baseX = baseX;
            this.baseZ = baseZ;
            this.palette = palette;
            this.layout = layout;
            this.floorY = layout.floorY();
        }

        @Override
        public void set(int x, int y, int z, BlockState state) {
            if (x < baseX || x > baseX + 15 || z < baseZ || z > baseZ + 15 || y < 0 || y > 127) {
                return;
            }
            written.put(LabyrinthLayout.key(x, y, z), state);
            setter.set(x, y, z, state);
        }

        public BlockState get(int x, int y, int z) {
            if (x < baseX || x > baseX + 15 || z < baseZ || z > baseZ + 15 || y < 0 || y > 127) {
                return AIR;
            }
            BlockState tracked = written.get(LabyrinthLayout.key(x, y, z));
            return tracked == null ? AIR : tracked;
        }

        public boolean isAirAt(int x, int y, int z) {
            return get(x, y, z).isAir();
        }

        @Override
        public LabyrinthPalette palette() {
            return palette;
        }

        @Override
        public LabyrinthLayout layout() {
            return layout;
        }

        @Override
        public int floorY() {
            return floorY;
        }
    }
}
