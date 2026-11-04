package com.hollowlight.sanity;

import com.hollowlight.Hollowlight;
import com.hollowlight.config.ModConfig;
import com.hollowlight.registry.ModEntities;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.world.labyrinth.LabyrinthManager;
import com.hollowlight.world.labyrinth.LabyrinthMapData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * The room tone of the labyrinth.
 *
 * <p>Horror in Hollowlight is mostly a mixing decision: the level is never silent, but nothing in
 * it is louder than the player's own footsteps. This class schedules the four beds that carry
 * that idea — moving air in a corridor, the particular warmth of a calm room, the Lamplighter's
 * theme when she is close enough to be the only thing worth listening to, and the single sharp
 * noise a trap is allowed to make.</p>
 *
 * <p>Every bed has a per-player cooldown so that walking in circles does not turn the mix into a
 * loop, and every volume is scaled by sanity: the world does not get louder as it gets worse, it
 * gets <em>closer</em>.</p>
 */
public final class AmbienceDirector {

    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
    private static int tick;

    private static final int WIND_SLOT = 0;
    private static final int CALM_SLOT = 1;
    private static final int BOSS_SLOT = 2;

    private AmbienceDirector() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 40 != 0) {
                return;
            }
            long now = server.getTicks();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player, now);
            }
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float base, float taken, boolean blocked) -> {
                    if (blocked || taken <= 0.0F || !(entity instanceof ServerPlayerEntity player)) {
                        return;
                    }
                    if (!source.isOf(DamageTypes.HOT_FLOOR)) {
                        return;
                    }
                    if (LabyrinthMapData.currentLayer(player) < 1) {
                        return;
                    }
                    onTrap(player);
                });
        Hollowlight.LOGGER.debug("Ambience director ready.");
    }

    private static void tickPlayer(ServerPlayerEntity player, long now) {
        int layer = LabyrinthMapData.currentLayer(player);
        if (layer < 1 || !ModConfig.get().enableAmbience) {
            return;
        }
        long[] slots = COOLDOWNS.computeIfAbsent(player.getUuid(), key -> new long[3]);
        ServerWorld world = (ServerWorld) player.getWorld();
        Random random = world.getRandom();
        float dread = SanityManager.ambientTwist(player);

        // ---- air in the corridors -------------------------------------------------------
        if (now >= slots[WIND_SLOT]) {
            slots[WIND_SLOT] = now + 620L + random.nextInt(560);
            BlockPos pos = player.getBlockPos().add(random.nextInt(13) - 6, random.nextInt(5) - 2,
                    random.nextInt(13) - 6);
            world.playSound(null, pos, ModSounds.WIND_CORRIDOR, SoundCategory.AMBIENT,
                    0.18F + dread * 0.12F, 0.62F + random.nextFloat() * 0.2F);
        }

        // ---- a room that is allowed to be kind -----------------------------------------
        boolean calm = LabyrinthManager.isCalmRoom(world, player.getBlockPos());
        if (calm && now >= slots[CALM_SLOT]) {
            slots[CALM_SLOT] = now + 900L + random.nextInt(400);
            world.playSound(null, player.getBlockPos(), ModSounds.AMBIENCE_SAFE,
                    SoundCategory.AMBIENT, 0.55F, 1.0F);
        }

        // ---- the Lamplighter's theme, only when she is the nearest thing ------------------
        if (now >= slots[BOSS_SLOT] && world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(40.0),
                entity -> entity.getType() == ModEntities.LAMPKEEPER).size() > 0) {
            slots[BOSS_SLOT] = now + 4800L;
            world.playSound(null, player.getBlockPos(), ModSounds.MUSIC_BOSS,
                    SoundCategory.MUSIC, 0.5F, 1.0F);
        }
    }

    /** Magma under a careless boot: the one sound in the mod that is allowed to be sharp. */
    public static void onTrap(ServerPlayerEntity player) {
        player.getWorld().playSound(null, player.getBlockPos(), ModSounds.TRAP_TRIGGER,
                SoundCategory.BLOCKS, 0.65F, 1.0F + player.getWorld().getRandom().nextFloat() * 0.1F);
        SanityManager.drain(player, 1.4F, SanityManager.Cause.DAMAGE);
    }

    /** Called when a player leaves; keeps the maps from growing without bound. */
    public static void forget(UUID uuid) {
        COOLDOWNS.remove(uuid);
    }
}
