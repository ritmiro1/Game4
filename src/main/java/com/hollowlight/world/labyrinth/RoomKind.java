package com.hollowlight.world.labyrinth;

import java.util.List;

/**
 * The handcrafted rooms the procedural maze is built around.
 *
 * <p>Each layer is generated as a maze first and then furnished with a fixed <em>cast</em> of
 * rooms: always a landing and a way down, always at least two safe rooms, and then a mixture
 * that gives the layer its own subject matter. The rooms are the story; the maze is the
 * sentence the player has to walk to reach them.</p>
 */
public enum RoomKind {

    /** Where the player arrives, with the gate back the way they came. */
    LANDING(7, false),
    /** The way deeper. Always the farthest room from the landing. */
    DESCENT(14, false),
    /** A room that restores sanity, blocks spawns and holds a lamp and a chest. */
    SAFE_ROOM(5, true),
    /** Shelves, a lectern, and the layer's hardest-to-reach lore. */
    LORE_VAULT(7, false),
    /** Layer IV's impossible domestic rooms: a kitchen, a bedroom, a table set for three. */
    MEMORIAL_ROOM(5, true),
    /** Long shelves of salt-rotted ledgers. */
    LIBRARY(9, true),
    /** A room that tries to kill a careless player and rewards a careful one. */
    TRAP_ROOM(5, false),
    /** A slot in the wall at eye height, looking down a corridor that looks back. */
    OBSERVATION_CHAMBER(8, false),
    /** Ribbed stone, a hundred dead candles, the largest open space in the layer. */
    CHAPEL(12, true),
    /** Knee-deep water, luminous growth, and something waiting under the surface. */
    FLOODED_GARDEN(9, true),
    /** The arena. Layer V only. */
    BOSS_ARENA(22, false),
    /** The last room. Layer V only. */
    FINAL_CHAMBER(18, false);

    private final int ceilingHeight;
    private final boolean calm;

    RoomKind(int ceilingHeight, boolean calm) {
        this.ceilingHeight = ceilingHeight;
        this.calm = calm;
    }

    /** Height of the room's interior above its floor, before configuration scaling. */
    public int ceilingHeight() {
        return ceilingHeight;
    }

    /** Calm rooms suppress hostile spawning and slowly restore sanity. */
    public boolean calm() {
        return calm;
    }

    /** The cast of rooms for a layer, in placement priority order. */
    public static List<RoomKind> planFor(int layer) {
        return switch (layer) {
            case 1 -> List.of(LANDING, DESCENT, SAFE_ROOM, SAFE_ROOM, LORE_VAULT, LIBRARY, CHAPEL,
                    TRAP_ROOM, OBSERVATION_CHAMBER, SAFE_ROOM);
            case 2 -> List.of(LANDING, DESCENT, SAFE_ROOM, SAFE_ROOM, LORE_VAULT, LORE_VAULT,
                    FLOODED_GARDEN, TRAP_ROOM, OBSERVATION_CHAMBER, SAFE_ROOM);
            case 3 -> List.of(LANDING, DESCENT, SAFE_ROOM, SAFE_ROOM, LORE_VAULT, CHAPEL,
                    TRAP_ROOM, MEMORIAL_ROOM, OBSERVATION_CHAMBER, SAFE_ROOM);
            case 4 -> List.of(LANDING, DESCENT, SAFE_ROOM, SAFE_ROOM, MEMORIAL_ROOM, MEMORIAL_ROOM,
                    LIBRARY, LORE_VAULT, CHAPEL, SAFE_ROOM);
            case 5 -> List.of(LANDING, BOSS_ARENA, FINAL_CHAMBER, SAFE_ROOM, SAFE_ROOM, CHAPEL,
                    LORE_VAULT, OBSERVATION_CHAMBER);
            default -> List.of(LANDING, SAFE_ROOM);
        };
    }
}
