package com.hollowlight.story;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Everything the world remembers about one player, serialised to JSON between sessions.
 *
 * <p>Kept deliberately simple: a set of discovered fragment ids, a set of event flags, the
 * sanity value, the cells the player has walked in each layer (for the lens map), and the
 * ending they chose. No codecs, no version-specific NBT — see {@link StoryManager}.</p>
 */
public final class PlayerStory {

    private final Set<String> discovered = new LinkedHashSet<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final List<String> visitedCells = new ArrayList<>();
    private float sanity = 100.0F;
    private String ending = "";

    public Set<String> discovered() {
        return discovered;
    }

    public Set<String> flags() {
        return flags;
    }

    public List<String> visitedCells() {
        return visitedCells;
    }

    public float sanity() {
        return sanity;
    }

    public void setSanity(float sanity) {
        this.sanity = Math.max(0.0F, Math.min(100.0F, sanity));
    }

    public String ending() {
        return ending;
    }

    public void setEnding(String ending) {
        this.ending = ending == null ? "" : ending;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public boolean addFlag(String flag) {
        return flags.add(flag);
    }

    public boolean hasDiscovered(String id) {
        return discovered.contains(id);
    }

    public boolean discover(String id) {
        return discovered.add(id);
    }

    /** Progress through the readable fragments, 0..1, for the journal header. */
    public float progress() {
        int total = LoreCatalog.size();
        return total == 0 ? 0.0F : Math.min(1.0F, discovered.size() / (float) total);
    }
}
