package com.hollowlight.item;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** The three keys of the labyrinth, each cut for one depth. */
public enum KeyTier {

    RUSTED("rusted", 2, Formatting.GOLD),
    DROWNED("drowned", 3, Formatting.AQUA),
    HEART("heart", 5, Formatting.DARK_RED);

    private final String id;
    private final int depth;
    private final Formatting colour;

    KeyTier(String id, int depth, Formatting colour) {
        this.id = id;
        this.depth = depth;
        this.colour = colour;
    }

    public String id() {
        return id;
    }

    /** The layer this key was cut for. A gate deeper than the key refuses it. */
    public int depth() {
        return depth;
    }

    public Formatting colour() {
        return colour;
    }

    public Text displayName() {
        return Text.translatable("item.hollowlight." + id + "_key").formatted(colour);
    }
}
