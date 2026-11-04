package com.hollowlight.item;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import java.util.List;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;

/**
 * Everything the player eats, drinks, burns or inhales. Consumables are implemented directly
 * rather than through the food component so that every item can also move sanity, which is
 * the real resource of the labyrinth.
 */
public class ConsumableItem extends Item {

    private final float heal;
    private final float sanity;
    private final List<StatusEffectInstance> effects;
    private final String flavourKey;
    private final boolean drain;

    public ConsumableItem(Settings settings, float heal, float sanity, List<StatusEffectInstance> effects,
                          String flavourKey, boolean drain) {
        super(settings);
        this.heal = heal;
        this.sanity = sanity;
        this.effects = List.copyOf(effects);
        this.flavourKey = flavourKey;
        this.drain = drain;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            if (heal > 0.0F) {
                player.heal(heal);
            }
            if (sanity > 0.0F) {
                SanityManager.restore(player, sanity);
            } else if (sanity < 0.0F) {
                SanityManager.drain(player, -sanity, SanityManager.Cause.CONSUMED);
            }
            for (StatusEffectInstance effect : effects) {
                player.addStatusEffect(new StatusEffectInstance(effect));
            }
            if (drain) {
                player.getItemCooldownManager().set(this, 40);
            }
            world.playSound(null, player.getBlockPos(),
                    drain ? ModSounds.MEMORY_RETURN : ModSounds.PAGE_TURN,
                    SoundCategory.PLAYERS, 0.6F, 1.15F);
            stack.decrementUnlessCreative(1, player);
        }
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable(flavourKey).formatted(net.minecraft.util.Formatting.GRAY,
                net.minecraft.util.Formatting.ITALIC));
        if (sanity > 0.0F) {
            tooltip.add(Text.translatable("hollowlight.tooltip.sanity_gain", (int) sanity)
                    .formatted(net.minecraft.util.Formatting.DARK_AQUA));
        } else if (sanity < 0.0F) {
            tooltip.add(Text.translatable("hollowlight.tooltip.sanity_loss", (int) -sanity)
                    .formatted(net.minecraft.util.Formatting.DARK_RED));
        }
    }

    /** Convenience factory used by the registry. */
    public static StatusEffectInstance effect(net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
                                              int seconds, int amplifier) {
        return new StatusEffectInstance(effect, seconds * 20, amplifier);
    }

    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> REGEN = StatusEffects.REGENERATION;
    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> RESIST = StatusEffects.RESISTANCE;
    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> BREATH = StatusEffects.WATER_BREATHING;
    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> GRACE = StatusEffects.DOLPHINS_GRACE;
    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> SLOW = StatusEffects.SLOWNESS;
    public static final net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> NIGHT = StatusEffects.NIGHT_VISION;
}
