package com.hollowlight.block;

import com.hollowlight.story.LoreBook;
import com.hollowlight.story.StoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Marks scratched into stone by someone counting the days. */
public class TallyMarksBlock extends Block {

    private static final String LORE_ID = "tally_marks";

    public TallyMarksBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(ItemStack stack, BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            for (Text line : LoreBook.displayLines(LORE_ID)) {
                serverPlayer.sendMessage(line, false);
            }
            if (StoryManager.discover(serverPlayer, LORE_ID)) {
                StoryManager.playDiscoverSound(serverPlayer);
            }
        }
        return ActionResult.CONSUME;
    }
}
