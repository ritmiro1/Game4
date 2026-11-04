package com.hollowlight.world.labyrinth;

/**
 * A room placed onto the cell grid.
 *
 * @param kind     what kind of room it is
 * @param cellX    cell-space origin X
 * @param cellZ    cell-space origin Z
 * @param cellsWide footprint in cells
 * @param cellsDeep footprint in cells
 * @param roomSeed a stable per-room seed so furniture is the same every time the room loads
 */
public record PlacedRoom(RoomKind kind, int cellX, int cellZ, int cellsWide, int cellsDeep, long roomSeed) {

    public int minBlockX() {
        return cellX * LabyrinthLayout.PITCH;
    }

    public int minBlockZ() {
        return cellZ * LabyrinthLayout.PITCH;
    }

    public int maxBlockX() {
        return (cellX + cellsWide) * LabyrinthLayout.PITCH - 1;
    }

    public int maxBlockZ() {
        return (cellZ + cellsDeep) * LabyrinthLayout.PITCH - 1;
    }

    public int centerBlockX() {
        return LabyrinthLayout.centerBlockX(this);
    }

    public int centerBlockZ() {
        return LabyrinthLayout.centerBlockZ(this);
    }

    public boolean containsBlock(int x, int z) {
        return x >= minBlockX() && x <= maxBlockX() && z >= minBlockZ() && z <= maxBlockZ();
    }
}
