package com.hollowlight.horror;

import com.hollowlight.Hollowlight;
import com.hollowlight.config.ModConfig;
import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModEntities;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.structure.EntranceBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/**
 * The hand that moves the scenery.
 *
 * <p>Everything here is quiet on purpose. The director never spawns a mob on top of the player,
 * never plays a loud sound without a slow fade-in, and never takes control away. Its job is to
 * make the labyrinth feel occupied: a lamp that goes out behind you, footsteps keeping pace, a
 * figure at the end of a corridor that is not there when you look again.</p>
 */
public final class HorrorDirector {

    private static final Map<UUID, Integer> EVENT_TIMERS = new HashMap<>();
    private static final Map<UUID, Long> LAST_NOISE = new HashMap<>();
    private static int tick;

    private HorrorDirector() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 20 != 0) {
                return;
            }
            ServerWorld overworld = server.getOverworld();
            for (ServerPlayerEntity player : overworld.getPlayers()) {
                EntranceBuilder.ensureBuilt(overworld, player.getBlockPos());
            }
        });
        Hollowlight.LOGGER.info("Horror director ready.");
    }

    // ------------------------------------------------------------------ per-player tick --

    public static void tickPlayer(ServerPlayerEntity player, int layer, float sanity) {
        if (!ModConfig.get().enableHorrorEvents) {
            return;
        }
        UUID id = player.getUuid();
        int timer = EVENT_TIMERS.merge(id, 1, Integer::sum);
        int interval = Math.max(8, ModConfig.get().horrorEventIntervalSeconds / 20);
        if (timer < interval) {
            return;
        }
        EVENT_TIMERS.put(id, 0);

        ServerWorld world = player.getWorld();
        Random random = world.getRandom();
        int roll = random.nextInt(100);
        float dread = 1.0F - sanity / SanityManager.MAX_SANITY;

        if (roll < 22 + (int) (dread * 20)) {
            // Something walks, just out of sight.
            BlockPos behind = player.getBlockPos().add(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
            world.playSound(null, behind, ModSounds.HOLLOWED_STEP, SoundCategory.HOSTILE,
                    0.45F, 0.85F + random.nextFloat() * 0.2F);
            SanityManager.drain(player, 0.4F, SanityManager.Cause.EVENT);
        } else if (roll < 40) {
            // A lamp the player placed goes out. Only ever one.
            extinguishOneLight(world, player);
        } else if (roll < 58) {
            // The apparition: a person-shaped suggestion at the end of a corridor.
            apparition(player, ModConfig.get().apparitionDistance);
            SanityManager.drain(player, 1.2F, SanityManager.Cause.EVENT);
        } else if (roll < 70) {
            world.playSound(null, player.getBlockPos(), ModSounds.BREATH, SoundCategory.AMBIENT,
                    0.35F, 0.9F);
        } else if (roll < 82) {
            world.playSound(null, player.getBlockPos(), ModSounds.STONE_SETTLE, SoundCategory.AMBIENT,
                    0.5F, 0.7F);
            SanityManager.drain(player, 0.3F, SanityManager.Cause.EVENT);
        } else if (roll < 92 && layer == 4) {
            // The Memorial Depths have a resident who is not a threat and never will be.
            if (world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(28.0),
                    entity -> entity.getType() == ModEntities.WEEPING_ONE).isEmpty()) {
                spawnAt(player, ModEntities.WEEPING_ONE, 14, 24);
            }
        } else {
            world.playSound(null, player.getBlockPos(), ModSounds.DISTANT_BELL, SoundCategory.AMBIENT,
                    0.35F, 0.6F);
        }

        if (dread > 0.6F) {
            world.spawnParticles(com.hollowlight.registry.ModParticles.WHISPER_SMOKE,
                    player.getX() + random.nextDouble() * 6 - 3,
                    player.getY() + random.nextDouble() * 3,
                    player.getZ() + random.nextDouble() * 6 - 3,
                    6, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** Called when the player makes a noise loud enough to be heard through stone. */
    public static void onLoudNoise(ServerPlayerEntity player, double radius) {
        ServerWorld world = player.getWorld();
        LAST_NOISE.put(player.getUuid(), world.getTime());
        world.playSound(null, player.getBlockPos(), ModSounds.STINGER_SOFT, SoundCategory.AMBIENT, 0.5F, 0.8F);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(radius), entity -> true)) {
            if (mob.getTarget() == null) {
                mob.setTarget(player);
            }
        }
        if (world.getRandom().nextFloat() < 0.4F) {
            spawnAt(player, ModEntities.HOLLOWED, 12, 20);
        }
    }

    // --------------------------------------------------------------------- phenomena -----

    private static void extinguishOneLight(ServerWorld world, ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(origin.add(-10, -3, -10), origin.add(10, 3, 10))) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(ModBlocks.HOLLOW_LANTERN) || state.isOf(ModBlocks.LAMP_GLASS)) {
                world.removeBlock(pos, false);
                world.playSound(null, pos, ModSounds.LANTERN_EMPTY, SoundCategory.BLOCKS, 0.7F, 0.9F);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        10, 0.2, 0.2, 0.2, 0.01);
                SanityManager.drain(player, 0.8F, SanityManager.Cause.EVENT);
                return;
            }
        }
    }

    /** A shadow that is only ever a shadow: the echo entity despawns on its own. */
    public static void apparition(ServerPlayerEntity player, int distance) {
        spawnAt(player, ModEntities.ECHO, Math.max(8, distance - 6), distance + 4);
    }

    private static void spawnAt(ServerPlayerEntity player, EntityType<?> type, int min, int max) {
        ServerWorld world = player.getWorld();
        Random random = world.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = min + random.nextInt(Math.max(1, max - min));
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            BlockPos pos = new BlockPos(x, player.getBlockY(), z);
            BlockPos surface = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);
            BlockPos target = world.isAir(surface) ? surface : surface.up();
            if (!world.isAir(target) || !world.isAir(target.up())) {
                continue;
            }
            var entity = type.create(world, SpawnReason.EVENT);
            if (entity == null) {
                continue;
            }
            entity.refreshPositionAndAngles(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                    random.nextFloat() * 360.0F, 0.0F);
            world.spawnEntity(entity);
            return;
        }
    }
}
