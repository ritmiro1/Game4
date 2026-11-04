package com.hollowlight.world.labyrinth;

import com.hollowlight.Hollowlight;
import com.hollowlight.config.ModConfig;
import com.hollowlight.registry.ModEntities;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryManager;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.LootableInventory;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Runs the labyrinth at runtime: wires the chunk generator into the registry, hands chests their
 * loot tables when a chunk loads, spawns entities by hand so that every spawn respects walls and
 * lamplight, and keeps the five layers audible.
 */
public final class LabyrinthManager {

    private static final int SPAWN_INTERVAL = 40;
    private static final int AMBIENCE_INTERVAL = 220;
    private static int tickCounter;

    private LabyrinthManager() {
    }

    public static void bootstrap() {
        Registry.register(Registries.CHUNK_GENERATOR, Hollowlight.id("labyrinth"),
                LabyrinthChunkGeneratorHolder.CODEC);

        ServerChunkEvents.CHUNK_LOAD.register(LabyrinthManager::onChunkLoad);
        ServerTickEvents.END_WORLD_TICK.register(LabyrinthManager::tickWorld);

        Hollowlight.LOGGER.info("Labyrinth manager online: {} layers wired.", 5);
    }

    /** Indirection so the codec can be referenced without a hard circular import. */
    private static final class LabyrinthChunkGeneratorHolder {
        static final com.mojang.serialization.MapCodec<com.hollowlight.world.labyrinth.gen.LabyrinthChunkGenerator> CODEC =
                com.hollowlight.world.labyrinth.gen.LabyrinthChunkGenerator.CODEC;
    }

    // ------------------------------------------------------------------ chest loot -------

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        int layer = LabyrinthLayouts.layerOfWorld(world);
        if (layer < 1) {
            return;
        }
        LabyrinthLayout layout = LabyrinthLayouts.layout(layer, world.getSeed());
        if (layout.containers().isEmpty()) {
            return;
        }
        int baseX = chunk.getPos().getStartX();
        int baseZ = chunk.getPos().getStartZ();
        for (Map.Entry<Long, String> entry : layout.containers().entrySet()) {
            long key = entry.getKey();
            int x = LabyrinthLayout.keyX(key);
            int z = LabyrinthLayout.keyZ(key);
            if (x < baseX || x > baseX + 15 || z < baseZ || z > baseZ + 15) {
                continue;
            }
            BlockPos pos = new BlockPos(x, LabyrinthLayout.keyY(key), z);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof LootableInventory lootable) {
                RegistryKey<net.minecraft.loot.LootTable> tableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                        Identifier.of(entry.getValue()));
                lootable.setLootTable(tableKey, world.getRandom().nextLong());
            }
        }
    }

    // --------------------------------------------------------------------- ticking -------

    private static void tickWorld(ServerWorld world) {
        int layer = LabyrinthLayouts.layerOfWorld(world);
        if (layer < 1) {
            return;
        }
        tickCounter++;
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }
        boolean spawnTick = tickCounter % SPAWN_INTERVAL == 0;
        boolean ambienceTick = tickCounter % AMBIENCE_INTERVAL == 0;
        for (ServerPlayerEntity player : players) {
            if (spawnTick) {
                trySpawn(world, player, layer);
            }
            if (ambienceTick) {
                playAmbience(world, player, layer);
            }
        }
    }

    private static void playAmbience(ServerWorld world, ServerPlayerEntity player, int layer) {
        Random random = world.getRandom();
        var sound = switch (layer) {
            case 2 -> ModSounds.AMBIENCE_DROWNED;
            case 3 -> ModSounds.AMBIENCE_LIVING;
            case 4 -> ModSounds.AMBIENCE_MEMORIAL;
            case 5 -> ModSounds.AMBIENCE_HEART;
            default -> ModSounds.AMBIENCE_HALLS;
        };
        if (random.nextFloat() < 0.6F) {
            world.playSound(null, player.getBlockPos(), sound, SoundCategory.AMBIENT,
                    ModConfig.get().soundVolume * 0.55F, 1.0F);
        }
        if (random.nextFloat() < 0.12F) {
            world.playSound(null, player.getBlockPos(), ModSounds.DRIP, SoundCategory.AMBIENT,
                    ModConfig.get().soundVolume * 0.4F, 0.9F + random.nextFloat() * 0.3F);
        }
        if (random.nextFloat() < 0.05F) {
            world.playSound(null, player.getBlockPos(), ModSounds.RUMBLE, SoundCategory.AMBIENT,
                    ModConfig.get().soundVolume * 0.5F, 0.7F);
        }
        if (random.nextFloat() < 0.03F && ModConfig.get().enableMusic) {
            world.playSound(null, player.getBlockPos(), musicFor(layer), SoundCategory.MUSIC,
                    ModConfig.get().soundVolume * 0.5F, 1.0F);
        }
    }

    private static net.minecraft.sound.SoundEvent musicFor(int layer) {
        return switch (layer) {
            case 2 -> ModSounds.MUSIC_DROWNED;
            case 3 -> ModSounds.MUSIC_LIVING;
            case 4 -> ModSounds.MUSIC_MEMORIAL;
            case 5 -> ModSounds.MUSIC_HEART;
            default -> ModSounds.MUSIC_HALLS;
        };
    }

    // --------------------------------------------------------------------- spawning ------

    private static void trySpawn(ServerWorld world, ServerPlayerEntity player, int layer) {
        ModConfig config = ModConfig.get();
        if (config.entitySpawnMultiplier <= 0.0F) {
            return;
        }
        Random random = world.getRandom();
        if (random.nextFloat() > 0.55F * config.entitySpawnMultiplier) {
            return;
        }
        LabyrinthLayout layout = LabyrinthLayouts.layout(layer, world.getSeed());
        if (layout.spawners().isEmpty()) {
            return;
        }
        int cap = 5 + layer * 2;
        int nearby = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(48.0),
                entity -> entity.getType() == ModEntities.HOLLOWED || entity.getType() == ModEntities.STITCHER
                        || entity.getType() == ModEntities.PAPERFACE || entity.getType() == ModEntities.DROWNED_CHOIR
                        || entity.getType() == ModEntities.WEEPING_ONE).size();
        if (nearby >= cap) {
            return;
        }

        // Deterministic spawn points from the layout, but the choice of which one is random.
        int attempts = 0;
        for (Map.Entry<Long, String> entry : layout.spawners().entrySet()) {
            if (attempts++ > 24) {
                break;
            }
            long key = entry.getKey();
            BlockPos pos = new BlockPos(LabyrinthLayout.keyX(key), LabyrinthLayout.keyY(key),
                    LabyrinthLayout.keyZ(key));
            double distance = pos.getSquaredDistance(player.getBlockPos());
            if (distance < 13.0 * 13.0 || distance > 38.0 * 38.0) {
                continue;
            }
            if (world.getLightLevel(LightType.BLOCK, pos) > 7) {
                continue;
            }
            if (!world.isAir(pos) || !world.isAir(pos.up())) {
                continue;
            }
            if (random.nextFloat() > 0.35F) {
                continue;
            }
            EntityType<?> type = Registries.ENTITY_TYPE.get(Identifier.of(Hollowlight.MOD_ID, entry.getValue()));
            if (type == null) {
                continue;
            }
            var entity = type.create(world, SpawnReason.STRUCTURE);
            if (entity == null) {
                continue;
            }
            entity.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(entity);
            return;
        }
    }

    /** Used by the weeping one and the patterns of the living walls. */
    public static boolean isCalmRoom(World world, BlockPos pos) {
        LabyrinthLayout layout = LabyrinthLayouts.layout(world);
        if (layout == null) {
            return false;
        }
        PlacedRoom room = layout.roomAt(Math.floorDiv(pos.getX(), LabyrinthLayout.PITCH),
                Math.floorDiv(pos.getZ(), LabyrinthLayout.PITCH));
        return room != null && room.kind().calm();
    }
}
