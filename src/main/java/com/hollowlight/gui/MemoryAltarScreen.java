package com.hollowlight.gui;

import com.hollowlight.Hollowlight;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The memory altar interface. Three sockets, one result, and a lot of dark stone.
 *
 * <p>Left of the sockets is the altar's own light; right of them is the place the result sits.
 * The background is a hand-drawn 176x166 texture, and the whole screen is written so that it
 * draws correctly at GUI scale 1, 2, 3 and auto: nothing is positioned relative to the font
 * size, only to the panel.</p>
 */
public class MemoryAltarScreen extends HandledScreen<MemoryAltarScreenHandler> {

    private static final Identifier TEXTURE = Hollowlight.id("textures/gui/altar.png");

    public MemoryAltarScreen(MemoryAltarScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 8;
        this.titleY = 6;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
