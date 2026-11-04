package com.hollowlight.item;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A flask of bottled lamplight. Thrown at the floor it shatters into a small sun that lasts
 * until something eats it. This is the player's emergency light — the thing they reach for
 * when the lantern is out and they can hear breathing.
 */
public class FlaskItem extends Item {

    public FlaskItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            BlockHitResult hit = (BlockHitResult) player.raycast(8.0, 0.0F, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos target = hit.getBlockPos().offset(hit.getSide());
                if (player.getWorld().getBlockState(target).isAir()) {
                    player.getWorld().setBlockState(target, ModBlocks.LAMP_GLASS.getDefaultState());
                    if (player.getWorld() instanceof ServerWorld serverWorld) {
                        serverWorld.spawnParticles(ModParticles.PALE_MOTE, target.getX() + 0.5, target.getY() + 0.5,
                                target.getZ() + 0.5, 24, 0.4, 0.4, 0.4, 0.02);
                    }
                    SanityManager.restore(player, 2.0F);
                }
            }
            player.getWorld().playSound(null, player.getBlockPos(), ModSounds.LANTERN_LIGHT,
                    SoundCategory.PLAYERS, 0.9F, 1.35F);
            stack.decrementUnlessCreative(1, player);
            player.getItemCooldownManager().set(this, 20);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.luminous_flask.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
