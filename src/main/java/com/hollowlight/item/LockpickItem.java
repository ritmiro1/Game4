package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

/**
 * A bent piece of a mining pick. It can convince a gate that has never met you, but it breaks
 * a little every time. Used as a fallback when the player has lost a key.
 */
public class LockpickItem extends Item {

    private final Random random = Random.create();

    public LockpickItem(Settings settings) {
        super(settings);
    }

    public boolean tryPick(ServerPlayerEntity player, World world, BlockPos pos, ItemStack stack) {
        boolean success = random.nextFloat() < 0.75F;
        if (success) {
            world.playSound(null, pos, ModSounds.GATE_OPEN, SoundCategory.BLOCKS, 1.0F, 1.25F);
        } else {
            world.playSound(null, pos, ModSounds.GATE_LOCKED, SoundCategory.BLOCKS, 1.0F, 0.7F);
        }
        stack.damage(1, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
        return success;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.lockpick.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
    }
}
