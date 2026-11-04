package com.hollowlight.gui;

import com.hollowlight.Hollowlight;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The cracked lens: a map that only ever shows ground the player has already walked.
 *
 * <p>Cells are 8x8 blocks, so the whole layer fits on one page as a 40x40 grid. The player's
 * current cell is marked. Nothing here is generated client-side: the server sends the set of
 * visited cells and the screen draws them.</p>
 */
public class LabyrinthMapScreen extends Screen {

    private static final Identifier TEXTURE = Hollowlight.id("textures/gui/map.png");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 232;
    private static final int GRID = 40;
    private static final int CELL_PIXELS = 4;

    public LabyrinthMapScreen() {
        super(Text.translatable("gui.hollowlight.map.title"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        context.drawTexture(TEXTURE, left, top, 0.0F, 0.0F, WIDTH, HEIGHT, 256, 232);
        context.drawText(this.textRenderer, this.title, left + 12, top + 9, 0xE8DCC0, false);

        int layer = ClientUiState.mapLayer();
        Text layerName = layer >= 1 && layer <= 5
                ? Text.translatable("hollowlight.layer." + layer)
                : Text.translatable("gui.hollowlight.map.surface");
        context.drawText(this.textRenderer, layerName, left + 12, top + 22, 0x9C8A6A, false);

        int gridLeft = left + (WIDTH - GRID * CELL_PIXELS) / 2;
        int gridTop = top + 40;

        // Frame
        context.fill(gridLeft - 2, gridTop - 2, gridLeft + GRID * CELL_PIXELS + 2,
                gridTop + GRID * CELL_PIXELS + 2, 0xFF171310);

        Set<Long> visited = new HashSet<>();
        for (String cell : ClientUiState.mapCells()) {
            String[] parts = cell.split(":");
            if (parts.length != 3) {
                continue;
            }
            try {
                int cellLayer = Integer.parseInt(parts[0]);
                if (cellLayer != layer) {
                    continue;
                }
                int cx = Integer.parseInt(parts[1]);
                int cz = Integer.parseInt(parts[2]);
                if (cx < 0 || cz < 0 || cx >= GRID || cz >= GRID) {
                    continue;
                }
                visited.add((long) cx * GRID + cz);
            } catch (NumberFormatException ignored) {
                // Malformed cells are silently skipped: the map must never crash the game.
            }
        }

        for (long key : visited) {
            int cx = (int) (key / GRID);
            int cz = (int) (key % GRID);
            int x = gridLeft + cx * CELL_PIXELS;
            int y = gridTop + cz * CELL_PIXELS;
            context.fill(x, y, x + CELL_PIXELS - 1, y + CELL_PIXELS - 1, 0xFF6E6247);
        }

        // Player marker: the newest cell is always the one the player stands in.
        for (String cell : ClientUiState.mapCells()) {
            String[] parts = cell.split(":");
            if (parts.length != 3) {
                continue;
            }
            try {
                if (Integer.parseInt(parts[0]) != layer) {
                    continue;
                }
            } catch (NumberFormatException ignored) {
                continue;
            }
        }
        if (!ClientUiState.mapCells().isEmpty()) {
            String[] parts = ClientUiState.mapCells().get(ClientUiState.mapCells().size() - 1).split(":");
            if (parts.length == 3) {
                try {
                    int cx = Integer.parseInt(parts[1]);
                    int cz = Integer.parseInt(parts[2]);
                    if (cx >= 0 && cz >= 0 && cx < GRID && cz < GRID) {
                        int x = gridLeft + cx * CELL_PIXELS;
                        int y = gridTop + cz * CELL_PIXELS;
                        context.fill(x, y, x + CELL_PIXELS - 1, y + CELL_PIXELS - 1, 0xFFF2E3BE);
                    }
                } catch (NumberFormatException ignored) {
                    // ignore
                }
            }
        }

        context.drawText(this.textRenderer, Text.translatable("gui.hollowlight.map.footer",
                        visited.size(), GRID * GRID), left + 12, top + HEIGHT - 20, 0x6E634F, false);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
