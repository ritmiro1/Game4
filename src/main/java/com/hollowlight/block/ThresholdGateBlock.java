package com.hollowlight.block;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.util.DimensionTeleport;
import com.hollowlight.world.labyrinth.LabyrinthLayouts;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A threshold between two depths. Standing in it and using it carries a player one layer
 * down (or back up, if the gate links upward). The gate does not care who you are; the
 * labyrinth does.
 */
public class ThresholdGateBlock extends Block {

    /** 0 = back toward the surface, 1..5 = the layer this gate leads to. */
    public static final IntProperty LINK = IntProperty.of("link", 0, 5);

    public ThresholdGateBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(LINK, 1));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LINK);
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
        int link = state.get(LINK);
        if (link <= 0) {
            DimensionTeleport.toSurfaceShrine(serverPlayer);
        } else {
            if (!DimensionTeleport.toLayer(serverPlayer, link)) {
                serverPlayer.sendMessage(Text.translatable("hollowlight.gate.sealed"), true);
                world.playSound(null, pos, ModSounds.GATE_LOCKED, SoundCategory.BLOCKS, 0.8F, 0.6F);
                return ActionResult.CONSUME;
            }
        }
        world.playSound(null, pos, ModSounds.GATE_OPEN, SoundCategory.BLOCKS, 1.0F, 0.55F);
        LabyrinthLayouts.markGateUsed(serverPlayer, link);
        return ActionResult.CONSUME;
    }
}
