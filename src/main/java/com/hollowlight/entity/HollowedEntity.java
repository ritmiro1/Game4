package com.hollowlight.entity;

import com.hollowlight.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.LightType;

/**
 * A miner who was sealed in and stayed. The hollowed walk slowly, always toward light, and stop
 * in perfect darkness — which is the only mercy the labyrinth offers: put your lamp down and
 * they will stand still around it, staring at the flame, until it dies.
 */
public class HollowedEntity extends HostileEntity {

    public HollowedEntity(EntityType<? extends HollowedEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 6;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 24.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.19)
                .add(EntityAttributes.ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new com.hollowlight.entity.ai.LightSeekingGoal(this, 1.0));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.6));
        this.goalSelector.add(4, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, net.minecraft.entity.player.PlayerEntity.class, 12.0F));
        this.goalSelector.add(5, new net.minecraft.entity.ai.goal.LookAroundGoal(this));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this, net.minecraft.entity.player.PlayerEntity.class, false));
        this.targetSelector.add(2, new net.minecraft.entity.ai.goal.RevengeGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient() && this.age % 40 == 0) {
            int light = this.getWorld().getLightLevel(LightType.BLOCK, this.getBlockPos());
            if (light < 3) {
                // Total darkness is the one thing they will not enter.
                this.getNavigation().stop();
                this.setVelocity(0.0, this.getVelocity().y, 0.0);
            }
            if (this.getWorld().getRandom().nextFloat() < 0.12F) {
                this.playSound(ModSounds.HOLLOWED_IDLE, 0.5F, 0.8F + this.getWorld().getRandom().nextFloat() * 0.3F);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HOLLOWED_IDLE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HOLLOWED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOLLOWED_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F;
    }

    public static boolean canSpawn(EntityType<HollowedEntity> type, WorldAccess world, SpawnReason reason,
                                   BlockPos pos, Random random) {
        return world.getLightLevel(LightType.BLOCK, pos) < 9 && pos.getY() > 4;
    }
}
