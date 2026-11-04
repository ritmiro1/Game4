package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.registry.ModPayloads;
import com.hollowlight.world.labyrinth.LabyrinthMapData;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * The cracked lens: a mapping tool. It draws what the player has already walked, never what
 * they have not. Looking through it in the dark is slower, and the glass fogs at low sanity
 * (client side), which is a warning in itself.
 */
public class LensItem extends Item {

    public LensItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            ModPayloads.openLens(player);
            world.playSound(null, player.getBlockPos(), ModSounds.PAGE_TURN, SoundCategory.PLAYERS, 0.5F, 1.3F);
            LabyrinthMapData.rememberCurrentCell(player);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.cracked_lens.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("hollowlight.tooltip.open_map").formatted(Formatting.DARK_GRAY));
    }
}
