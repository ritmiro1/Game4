package com.hollowlight.world.ending;

import com.hollowlight.Hollowlight;
import com.hollowlight.registry.ModItems;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.LoreCatalog;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import com.hollowlight.util.DimensionTeleport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * The three endings, and the machinery that plays them out over about twenty seconds instead of
 * dropping a wall of text.
 *
 * <p>All three endings can be reached from the final chamber. Two of them are the obvious
 * choices, offered by the last altar; the third is only available to a player who did the work
 * of carrying Iver's whistle and the dust of the seventh seam all the way down — and who
 * understands that the way to end a story about looking for someone is to stop looking and
 * carry them instead.</p>
 */
public final class EndingSequence {

    private enum Choice {
        RELEASE, KEEP, RETURN
    }

    private record Beat(ServerPlayerEntity player, long executeAt, Runnable action) {
    }

    private static final List<Beat> QUEUE = new ArrayList<>();
    private static ServerWorld keepTicking;

    private EndingSequence() {
    }

    public static void bootstrap() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (QUEUE.isEmpty()) {
                return;
            }
            long now = server.getTicks();
            Iterator<Beat> iterator = QUEUE.iterator();
            while (iterator.hasNext()) {
                Beat beat = iterator.next();
                if (beat.executeAt() <= now) {
                    iterator.remove();
                    try {
                        beat.action().run();
                    } catch (RuntimeException exception) {
                        Hollowlight.LOGGER.error("Ending beat failed", exception);
                    }
                }
            }
        });
        Hollowlight.LOGGER.info("Ending sequences armed.");
    }

    /** Called from the altar interface or from a payload when the player makes a choice. */
    public static void choose(ServerPlayerEntity player, String rawChoice) {
        Choice choice;
        try {
            choice = Choice.valueOf(rawChoice.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            Hollowlight.LOGGER.warn("Unknown ending choice: {}", rawChoice);
            return;
        }
        if (choice == Choice.RETURN && !canReturn(player)) {
            player.sendMessage(Text.translatable("hollowlight.ending.return.missing")
                    .formatted(Formatting.DARK_GRAY, Formatting.ITALIC), false);
            return;
        }
        start(player, choice);
    }

    public static boolean tryThirdEnding(ServerPlayerEntity player) {
        if (player.getWorld().getRegistryKey() == com.hollowlight.world.labyrinth.LabyrinthMapData.layerKey(5)
                && canReturn(player)) {
            start(player, Choice.RETURN);
            return true;
        }
        return false;
    }

    private static boolean canReturn(ServerPlayerEntity player) {
        boolean whistle = false;
        boolean dust = false;
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            var stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.IVER_WHISTLE)) {
                whistle = true;
            }
            if (stack.isOf(ModItems.SEVENTH_SEAM_DUST)) {
                dust = true;
            }
        }
        return whistle && dust;
    }

    private static void start(ServerPlayerEntity player, Choice choice) {
        ServerWorld world = (ServerWorld) player.getWorld();
        long now = world.getServer().getTicks();
        BlockPos pos = player.getBlockPos();
        StoryManager.discover(player, "lampkeeper_whisper");

        world.playSound(null, pos, ModSounds.MUSIC_ENDING, SoundCategory.MUSIC, 0.8F, 1.0F);
        world.playSound(null, pos, ModSounds.LAMPKEEPER_DEATH, SoundCategory.HOSTILE, 0.9F, 0.7F);
        spawnMemoryParticles(world, pos);

        switch (choice) {
            case RELEASE -> {
                StoryManager.grant(player, StoryFlags.ENDING_RELEASE);
                StoryManager.story(player).setEnding("release");
                StoryManager.discover(player, "ending_release");
                beat(player, now + 20, () -> say(player, "hollowlight.ending.release.one"));
                beat(player, now + 70, () -> say(player, "hollowlight.ending.release.two"));
                beat(player, now + 130, () -> say(player, "hollowlight.ending.release.three"));
                beat(player, now + 200, () -> finish(player, "hollowlight.ending.release.epilogue"));
            }
            case KEEP -> {
                StoryManager.grant(player, StoryFlags.ENDING_KEEP);
                StoryManager.story(player).setEnding("keep");
                StoryManager.discover(player, "ending_keep");
                beat(player, now + 20, () -> say(player, "hollowlight.ending.keep.one"));
                beat(player, now + 80, () -> say(player, "hollowlight.ending.keep.two"));
                beat(player, now + 150, () -> say(player, "hollowlight.ending.keep.three"));
                beat(player, now + 220, () -> finish(player, "hollowlight.ending.keep.epilogue"));
            }
            case RETURN -> {
                StoryManager.grant(player, StoryFlags.ENDING_RETURN);
                StoryManager.story(player).setEnding("return");
                StoryManager.discover(player, "ending_return");
                beat(player, now + 20, () -> say(player, "hollowlight.ending.return.one"));
                beat(player, now + 80, () -> {
                    say(player, "hollowlight.ending.return.two");
                    player.getWorld().playSound(null, player.getBlockPos(), ModSounds.ECHO_VOICE,
                            SoundCategory.AMBIENT, 0.8F, 0.6F);
                });
                beat(player, now + 150, () -> say(player, "hollowlight.ending.return.three"));
                beat(player, now + 230, () -> finish(player, "hollowlight.ending.return.epilogue"));
            }
        }
    }

    private static void beat(ServerPlayerEntity player, long at, Runnable action) {
        QUEUE.add(new Beat(player, at, action));
    }

    private static void say(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.GRAY, Formatting.ITALIC), false);
        player.getWorld().playSound(null, player.getBlockPos(), ModSounds.WHISPER_CLOSE,
                SoundCategory.AMBIENT, 0.5F, 0.85F);
    }

    private static void finish(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.GOLD), false);
        player.getWorld().playSound(null, player.getBlockPos(), ModSounds.DISTANT_BELL,
                SoundCategory.AMBIENT, 0.8F, 0.6F);
        StoryManager.save();
        DimensionTeleport.toSurfaceShrine(player);
        ModPayloads.syncJournal(player);
    }

    private static void spawnMemoryParticles(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 120; i++) {
            double x = pos.getX() + (world.getRandom().nextDouble() - 0.5) * 12.0;
            double y = pos.getY() + world.getRandom().nextDouble() * 6.0;
            double z = pos.getZ() + (world.getRandom().nextDouble() - 0.5) * 12.0;
            world.spawnParticles(com.hollowlight.registry.ModParticles.MEMORY_GLINT, x, y, z, 1, 0.0, 0.02, 0.0, 0.01);
            if (i % 3 == 0) {
                world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.01, 0.0, 0.0);
            }
        }
    }

    /** Total number of lore fragments the endings can award, used in the final advancement text. */
    public static int endingLoreCount() {
        return (int) LoreCatalog.all().stream().filter(entry -> entry.id().startsWith("ending_")).count();
    }

    /** Called by the boss when it dies to offer the choice. */
    public static void offerChoice(ServerPlayerEntity player) {
        StoryManager.grant(player, StoryFlags.LAMPKEEPER_DEFEATED);
        player.sendMessage(Text.translatable("hollowlight.ending.offer")
                .formatted(Formatting.GOLD, Formatting.ITALIC), false);
        ModPayloads.openEndingChoice(player);
    }

    static void clearQueueForTesting() {
        QUEUE.clear();
        keepTicking = null;
    }
}
