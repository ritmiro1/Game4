package com.hollowlight.item;

import com.hollowlight.registry.ModPayloads;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/** Mara's journal — a book of everything the player has been told so far. */
public class JournalItem extends Item {

    public JournalItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            ModPayloads.openJournal(player);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.lamplighters_journal.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("hollowlight.tooltip.open_journal").formatted(Formatting.DARK_GRAY));
    }
}
