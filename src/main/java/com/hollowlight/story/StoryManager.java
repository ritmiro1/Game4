package com.hollowlight.story;

import com.hollowlight.Hollowlight;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.sanity.SanityManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Server-side keeper of every player's story state.
 *
 * <p>Persistence is a single JSON document in the world folder ("hollowlight_story.json").
 * It is written on world stop and every few minutes while playing, written atomically via a
 * temporary file, and never read on a client. This deliberately avoids version-specific
 * persistent-state APIs (which have changed three times across 1.20-1.21) at the cost of a
 * little manual serialisation.</p>
 */
public final class StoryManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hollowlight_story.json";
    private static final int AUTOSAVE_TICKS = 20 * 120;

    private static final Map<UUID, PlayerStory> STORIES = new HashMap<>();
    private static MinecraftServer server;
    private static int tickCounter;

    private StoryManager() {
    }

    public static void bootstrap() {
        ServerLifecycleEvents.SERVER_STARTING.register(starting -> {
            server = starting;
            load();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(stopping -> {
            save();
            STORIES.clear();
            server = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(currentServer -> {
            if (++tickCounter % AUTOSAVE_TICKS == 0) {
                save();
            }
        });
        Hollowlight.LOGGER.info("Story manager ready — {} fragments in the catalog", LoreCatalog.size());
    }

    public static PlayerStory story(ServerPlayerEntity player) {
        return STORIES.computeIfAbsent(player.getUuid(), uuid -> new PlayerStory());
    }

    /** Returns true the first time this fragment is found. */
    public static boolean discover(ServerPlayerEntity player, String loreId) {
        if (!LoreCatalog.contains(loreId)) {
            Hollowlight.LOGGER.warn("Unknown lore id discovered: {}", loreId);
            return false;
        }
        PlayerStory story = story(player);
        boolean first = story.discover(loreId);
        if (first) {
            LoreEntry entry = LoreCatalog.get(loreId);
            Advancements.onLoreDiscovered(player, entry);
            ModPayloads.syncJournal(player);
        }
        return first;
    }

    public static void grant(ServerPlayerEntity player, String flag) {
        if (story(player).addFlag(flag)) {
            Advancements.onFlag(player, flag);
            if (ModConfigHooks.enabled()) {
                ModPayloads.syncJournal(player);
            }
        }
    }

    public static boolean has(ServerPlayerEntity player, String flag) {
        return story(player).hasFlag(flag);
    }

    public static void playDiscoverSound(ServerPlayerEntity player) {
        player.getWorld().playSound(null, player.getBlockPos(), ModSounds.LORE_DISCOVER,
                SoundCategory.PLAYERS, 0.6F, 1.0F);
    }

    /**
     * The whisper that follows the player around. It is the lamplighter calling her brother's
     * name; the player will not know that for several hours.
     */
    public static void whisperTo(ServerPlayerEntity player, boolean close) {
        player.getWorld().playSound(null, player.getBlockPos(),
                close ? ModSounds.WHISPER_CLOSE : ModSounds.WHISPER,
                SoundCategory.AMBIENT, close ? 0.7F : 0.45F, 0.9F + player.getWorld().getRandom().nextFloat() * 0.15F);
    }

    // ==================================== persistence ===================================

    private static Path path() {
        if (server == null) {
            return null;
        }
        return server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve(FILE_NAME);
    }

    public static void save() {
        Path path = path();
        if (path == null) {
            return;
        }
        JsonObject root = new JsonObject();
        JsonArray players = new JsonArray();
        for (Map.Entry<UUID, PlayerStory> entry : STORIES.entrySet()) {
            PlayerStory story = entry.getValue();
            JsonObject node = new JsonObject();
            node.addProperty("uuid", entry.getKey().toString());
            node.addProperty("sanity", story.sanity());
            node.addProperty("ending", story.ending());
            JsonArray discovered = new JsonArray();
            story.discovered().forEach(discovered::add);
            node.add("discovered", discovered);
            JsonArray flags = new JsonArray();
            story.flags().forEach(flags::add);
            node.add("flags", flags);
            JsonArray visited = new JsonArray();
            story.visitedCells().forEach(visited::add);
            node.add("visited", visited);
            players.add(node);
        }
        root.add("players", players);
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(FILE_NAME + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
            Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            Hollowlight.LOGGER.error("Could not save Hollowlight story data", exception);
        }
    }

    public static void load() {
        STORIES.clear();
        Path path = path();
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                return;
            }
            JsonArray players = parsed.getAsJsonObject().getAsJsonArray("players");
            if (players == null) {
                return;
            }
            for (JsonElement element : players) {
                JsonObject node = element.getAsJsonObject();
                UUID uuid = UUID.fromString(node.get("uuid").getAsString());
                PlayerStory story = new PlayerStory();
                story.setSanity(node.has("sanity") ? node.get("sanity").getAsFloat() : 100.0F);
                story.setEnding(node.has("ending") ? node.get("ending").getAsString() : "");
                readStrings(node.getAsJsonArray("discovered"), story.discovered());
                readStrings(node.getAsJsonArray("flags"), story.flags());
                List<String> visited = new ArrayList<>();
                readStrings(node.getAsJsonArray("visited"), new LinkedHashSet<>(visited));
                JsonArray visitedArray = node.getAsJsonArray("visited");
                if (visitedArray != null) {
                    for (JsonElement cell : visitedArray) {
                        story.visitedCells().add(cell.getAsString());
                    }
                }
                STORIES.put(uuid, story);
            }
            Hollowlight.LOGGER.info("Loaded story data for {} players", STORIES.size());
        } catch (Exception exception) {
            Hollowlight.LOGGER.error("Could not read Hollowlight story data", exception);
        }
    }

    private static void readStrings(JsonArray array, Set<String> target) {
        if (array == null) {
            return;
        }
        for (JsonElement element : array) {
            target.add(element.getAsString());
        }
    }

    // ================================ small conveniences ===============================

    /** Called every second by the sanity manager: ambient whispers grow with proximity to crisis. */
    public static void ambientWhisper(ServerPlayerEntity player, float sanity) {
        if (sanity > 65.0F) {
            return;
        }
        if (player.getWorld().getRandom().nextFloat() > (65.0F - sanity) / 260.0F) {
            return;
        }
        whisperTo(player, sanity < 25.0F);
        SanityManager.drain(player, 0.05F, SanityManager.Cause.WHISPER);
    }

    public static List<String> journalLines(ServerPlayerEntity player) {
        List<String> lines = new ArrayList<>();
        for (LoreEntry entry : LoreCatalog.all()) {
            if (story(player).hasDiscovered(entry.id())) {
                lines.add(entry.id());
            }
        }
        return lines;
    }

    public static void sendUnreadHint(ServerPlayerEntity player) {
        int found = story(player).discovered().size();
        player.sendMessage(Text.translatable("hollowlight.journal.progress", found, LoreCatalog.size()), true);
    }

    /** Used by the altar and by the ending sequence. */
    public static void rememberPosition(ServerPlayerEntity player, BlockPos pos) {
        String cell = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        PlayerStory story = story(player);
        if (!story.visitedCells().contains(cell) && story.visitedCells().size() < 20000) {
            story.visitedCells().add(cell);
        }
    }

    /** Placeholder hook so the story manager can be disabled wholesale from config. */
    static final class ModConfigHooks {
        static boolean enabled() {
            return true;
        }
    }
}
