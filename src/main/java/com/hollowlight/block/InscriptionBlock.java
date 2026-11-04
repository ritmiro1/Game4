package com.hollowlight.block;

import com.hollowlight.story.LoreEntry;
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

import java.util.List;

/**
 * A wall inscription. The text is not stored on the block: the labyrinth layout knows which
 * inscription lives where, so the words are always the same no matter who reads them, and a
 * reader who has already found the words gets only the quiet version.
 */
public class InscriptionBlock extends Block {

    public InscriptionBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(ItemStack stack, BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        String loreId = com.hollowlight.world.labyrinth.LabyrinthLayouts.inscriptionAt(world, pos);
        if (loreId == null) {
            return ActionResult.PASS;
        }
        boolean first = StoryManager.discover(serverPlayer, loreId);
        List<Text> lines = LoreBook.displayLines(loreId);
        for (Text line : lines) {
            serverPlayer.sendMessage(line, false);
        }
        if (first) {
            LoreEntry entry = LoreBook.get(loreId);
            if (entry != null) {
                serverPlayer.sendMessage(Text.translatable("hollowlight.lore.found", entry.title()).formatted(
                        net.minecraft.util.Formatting.GRAY), false);
            }
            StoryManager.playDiscoverSound(serverPlayer);
        }
        return ActionResult.CONSUME;
    }
}
