package com.hollowlight.world.labyrinth.room;

import com.hollowlight.world.labyrinth.LabyrinthLayout;
import com.hollowlight.world.labyrinth.LabyrinthPalette;
import com.hollowlight.world.labyrinth.PlacedRoom;
import com.hollowlight.world.labyrinth.gen.BlockCanvas;
import com.hollowlight.world.labyrinth.gen.LabyrinthBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * The twelve handcrafted rooms of the labyrinth.
 *
 * <p>Each method furnishes a room inside its cell footprint. Furniture is placed with
 * {@code setIfAir} so that nothing ever overwrites the shell, the gates or the chests; and
 * every placement is a pure function of the room's own seed, so a room looks identical every
 * time a player re-enters it — which matters, because these rooms are where the story lives and
 * they must not flicker.</p>
 */
public final class RoomFurnisher {

    private RoomFurnisher() {
    }

    public static void furnish(BlockCanvas canvas, PlacedRoom room) {
        int x1 = room.minBlockX() + 1;
        int z1 = room.minBlockZ() + 1;
        int x2 = room.maxBlockX() - 2;
        int z2 = room.maxBlockZ() - 2;
        int floor = canvas.floorY();
        LabyrinthPalette palette = canvas.palette();
        switch (room.kind()) {
            case LANDING -> landing(canvas, palette, x1, z1, x2, z2, floor);
            case DESCENT -> descent(canvas, palette, x1, z1, x2, z2, floor);
            case SAFE_ROOM -> safeRoom(canvas, palette, x1, z1, x2, z2, floor);
            case LORE_VAULT -> loreVault(canvas, palette, x1, z1, x2, z2, floor);
            case MEMORIAL_ROOM -> memorialRoom(canvas, palette, x1, z1, x2, z2, floor);
            case LIBRARY -> library(canvas, palette, x1, z1, x2, z2, floor);
            case TRAP_ROOM -> trapRoom(canvas, palette, x1, z1, x2, z2, floor);
            case OBSERVATION_CHAMBER -> observation(canvas, palette, x1, z1, x2, z2, floor);
            case CHAPEL -> chapel(canvas, palette, x1, z1, x2, z2, floor);
            case FLOODED_GARDEN -> garden(canvas, palette, x1, z1, x2, z2, floor);
            case BOSS_ARENA -> arena(canvas, palette, x1, z1, x2, z2, floor);
            case FINAL_CHAMBER -> finalChamber(canvas, palette, x1, z1, x2, z2, floor);
        }
    }

    // ------------------------------------------------------------------ scaffolding ------

    private static void setIfAir(BlockCanvas canvas, int x, int y, int z, BlockState state) {
        if (canvas instanceof LabyrinthBuilder.Canvas concrete) {
            if (concrete.isAirAt(x, y, z)) {
                canvas.set(x, y, z, state);
            }
            return;
        }
        canvas.set(x, y, z, state);
    }

    private static void platform(BlockCanvas canvas, int x1, int z1, int x2, int z2, int y, BlockState state) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                canvas.set(x, y, z, state);
            }
        }
    }

    private static void ring(BlockCanvas canvas, int cx, int cz, int radius, int y, BlockState state) {
        for (int angle = 0; angle < 360; angle += 3) {
            double radians = Math.toRadians(angle);
            int x = cx + (int) Math.round(Math.cos(radians) * radius);
            int z = cz + (int) Math.round(Math.sin(radians) * radius);
            canvas.set(x, y, z, state);
        }
    }

    private static void disc(BlockCanvas canvas, int cx, int cz, int radius, int y, BlockState state) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    canvas.set(cx + x, y, cz + z, state);
                }
            }
        }
    }

    private static void pillar(BlockCanvas canvas, int x, int z, int floor, int height, BlockState state) {
        for (int y = floor; y < floor + height; y++) {
            canvas.set(x, y, z, state);
        }
    }

    private static void corners(BlockCanvas canvas, int x1, int z1, int x2, int z2, int floor, int height,
                                BlockState state) {
        pillar(canvas, x1 + 1, z1 + 1, floor, height, state);
        pillar(canvas, x2 - 1, z1 + 1, floor, height, state);
        pillar(canvas, x1 + 1, z2 - 1, floor, height, state);
        pillar(canvas, x2 - 1, z2 - 1, floor, height, state);
    }

    // ---------------------------------------------------------------------- the rooms ----

    private static void landing(BlockCanvas canvas, LabyrinthPalette palette,
                                int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        disc(canvas, cx, cz, 6, floor - 1, palette.floor());
        ring(canvas, cx, cz, 6, floor - 1, palette.accent());
        corners(canvas, x1, z1, x2, z2, floor, 5, palette.rib());
        for (int[] corner : new int[][]{{x1 + 1, z1 + 1}, {x2 - 1, z1 + 1}, {x1 + 1, z2 - 1}, {x2 - 1, z2 - 1}}) {
            canvas.set(corner[0], floor + 5, corner[1], palette.feature("hollow_lantern"));
        }
        // The step that all of Hollowmere was supposed to leave bread on.
        canvas.set(cx, floor, cz - 8, Blocks.STONE_STAIRS.getDefaultState());
        canvas.set(cx - 1, floor, cz - 8, Blocks.STONE_STAIRS.getDefaultState());
        canvas.set(cx + 1, floor, cz - 8, Blocks.STONE_STAIRS.getDefaultState());
        canvas.set(cx, floor + 1, cz - 8, palette.accent());
        // A bench for a person who is not coming back.
        canvas.set(cx - 6, floor, cz + 6, Blocks.STONE_STAIRS.getDefaultState());
        canvas.set(cx - 5, floor, cz + 6, Blocks.STONE_STAIRS.getDefaultState());
    }

    private static void descent(BlockCanvas canvas, LabyrinthPalette palette,
                                int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        // A sunken well around the gate: the floor drops two steps and the gate stands alone.
        disc(canvas, cx, cz, 7, floor - 1, palette.accent());
        disc(canvas, cx, cz, 5, floor - 1, palette.deep());
        ring(canvas, cx, cz, 5, floor - 1, palette.accent());
        ring(canvas, cx, cz, 4, floor - 2, palette.deep());
        disc(canvas, cx, cz, 3, floor - 2, palette.floor());
        for (int dx = -3; dx <= 3; dx++) {
            canvas.set(cx + dx, floor - 1, cz - 3, Blocks.STONE_BRICK_STAIRS.getDefaultState());
            canvas.set(cx + dx, floor - 1, cz + 3, Blocks.STONE_BRICK_STAIRS.getDefaultState());
        }
        // Chains reach down into the dark above the shaft, and none of them are holding anything.
        for (int dx = -5; dx <= 5; dx += 5) {
            for (int dz = -5; dz <= 5; dz += 5) {
                for (int y = floor + 4; y < floor + 11; y++) {
                    setIfAir(canvas, cx + dx, y, cz + dz, Blocks.CHAIN.getDefaultState());
                }
            }
        }
        for (int[] corner : new int[][]{{x1 + 1, z1 + 1}, {x2 - 1, z1 + 1}, {x1 + 1, z2 - 1}, {x2 - 1, z2 - 1}}) {
            pillar(canvas, corner[0], corner[1], floor, 9, palette.rib());
            canvas.set(corner[0], floor + 9, corner[1], palette.feature("hollow_lantern"));
        }
    }

    private static void safeRoom(BlockCanvas canvas, LabyrinthPalette palette,
                                 int x1, int z1, int x2, int z2, int floor) {
        // Warm floor, a bed, a chest, a candle that someone left burning for whoever came next.
        platform(canvas, x1 + 2, z1 + 2, x2 - 2, z2 - 2, floor - 1, palette.feature("root_planks"));
        corners(canvas, x1, z1, x2, z2, floor, 4, Blocks.OAK_LOG.getDefaultState());
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        // bed, made
        canvas.set(cx - 3, floor, cz - 3, Blocks.RED_WOOL.getDefaultState());
        canvas.set(cx - 2, floor, cz - 3, Blocks.RED_WOOL.getDefaultState());
        canvas.set(cx - 4, floor, cz - 3, Blocks.OAK_TRAPDOOR.getDefaultState());
        canvas.set(cx - 3, floor + 1, cz - 3, Blocks.WHITE_CARPET.getDefaultState());
        // table and stool
        canvas.set(cx + 2, floor, cz + 2, Blocks.OAK_FENCE.getDefaultState());
        canvas.set(cx + 2, floor + 1, cz + 2, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
        canvas.set(cx + 3, floor, cz + 2, Blocks.OAK_STAIRS.getDefaultState());
        setIfAir(canvas, cx + 2, floor + 2, cz + 2, Blocks.CANDLE.getDefaultState());
        // a blanket folded twice, and a pair of boots that fit a smaller person
        canvas.set(cx - 3, floor, cz + 3, Blocks.WHITE_WOOL.getDefaultState());
        canvas.set(cx - 2, floor, cz + 3, Blocks.BROWN_CARPET.getDefaultState());
        ring(canvas, cx, cz, 5, floor - 1, palette.accent());
    }

    private static void loreVault(BlockCanvas canvas, LabyrinthPalette palette,
                                  int x1, int z1, int x2, int z2, int floor) {
        corners(canvas, x1, z1, x2, z2, floor, 6, palette.rib());
        for (int x = x1 + 2; x <= x2 - 2; x += 3) {
            for (int z = z1 + 2; z <= z2 - 2; z += 3) {
                if (Math.abs(x - (x1 + x2) / 2) < 3 && Math.abs(z - (z1 + z2) / 2) < 3) {
                    continue;
                }
                for (int y = floor; y < floor + 3; y++) {
                    canvas.set(x, y, z, Blocks.BOOKSHELF.getDefaultState());
                }
                setIfAir(canvas, x, floor + 3, z, Blocks.CANDLE.getDefaultState());
            }
        }
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        canvas.set(cx, floor, cz, Blocks.LECTERN.getDefaultState());
        ring(canvas, cx, cz, 6, floor - 1, palette.accent());
        // cobwebs in the corners: nobody has read these in a long time
        canvas.set(x1 + 1, floor + 4, z1 + 1, Blocks.COBWEB.getDefaultState());
        canvas.set(x2 - 1, floor + 4, z2 - 1, Blocks.COBWEB.getDefaultState());
        canvas.set(x1 + 1, floor + 5, z2 - 1, Blocks.COBWEB.getDefaultState());
    }

    private static void memorialRoom(BlockCanvas canvas, LabyrinthPalette palette,
                                     int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        platform(canvas, x1 + 2, z1 + 2, x2 - 2, z2 - 2, floor - 1, palette.feature("root_planks"));
        corners(canvas, x1, z1, x2, z2, floor, 4, Blocks.STRIPPED_OAK_LOG.getDefaultState());
        // A table set for three, one plate smaller than the others.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                canvas.set(cx + dx, floor + 1, cz + dz, Blocks.OAK_SLAB.getDefaultState());
            }
        }
        canvas.set(cx - 2, floor, cz, Blocks.OAK_FENCE.getDefaultState());
        canvas.set(cx + 2, floor, cz, Blocks.OAK_FENCE.getDefaultState());
        canvas.set(cx - 3, floor + 2, cz - 1, Blocks.WHITE_CANDLE.getDefaultState());
        canvas.set(cx + 3, floor + 2, cz - 1, Blocks.WHITE_CANDLE.getDefaultState());
        canvas.set(cx, floor + 2, cz, Blocks.FLOWER_POT.getDefaultState());
        // Chairs pushed in. Nobody got up from this table and left it like this.
        canvas.set(cx - 2, floor, cz - 3, Blocks.OAK_STAIRS.getDefaultState());
        canvas.set(cx, floor, cz - 3, Blocks.OAK_STAIRS.getDefaultState());
        canvas.set(cx + 2, floor, cz - 3, Blocks.OAK_STAIRS.getDefaultState());
        // The window. Underground. With a curtain.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                canvas.set(cx + dx, floor + 2 + dy, z1 + 1, Blocks.GLASS_PANE.getDefaultState());
            }
        }
        for (int dx = -3; dx <= 3; dx++) {
            canvas.set(cx + dx, floor + 6, z1 + 1, Blocks.WHITE_WOOL.getDefaultState());
        }
        // A cold hearth and a pot on a hook.
        canvas.set(cx - 6, floor, cz + 4, Blocks.BRICKS.getDefaultState());
        canvas.set(cx - 6, floor + 1, cz + 4, Blocks.CAULDRON.getDefaultState());
        canvas.set(cx - 6, floor + 2, cz + 4, Blocks.CHAIN.getDefaultState());
    }

    private static void library(BlockCanvas canvas, LabyrinthPalette palette,
                                int x1, int z1, int x2, int z2, int floor) {
        for (int z = z1 + 2; z <= z2 - 2; z += 2) {
            for (int y = floor; y < floor + 4; y++) {
                canvas.set(x1 + 1, y, z, Blocks.BOOKSHELF.getDefaultState());
                canvas.set(x2 - 1, y, z, Blocks.BOOKSHELF.getDefaultState());
            }
        }
        for (int x = x1 + 3; x <= x2 - 3; x += 2) {
            for (int y = floor; y < floor + 4; y++) {
                canvas.set(x, y, z1 + 1, Blocks.BOOKSHELF.getDefaultState());
            }
        }
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        for (int dz = -4; dz <= 4; dz += 4) {
            canvas.set(cx + 2, floor, cz + dz, Blocks.LECTERN.getDefaultState());
        }
        platform(canvas, cx - 4, cz - 6, cx + 4, cz + 6, floor - 1, palette.feature("root_planks"));
        for (int x = cx - 4; x <= cx + 4; x += 4) {
            for (int z = cz - 6; z <= cz + 6; z += 6) {
                pillar(canvas, x, z, floor, 8, palette.rib());
            }
        }
        // The ladder that goes up into a ceiling with nothing above it.
        for (int y = floor; y < floor + 8; y++) {
            canvas.set(cx - 2, y, cz + 7, Blocks.LADDER.getDefaultState());
        }
    }

    private static void trapRoom(BlockCanvas canvas, LabyrinthPalette palette,
                                 int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        // Magma in a deliberate pattern: it glows, so a careful player sees the floor is wrong.
        for (int x = x1 + 2; x <= x2 - 2; x += 4) {
            for (int z = z1 + 2; z <= z2 - 2; z += 4) {
                canvas.set(x, floor - 1, z, Blocks.MAGMA_BLOCK.getDefaultState());
                canvas.set(x + 1, floor - 1, z, palette.accent());
            }
        }
        // A cage around the reward, with a way in for anyone who looks up.
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    for (int y = floor; y < floor + 3; y++) {
                        canvas.set(cx + dx, y, cz + dz, Blocks.IRON_BARS.getDefaultState());
                    }
                }
            }
        }
        canvas.set(cx, floor, cz, Blocks.CHEST.getDefaultState());
        for (int y = floor; y < floor + 3; y++) {
            setIfAir(canvas, cx - 3, y, cz, Blocks.IRON_BARS.getDefaultState());
        }
        // Cobwebs slow the careless; the pattern of them shows which way the room wants you to go.
        for (int i = 0; i < 12; i++) {
            int x = x1 + 2 + (i * 5) % Math.max(1, x2 - x1 - 3);
            int z = z1 + 2 + (i * 7) % Math.max(1, z2 - z1 - 3);
            setIfAir(canvas, x, floor, z, Blocks.COBWEB.getDefaultState());
        }
        canvas.set(cx, floor + 3, cz, palette.feature("hollow_lantern"));
    }

    private static void observation(BlockCanvas canvas, LabyrinthPalette palette,
                                    int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        // A tiny room inside the big one.
        for (int x = cx - 4; x <= cx + 2; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                boolean edge = x == cx - 4 || x == cx + 2 || z == cz - 3 || z == cz + 3;
                if (edge) {
                    for (int y = floor; y < floor + 4; y++) {
                        canvas.set(x, y, z, palette.wall());
                    }
                } else {
                    canvas.set(x, floor - 1, z, palette.feature("root_planks"));
                    for (int y = floor; y < floor + 4; y++) {
                        canvas.set(x, y, z, Blocks.CAVE_AIR.getDefaultState());
                    }
                }
            }
        }
        // the slot
        for (int y = floor + 2; y <= floor + 3; y++) {
            canvas.set(cx + 2, y, cz, Blocks.CAVE_AIR.getDefaultState());
        }
        // a chair worn smooth, and a shelf of things someone kept to stay awake
        canvas.set(cx - 2, floor, cz + 2, Blocks.OAK_STAIRS.getDefaultState());
        for (int y = floor; y < floor + 3; y++) {
            canvas.set(cx - 3, y, cz - 2, Blocks.BOOKSHELF.getDefaultState());
        }
        setIfAir(canvas, cx - 3, floor + 3, cz - 2, Blocks.CANDLE.getDefaultState());
        // tallies, in the corner, going up the wall
        for (int y = floor + 1; y < floor + 6; y++) {
            canvas.set(cx - 4, y, cz + 1, palette.feature("tally"));
        }
    }

    private static void chapel(BlockCanvas canvas, LabyrinthPalette palette,
                               int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        // Two colonnades and a ribbed vault: the largest open space the layer can offer.
        for (int z = z1 + 3; z <= z2 - 3; z += 4) {
            for (int height = 9; height <= 11; height++) {
                canvas.set(x1 + 4, floor + height, z, palette.rib());
                canvas.set(x2 - 4, floor + height, z, palette.rib());
                if (z % 8 == 0) {
                    canvas.set(cx, floor + height + 1, z, palette.accent());
                }
            }
            pillar(canvas, x1 + 4, z, floor, 9, palette.rib());
            pillar(canvas, x2 - 4, z, floor, 9, palette.rib());
            canvas.set(x1 + 4, floor + 9, z, palette.accent());
            canvas.set(x2 - 4, floor + 9, z, palette.accent());
        }
        for (int z = z1 + 4; z <= z2 - 4; z += 2) {
            canvas.set(cx - 3, floor, z, Blocks.STONE_BRICK_STAIRS.getDefaultState());
            canvas.set(cx + 3, floor, z, Blocks.STONE_BRICK_STAIRS.getDefaultState());
        }
        // Dead candles on every surface. Hundreds of them. Someone kept a vigil here.
        for (int i = 0; i < 40; i++) {
            int x = x1 + 2 + (i * 13) % Math.max(1, x2 - x1 - 3);
            int z = z1 + 2 + (i * 29) % Math.max(1, z2 - z1 - 3);
            setIfAir(canvas, x, floor, z, Blocks.CANDLE.getDefaultState());
        }
        // A rose window of lamp glass, high up, that lights nothing.
        disc(canvas, cx, cz, 3, floor + 11, palette.feature("lamp_glass"));
    }

    private static void garden(BlockCanvas canvas, LabyrinthPalette palette,
                               int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        disc(canvas, cx, cz, 7, floor - 1, palette.deep());
        disc(canvas, cx, cz, 6, floor, Blocks.WATER.getDefaultState());
        disc(canvas, cx, cz, 6, floor - 1, Blocks.WATER.getDefaultState());
        ring(canvas, cx, cz, 7, floor - 1, palette.accent());
        // Growth climbs everything that does not move.
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int hash = LabyrinthLayout.hash(x, z, 991);
                if (Math.floorMod(hash, 100) < 18) {
                    setIfAir(canvas, x, floor, z, palette.feature("bioluminescent_growth"));
                }
                if (Math.floorMod(hash >> 5, 100) < 8) {
                    setIfAir(canvas, x, floor + 2, z, palette.feature("bioluminescent_growth"));
                }
            }
        }
        // The bridge: a line of stone over the water, exactly wide enough for one person.
        for (int z = cz - 7; z <= cz + 7; z++) {
            canvas.set(cx, floor, z, palette.accent());
            canvas.set(cx + 1, floor, z, palette.accent());
        }
        for (int dz = -7; dz <= 7; dz += 3) {
            canvas.set(cx - 1, floor + 1, cz + dz, Blocks.IRON_BARS.getDefaultState());
            canvas.set(cx + 2, floor + 1, cz + dz, Blocks.IRON_BARS.getDefaultState());
        }
    }

    private static void arena(BlockCanvas canvas, LabyrinthPalette palette,
                              int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        disc(canvas, cx, cz, 10, floor - 1, palette.floor());
        disc(canvas, cx, cz, 4, floor, palette.accent());
        disc(canvas, cx, cz, 4, floor - 1, ModBlocks.HEART_CRYSTAL_BLOCK.getDefaultState());
        ring(canvas, cx, cz, 10, floor - 1, palette.accent());
        ring(canvas, cx, cz, 6, floor, Blocks.CHISELED_DEEPSLATE.getDefaultState());
        // Twelve pillars, in the round, each one lit from within.
        for (int i = 0; i < 12; i++) {
            double radians = Math.toRadians(i * 30.0);
            int x = cx + (int) Math.round(Math.cos(radians) * 9);
            int z = cz + (int) Math.round(Math.sin(radians) * 9);
            for (int y = floor; y < floor + 14; y++) {
                canvas.set(x, y, z, palette.rib());
            }
            canvas.set(x, floor + 14, z, palette.feature("heart_crystal"));
            canvas.set(x, floor + 2, z, palette.feature("hollow_lantern"));
        }
        // The impossible geometry: rings of crystal that hang in the air with nothing holding them.
        for (int radius = 3; radius <= 8; radius += 5) {
            ring(canvas, cx, cz, radius, floor + 9, palette.feature("heart_crystal"));
        }
        for (int dx = -6; dx <= 6; dx += 3) {
            for (int dz = -6; dz <= 6; dz += 3) {
                if (Math.abs(dx) == 6 || Math.abs(dz) == 6) {
                    for (int y = floor + 5; y < floor + 8; y++) {
                        canvas.set(cx + dx, y, cz + dz, Blocks.CHAIN.getDefaultState());
                    }
                }
            }
        }
    }

    private static void finalChamber(BlockCanvas canvas, LabyrinthPalette palette,
                                     int x1, int z1, int x2, int z2, int floor) {
        int cx = (x1 + x2) / 2;
        int cz = (z1 + z2) / 2;
        disc(canvas, cx, cz, 6, floor - 1, palette.wall());
        ring(canvas, cx, cz, 5, floor - 1, palette.accent());
        // One lamp, and a circle of candles that nobody has re-lit since the last person came.
        for (int i = 0; i < 16; i++) {
            double radians = Math.toRadians(i * 22.5);
            int x = cx + (int) Math.round(Math.cos(radians) * 4);
            int z = cz + (int) Math.round(Math.sin(radians) * 4);
            setIfAir(canvas, x, floor, z, Blocks.CANDLE.getDefaultState());
        }
        for (int y = floor; y < floor + 6; y++) {
            canvas.set(cx - 7, y, cz, palette.rib());
            canvas.set(cx + 7, y, cz, palette.rib());
        }
        for (int dx = -7; dx <= 7; dx++) {
            canvas.set(cx + dx, floor + 7, cz, palette.rib());
        }
        setIfAir(canvas, cx, floor + 6, cz, palette.feature("memory_lamp"));
    }
}
