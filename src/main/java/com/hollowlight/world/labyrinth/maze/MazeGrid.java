package com.hollowlight.world.labyrinth.maze;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * A braided recursive-backtracking maze on a rectangular grid of square cells.
 *
 * <p>Pure logic with no Minecraft dependencies, so the generator can be reasoned about and
 * tested in isolation. Every cell is connected to every other cell by construction (the
 * carved result is a spanning tree), and braiding only ever <em>adds</em> connections, so
 * reachability can never be broken by a later step.</p>
 *
 * <p>Two properties matter for the labyrinth's feel:</p>
 * <ul>
 *   <li>the spanning tree gives long, honest dead ends where lore and loot can hide;</li>
 *   <li>braiding (opening a fraction of dead ends) removes the tedium of a perfect maze and
 *       creates the loops that make a labyrinth feel like a place instead of a puzzle.</li>
 * </ul>
 */
public final class MazeGrid {

    /** Blocks between two cell origins. */
    public static final int PITCH = 8;
    /** Blocks of open space inside a cell (the rest is wall thickness). */
    public static final int CELL_INNER = 5;
    /** Wall thickness between two adjacent cell interiors. */
    public static final int WALL_THICKNESS = PITCH - CELL_INNER;

    private final int width;
    private final int height;
    private final byte[] links;

    public MazeGrid(int width, int height, long seed, double braidChance) {
        if (width < 4 || height < 4) {
            throw new IllegalArgumentException("Maze too small: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.links = new byte[width * height];
        carve(seed);
        braid(new Random(seed ^ 0x5DEECE66DL), braidChance);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(int cx, int cz) {
        return cx >= 0 && cz >= 0 && cx < width && cz < height;
    }

    private int index(int cx, int cz) {
        return cz * width + cx;
    }

    private void carve(long seed) {
        Random random = new Random(seed);
        boolean[] visited = new boolean[width * height];
        Deque<int[]> stack = new ArrayDeque<>();

        int startX = width / 2;
        int startZ = height / 2;
        visited[index(startX, startZ)] = true;
        stack.push(new int[]{startX, startZ});

        int[] sideOrder = {0, 1, 2, 3};
        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int cx = current[0];
            int cz = current[1];

            // Fisher-Yates shuffle of the four sides, deterministic per seed.
            for (int i = sideOrder.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int tmp = sideOrder[i];
                sideOrder[i] = sideOrder[j];
                sideOrder[j] = tmp;
            }

            boolean carvedAny = false;
            for (int sideIndex : sideOrder) {
                Side side = Side.byBit(sideIndex);
                int nx = cx + side.dx();
                int nz = cz + side.dz();
                if (!contains(nx, nz) || visited[index(nx, nz)]) {
                    continue;
                }
                open(cx, cz, side);
                visited[index(nx, nz)] = true;
                stack.push(new int[]{nx, nz});
                carvedAny = true;
                break;
            }
            if (!carvedAny) {
                stack.pop();
            }
        }

        // Guarantee that the four corner cells are reachable (they always are through the
        // spanning tree, but a carved corner that only opens outward would be a trap).
        for (int cx = 0; cx < width; cx++) {
            for (int cz = 0; cz < height; cz++) {
                if (sideCount(index(cx, cz)) == 0) {
                    for (Side side : Side.values()) {
                        if (contains(cx + side.dx(), cz + side.dz())) {
                            open(cx, cz, side);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void braid(Random random, double chance) {
        for (int cx = 0; cx < width; cx++) {
            for (int cz = 0; cz < height; cz++) {
                if (sideCount(index(cx, cz)) != 1 || random.nextDouble() >= chance) {
                    continue;
                }
                List<Side> closed = new ArrayList<>(4);
                for (Side side : Side.values()) {
                    if (!isOpen(cx, cz, side) && contains(cx + side.dx(), cz + side.dz())) {
                        closed.add(side);
                    }
                }
                if (!closed.isEmpty()) {
                    open(cx, cz, closed.get(random.nextInt(closed.size())));
                }
            }
        }
    }

    public boolean isOpen(int cx, int cz, Side side) {
        return contains(cx, cz) && (links[index(cx, cz)] & (1 << side.bit())) != 0;
    }

    public void open(int cx, int cz, Side side) {
        if (!contains(cx, cz) || !contains(cx + side.dx(), cz + side.dz())) {
            return;
        }
        links[index(cx, cz)] |= (byte) (1 << side.bit());
        links[index(cx + side.dx(), cz + side.dz())] |= (byte) (1 << side.opposite().bit());
    }

    /** Forces two adjacent cells open. Used by room placement. */
    public void forceOpen(int cx, int cz, Side side) {
        open(cx, cz, side);
    }

    public void closeAll(int cx, int cz) {
        if (contains(cx, cz)) {
            links[index(cx, cz)] = 0;
        }
    }

    public int sideCount(int index) {
        return Integer.bitCount(links[index] & 0xFF);
    }

    public int sideCount(int cx, int cz) {
        return contains(cx, cz) ? sideCount(index(cx, cz)) : 0;
    }

    public boolean isDeadEnd(int cx, int cz) {
        return sideCount(cx, cz) == 1;
    }

    /**
     * Breadth-first distance map from a cell, in cells. Unreachable cells keep -1. In a
     * correctly built labyrinth nothing is unreachable, and the tests assert exactly that.
     */
    public int[] distancesFrom(int startX, int startZ) {
        int[] dist = new int[width * height];
        for (int i = 0; i < dist.length; i++) {
            dist[i] = -1;
        }
        if (!contains(startX, startZ)) {
            return dist;
        }
        Deque<Integer> queue = new ArrayDeque<>();
        dist[index(startX, startZ)] = 0;
        queue.add(index(startX, startZ));
        while (!queue.isEmpty()) {
            int current = queue.poll();
            int cx = current % width;
            int cz = current / width;
            for (Side side : Side.values()) {
                if (!isOpen(cx, cz, side)) {
                    continue;
                }
                int nx = cx + side.dx();
                int nz = cz + side.dz();
                int next = index(nx, nz);
                if (dist[next] != -1) {
                    continue;
                }
                dist[next] = dist[current] + 1;
                queue.add(next);
            }
        }
        return dist;
    }

    /** Number of cells that cannot be reached from the given cell. Must be zero. */
    public int unreachableFrom(int startX, int startZ) {
        int[] dist = distancesFrom(startX, startZ);
        int unreachable = 0;
        for (int value : dist) {
            if (value < 0) {
                unreachable++;
            }
        }
        return unreachable;
    }
}
