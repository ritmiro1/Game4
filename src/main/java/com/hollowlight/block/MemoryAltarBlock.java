package com.hollowlight.block;

import com.hollowlight.gui.MemoryAltarScreenHandler;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/**
 * The memory altar: the mod's crafting station. Restoring a broken belonging or binding a
 * key happens here. Opening it is intentionally slow and quiet — the altar "listens" first.
 */
public class MemoryAltarBlock extends Block {

    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 12.0, 15.0);

    public MemoryAltarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world,
                                         BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking()) {
            // Sneaking inspects the altar instead of opening it: a small lore beat.
            if (player instanceof ServerPlayerEntity serverPlayer) {
                StoryManager.grant(serverPlayer, StoryFlags.ALTAR_INSPECTED);
            }
            player.sendMessage(Text.translatable("block.hollowlight.memory_altar.hum"), true);
            world.playSound(null, pos, ModSounds.MEMORY_RETURN, SoundCategory.BLOCKS, 0.7F, 0.8F);
            return ActionResult.CONSUME;
        }
        NamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, ignored) -> new MemoryAltarScreenHandler(syncId, inventory),
                Text.translatable("container.hollowlight.memory_altar"));
        player.openHandledScreen(factory);
        world.playSound(null, pos, ModSounds.ALTAR_ACTIVATE, SoundCategory.BLOCKS, 0.8F, 1.0F);
        return ActionResult.CONSUME;
    }
}
