package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.labyrinth.LabyrinthLayouts;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A key is not consumed when it opens a gate. Mara cut each of these once, for one door, and
 * she never had a second one made. Unlocking is remembered per player in the story flags.
 */
public class LabyrinthKeyItem extends Item {

    private final KeyTier tier;
    private final String loreKey;

    public LabyrinthKeyItem(Settings settings, KeyTier tier, String loreKey) {
        super(settings);
        this.tier = tier;
        this.loreKey = loreKey;
    }

    public KeyTier tier() {
        return tier;
    }

    /** Called from {@link com.hollowlight.block.SealedGateBlock}: decides whether this gate listens. */
    public boolean tryUnlock(ServerPlayerEntity player, World world, BlockPos gatePos) {
        int depth = LabyrinthLayouts.layerOfWorld(world);
        if (depth < 0) {
            speakerMessage(player, "hollowlight.key.wrong_place");
            return false;
        }
        if (depth > tier.depth()) {
            speakerMessage(player, "hollowlight.key.refuses");
            return false;
        }
        StoryManager.grant(player, "key_" + tier.id() + "_used");
        world.playSound(null, gatePos, ModSounds.KEY_INSERT, SoundCategory.BLOCKS, 1.0F, 1.1F);
        return true;
    }

    private void speakerMessage(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.DARK_GRAY, Formatting.ITALIC), true);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(loreKey).formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("hollowlight.tooltip.key_depth", tier.depth()).formatted(Formatting.DARK_GRAY));
    }
}
