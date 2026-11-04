package com.hollowlight.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What the client currently knows about the player's story and sanity. Written by the packet
 * handlers, read by the screens. Deliberately tiny and read-only from the UI's point of view:
 * screens never decide what has been discovered, they only display the server's list.
 */
public final class ClientUiState {

    private static final Set<String> DISCOVERED = new LinkedHashSet<>();
    private static List<String> mapCells = new ArrayList<>();
    private static int mapLayer;
    private static float sanity = 100.0F;
    private static float twist;

    private ClientUiState() {
    }

    public static void setDiscovered(String payload) {
        DISCOVERED.clear();
        for (String line : payload.split("\n")) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            DISCOVERED.add(line);
        }
    }

    public static boolean hasDiscovered(String id) {
        return DISCOVERED.contains(id);
    }

    public static int discoveredCount() {
        return DISCOVERED.size();
    }

    public static Set<String> discovered() {
        return DISCOVERED;
    }

    public static void setMap(String payload) {
        List<String> cells = new ArrayList<>();
        int layer = 0;
        for (String part : payload.split(";")) {
            if (part.startsWith("layer=")) {
                try {
                    layer = Integer.parseInt(part.substring(6).trim());
                } catch (NumberFormatException ignored) {
                    layer = 0;
                }
            } else if (part.startsWith("visited=")) {
                String list = part.substring(8);
                if (!list.isEmpty()) {
                    for (String cell : list.split(",")) {
                        if (!cell.isEmpty()) {
                            cells.add(cell);
                        }
                    }
                }
            }
        }
        mapCells = cells;
        mapLayer = layer;
    }

    public static List<String> mapCells() {
        return mapCells;
    }

    public static int mapLayer() {
        return mapLayer;
    }

    public static void setSanity(float sanity, float twist) {
        ClientUiState.sanity = sanity;
        ClientUiState.twist = twist;
    }

    public static float sanity() {
        return sanity;
    }

    public static float twist() {
        return twist;
    }
}
