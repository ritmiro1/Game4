package com.hollowlight.item;

import com.hollowlight.sanity.SanityManager;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * An artifact: carried, not used. While it is in the player's inventory it changes how the
 * labyrinth touches them.
 */
public class ArtifactItem extends Item {

    public enum Kind {
        /** Steadies the mind: slows sanity loss. */
        ANCHOR,
        /** Remembers for you: slowly restores sanity in darkness. */
        KEEPER,
        /** The last lamp: the ending item. */
        FINAL
    }

    private final Kind kind;
    private final String flavourKey;

    public ArtifactItem(Settings settings, Kind kind, String flavourKey) {
        super(settings);
        this.kind = kind;
        this.flavourKey = flavourKey;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient() || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (world.getTime() % 100L != 0L) {
            return;
        }
        switch (kind) {
            case ANCHOR -> SanityManager.restore(player, 0.6F);
            case KEEPER -> {
                long time = world.getTimeOfDay() % 24000L;
                boolean dark = player.getWorld().getLightLevel(
                        net.minecraft.world.LightType.BLOCK, player.getBlockPos()) < 4;
                if (dark) {
                    SanityManager.restore(player, 0.8F);
                }
            }
            default -> {
                // The final item does nothing while carried; it is only what it is.
            }
        }
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(flavourKey).formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
