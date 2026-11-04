package com.hollowlight.item;

import com.hollowlight.registry.ModItems;
import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModSounds;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The wickbow does not fire arrows. It fires a thin thread of lamplight that travels instantly
 * and burns whatever it touches — and for a moment, marks where you are in the dark.
 *
 * <p>Implemented as a hitscan so that it stays reliable in narrow labyrinth corridors where a
 * physics projectile would clip into walls. It consumes glass bolts.</p>
 */
public class WickbowItem extends Item {

    private static final double RANGE = 24.0;
    private static final float DAMAGE = 5.0F;

    public WickbowItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return ActionResult.PASS;
        }
        boolean hasBolt = user.getAbilities().creativeMode
                || user.getInventory().contains(new ItemStack(ModItems.GLASS_BOLT))
                || stack == user.getStackInHand(hand) && user.getOffHandStack().isOf(ModItems.GLASS_BOLT);
        if (!hasBolt) {
            if (!world.isClient()) {
                user.sendMessage(Text.translatable("item.hollowlight.wickbow.empty").formatted(Formatting.DARK_GRAY), true);
                world.playSound(null, user.getBlockPos(), ModSounds.LANTERN_EMPTY, SoundCategory.PLAYERS, 0.5F, 1.4F);
            }
            return ActionResult.FAIL;
        }

        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            consumeBolt(player);
            player.getItemCooldownManager().set(this, 14);
            world.playSound(null, player.getBlockPos(), ModSounds.LANTERN_LIGHT, SoundCategory.PLAYERS, 0.7F, 1.6F);

            Vec3d start = player.getEyePos();
            Vec3d direction = player.getRotationVec(1.0F);
            HitResult hit = player.getWorld().raycast(new net.minecraft.world.RaycastContext(start,
                            start.add(direction.multiply(RANGE)), net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE, player));
            Vec3d end = hit.getType() == HitResult.Type.MISS ? start.add(direction.multiply(RANGE)) : hit.getPos();

            if (world instanceof ServerWorld serverWorld) {
                // Trace for the first living thing along the thread of light.
                Entity victim = null;
                double best = Double.MAX_VALUE;
                for (Entity candidate : serverWorld.getOtherEntities(player, player.getBoundingBox().expand(RANGE))) {
                    if (!(candidate instanceof LivingEntity living) || candidate == player) {
                        continue;
                    }
                    var box = living.getBoundingBox().expand(0.4);
                    var intersection = box.raycast(start, end);
                    if (intersection.isPresent()) {
                        double distance = start.squaredDistanceTo(intersection.get());
                        if (distance < best) {
                            best = distance;
                            victim = living;
                        }
                    }
                }
                if (victim instanceof LivingEntity living) {
                    end = living.getPos().add(0.0, living.getHeight() * 0.5, 0.0);
                    living.damage(serverWorld.getDamageSources().indirectMagic(player, player), DAMAGE);
                    living.setOnFireFor(2);
                }
                for (int i = 0; i <= 40; i++) {
                    Vec3d point = start.lerp(end, i / 40.0);
                    serverWorld.spawnParticles(ModParticles.MEMORY_GLINT, point.x, point.y, point.z, 1, 0.02, 0.02, 0.02, 0.0);
                    if (i % 4 == 0) {
                        serverWorld.spawnParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
                    }
                }
            }
        }
        user.swingHand(hand, true);
        return (world.isClient() ? ActionResult.SUCCESS : ActionResult.SUCCESS_SERVER);
    }

    private void consumeBolt(ServerPlayerEntity player) {
        if (player.getAbilities().creativeMode) {
            return;
        }
        if (player.getOffHandStack().isOf(ModItems.GLASS_BOLT)) {
            player.getOffHandStack().decrement(1);
            return;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack candidate = player.getInventory().getStack(slot);
            if (candidate.isOf(ModItems.GLASS_BOLT)) {
                candidate.decrement(1);
                return;
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.hollowlight.wickbow.flavour").formatted(Formatting.GRAY, Formatting.ITALIC));
        tooltip.add(Text.translatable("item.hollowlight.wickbow.ammo").formatted(Formatting.DARK_GRAY));
    }
}
