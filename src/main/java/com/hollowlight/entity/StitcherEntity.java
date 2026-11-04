package com.hollowlight.entity;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The thing that sews the corridors shut.
 *
 * <p>Three blocks tall, four arms, and it does not walk to you — it is simply closer the next
 * time you look. It becomes visible only inside lamplight, and while it is close it puts out
 * every light it can reach.</p>
 */
public class StitcherEntity extends HostileEntity {

    private int blinkCooldown = 80;

    public StitcherEntity(EntityType<? extends StitcherEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 16;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.4));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this,
                PlayerEntity.class, false));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }
        if (this.age % 20 == 0 && this.getWorld().getRandom().nextFloat() < 0.2F) {
            this.playSound(ModSounds.STITCHER_AMBIENT, 0.5F, 0.7F);
        }
        PlayerEntity target = this.getWorld().getClosestPlayer(this, 24.0);
        if (target == null) {
            return;
        }
        // It puts out lamps within reach: the player's own light is its first enemy.
        if (this.age % 40 == 0) {
            for (BlockPos pos : BlockPos.iterate(this.getBlockPos().add(-3, -2, -3), this.getBlockPos().add(3, 2, 3))) {
                BlockState state = this.getWorld().getBlockState(pos);
                if (state.isOf(ModBlocks.HOLLOW_LANTERN) || state.isOf(ModBlocks.LAMP_GLASS)) {
                    this.getWorld().removeBlock(pos, false);
                    this.playSound(ModSounds.LANTERN_EMPTY, 0.6F, 1.0F);
                    break;
                }
            }
        }
        // The step: when the player is not looking, it is somewhere else, closer.
        if (--this.blinkCooldown <= 0) {
            this.blinkCooldown = 100 + this.random.nextInt(80);
            double distance = this.squaredDistanceTo(target);
            if (distance > 36.0 && distance < 400.0 && this.getWorld().getRandom().nextFloat() < 0.35F) {
                double angle = this.random.nextDouble() * Math.PI * 2.0;
                BlockPos candidate = target.getBlockPos().add((int) Math.round(Math.cos(angle) * 7.0),
                        0, (int) Math.round(Math.sin(angle) * 7.0));
                if (this.getWorld().isAir(candidate) && this.getWorld().isAir(candidate.up())
                        && this.getWorld().isAir(candidate.up(2))) {
                    this.teleport(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5, false);
                    this.playSound(ModSounds.STITCHER_ATTACK, 0.6F, 0.6F);
                }
            }
        }
    }

    @Override
    public int getSafeFallDistance() {
        return 20;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.STITCHER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STITCHER_ATTACK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.STITCHER_ATTACK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }
}
