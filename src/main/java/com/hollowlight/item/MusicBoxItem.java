package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.StoryManager;
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
 * A music box with a broken comb. Restored at a memory altar, it plays four notes — the tune
 * Odren used to whistle when he came up from the shaft. Playing it in the labyrinth is the
 * loudest thing a player can do, and it makes the player feel better, which is its own kind of
 * trap: every entity within thirty blocks hears it.
 */
public class MusicBoxItem extends Item {

    public MusicBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            world.playSound(null, player.getBlockPos(), ModSounds.MUSIC_ENDING, SoundCategory.PLAYERS, 0.75F, 1.4F);
            player.sendMessage(Text.translatable("item.hollowlight.broken_music_box.plays").formatted(
                    Formatting.GRAY, Formatting.ITALIC), false);
            SanityManager.restore(player, 12.0F);
            StoryManager.discover(player, "music_box_tune");
            StoryManager.playDiscoverSound(player);
            player.getItemCooldownManager().set(stack, 400);
            com.hollowlight.horror.HorrorDirector.onLoudNoise(player, 30.0);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.broken_music_box.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
