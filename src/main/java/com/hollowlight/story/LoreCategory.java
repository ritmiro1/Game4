package com.hollowlight.story;

/** How a fragment reached the player, which is also how the journal groups them. */
public enum LoreCategory {

    JOURNAL("journal", 1),
    LETTER("letter", 2),
    INSCRIPTION("inscription", 3),
    ROOM("room", 4),
    ITEM("item", 5),
    VOICE("voice", 6);

    private final String id;
    private final int order;

    LoreCategory(String id, int order) {
        this.id = id;
        this.order = order;
    }

    public String id() {
        return id;
    }

    public int order() {
        return order;
    }
}
