package com.hollowlight.block;

import com.hollowlight.item.LabyrinthKeyItem;
import com.hollowlight.registry.ModSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

/**
 * A gate that answers only to the right key. Keys are not consumed: once a gate has been
 * opened the labyrinth remembers, and it stays open for that player (tracked in story flags).
 */
public class SealedGateBlock extends Block {

    public static final BooleanProperty OPEN = Properties.OPEN;

    public SealedGateBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(OPEN, Boolean.FALSE));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.BlockView world,
                                           BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (state.get(OPEN)) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        boolean unlocked = stack.getItem() instanceof LabyrinthKeyItem key
                && player instanceof ServerPlayerEntity serverPlayer
                && key.tryUnlock(serverPlayer, world, pos);

        if (unlocked) {
            world.setBlockState(pos, state.with(OPEN, Boolean.TRUE), Block.NOTIFY_ALL);
            world.playSound(null, pos, ModSounds.GATE_OPEN, SoundCategory.BLOCKS, 1.0F, 0.7F);
            player.sendMessage(Text.translatable("block.hollowlight.sealed_gate.opens"), true);
            return ActionResult.CONSUME;
        }
        world.playSound(null, pos, ModSounds.GATE_LOCKED, SoundCategory.BLOCKS, 0.9F, 0.9F);
        player.sendMessage(Text.translatable("block.hollowlight.sealed_gate.locked"), true);
        return ActionResult.CONSUME;
    }
}
