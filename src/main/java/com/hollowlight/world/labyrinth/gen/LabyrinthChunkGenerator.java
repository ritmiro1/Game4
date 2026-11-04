package com.hollowlight.world.labyrinth.gen;

import com.hollowlight.world.labyrinth.LabyrinthLayout;
import com.hollowlight.world.labyrinth.LabyrinthLayouts;
import com.hollowlight.world.labyrinth.LabyrinthPalette;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

/**
 * The generator behind the five labyrinth dimensions.
 *
 * <p>There is no noise, no surface rule and no carver: everything the player walks through is
 * written by {@link LabyrinthBuilder} from a {@link LabyrinthLayout}, which is a pure function of
 * the world seed and the layer index. That gives three properties this mod depends on:</p>
 *
 * <ul>
 *   <li>the same seed always produces the same labyrinth, so a player's map is never a lie;</li>
 *   <li>the layout can be built and inspected without generating a single chunk, which is how
 *       the connectivity tests run;</li>
 *   <li>chunk generation is cheap — a few thousand block writes per chunk instead of noise
 *       sampling every column.</li>
 * </ul>
 *
 * <p>Block writing happens in {@code generateFeatures}, because that is the stage that receives
 * a world accessor (and therefore the world seed) and where vanilla expects structures and
 * features to place blocks.</p>
 */
public class LabyrinthChunkGenerator extends ChunkGenerator {

    public static final MapCodec<LabyrinthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    Codec.INT.optionalFieldOf("layer", 1).forGetter(generator -> generator.layer),
                    Codec.INT.optionalFieldOf("floor_y", 44).forGetter(generator -> generator.floorY)
            ).apply(instance, LabyrinthChunkGenerator::new));

    private static final int MIN_Y = 0;
    private static final int HEIGHT = 128;

    private final int layer;
    private final int floorY;

    public LabyrinthChunkGenerator(BiomeSource biomeSource, int layer, int floorY) {
        super(biomeSource);
        this.layer = Math.max(1, Math.min(5, layer));
        this.floorY = floorY;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    public int layer() {
        return layer;
    }

    public int floorY() {
        return floorY;
    }

    private LabyrinthLayout layout(long seed) {
        return LabyrinthLayouts.layout(layer, seed);
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        // Nothing to do here: terrain is written in generateFeatures where the seed is available.
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        LabyrinthLayout layout = layout(world.getSeed());
        LabyrinthPalette palette = LabyrinthPalette.forLayer(layer);
        LabyrinthBuilder.build((x, y, z, state) ->
                        world.setBlockState(new BlockPos(x, y, z), state, Block.NOTIFY_LISTENERS),
                chunk.getPos(), layout, palette);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // The shell already includes floor and ceiling.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk) {
        // No carvers: caves would break both the maze and the story.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Entity placement is handled by LabyrinthManager so that spawns respect corridors,
        // light levels and the player's sanity rather than biome spawn tables.
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        // No heightmap is meaningful here: the labyrinth has a ceiling. Report the highest point
        // of the tallest possible room so that vanilla spawn placement never picks a ceiling.
        return Math.min(getWorldHeight() - 1, floorY + LabyrinthLayout.CEILING_CAP + 3);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        LabyrinthPalette palette = LabyrinthPalette.forLayer(layer);
        int ceiling = LabyrinthLayout.CEILING_CAP;
        int floor = floorY;
        BlockState[] states = new BlockState[HEIGHT];
        BlockState air = Blocks.CAVE_AIR.getDefaultState();
        for (int y = 0; y < HEIGHT; y++) {
            if (y <= 1) {
                states[y] = Blocks.BEDROCK.getDefaultState();
            } else if (y < floor - 1) {
                states[y] = palette.deep();
            } else if (y == floor - 1) {
                states[y] = palette.floor();
            } else if (y < floor + ceiling) {
                states[y] = air;
            } else if (y < floor + ceiling + 3) {
                states[y] = palette.ceiling();
            } else {
                states[y] = air;
            }
        }
        return new VerticalBlockSample(MIN_Y, states);
    }

    @Override
    public int getMinimumY() {
        return MIN_Y;
    }

    @Override
    public int getWorldHeight() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public void appendDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Hollowlight layer " + layer + " cell "
                + Math.floorDiv(pos.getX(), LabyrinthLayout.PITCH) + ","
                + Math.floorDiv(pos.getZ(), LabyrinthLayout.PITCH));
    }
}
