package com.hollowlight.item;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * The lamplighter's lantern — the only starting item that matters.
 *
 * <p>Used on a block it sets a lamp down (a permanent light in the world). Used in the air it
 * keeps the flame in the hand: a small pocket of warmth that slowly pushes sanity back up and
 * keeps the dark from touching the player's eyes. Every use burns oil, and the lantern
 * remembers how much is left in its own damage bar.</p>
 */
public class LanternItem extends Item {

    public static final int OIL_PER_USE = 1;

    public LanternItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (stack.getDamage() >= stack.getMaxDamage() - OIL_PER_USE) {
            if (!world.isClient()) {
                user.sendMessage(Text.translatable("item.hollowlight.lamplighter_lantern.empty").formatted(Formatting.DARK_GRAY), true);
                world.playSound(null, user.getBlockPos(), ModSounds.LANTERN_EMPTY, SoundCategory.PLAYERS, 0.6F, 0.9F);
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            BlockHitResult hit = (BlockHitResult) player.raycast(5.0, 0.0F, false);
            boolean placed = false;
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos target = hit.getBlockPos().offset(hit.getSide());
                if (player.getWorld().getBlockState(target).isAir()
                        && ModBlocks.HOLLOW_LANTERN.getDefaultState().canPlaceAt(player.getWorld(), target)) {
                    player.getWorld().setBlockState(target, ModBlocks.HOLLOW_LANTERN.getDefaultState());
                    placed = true;
                    world.playSound(null, target, ModSounds.LANTERN_LIGHT, SoundCategory.BLOCKS, 0.9F, 1.0F);
                }
            }
            if (!placed) {
                player.addStatusEffect(new StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION, 900, 0, true, false, false));
                SanityManager.restore(player, 3.0F);
                world.playSound(null, player.getBlockPos(), ModSounds.LANTERN_LIGHT, SoundCategory.PLAYERS, 0.5F, 1.2F);
            } else {
                SanityManager.restore(player, 1.0F);
            }
            stack.damage(OIL_PER_USE, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.lamplighter_lantern.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.hollowlight.lamplighter_lantern.use").formatted(Formatting.DARK_GRAY));
        int oil = stack.getMaxDamage() - stack.getDamage();
        tooltip.add(Text.translatable("item.hollowlight.lamplighter_lantern.oil", oil).formatted(Formatting.GOLD));
    }
}
