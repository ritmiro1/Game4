package com.hollowlight.world.structure;

import com.hollowlight.block.ThresholdGateBlock;
import com.hollowlight.registry.ModBlocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

/**
 * The Hollowlight Shrine: the overworld entrance to the labyrinth and the mod's one piece of
 * surface world generation.
 *
 * <p>It is built lazily rather than through a structure feature. The shrine has to guarantee
 * that a specific block (the threshold gate) ends up at a specific position with a specific
 * block state, and that a chest at the bottom carries a specific loot table. Building it on the
 * server thread, once, the first time a player comes near, is deterministic, cheap, and cannot
 * half-generate.</p>
 *
 * <p>The location is derived from the world seed and the world spawn, so every world has exactly
 * one shrine, always within a few hundred blocks of spawn, and it never moves.</p>
 */
public final class EntranceBuilder {

    private static final int CHAMBER_DEPTH = 9;
    private static final Map<RegistryKey<World>, BlockPos> BUILT = new ConcurrentHashMap<>();

    private EntranceBuilder() {
    }

    /** Deterministic shrine centre, searched for dry land above sea level. */
    public static BlockPos shrineOrigin(ServerWorld world) {
        BlockPos spawn = world.getSpawnPos();
        long seed = world.getSeed();
        for (int attempt = 0; attempt < 12; attempt++) {
            int angle = (int) (((seed >> (attempt * 3)) & 0xFF) + attempt * 73 + 41);
            double radians = Math.toRadians(angle % 360);
            int distance = 220 + attempt * 40;
            int x = spawn.getX() + (int) Math.round(Math.cos(radians) * distance);
            int z = spawn.getZ() + (int) Math.round(Math.sin(radians) * distance);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            boolean water = !world.getBlockState(ground).getFluidState().isEmpty();
            if (!water && y > 40 && y < 200) {
                return new BlockPos(x, y, z);
            }
        }
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn.getX() + 220, spawn.getZ());
        return new BlockPos(spawn.getX() + 220, y, spawn.getZ());
    }

    /** Where a player stands after climbing out of the labyrinth. */
    public static BlockPos exitPoint(ServerWorld world) {
        BlockPos origin = shrineOrigin(world);
        return new BlockPos(origin.getX() + 4, origin.getY(), origin.getZ() + 4);
    }

    public static boolean isBuilt(ServerWorld world) {
        return BUILT.containsKey(world.getRegistryKey());
    }

    /** Builds the shrine if a player has come close enough and it is not there yet. */
    public static void ensureBuilt(ServerWorld world, BlockPos playerPos) {
        if (BUILT.containsKey(world.getRegistryKey())) {
            return;
        }
        BlockPos origin = shrineOrigin(world);
        if (playerPos.getSquaredDistance(origin) > 96.0 * 96.0) {
            return;
        }
        if (world.getBlockState(origin).isOf(ModBlocks.SHRINE_LANTERN)) {
            BUILT.put(world.getRegistryKey(), origin);
            return;
        }
        build(world, origin);
        BUILT.put(world.getRegistryKey(), origin);
    }

    public static void build(ServerWorld world, BlockPos origin) {
        int y = origin.getY();

        // Clear a small apron so the shrine is never buried in a hillside.
        clear(world, origin, 8, 7);

        // Platform and low wall.
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                boolean edge = Math.abs(dx) == 7 || Math.abs(dz) == 7;
                world.setBlockState(new BlockPos(origin.getX() + dx, y - 1, origin.getZ() + dz),
                        ModBlocks.SHRINE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
                if (edge) {
                    for (int dy = 0; dy <= 4; dy++) {
                        world.setBlockState(new BlockPos(origin.getX() + dx, y + dy, origin.getZ() + dz),
                                ModBlocks.SHRINE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }

        // Four pillars with lanterns, and an opening on the south side.
        for (int[] corner : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            for (int dy = 0; dy <= 6; dy++) {
                world.setBlockState(new BlockPos(origin.getX() + corner[0], y + dy, origin.getZ() + corner[1]),
                        ModBlocks.SHRINE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
            }
            world.setBlockState(new BlockPos(origin.getX() + corner[0], y + 7, origin.getZ() + corner[1]),
                    ModBlocks.SHRINE_LANTERN.getDefaultState(), Block.NOTIFY_ALL);
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 3; dy++) {
                world.setBlockState(new BlockPos(origin.getX() + dx, y + dy, origin.getZ() + 7),
                        Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        // The dedication stone, facing the entrance.
        world.setBlockState(new BlockPos(origin.getX() + 2, y, origin.getZ() - 6),
                ModBlocks.INSCRIPTION_STONE.getDefaultState(), Block.NOTIFY_ALL);

        // The well: a three by three shaft down to a lamplit chamber.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < CHAMBER_DEPTH; dy++) {
                    world.setBlockState(new BlockPos(origin.getX() + dx, y - dy, origin.getZ() + dz),
                            Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        int chamberY = y - CHAMBER_DEPTH + 1;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 4;
                world.setBlockState(new BlockPos(origin.getX() + dx, chamberY - 1, origin.getZ() + dz),
                        ModBlocks.SHRINE_BRICKS.getDefaultState(), Block.NOTIFY_ALL);
                for (int dy = 0; dy <= 3; dy++) {
                    world.setBlockState(new BlockPos(origin.getX() + dx, chamberY + dy, origin.getZ() + dz),
                            edge ? ModBlocks.SHRINE_BRICKS.getDefaultState() : Blocks.AIR.getDefaultState(),
                            Block.NOTIFY_ALL);
                }
            }
        }

        // The gate in the middle of the chamber floor.
        world.setBlockState(new BlockPos(origin.getX(), chamberY - 1, origin.getZ()),
                ModBlocks.THRESHOLD_GATE.getDefaultState().with(ThresholdGateBlock.LINK, 1), Block.NOTIFY_ALL);
        world.setBlockState(new BlockPos(origin.getX() - 3, chamberY + 2, origin.getZ()),
                ModBlocks.SHRINE_LANTERN.getDefaultState(), Block.NOTIFY_ALL);
        world.setBlockState(new BlockPos(origin.getX() + 3, chamberY + 2, origin.getZ()),
                ModBlocks.SHRINE_LANTERN.getDefaultState(), Block.NOTIFY_ALL);

        // A ladder up the shaft, so a player who changes their mind can climb out.
        for (int dy = 1; dy < CHAMBER_DEPTH; dy++) {
            world.setBlockState(new BlockPos(origin.getX(), y - dy, origin.getZ() - 1),
                    Blocks.LADDER.getDefaultState(), Block.NOTIFY_ALL);
        }

        // Starter cache: the lantern, the journal, oil, and one page of the story.
        BlockPos chestPos = new BlockPos(origin.getX() + 3, chamberY, origin.getZ() + 3);
        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(RegistryKey.of(RegistryKeys.LOOT_TABLE,
                    Identifier.of("hollowlight", "chests/shrine_starter")), world.getSeed());
        }
    }

    private static void clear(ServerWorld world, BlockPos origin, int radius, int height) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= height; dy++) {
                    world.setBlockState(new BlockPos(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz),
                            Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
    }
}
