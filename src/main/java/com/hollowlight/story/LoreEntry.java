package com.hollowlight.story;

import java.util.List;

/**
 * One discovered piece of the story. Fragments are deliberately short — three or four lines
 * that a player will read in a dark corridor with a lamp in one hand.
 *
 * @param id       stable identifier, used in save data and in the journal
 * @param category how it was found
 * @param layer    the depth it belongs to (1..5), or 0 for the surface
 * @param title    the heading shown in the journal
 * @param author   whose voice it is, or empty for anonymous inscriptions
 * @param lines    the body, one entry per displayed line/paragraph
 */
public record LoreEntry(String id, LoreCategory category, int layer, String title, String author,
                        List<String> lines) {

    public LoreEntry {
        lines = List.copyOf(lines);
    }

    /** The first line, used for one-line previews. */
    public String preview() {
        return lines.isEmpty() ? "" : lines.get(0);
    }
}
