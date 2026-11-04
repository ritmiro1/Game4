package com.hollowlight.block;

import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Stone that has learned a voice. Standing near it costs sanity, slowly, and the cost is
 * visible in the air around it. It is never lethal — only wearing.
 */
public class WhisperingStoneBlock extends Block {

    public WhisperingStoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.world.ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        world.playSound(null, pos, ModSounds.WHISPER, SoundCategory.AMBIENT, 0.5F, 0.85F + random.nextFloat() * 0.2F);
        world.spawnParticles(ModParticles.WHISPER_SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                4, 0.35, 0.4, 0.35, 0.01);
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 36.0
                    && player instanceof ServerPlayerEntity serverPlayer) {
                SanityManager.drain(serverPlayer, 0.12F, SanityManager.Cause.WHISPERING_STONE);
            }
        }
    }
}
