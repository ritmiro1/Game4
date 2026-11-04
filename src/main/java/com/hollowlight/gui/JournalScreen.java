package com.hollowlight.gui;

import com.hollowlight.Hollowlight;
import com.hollowlight.story.LoreCatalog;
import com.hollowlight.story.LoreEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Mara's journal: everything the player has been told so far, filed by chapter.
 *
 * <p>Two pages, five chapters, one entry at a time. Undiscovered fragments appear as a grey
 * rule, which is the only "collectible" pressure in the mod: the journal shows how much of a
 * person's life is still missing.</p>
 */
public class JournalScreen extends Screen {

    private static final Identifier TEXTURE = Hollowlight.id("textures/gui/journal.png");
    private static final int WIDTH = 320;
    private static final int HEIGHT = 200;
    private static final String[] CHAPTER_TITLES = {
            "hollowlight.chapter.surface", "hollowlight.chapter.halls", "hollowlight.chapter.drowned",
            "hollowlight.chapter.living", "hollowlight.chapter.memorial", "hollowlight.chapter.heart"
    };

    private final List<String> visibleEntries = new ArrayList<>();
    private int chapter;
    private int selected;
    private int left;
    private int top;

    public JournalScreen() {
        super(Text.translatable("gui.hollowlight.journal.title"));
    }

    @Override
    protected void init() {
        this.left = (this.width - WIDTH) / 2;
        this.top = (this.height - HEIGHT) / 2;
        rebuildEntries();

        int buttonY = this.top + HEIGHT - 22;
        for (int index = 0; index < CHAPTER_TITLES.length; index++) {
            final int target = index;
            this.addDrawableChild(ButtonWidget.builder(Text.translatable(CHAPTER_TITLES[index]),
                            button -> {
                                this.chapter = target;
                                this.selected = 0;
                                rebuildEntries();
                            })
                    .dimensions(this.left + 12 + index * 49, buttonY, 46, 16)
                    .build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.hollowlight.close"),
                        button -> this.close())
                .dimensions(this.left + WIDTH - 56, this.top + 6, 50, 14)
                .build());
    }

    private void rebuildEntries() {
        visibleEntries.clear();
        for (LoreEntry entry : LoreCatalog.all()) {
            if (entry.layer() != this.chapter) {
                continue;
            }
            if (entry.category() == com.hollowlight.story.LoreCategory.VOICE
                    && !ClientUiState.hasDiscovered(entry.id())) {
                continue;
            }
            visibleEntries.add(entry.id());
        }
        this.selected = Math.max(0, Math.min(this.selected, visibleEntries.size() - 1));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawTexture(TEXTURE, this.left, this.top, 0.0F, 0.0F, WIDTH, HEIGHT, 320, 200);
        context.drawText(this.textRenderer, this.title, this.left + 12, this.top + 9, 0xE8DCC0, false);

        int found = ClientUiState.discoveredCount();
        int total = LoreCatalog.size();
        String progress = found + " / " + total + " " + Text.translatable("gui.hollowlight.fragments").getString();
        context.drawText(this.textRenderer, progress, this.left + 118, this.top + 9, 0x8C7B5E, false);

        renderEntryList(context);
        renderEntryBody(context);
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderEntryList(DrawContext context) {
        int listX = this.left + 14;
        int listY = this.top + 28;
        for (int index = 0; index < visibleEntries.size() && index < 9; index++) {
            String id = visibleEntries.get(index);
            boolean discovered = ClientUiState.hasDiscovered(id);
            LoreEntry entry = LoreCatalog.get(id);
            String title = discovered && entry != null
                    ? this.textRenderer.trimToWidth(entry.title(), 128)
                    : "- - - - - -";
            int colour = index == this.selected ? 0xF2E3BE : discovered ? 0xB6A483 : 0x5B5142;
            if (index == this.selected) {
                context.fill(listX - 3, listY + index * 16 - 2, listX + 136, listY + index * 16 + 12, 0x33F2E3BE);
            }
            context.drawText(this.textRenderer, title, listX, listY + index * 16, colour, false);
        }
        if (visibleEntries.isEmpty()) {
            context.drawText(this.textRenderer, Text.translatable("gui.hollowlight.empty"),
                    listX, listY, 0x6E634F, false);
        }
    }

    private void renderEntryBody(DrawContext context) {
        int bodyX = this.left + 164;
        int bodyY = this.top + 28;
        if (visibleEntries.isEmpty()) {
            return;
        }
        if (this.selected >= visibleEntries.size()) {
            this.selected = 0;
        }
        String id = visibleEntries.get(this.selected);
        LoreEntry entry = LoreCatalog.get(id);
        if (entry == null) {
            return;
        }
        if (!ClientUiState.hasDiscovered(id)) {
            context.drawWrappedText(this.textRenderer, Text.translatable("gui.hollowlight.undiscovered"),
                    bodyX, bodyY, 140, 0x6E634F, false);
            return;
        }
        context.drawText(this.textRenderer, Text.literal(entry.title()).formatted(Formatting.BOLD),
                bodyX, bodyY, 0xF2E3BE, false);
        String author = entry.author().isEmpty() ? "" : entry.author();
        if (!author.isEmpty()) {
            context.drawText(this.textRenderer, Text.literal(author),
                    bodyX, bodyY + 11, 0x9C8A6A, false);
        }
        int y = bodyY + 26;
        for (String line : entry.lines()) {
            List<net.minecraft.text.OrderedText> wrapped = this.textRenderer.wrapLines(
                    Text.literal(line), 138);
            for (net.minecraft.text.OrderedText ordered : wrapped) {
                context.drawText(this.textRenderer, ordered, bodyX, y, 0xC9B994, false);
                y += 10;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int listX = this.left + 14;
        int listY = this.top + 28;
        for (int index = 0; index < visibleEntries.size() && index < 9; index++) {
            if (mouseX >= listX - 3 && mouseX <= listX + 136
                    && mouseY >= listY + index * 16 - 2 && mouseY <= listY + index * 16 + 12) {
                this.selected = index;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
