package com.hollowlight.world.labyrinth;

import com.hollowlight.world.labyrinth.maze.MazeGrid;
import com.hollowlight.world.labyrinth.maze.Side;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A complete, immutable description of one labyrinth layer: the maze, where every handcrafted
 * room sits, how tall each ceiling is, what is flooded, where the lamps, chests, inscriptions,
 * gates and spawners belong.
 *
 * <p>This class contains <strong>no Minecraft code</strong>. It is a pure function of
 * (layer, world seed), which means chunk generation is deterministic no matter which thread
 * asks for which chunk, and means the layout can be tested on its own. Exactly that test lives
 * in {@code tools/labyrinth_check.py}, which re-implements this algorithm and asserts
 * connectivity, room separation and reachability for many seeds.</p>
 */
public final class LabyrinthLayout {

    /** Room footprint in cells (a room is 3x3 cells with one cell of breathing space). */
    public static final int ROOM_CELLS = 3;
    /** Cell pitch in blocks, mirrored from {@link MazeGrid}. */
    public static final int PITCH = MazeGrid.PITCH;
    /** The tallest ceiling any room kind asks for, before the config's ceiling scale. */
    public static final int MAX_CEILING = 22;
    /** Hard cap on generated ceiling height, so height queries and the world limit agree. */
    public static final int CEILING_CAP = 40;

    private final int layer;
    private final long seed;
    private final MazeGrid grid;
    private final int floorY;
    private final int[] ceilings;
    private final byte[] roomIndices;
    private final boolean[] flooded;
    private final List<PlacedRoom> rooms;
    private final int landingCellX;
    private final int landingCellZ;

    private final Map<Long, String> containers = new HashMap<>();
    private final Map<Long, String> inscriptions = new HashMap<>();
    private final Map<Long, Integer> gates = new HashMap<>();
    private final Map<Long, String> features = new HashMap<>();
    private final Map<Long, String> spawners = new HashMap<>();

    private LabyrinthLayout(int layer, long seed, MazeGrid grid, int floorY, int[] ceilings,
                            byte[] roomIndices, boolean[] flooded, List<PlacedRoom> rooms,
                            int landingCellX, int landingCellZ) {
        this.layer = layer;
        this.seed = seed;
        this.grid = grid;
        this.floorY = floorY;
        this.ceilings = ceilings;
        this.roomIndices = roomIndices;
        this.flooded = flooded;
        this.rooms = List.copyOf(rooms);
        this.landingCellX = landingCellX;
        this.landingCellZ = landingCellZ;
    }

    // ------------------------------------------------------------------------ creation ---

    public static LabyrinthLayout create(int layer, long worldSeed, int width, int height,
                                         double braidChance, int floorY, float ceilingScale) {
        long layoutSeed = worldSeed * 31L + layer * 0x9E3779B9L;
        Random random = new Random(layoutSeed);
        MazeGrid grid = new MazeGrid(width, height, layoutSeed, braidChance);

        int cells = width * height;
        int[] ceilings = new int[cells];
        byte[] roomIndices = new byte[cells];
        java.util.Arrays.fill(roomIndices, (byte) -1);

        List<RoomKind> plan = RoomKind.planFor(layer);
        List<PlacedRoom> rooms = new ArrayList<>();
        boolean[] occupied = new boolean[cells];
        List<int[]> slots = new ArrayList<>();
        for (int x = 1; x + ROOM_CELLS <= width - 1; x += ROOM_CELLS + 1) {
            for (int z = 1; z + ROOM_CELLS <= height - 1; z += ROOM_CELLS + 1) {
                slots.add(new int[]{x, z});
            }
        }

        int centerX = width / 2;
        int centerZ = height / 2;

        // 1. The landing is the free slot closest to the middle of the layer: the player should
        //    never spawn in a corner of a hundred-block maze.
        int[] landing = null;
        double best = Double.MAX_VALUE;
        for (int[] slot : slots) {
            double distance = Math.hypot(slot[0] + 1 - centerX, slot[1] + 1 - centerZ);
            if (distance < best && fits(slot, occupied, width, height) && plan.contains(RoomKind.LANDING)) {
                best = distance;
                landing = slot;
            }
        }
        if (landing == null) {
            landing = slots.get(random.nextInt(slots.size()));
        }
        rooms.add(place(RoomKind.LANDING, landing, rooms.size(), occupied, width, height, roomIndices, random));
        reserve(landing, occupied, width, height);

        // 2. The way down is the free slot furthest from the landing.
        int[] descent = farthestSlot(slots, landing, occupied, width, height);
        if (descent != null && plan.contains(RoomKind.DESCENT)) {
            rooms.add(place(RoomKind.DESCENT, descent, rooms.size(), occupied, width, height, roomIndices, random));
            reserve(descent, occupied, width, height);
        }

        // 3. Layer V uses an arena instead of a descent; it goes as far away as possible too.
        if (plan.contains(RoomKind.BOSS_ARENA)) {
            int[] arena = farthestSlot(slots, landing, occupied, width, height);
            if (arena != null) {
                rooms.add(place(RoomKind.BOSS_ARENA, arena, rooms.size(), occupied, width, height, roomIndices, random));
                reserve(arena, occupied, width, height);
            }
        }

        // 4. Everything else, shuffled but deterministic.
        List<int[]> candidates = new ArrayList<>(slots);
        java.util.Collections.shuffle(candidates, random);
        for (RoomKind kind : plan) {
            if (kind == RoomKind.LANDING || kind == RoomKind.DESCENT || kind == RoomKind.BOSS_ARENA) {
                continue;
            }
            for (int[] slot : candidates) {
                if (!fits(slot, occupied, width, height)) {
                    continue;
                }
                rooms.add(place(kind, slot, rooms.size(), occupied, width, height, roomIndices, random));
                reserve(slot, occupied, width, height);
                break;
            }
        }

        // 5. Open the rooms into the maze: all internal walls first, then one door per side.
        for (PlacedRoom room : rooms) {
            for (int dx = 0; dx < ROOM_CELLS; dx++) {
                for (int dz = 0; dz < ROOM_CELLS; dz++) {
                    if (dx < ROOM_CELLS - 1) {
                        grid.forceOpen(room.cellX() + dx, room.cellZ() + dz, Side.EAST);
                    }
                    if (dz < ROOM_CELLS - 1) {
                        grid.forceOpen(room.cellX() + dx, room.cellZ() + dz, Side.SOUTH);
                    }
                }
            }
            grid.forceOpen(room.cellX() + 1, room.cellZ(), Side.NORTH);
            grid.forceOpen(room.cellX() + 1, room.cellZ() + ROOM_CELLS - 1, Side.SOUTH);
            grid.forceOpen(room.cellX(), room.cellZ() + 1, Side.WEST);
            grid.forceOpen(room.cellX() + ROOM_CELLS - 1, room.cellZ() + 1, Side.EAST);
        }

        // 6. Ceilings and flooding.
        boolean[] flooded = new boolean[cells];
        for (int cx = 0; cx < width; cx++) {
            for (int cz = 0; cz < height; cz++) {
                int index = cz * width + cx;
                byte roomIndex = roomIndices[index];
                int hash = hash(cx, cz, layer);
                int ceiling;
                if (roomIndex >= 0) {
                    RoomKind kind = rooms.get(roomIndex).kind();
                    ceiling = Math.round(kind.ceilingHeight() * ceilingScale);
                } else {
                    ceiling = 4 + Math.floorMod(hash, 5);
                    // Occasional tall corridor: a place where the ceiling goes up and out of
                    // the lamplight, which is where the horror lives.
                    if (Math.floorMod(hash >> 8, 23) == 0) {
                        ceiling += 4;
                    }
                }
                ceilings[index] = Math.min(CEILING_CAP, Math.max(3, ceiling));
                if (layer == 2) {
                    boolean calmRoom = roomIndex >= 0 && rooms.get(roomIndex).kind().calm();
                    flooded[index] = !calmRoom && Math.floorMod(hash >> 4, 100) < 58;
                }
            }
        }

        LabyrinthLayout layout = new LabyrinthLayout(layer, layoutSeed, grid, floorY, ceilings,
                roomIndices, flooded, rooms, landing[0] + 1, landing[1] + 1);
        layout.populateContent(new Random(layoutSeed ^ 0xC0FFEEL));
        return layout;
    }

    private static int[] farthestSlot(List<int[]> slots, int[] origin, boolean[] occupied, int width, int height) {
        int[] best = null;
        double bestDistance = -1.0;
        for (int[] slot : slots) {
            if (!fits(slot, occupied, width, height)) {
                continue;
            }
            double distance = Math.hypot(slot[0] - origin[0], slot[1] - origin[1]);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = slot;
            }
        }
        return best;
    }

    private static boolean fits(int[] slot, boolean[] occupied, int width, int height) {
        for (int dx = -1; dx <= ROOM_CELLS; dx++) {
            for (int dz = -1; dz <= ROOM_CELLS; dz++) {
                int x = slot[0] + dx;
                int z = slot[1] + dz;
                if (x < 0 || z < 0 || x >= width || z >= height) {
                    continue;
                }
                if (occupied[z * width + x]) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void reserve(int[] slot, boolean[] occupied, int width, int height) {
        for (int dx = -1; dx <= ROOM_CELLS; dx++) {
            for (int dz = -1; dz <= ROOM_CELLS; dz++) {
                int x = slot[0] + dx;
                int z = slot[1] + dz;
                if (x < 0 || z < 0 || x >= width || z >= height) {
                    continue;
                }
                occupied[z * width + x] = true;
            }
        }
    }

    private static PlacedRoom place(RoomKind kind, int[] slot, int index, boolean[] occupied,
                                    int width, int height, byte[] roomIndices, Random random) {
        for (int dx = 0; dx < ROOM_CELLS; dx++) {
            for (int dz = 0; dz < ROOM_CELLS; dz++) {
                roomIndices[(slot[1] + dz) * width + (slot[0] + dx)] = (byte) index;
            }
        }
        return new PlacedRoom(kind, slot[0], slot[1], ROOM_CELLS, ROOM_CELLS, random.nextLong());
    }

    // ------------------------------------------------------------------------ content ----

    private void populateContent(Random random) {
        int floor = floorY;

        // ---- gates ---------------------------------------------------------------------
        PlacedRoom landing = roomOf(RoomKind.LANDING);
        if (landing != null) {
            addGate(centerBlockX(landing), floor, centerBlockZ(landing), layer == 1 ? 0 : layer - 1);
        }
        PlacedRoom descent = roomOf(RoomKind.DESCENT);
        if (descent != null && layer < 5) {
            addGate(centerBlockX(descent), floor, centerBlockZ(descent), layer + 1);
            sealDescentDoor(descent);
        }
        PlacedRoom finalRoom = roomOf(RoomKind.FINAL_CHAMBER);
        if (finalRoom != null) {
            // The Heart itself: an altar that ends the story.
            features.put(key(centerBlockX(finalRoom), floor, centerBlockZ(finalRoom)), "memory_altar");
            addGate(centerBlockX(finalRoom), floor, centerBlockZ(finalRoom) + 4, 0);
        }

        // ---- safe rooms: lamp, altar, chest, and no spawns -------------------------------
        for (PlacedRoom room : rooms) {
            switch (room.kind()) {
                case SAFE_ROOM -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    features.put(key(x - 3, floor, z - 3), "memory_lamp");
                    features.put(key(x + 3, floor, z + 3), "memory_lamp");
                    features.put(key(x, floor, z - 2), "memory_altar");
                    containers.put(key(x + 3, floor, z - 3), "hollowlight:chests/safe_room");
                    containers.put(key(x - 3, floor, z + 3), "hollowlight:chests/safe_room");
                    inscriptions.put(key(x + 2, floor + 2, z - 4), "safe_room_note");
                }
                case LORE_VAULT -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    features.put(key(x, floor, z), "memory_pedestal");
                    containers.put(key(x - 4, floor, z), "hollowlight:chests/lore_vault");
                    containers.put(key(x + 4, floor, z), "hollowlight:chests/lore_vault");
                    features.put(key(x, floor, z + 4), "pale_crystal");
                }
                case LIBRARY -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    containers.put(key(x - 5, floor, z - 5), "hollowlight:chests/library");
                    containers.put(key(x + 5, floor, z + 5), "hollowlight:chests/library");
                    features.put(key(x, floor + 7, z), "hollow_lantern");
                }
                case CHAPEL -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    features.put(key(x, floor, z), "memory_altar");
                    containers.put(key(x - 6, floor, z), "hollowlight:chests/chapel");
                    containers.put(key(x + 6, floor, z), "hollowlight:chests/chapel");
                    features.put(key(x - 9, floor + 3, z), "hollow_lantern");
                    features.put(key(x + 9, floor + 3, z), "hollow_lantern");
                }
                case TRAP_ROOM -> containers.put(key(centerBlockX(room) + 8, floor, centerBlockZ(room)),
                        "hollowlight:chests/trap_room");
                case OBSERVATION_CHAMBER -> features.put(key(centerBlockX(room), floor + 2, centerBlockZ(room)),
                        "whispering_stone");
                case BOSS_ARENA -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    features.put(key(x, floor + 12, z), "heart_crystal");
                    features.put(key(x - 10, floor + 2, z - 10), "hollow_lantern");
                    features.put(key(x + 10, floor + 2, z - 10), "hollow_lantern");
                    features.put(key(x - 10, floor + 2, z + 10), "hollow_lantern");
                    features.put(key(x + 10, floor + 2, z + 10), "hollow_lantern");
                    containers.put(key(x + 8, floor, z + 8), "hollowlight:chests/boss_arena");
                }
                case MEMORIAL_ROOM -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    containers.put(key(x - 4, floor, z + 4), "hollowlight:chests/memorial");
                    features.put(key(x, floor + 4, z), "memory_lamp");
                }
                case FLOODED_GARDEN -> {
                    int x = centerBlockX(room);
                    int z = centerBlockZ(room);
                    containers.put(key(x + 5, floor, z - 5), "hollowlight:chests/drowned");
                    features.put(key(x, floor + 6, z), "pale_crystal");
                }
                case LANDING -> inscriptions.put(key(centerBlockX(room) + 3, floor + 2, centerBlockZ(room) - 4),
                        layer == 1 ? "page_the_lamps" : "inscription_count_the_steps");
                default -> {
                    // DESCENT and FINAL_CHAMBER are furnished by their own room builders.
                }
            }
        }

        // ---- lamps: one every few corridor cells, so the player is never truly blind -----
        for (int cx = 0; cx < grid.width(); cx++) {
            for (int cz = 0; cz < grid.height(); cz++) {
                if (roomAt(cx, cz) != null) {
                    continue;
                }
                int hash = hash(cx, cz, layer * 17);
                if (Math.floorMod(hash, 100) < 16) {
                    Side side = Side.byBit(Math.floorMod(hash >> 6, 4));
                    features.put(key(wallBlockX(cx, cz, side), floor + 2, wallBlockZ(cx, cz, side)),
                            layer >= 4 ? "pale_crystal" : "hollow_lantern");
                }
                if (layer == 2 && Math.floorMod(hash >> 12, 100) < 22) {
                    features.put(key(cx * PITCH + 2, floor, cz * PITCH + 5), "bioluminescent_growth");
                }
                if (layer == 3 && Math.floorMod(hash >> 13, 100) < 18) {
                    features.put(key(cx * PITCH + 5, floor, cz * PITCH + 2), "corrupted_growth");
                }
            }
        }

        // ---- spawners -------------------------------------------------------------------
        for (int cx = 0; cx < grid.width(); cx++) {
            for (int cz = 0; cz < grid.height(); cz++) {
                if (roomAt(cx, cz) != null) {
                    continue;
                }
                int hash = hash(cx, cz, layer * 31 + 7);
                int chance = 7 + layer * 2;
                if (Math.floorMod(hash, 100) < chance) {
                    spawners.put(key(cx * PITCH + 3, floor, cz * PITCH + 3), spawnEntityFor(layer, hash));
                }
            }
        }

        // ---- dead ends: reward the curiosity that the maze punishes ----------------------
        for (int cx = 0; cx < grid.width(); cx++) {
            for (int cz = 0; cz < grid.height(); cz++) {
                if (roomAt(cx, cz) != null || !grid.isDeadEnd(cx, cz)) {
                    continue;
                }
                int hash = hash(cx, cz, layer * 53 + 3);
                if (Math.floorMod(hash, 100) < 62) {
                    containers.put(key(cx * PITCH + 3, floor, cz * PITCH + 3), "hollowlight:chests/layer_" + layer);
                }
            }
        }

        // ---- one guaranteed key per layer, in a dead end far from the landing -----------
        long keyPos = farthestDeadEnd(random);
        if (keyPos != 0L) {
            containers.put(keyPos, layer == 1 ? "hollowlight:chests/key_rusted"
                    : layer <= 3 ? "hollowlight:chests/key_drowned"
                    : "hollowlight:chests/key_heart");
        }

        // ---- inscriptions: the layer's carved text, spread along the corridors -----------
        List<String> carved = new ArrayList<>();
        for (String id : com.hollowlight.story.LoreCatalog.forLayer(layer)) {
            var entry = com.hollowlight.story.LoreCatalog.get(id);
            if (entry != null && (entry.category() == com.hollowlight.story.LoreCategory.INSCRIPTION)) {
                carved.add(id);
            }
        }
        int inscriptionIndex = 0;
        for (int cx = 0; cx < grid.width() && !carved.isEmpty(); cx++) {
            for (int cz = 0; cz < grid.height(); cz++) {
                if (roomAt(cx, cz) != null) {
                    continue;
                }
                int hash = hash(cx, cz, layer * 71);
                if (Math.floorMod(hash, 100) >= 9) {
                    continue;
                }
                Side side = Side.byBit(Math.floorMod(hash >> 5, 4));
                String lore = carved.get(inscriptionIndex % carved.size());
                inscriptions.put(key(wallBlockX(cx, cz, side), floor + 1, wallBlockZ(cx, cz, side)), lore);
                inscriptionIndex++;
            }
        }
    }

    private String spawnEntityFor(int layer, int hash) {
        return switch (layer) {
            case 1 -> Math.floorMod(hash >> 3, 10) < 7 ? "hollowed" : "ash_moth";
            case 2 -> Math.floorMod(hash >> 3, 10) < 5 ? "drowned_choir" : "wickmoth";
            case 3 -> Math.floorMod(hash >> 3, 10) < 5 ? "stitcher" : "paperface";
            case 4 -> Math.floorMod(hash >> 3, 10) < 4 ? "paperface" : "weeping_one";
            default -> Math.floorMod(hash >> 3, 10) < 5 ? "hollowed" : "stitcher";
        };
    }

    private long farthestDeadEnd(Random random) {
        int[] distances = grid.distancesFrom(landingCellX, landingCellZ);
        long best = 0L;
        int bestDistance = -1;
        for (int cx = 0; cx < grid.width(); cx++) {
            for (int cz = 0; cz < grid.height(); cz++) {
                if (roomAt(cx, cz) != null || !grid.isDeadEnd(cx, cz)) {
                    continue;
                }
                int distance = distances[cz * grid.width() + cx];
                if (distance > bestDistance) {
                    bestDistance = distance;
                    best = key(cx * PITCH + 3, floorY, cz * PITCH + 3);
                }
            }
        }
        return best;
    }

    private void sealDescentDoor(PlacedRoom room) {
        // A gate of salt-iron across the way down: three wide, two high.
        int x = centerBlockX(room);
        int z = room.cellZ() * PITCH + 3;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                containers.remove(key(x + dx, floorY + dy, z));
                features.put(key(x + dx, floorY + dy, z), "sealed_gate");
            }
        }
    }

    private void addGate(int x, int y, int z, int link) {
        gates.put(key(x, y, z), link);
        features.put(key(x, y, z), "threshold_gate");
    }

    // ------------------------------------------------------------------------ queries ----

    /** FNV-ish deterministic hash; used instead of Random so results are per-cell not per-order. */
    public static int hash(int x, int z, int salt) {
        int h = x * 0x27D4EB2D ^ z * 0x165667B1 ^ salt * 0x9E3779B9;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h & 0x7FFFFFFF;
    }

    public int layer() {
        return layer;
    }

    public long seed() {
        return seed;
    }

    public MazeGrid grid() {
        return grid;
    }

    public int floorY() {
        return floorY;
    }

    public List<PlacedRoom> rooms() {
        return rooms;
    }

    public int ceiling(int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= grid.width() || cz >= grid.height()) {
            return 5;
        }
        return ceilings[cz * grid.width() + cx];
    }

    public boolean flooded(int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= grid.width() || cz >= grid.height()) {
            return false;
        }
        return flooded[cz * grid.width() + cx];
    }

    public PlacedRoom roomAt(int cx, int cz) {
        if (cx < 0 || cz < 0 || cx >= grid.width() || cz >= grid.height()) {
            return null;
        }
        int index = roomIndices[cz * grid.width() + cx];
        return index < 0 ? null : rooms.get(index);
    }

    public PlacedRoom roomOf(RoomKind kind) {
        for (PlacedRoom room : rooms) {
            if (room.kind() == kind) {
                return room;
            }
        }
        return null;
    }

    public int landingCellX() {
        return landingCellX;
    }

    public int landingCellZ() {
        return landingCellZ;
    }

    public Map<Long, String> containers() {
        return Map.copyOf(containers);
    }

    public Map<Long, String> inscriptions() {
        return Map.copyOf(inscriptions);
    }

    public Map<Long, Integer> gates() {
        return Map.copyOf(gates);
    }

    public Map<Long, String> features() {
        return Map.copyOf(features);
    }

    public Map<Long, String> spawners() {
        return Map.copyOf(spawners);
    }

    public String inscriptionAt(long packed) {
        return inscriptions.get(packed);
    }

    // ------------------------------------------------------------------- coordinates ----

    public static int centerBlockX(PlacedRoom room) {
        return room.cellX() * PITCH + 3;
    }

    public static int centerBlockZ(PlacedRoom room) {
        return room.cellZ() * PITCH + 3;
    }

    public static int cellOrigin(int cell) {
        return cell * PITCH;
    }

    /** X of the wall block on the given side of a cell (the middle of the wall band). */
    public static int wallBlockX(int cx, int cz, Side side) {
        return switch (side) {
            case EAST -> cx * PITCH + 7;
            case WEST -> cx * PITCH;
            default -> cx * PITCH + 3;
        };
    }

    public static int wallBlockZ(int cx, int cz, Side side) {
        return switch (side) {
            case SOUTH -> cz * PITCH + 7;
            case NORTH -> cz * PITCH;
            default -> cz * PITCH + 3;
        };
    }

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    public static int keyX(long key) {
        return (int) (key >> 38);
    }

    public static int keyZ(long key) {
        return (int) ((key >> 12) & 0x3FFFFFF);
    }

    public static int keyY(long key) {
        return (int) (key & 0xFFFL);
    }
}
