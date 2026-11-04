package com.hollowlight.item;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * An improvised weapon. Hollowlight's weapons are not forged — they are the things people
 * grabbed on the way down. Each one carries a line about its owner.
 */
public class WeaponItem extends Item {

    private final String flavourKey;
    private final boolean heavy;

    public WeaponItem(Settings settings, String flavourKey, boolean heavy) {
        super(settings);
        this.flavourKey = flavourKey;
        this.heavy = heavy;
    }

    public boolean heavy() {
        return heavy;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(flavourKey).formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
