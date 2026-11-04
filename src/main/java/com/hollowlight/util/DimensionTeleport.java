package com.hollowlight.util;

import com.hollowlight.Hollowlight;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.labyrinth.LabyrinthLayouts;
import com.hollowlight.world.labyrinth.LabyrinthMapData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

/** All movement between the overworld shrine and the five layers goes through here. */
public final class DimensionTeleport {

    private DimensionTeleport() {
    }

    public static ServerWorld layerWorld(net.minecraft.server.MinecraftServer server, int layer) {
        if (server == null || layer < 1 || layer > 5) {
            return null;
        }
        return server.getWorld(LabyrinthMapData.layerKey(layer));
    }

    /** Sends a player to the landing room of a layer. Returns false if the layer is unavailable. */
    public static boolean toLayer(ServerPlayerEntity player, int layer) {
        ServerWorld world = layerWorld(player.getServer(), layer);
        if (world == null) {
            Hollowlight.LOGGER.warn("Layer {} is not loaded; is the datapack dimension present?", layer);
            return false;
        }
        BlockPos landing = LabyrinthLayouts.landingPos(world, layer);
        moveTo(player, world, landing);
        StoryManager.grant(player, "entered_layer_" + layer);
        player.sendMessage(Text.translatable("hollowlight.descend." + layer)
                .formatted(Formatting.DARK_GRAY, Formatting.ITALIC), false);
        world.playSound(null, landing, ModSounds.STONE_SETTLE, SoundCategory.AMBIENT, 1.0F, 0.6F);
        return true;
    }

    public static boolean toSurfaceShrine(ServerPlayerEntity player) {
        ServerWorld overworld = player.getServer().getOverworld();
        BlockPos target = LabyrinthLayouts.shrineExit(overworld);
        moveTo(player, overworld, target);
        StoryManager.grant(player, StoryFlags.ENTERED_SHRINE);
        player.sendMessage(Text.translatable("hollowlight.ascend.surface")
                .formatted(Formatting.GRAY, Formatting.ITALIC), false);
        return true;
    }

    public static void moveTo(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        Vec3d target = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        TeleportTarget teleportTarget = new TeleportTarget(world, target, Vec3d.ZERO,
                player.getYaw(), player.getPitch(), TeleportTarget.NO_OP);
        player.teleportTo(teleportTarget);
        player.setVelocity(0.0, 0.0, 0.0);
        player.fallDistance = 0.0F;
        world.playSound(null, pos, ModSounds.GATE_OPEN, SoundCategory.PLAYERS, 0.8F, 0.8F);
    }
}
