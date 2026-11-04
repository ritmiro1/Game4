package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.block.InscriptionBlock;
import com.hollowlight.block.MemoryAltarBlock;
import com.hollowlight.block.SealedGateBlock;
import com.hollowlight.block.TallyMarksBlock;
import com.hollowlight.block.TallCrossBlock;
import com.hollowlight.block.ThresholdGateBlock;
import com.hollowlight.block.WhisperingStoneBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Every block in the mod. The labyrinth layers each have their own material family, which is
 * how a player tells depths apart without a map: colour, grain and how the dust lies.
 */
public final class ModBlocks {

    private static final List<Block> ALL = new ArrayList<>();
    private static final List<Item> BLOCK_ITEMS = new ArrayList<>();

    // ============================ LAYER I — THE FORGOTTEN HALLS ============================
    public static final Block FORGOTTEN_STONE = register("forgotten_stone",
            Block::new, stone(MapColor.STONE_GRAY, 1.9F, 7.0F, BlockSoundGroup.STONE));
    public static final Block FORGOTTEN_BRICKS = register("forgotten_bricks",
            Block::new, stone(MapColor.STONE_GRAY, 2.1F, 7.5F, BlockSoundGroup.STONE));
    public static final Block CRACKED_FORGOTTEN_BRICKS = register("cracked_forgotten_bricks",
            Block::new, stone(MapColor.STONE_GRAY, 1.7F, 6.5F, BlockSoundGroup.STONE));
    public static final Block FORGOTTEN_PILLAR = register("forgotten_pillar",
            PillarBlock::new, stone(MapColor.STONE_GRAY, 2.4F, 8.0F, BlockSoundGroup.STONE));

    // ========================== LAYER II — THE DROWNED CORRIDORS =========================
    public static final Block DROWNED_STONE = register("drowned_stone",
            Block::new, stone(MapColor.TERRACOTTA_CYAN, 2.3F, 8.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block DROWNED_BRICKS = register("drowned_bricks",
            Block::new, stone(MapColor.TERRACOTTA_CYAN, 2.5F, 8.5F, BlockSoundGroup.DEEPSLATE));
    public static final Block BARNACLED_STONE = register("barnacled_stone",
            Block::new, stone(MapColor.TERRACOTTA_CYAN, 2.0F, 7.0F, BlockSoundGroup.DEEPSLATE));

    // ============================ LAYER III — THE LIVING WALLS ===========================
    public static final Block LIVING_WALL = register("living_wall",
            Block::new, stone(MapColor.DULL_RED, 3.0F, 9.0F, BlockSoundGroup.MOSS_BLOCK));
    public static final Block VEINED_LIVING_STONE = register("veined_living_stone",
            Block::new, stone(MapColor.DULL_RED, 3.2F, 9.5F, BlockSoundGroup.MOSS_BLOCK));
    public static final Block CORRUPTED_GROWTH = register("corrupted_growth",
            TallCrossBlock::new, AbstractBlock.Settings.create()
                    .mapColor(MapColor.DARK_RED).strength(0.4F).nonOpaque().noCollision()
                    .luminance(state -> 2).sounds(BlockSoundGroup.MOSS_BLOCK).ticksRandomly());

    // =========================== LAYER IV — THE MEMORIAL DEPTHS ==========================
    public static final Block MEMORIAL_STONE = register("memorial_stone",
            Block::new, stone(MapColor.PALE_YELLOW, 2.6F, 8.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block PALE_MEMORIAL_TILE = register("pale_memorial_tile",
            Block::new, stone(MapColor.PALE_YELLOW, 2.2F, 7.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block TALLY_MARKS = register("tally_marks",
            TallyMarksBlock::new, stone(MapColor.PALE_YELLOW, 1.8F, 6.0F, BlockSoundGroup.DEEPSLATE));

    // ================================ LAYER V — THE HEART ================================
    public static final Block HEART_STONE = register("heart_stone",
            Block::new, stone(MapColor.DARK_CRIMSON, 4.0F, 12.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block HEART_CRYSTAL_BLOCK = register("heart_crystal_block",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.DARK_CRIMSON)
                    .strength(3.0F, 10.0F).requiresTool().luminance(state -> 6)
                    .sounds(BlockSoundGroup.GLASS));

    // ================================ SHARED MATERIALS ===================================
    public static final Block RUSTED_GRATE = register("rusted_grate",
            Block::new, stone(MapColor.ORANGE, 2.8F, 9.0F, BlockSoundGroup.METAL));
    public static final Block ROOT_PLANKS = register("root_planks",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.BROWN)
                    .strength(2.0F, 6.0F).sounds(BlockSoundGroup.WOOD));
    public static final Block GRAVE_DUST = register("grave_dust",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.LIGHT_GRAY)
                    .strength(0.6F).sounds(BlockSoundGroup.SAND));
    public static final Block PALE_CRYSTAL = register("pale_crystal",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.PALE_PURPLE)
                    .strength(1.5F, 4.0F).luminance(state -> 7).sounds(BlockSoundGroup.GLASS));
    public static final Block BIOLUMINESCENT_GROWTH = register("bioluminescent_growth",
            TallCrossBlock::new, AbstractBlock.Settings.create().mapColor(MapColor.TEAL)
                    .strength(0.3F).nonOpaque().noCollision().luminance(state -> 10)
                    .sounds(BlockSoundGroup.MOSS_BLOCK).ticksRandomly());
    public static final Block LAMP_GLASS = register("lamp_glass",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.PALE_YELLOW)
                    .strength(0.5F).luminance(state -> 9).sounds(BlockSoundGroup.GLASS));

    // ================================ FUNCTIONAL BLOCKS ==================================
    public static final Block HOLLOW_LANTERN = register("hollow_lantern",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.ORANGE)
                    .strength(1.2F).luminance(state -> 13).nonOpaque()
                    .sounds(BlockSoundGroup.LANTERN));
    public static final Block MEMORY_LAMP = register("memory_lamp",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.PALE_YELLOW)
                    .strength(1.4F).luminance(state -> 15).sounds(BlockSoundGroup.LANTERN));
    public static final Block MEMORY_ALTAR = register("memory_altar",
            MemoryAltarBlock::new, AbstractBlock.Settings.create().mapColor(MapColor.DARK_GREEN)
                    .strength(3.0F, 30.0F).requiresTool().luminance(state -> 5)
                    .sounds(BlockSoundGroup.DEEPSLATE));
    public static final Block MEMORY_PEDESTAL = register("memory_pedestal",
            Block::new, stone(MapColor.STONE_GRAY, 2.0F, 8.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block SEALED_GATE = register("sealed_gate",
            SealedGateBlock::new, AbstractBlock.Settings.create().mapColor(MapColor.IRON_GRAY)
                    .strength(6.0F, 60.0F).requiresTool().sounds(BlockSoundGroup.METAL));
    public static final Block INSCRIPTION_STONE = register("inscription_stone",
            InscriptionBlock::new, stone(MapColor.STONE_GRAY, 2.0F, 8.0F, BlockSoundGroup.DEEPSLATE));
    public static final Block WHISPERING_STONE = register("whispering_stone",
            WhisperingStoneBlock::new, AbstractBlock.Settings.create().mapColor(MapColor.DARK_GRAY)
                    .strength(2.2F, 8.0F).requiresTool().ticksRandomly()
                    .sounds(BlockSoundGroup.DEEPSLATE));
    public static final Block THRESHOLD_GATE = register("threshold_gate",
            ThresholdGateBlock::new, AbstractBlock.Settings.create().mapColor(MapColor.BLACK)
                    .strength(-1.0F, 3600000.0F).luminance(state -> 11).nonOpaque()
                    .sounds(BlockSoundGroup.AMETHYST_BLOCK));

    // ============================== THE SURFACE SHRINE ===================================
    public static final Block SHRINE_BRICKS = register("shrine_bricks",
            Block::new, stone(MapColor.TERRACOTTA_GRAY, 2.5F, 9.0F, BlockSoundGroup.STONE));
    public static final Block SHRINE_LANTERN = register("shrine_lantern",
            Block::new, AbstractBlock.Settings.create().mapColor(MapColor.ORANGE)
                    .strength(2.0F).luminance(state -> 14).sounds(BlockSoundGroup.LANTERN));

    private ModBlocks() {
    }

    private static AbstractBlock.Settings stone(MapColor color, float hardness, float resistance,
                                                BlockSoundGroup sounds) {
        return AbstractBlock.Settings.create().mapColor(color)
                .strength(hardness, resistance).requiresTool().sounds(sounds);
    }

    private static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, Hollowlight.id(name));
        Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(settings.registryKey(blockKey)));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Hollowlight.id(name));
        Item item = Registry.register(Registries.ITEM, itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey)));
        BLOCK_ITEMS.add(item);
        ALL.add(block);
        return block;
    }

    /** Blocks in registration order — used by the creative tab and by data validation. */
    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The block items, in registration order. */
    public static List<Item> blockItems() {
        return Collections.unmodifiableList(BLOCK_ITEMS);
    }

    public static void bootstrap() {
        // Static initialisation happens on first touch; this method documents the entry point.
        Hollowlight.LOGGER.debug("Registered {} blocks", ALL.size());
    }
}
