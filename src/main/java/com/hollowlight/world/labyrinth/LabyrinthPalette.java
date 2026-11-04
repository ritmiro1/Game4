package com.hollowlight.world.labyrinth;

import com.hollowlight.block.SealedGateBlock;
import com.hollowlight.block.ThresholdGateBlock;
import com.hollowlight.registry.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

/**
 * The material identity of each layer. Five families of stone, each with a floor, a wall, a
 * ceiling, a deep fill and an accent, plus the shared furniture of the labyrinth.
 */
public final class LabyrinthPalette {

    private final BlockState wall;
    private final BlockState floor;
    private final BlockState ceiling;
    private final BlockState deep;
    private final BlockState accent;
    private final BlockState rib;
    private final Map<String, BlockState> features = new HashMap<>();

    private LabyrinthPalette(BlockState wall, BlockState floor, BlockState ceiling, BlockState deep,
                             BlockState accent, BlockState rib) {
        this.wall = wall;
        this.floor = floor;
        this.ceiling = ceiling;
        this.deep = deep;
        this.accent = accent;
        this.rib = rib;

        features.put("hollow_lantern", ModBlocks.HOLLOW_LANTERN.getDefaultState());
        features.put("memory_lamp", ModBlocks.MEMORY_LAMP.getDefaultState());
        features.put("memory_altar", ModBlocks.MEMORY_ALTAR.getDefaultState());
        features.put("memory_pedestal", ModBlocks.MEMORY_PEDESTAL.getDefaultState());
        features.put("pale_crystal", ModBlocks.PALE_CRYSTAL.getDefaultState());
        features.put("heart_crystal", ModBlocks.HEART_CRYSTAL_BLOCK.getDefaultState());
        features.put("whispering_stone", ModBlocks.WHISPERING_STONE.getDefaultState());
        features.put("bioluminescent_growth", ModBlocks.BIOLUMINESCENT_GROWTH.getDefaultState());
        features.put("corrupted_growth", ModBlocks.CORRUPTED_GROWTH.getDefaultState());
        features.put("inscription", ModBlocks.INSCRIPTION_STONE.getDefaultState());
        features.put("sealed_gate", ModBlocks.SEALED_GATE.getDefaultState().with(SealedGateBlock.OPEN, false));
        features.put("chest", Blocks.CHEST.getDefaultState());
        features.put("barrel", Blocks.BARREL.getDefaultState());
        features.put("bed", Blocks.RED_WOOL.getDefaultState());
        features.put("bookshelf", Blocks.BOOKSHELF.getDefaultState());
        features.put("table", Blocks.OAK_TRAPDOOR.getDefaultState());
        features.put("candle", Blocks.CANDLE.getDefaultState());
        features.put("iron_bars", Blocks.IRON_BARS.getDefaultState());
        features.put("chain", Blocks.CHAIN.getDefaultState());
        features.put("water", Blocks.WATER.getDefaultState());
        features.put("cobweb", Blocks.COBWEB.getDefaultState());
        features.put("gravel", Blocks.GRAVEL.getDefaultState());
        features.put("root_planks", ModBlocks.ROOT_PLANKS.getDefaultState());
        features.put("grave_dust", ModBlocks.GRAVE_DUST.getDefaultState());
        features.put("rust", ModBlocks.RUSTED_GRATE.getDefaultState());
        features.put("lamp_glass", ModBlocks.LAMP_GLASS.getDefaultState());
        features.put("dust", ModBlocks.GRAVE_DUST.getDefaultState());
        features.put("tally", ModBlocks.TALLY_MARKS.getDefaultState());
        features.put("shrine_bricks", ModBlocks.SHRINE_BRICKS.getDefaultState());
        features.put("shrine_lantern", ModBlocks.SHRINE_LANTERN.getDefaultState());
    }

    public static LabyrinthPalette forLayer(int layer) {
        return switch (layer) {
            case 2 -> new LabyrinthPalette(
                    ModBlocks.DROWNED_STONE.getDefaultState(),
                    ModBlocks.DROWNED_BRICKS.getDefaultState(),
                    ModBlocks.DROWNED_STONE.getDefaultState(),
                    ModBlocks.DROWNED_STONE.getDefaultState(),
                    ModBlocks.BARNACLED_STONE.getDefaultState(),
                    ModBlocks.DROWNED_BRICKS.getDefaultState());
            case 3 -> new LabyrinthPalette(
                    ModBlocks.LIVING_WALL.getDefaultState(),
                    ModBlocks.VEINED_LIVING_STONE.getDefaultState(),
                    ModBlocks.LIVING_WALL.getDefaultState(),
                    ModBlocks.VEINED_LIVING_STONE.getDefaultState(),
                    ModBlocks.CORRUPTED_GROWTH.getDefaultState(),
                    ModBlocks.LIVING_WALL.getDefaultState());
            case 4 -> new LabyrinthPalette(
                    ModBlocks.MEMORIAL_STONE.getDefaultState(),
                    ModBlocks.PALE_MEMORIAL_TILE.getDefaultState(),
                    ModBlocks.MEMORIAL_STONE.getDefaultState(),
                    ModBlocks.MEMORIAL_STONE.getDefaultState(),
                    ModBlocks.PALE_MEMORIAL_TILE.getDefaultState(),
                    ModBlocks.MEMORIAL_STONE.getDefaultState());
            case 5 -> new LabyrinthPalette(
                    ModBlocks.HEART_STONE.getDefaultState(),
                    ModBlocks.HEART_STONE.getDefaultState(),
                    ModBlocks.HEART_STONE.getDefaultState(),
                    ModBlocks.HEART_STONE.getDefaultState(),
                    ModBlocks.HEART_CRYSTAL_BLOCK.getDefaultState(),
                    ModBlocks.HEART_STONE.getDefaultState());
            default -> new LabyrinthPalette(
                    ModBlocks.FORGOTTEN_STONE.getDefaultState(),
                    ModBlocks.FORGOTTEN_BRICKS.getDefaultState(),
                    ModBlocks.FORGOTTEN_STONE.getDefaultState(),
                    ModBlocks.FORGOTTEN_STONE.getDefaultState(),
                    ModBlocks.CRACKED_FORGOTTEN_BRICKS.getDefaultState(),
                    ModBlocks.FORGOTTEN_PILLAR.getDefaultState());
        };
    }

    public BlockState wall() {
        return wall;
    }

    public BlockState floor() {
        return floor;
    }

    public BlockState ceiling() {
        return ceiling;
    }

    public BlockState deep() {
        return deep;
    }

    public BlockState accent() {
        return accent;
    }

    public BlockState rib() {
        return rib;
    }

    public BlockState feature(String id) {
        return features.get(id);
    }

    /** The threshold gate for a given link (0 = up to the surface, 1..5 = a layer). */
    public BlockState gate(int link) {
        return ModBlocks.THRESHOLD_GATE.getDefaultState().with(ThresholdGateBlock.LINK, link);
    }
}
