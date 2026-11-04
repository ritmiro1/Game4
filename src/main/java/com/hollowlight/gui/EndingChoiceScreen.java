package com.hollowlight.gui;

import com.hollowlight.Hollowlight;
import com.hollowlight.registry.ModPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The last three choices. It is deliberately the plainest screen in the mod: by the time it
 * appears the player should not be looking at interface design, they should be deciding
 * something.
 */
public class EndingChoiceScreen extends Screen {

    private static final Identifier TEXTURE = Hollowlight.id("textures/gui/ending.png");
    private static final int WIDTH = 280;
    private static final int HEIGHT = 180;

    public EndingChoiceScreen() {
        super(Text.translatable("gui.hollowlight.ending.title"));
    }

    @Override
    protected void init() {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.hollowlight.ending.release"),
                        button -> choose("release"))
                .dimensions(left + 20, top + 70, 240, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.hollowlight.ending.keep"),
                        button -> choose("keep"))
                .dimensions(left + 20, top + 96, 240, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.hollowlight.ending.return"),
                        button -> choose("return"))
                .dimensions(left + 20, top + 122, 240, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.hollowlight.ending.wait"),
                        button -> this.close())
                .dimensions(left + 20, top + 148, 240, 18).build());
    }

    private void choose(String choice) {
        ClientPlayNetworking.send(new ModPayloads.EndingChoicePayload(choice));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        context.drawTexture(TEXTURE, left, top, 0.0F, 0.0F, WIDTH, HEIGHT, 280, 180);
        context.drawWrappedText(this.textRenderer, Text.translatable("gui.hollowlight.ending.body"),
                left + 20, top + 18, WIDTH - 40, 0xC9B994, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
