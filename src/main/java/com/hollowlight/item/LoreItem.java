package com.hollowlight.item;

import com.hollowlight.story.LoreBook;
import com.hollowlight.story.StoryManager;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * A physical piece of the story: a page, a letter, a photograph, a token. Using it in the
 * world reads it aloud into the chat and files it in the journal. Reading the same page twice
 * is allowed — grief is repetitive like that — but it only counts once.
 */
public class LoreItem extends Item {

    private final String loreId;
    private final String tooltipKey;

    public LoreItem(Settings settings, String loreId, String tooltipKey) {
        super(settings);
        this.loreId = loreId;
        this.tooltipKey = tooltipKey;
    }

    public String loreId() {
        return loreId;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            boolean first = StoryManager.discover(player, loreId);
            for (Text line : LoreBook.displayLines(loreId)) {
                player.sendMessage(line, false);
            }
            if (first) {
                StoryManager.playDiscoverSound(player);
            } else {
                player.sendMessage(Text.translatable("hollowlight.lore.reread").formatted(Formatting.DARK_GRAY,
                        Formatting.ITALIC), false);
            }
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("hollowlight.tooltip.read").formatted(Formatting.DARK_GRAY));
        if (tooltipKey != null) {
            tooltip.add(Text.translatable(tooltipKey).formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        tooltip.add(Text.translatable("hollowlight.tooltip.journal_hint").formatted(Formatting.DARK_GRAY,
                Formatting.ITALIC));
    }
}
