package com.hollowlight.entity;

import com.hollowlight.registry.ModItems;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Sorrow — a woman-shaped thing that kneels in corners and cries, and has never once raised
 * a hand to anybody.
 *
 * <p>It is the one entity in the mod that is more sad than frightening. It does not fight. It
 * avoids the player. Killing it is possible, and the mod notices: the act costs sanity, changes
 * an advancement, and quietly rewrites a line of the epilogue. Leaving it alone is the only
 * "quest" in Hollowlight that is completed by walking past.</p>
 */
public class WeepingOneEntity extends PathAwareEntity {

    private int kneelingTicks;

    public WeepingOneEntity(EntityType<? extends WeepingOneEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.kneelingTicks = 40 + world.getRandom().nextInt(120);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.16)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new EscapeDangerGoal(this, 1.6));
        this.goalSelector.add(2, new WanderAroundGoal(this, 0.4));
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 16.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
    }

    public boolean isKneeling() {
        return this.kneelingTicks > 0 || this.getVelocity().horizontalLengthSquared() < 0.001;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            if (this.isKneeling() && this.age % 7 == 0) {
                this.getWorld().addParticle(ParticleTypes.FALLING_WATER,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.4,
                        this.getY() + 0.9,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.4,
                        0.0, -0.02, 0.0);
            }
            return;
        }
        if (this.kneelingTicks > 0) {
            this.kneelingTicks--;
            this.getNavigation().stop();
        }
        if (this.age % 60 == 0) {
            this.playSound(ModSounds.WEEPING_CRY, 0.6F, 0.95F + this.random.nextFloat() * 0.1F);
            for (PlayerEntity player : this.getWorld().getPlayers()) {
                if (player.squaredDistanceTo(this) < 144.0 && player instanceof ServerPlayerEntity serverPlayer) {
                    // Being near grief is tiring.
                    SanityManager.drain(serverPlayer, 0.3F, SanityManager.Cause.ENTITY);
                    if (this.getWorld().getRandom().nextFloat() < 0.15F) {
                        serverPlayer.sendMessage(net.minecraft.text.Text.translatable(
                                "hollowlight.weeping.notice").formatted(net.minecraft.util.Formatting.DARK_GRAY,
                                net.minecraft.util.Formatting.ITALIC), true);
                    }
                }
            }
        }
        if (this.age % 200 == 0 && this.getWorld().getRandom().nextFloat() < 0.15F) {
            this.kneelingTicks = 60 + this.random.nextInt(200);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        if (damaged && source.getAttacker() instanceof ServerPlayerEntity player) {
            SanityManager.drain(player, 2.5F, SanityManager.Cause.ENTITY);
            player.sendMessage(net.minecraft.text.Text.translatable("hollowlight.weeping.struck")
                    .formatted(net.minecraft.util.Formatting.DARK_RED, net.minecraft.util.Formatting.ITALIC), true);
        }
        return damaged;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (this.getWorld() instanceof ServerWorld world) {
            for (int i = 0; i < 3; i++) {
                ItemEntity drop = new ItemEntity(world, this.getX(), this.getY() + 0.5, this.getZ(),
                        new ItemStack(ModItems.MEMORY_SHARD));
                world.spawnEntity(drop);
            }
            if (source.getAttacker() instanceof ServerPlayerEntity player) {
                StoryManager.grant(player, StoryFlags.WEEPING_KILLED);
                SanityManager.drain(player, 12.0F, SanityManager.Cause.ENTITY);
                player.sendMessage(net.minecraft.text.Text.translatable("hollowlight.weeping.death")
                        .formatted(net.minecraft.util.Formatting.DARK_RED), false);
            }
        }
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WEEPING_SOFT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WEEPING_CRY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WEEPING_CRY;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    /** Called when the player decides to walk past instead. */
    public void markSpared(ServerPlayerEntity player) {
        StoryManager.grant(player, StoryFlags.WEEPING_SPARED);
        player.sendMessage(net.minecraft.text.Text.translatable("hollowlight.weeping.spared")
                .formatted(net.minecraft.util.Formatting.GRAY, net.minecraft.util.Formatting.ITALIC), true);
        SanityManager.restore(player, 4.0F);
    }

    public Vec3d lookAway() {
        return this.getRotationVec(1.0F).negate();
    }
}
