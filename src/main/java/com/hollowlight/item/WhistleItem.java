package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.ending.EndingSequence;
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
 * Iver's tin whistle. A child's thing: two notes, both of them wrong.
 *
 * <p>Blowing it in the Heart while carrying the dust from the seventh seam is the secret
 * ending — carrying him back up instead of following her down.</p>
 */
public class WhistleItem extends Item {

    public WhistleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            world.playSound(null, player.getBlockPos(), ModSounds.ECHO_VOICE, SoundCategory.PLAYERS, 0.8F, 0.7F);
            StoryManager.discover(player, "iver_whistle_note");
            if (EndingSequence.tryThirdEnding(player)) {
                return ActionResult.SUCCESS_SERVER;
            }
            player.sendMessage(Text.translatable("item.hollowlight.iver_whistle.alone").formatted(
                    Formatting.DARK_GRAY, Formatting.ITALIC), true);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.iver_whistle.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
