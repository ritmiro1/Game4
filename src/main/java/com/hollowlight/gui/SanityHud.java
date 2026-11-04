package com.hollowlight.gui;

import com.hollowlight.Hollowlight;
import com.hollowlight.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * The sanity readout: a small lantern glyph that dims, and a vignette that closes in.
 *
 * <p>Readability rules followed here: the vignette never exceeds 45% opacity so the world is
 * always visible, nothing flashes, nothing shakes, and the glyph is drawn in the corner where it
 * cannot obscure the crosshair or the hotbar.</p>
 */
public final class SanityHud {

    private static final Identifier TEXTURE = Hollowlight.id("textures/gui/hud.png");

    private SanityHud() {
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !ModConfig.get().showSanityHud) {
            return;
        }
        float sanity = ClientUiState.sanity();
        float twist = MathHelper.clamp(ClientUiState.twist(), 0.0F, 1.0F);

        if (twist > 0.15F) {
            int width = context.getScaledWindowWidth();
            int height = context.getScaledWindowHeight();
            int alpha = (int) (twist * 0.45F * 255.0F);
            int colour = (alpha << 24);
            int band = Math.max(24, height / 6);
            // Four gradients: heavier at the edges, absent in the middle of the screen.
            context.fillGradient(0, 0, width, band, colour | 0x0A0406, 0x000A0406);
            context.fillGradient(0, height - band, width, height, 0x000A0406, colour | 0x0A0406);
            context.fillGradient(0, 0, band, height, colour | 0x0A0406, 0x000A0406);
            context.fillGradient(width - band, 0, width, height, 0x000A0406, colour | 0x0A0406);
        }

        int x = context.getScaledWindowWidth() / 2 - 91;
        int y = context.getScaledWindowHeight() - 46;
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0.0F, 0.0F, 12, 12, 16, 16);
        int barWidth = (int) (48.0F * (sanity / 100.0F));
        context.fill(x + 14, y + 3, x + 14 + 48, y + 9, 0x66000000);
        int colour = sanity > 60.0F ? 0xFFE0B45C : sanity > 30.0F ? 0xFFB98241 : 0xFF8C3A2E;
        context.fill(x + 14, y + 3, x + 14 + barWidth, y + 9, colour);
    }
}
