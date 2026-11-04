package com.hollowlight.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Growth that is rendered as a cross sprite (bioluminescent moss, corrupted fibres, root
 * matting). It has no placement restrictions on purpose: the labyrinth generator plants it
 * on walls, floors and ceilings, and the player can take it home.
 */
public class TallCrossBlock extends Block {

    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 13.0, 15.0);

    public TallCrossBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected boolean isTransparent(BlockState state) {
        return true;
    }
}
