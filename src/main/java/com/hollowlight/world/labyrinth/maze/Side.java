package com.hollowlight.world.labyrinth.maze;

/** The four sides of a maze cell, in clockwise order starting north (negative Z). */
public enum Side {

    NORTH(0, -1, 0),
    EAST(1, 0, 1),
    SOUTH(0, 1, 2),
    WEST(-1, 0, 3);

    private final int dx;
    private final int dz;
    private final int bit;

    Side(int dx, int dz, int bit) {
        this.dx = dx;
        this.dz = dz;
        this.bit = bit;
    }

    public int dx() {
        return dx;
    }

    public int dz() {
        return dz;
    }

    public int bit() {
        return bit;
    }

    public Side opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public static Side byBit(int bit) {
        return switch (bit) {
            case 0 -> NORTH;
            case 1 -> EAST;
            case 2 -> SOUTH;
            default -> WEST;
        };
    }
}
