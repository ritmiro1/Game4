package com.hollowlight.sanity;

import com.hollowlight.config.ModConfig;
import com.hollowlight.horror.HorrorDirector;
import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.labyrinth.LabyrinthMapData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

/**
 * The player's grip on the world, 0 (gone) to 100 (whole).
 *
 * <p>Design constraints that are deliberately enforced here:</p>
 * <ul>
 *   <li>Sanity never kills. Nothing in this class can reduce a player below zero or apply a
 *       damaging effect — the horror does not need to be lethal to work.</li>
 *   <li>Sanity never softlocks. There is always light available in the labyrinth, and a safe
 *       room every few hundred blocks restores it passively.</li>
 *   <li>Effects are graded: below 60 the player hears things; below 40 they see things; below
 *       25 the world gets heavier (slowness while in darkness); below 12 the danger is real,
 *       because entities find them more easily — a gameplay consequence, not a punishment.</li>
 * </ul>
 */
public final class SanityManager {

    public static final float MAX_SANITY = 100.0F;

    /** Where a change came from. Used for tuning, logging and the debug command. */
    public enum Cause {
        DARKNESS, ENTITY, LORE, DAMAGE, WHISPER, WHISPERING_STONE, CONSUMED, EVENT, DEPTH, LIGHT
    }

    private static final Map<UUID, Float> SNEAK_PEEK_TIMER = new HashMap<>();
    private static int tick;

    private SanityManager() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tick % 20 != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ModPayloads.syncSanity(player);
            ModPayloads.syncJournal(player);
        });
    }

    // ------------------------------------------------------------------- public API -----

    public static float sanity(ServerPlayerEntity player) {
        return StoryManager.story(player).sanity();
    }

    public static void set(ServerPlayerEntity player, float value) {
        StoryManager.story(player).setSanity(value);
        ModPayloads.syncSanity(player);
    }

    public static void drain(ServerPlayerEntity player, float amount, Cause cause) {
        if (amount <= 0.0F) {
            return;
        }
        float scaled = amount * ModConfig.get().sanityDrainMultiplier * causeMultiplier(cause);
        if (hasAnchor(player)) {
            scaled *= 0.8F;
        }
        float current = sanity(player);
        float next = Math.max(0.0F, current - scaled);
        if (next == current) {
            return;
        }
        StoryManager.story(player).setSanity(next);
        announceThreshold(player, current, next);
    }

    public static void restore(ServerPlayerEntity player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        float current = sanity(player);
        float next = Math.min(MAX_SANITY, current + amount * ModConfig.get().sanityRecoveryMultiplier);
        if (next == current) {
            return;
        }
        StoryManager.story(player).setSanity(next);
        announceThreshold(player, current, next);
    }

    /** 0 = calm, 1 = the worst it gets. Drives vignette, fog and sound on the client. */
    public static float ambientTwist(ServerPlayerEntity player) {
        return MathHelper.clamp(1.0F - sanity(player) / MAX_SANITY, 0.0F, 1.0F);
    }

    // ------------------------------------------------------------------- internals ------

    private static float causeMultiplier(Cause cause) {
        return switch (cause) {
            case DARKNESS -> 1.0F;
            case ENTITY -> 1.6F;
            case LORE -> 0.5F;
            case DAMAGE -> 1.2F;
            case WHISPER -> 0.6F;
            case WHISPERING_STONE -> 1.0F;
            case CONSUMED -> 1.0F;
            case EVENT -> 2.0F;
            case DEPTH -> 1.0F;
            case LIGHT -> 1.0F;
        };
    }

    private static boolean hasAnchor(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            var stack = player.getInventory().getStack(slot);
            if (stack.isOf(com.hollowlight.registry.ModItems.LAMPLIGHTERS_BAND)) {
                return true;
            }
        }
        return false;
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        int layer = LabyrinthMapData.currentLayer(player);
        LabyrinthMapData.rememberCurrentCell(player);

        float light = player.getWorld().getLightLevel(LightType.BLOCK, player.getBlockPos());
        float drain = 0.0F;

        if (layer >= 1) {
            drain += 0.012F * layer;
        }
        if (light < 4.0F) {
            drain += 0.055F;
        } else if (light >= 10.0F) {
            restore(player, 0.035F);
        }
        if (player.getWorld().getBlockState(player.getBlockPos().down()).isOf(ModBlocks.MEMORY_LAMP)) {
            restore(player, 0.15F);
        }
        if (drain > 0.0F) {
            drain(player, drain, Cause.DARKNESS);
        }

        float sanity = sanity(player);
        if (layer >= 1) {
            StoryManager.ambientWhisper(player, sanity);
            HorrorDirector.tickPlayer(player, layer, sanity);
        }

        if (sanity < 25.0F && light < 6.0F) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, false));
        }
        if (sanity < 12.0F) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, true, false, false));
            if (player.getWorld().getRandom().nextFloat() < 0.25F) {
                player.getWorld().playSound(null, player.getBlockPos(), ModSounds.HEARTBEAT,
                        SoundCategory.AMBIENT, 0.5F, 1.0F);
            }
        }
        ModPayloads.syncSanity(player);
    }

    private static void announceThreshold(ServerPlayerEntity player, float before, float after) {
        if (before >= 40.0F && after < 40.0F) {
            player.sendMessage(Text.translatable("hollowlight.sanity.thin")
                    .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC), true);
            player.getWorld().playSound(null, player.getBlockPos(), ModSounds.STINGER_SOFT,
                    SoundCategory.AMBIENT, 0.8F, 0.7F);
        } else if (before >= 20.0F && after < 20.0F) {
            player.sendMessage(Text.translatable("hollowlight.sanity.low")
                    .formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
            player.getWorld().playSound(null, player.getBlockPos(), ModSounds.STINGER_DEEP,
                    SoundCategory.AMBIENT, 0.9F, 0.6F);
        } else if (before < 20.0F && after >= 20.0F) {
            player.sendMessage(Text.translatable("hollowlight.sanity.recovering")
                    .formatted(Formatting.GRAY, Formatting.ITALIC), true);
        }
    }

    /** Used by the /hollowlight debug command and by the altar's memory draught. */
    public static void setForDebug(ServerPlayerEntity player, float value) {
        set(player, value);
    }

    public static BlockPos lightNearest(ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(origin.add(-4, -2, -4), origin.add(4, 2, 4))) {
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.getLuminance() > 0) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    static void onSneak(ServerPlayerEntity player) {
        SNEAK_PEEK_TIMER.merge(player.getUuid(), 1.0F, Float::sum);
    }
}
