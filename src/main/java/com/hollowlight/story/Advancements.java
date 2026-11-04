package com.hollowlight.story;

import com.hollowlight.Hollowlight;
import java.util.Map;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Bridges gameplay events to the advancement tree.
 *
 * <p>All Hollowlight advancements use the {@code minecraft:impossible} trigger and are granted
 * from code, which keeps the conditions in one readable place instead of spread across two
 * dozen JSON files. Advancement ids are grouped under {@code hollowlight:story/} and
 * {@code hollowlight:depth/}.</p>
 */
public final class Advancements {

    /** Fragment id → advancement id (without namespace). */
    private static final Map<String, String> LORE_TRIGGERS = Map.ofEntries(
            Map.entry("shrine_dedication", "story/where_the_lamp_ends"),
            Map.entry("page_the_first_descent", "story/nine_names"),
            Map.entry("page_the_lamps", "story/a_lamp_is_a_promise"),
            Map.entry("page_first_gift", "story/the_first_trade"),
            Map.entry("page_the_bargain", "story/what_it_wants"),
            Map.entry("page_what_i_gave_next", "story/the_sound_of_his_laugh"),
            Map.entry("page_she_gave_her_voice", "story/four_trades"),
            Map.entry("page_a_kitchen_underground", "story/a_window_underground"),
            Map.entry("iver_first_carving", "story/i_am_here"),
            Map.entry("iver_second_carving", "story/you_walked_past"),
            Map.entry("iver_last_carving", "story/it_was_never_your_fault"),
            Map.entry("page_the_last_lamp", "story/the_last_entry"),
            Map.entry("lampkeeper_whisper", "story/she_is_asking_you"),
            Map.entry("memorial_room_bed", "story/two_pillows"),
            Map.entry("choir_note", "story/a_list_of_names"),
            Map.entry("tally_marks", "story/every_day_counted"));

    /** Story flag → advancement id (without namespace). */
    private static final Map<String, String> FLAG_TRIGGERS = Map.ofEntries(
            Map.entry(StoryFlags.ENTERED_SHRINE, "depth/the_dedication_stone"),
            Map.entry(StoryFlags.ENTERED_LAYER_1, "depth/the_forgotten_halls"),
            Map.entry(StoryFlags.ENTERED_LAYER_2, "depth/the_drowned_corridors"),
            Map.entry(StoryFlags.ENTERED_LAYER_3, "depth/the_living_walls"),
            Map.entry(StoryFlags.ENTERED_LAYER_4, "depth/the_memorial_depths"),
            Map.entry(StoryFlags.ENTERED_LAYER_5, "depth/the_heart"),
            Map.entry(StoryFlags.IVER_FOUND, "story/somebody_was_looking"),
            Map.entry(StoryFlags.LAMPKEEPER_MET, "story/she_does_not_know_you"),
            Map.entry(StoryFlags.LAMPKEEPER_DEFEATED, "story/the_quiet_after"),
            Map.entry(StoryFlags.ENDING_RELEASE, "ending/extinguishing"),
            Map.entry(StoryFlags.ENDING_KEEP, "ending/keeping"),
            Map.entry(StoryFlags.ENDING_RETURN, "ending/carrying_him_up"),
            Map.entry(StoryFlags.WEEPING_SPARED, "hidden/you_did_not_have_to"),
            Map.entry(StoryFlags.WEEPING_KILLED, "hidden/a_thing_that_only_cried"),
            Map.entry(StoryFlags.MUSIC_BOX_RESTORED, "hidden/four_notes"),
            Map.entry(StoryFlags.WHISTLE_FOUND, "hidden/two_notes_both_wrong"));

    private Advancements() {
    }

    public static void onLoreDiscovered(ServerPlayerEntity player, LoreEntry entry) {
        String advancement = LORE_TRIGGERS.get(entry.id());
        if (advancement != null) {
            grant(player, advancement);
        }
    }

    public static void onFlag(ServerPlayerEntity player, String flag) {
        String advancement = FLAG_TRIGGERS.get(flag);
        if (advancement != null) {
            grant(player, advancement);
        }
    }

    /** Grants an advancement by path, e.g. {@code "story/nine_names"}. Safe if absent. */
    public static void grant(ServerPlayerEntity player, String path) {
        if (player.getServer() == null) {
            return;
        }
        Identifier id = Hollowlight.id(path);
        AdvancementEntry entry = player.getServer().getAdvancementLoader().get(id);
        if (entry == null) {
            Hollowlight.LOGGER.debug("Advancement {} is not present in this build", id);
            return;
        }
        player.getAdvancementTracker().grantCriterion(entry, "code");
    }
}
