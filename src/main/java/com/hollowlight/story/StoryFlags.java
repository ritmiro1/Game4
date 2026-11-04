package com.hollowlight.story;

/**
 * Named events the world remembers about a player. Flags are plain strings so that new ones can
 * be added without touching save format; they gate gates, spawns, dialogue and the endings.
 */
public final class StoryFlags {

    public static final String ENTERED_SHRINE = "entered_shrine";
    public static final String ENTERED_LAYER_1 = "entered_layer_1";
    public static final String ENTERED_LAYER_2 = "entered_layer_2";
    public static final String ENTERED_LAYER_3 = "entered_layer_3";
    public static final String ENTERED_LAYER_4 = "entered_layer_4";
    public static final String ENTERED_LAYER_5 = "entered_layer_5";
    public static final String ALTAR_INSPECTED = "altar_inspected";
    public static final String IVER_FOUND = "iver_found";
    public static final String LAMPKEEPER_MET = "lampkeeper_met";
    public static final String LAMPKEEPER_DEFEATED = "lampkeeper_defeated";
    public static final String ENDING_RELEASE = "ending_release";
    public static final String ENDING_KEEP = "ending_keep";
    public static final String ENDING_RETURN = "ending_return";
    public static final String WEEPING_SPARED = "weeping_spared";
    public static final String WEEPING_KILLED = "weeping_killed";
    public static final String MUSIC_BOX_RESTORED = "music_box_restored";
    public static final String WHISTLE_FOUND = "whistle_found";

    private StoryFlags() {
    }
}
