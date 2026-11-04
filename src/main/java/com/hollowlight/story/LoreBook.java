package com.hollowlight.story;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Turns {@link LoreEntry} text into chat lines and journal widgets. */
public final class LoreBook {

    private LoreBook() {
    }

    public static LoreEntry get(String id) {
        return LoreCatalog.get(id);
    }

    public static List<Text> displayLines(String id) {
        LoreEntry entry = LoreCatalog.get(id);
        if (entry == null) {
            return List.of(Text.literal("...").formatted(Formatting.DARK_GRAY));
        }
        List<Text> out = new ArrayList<>();
        String header = entry.author().isEmpty() ? entry.title() : entry.title() + " — " + entry.author();
        out.add(Text.literal(header).formatted(Formatting.GOLD, Formatting.ITALIC));
        for (String line : entry.lines()) {
            MutableText text = Text.literal(line).formatted(Formatting.GRAY);
            out.add(text);
        }
        return out;
    }

    public static Text title(String id) {
        LoreEntry entry = LoreCatalog.get(id);
        return entry == null ? Text.literal(id) : Text.literal(entry.title());
    }

    public static Text categoryLabel(LoreCategory category) {
        return Text.translatable("hollowlight.lore.category." + category.id());
    }
}
