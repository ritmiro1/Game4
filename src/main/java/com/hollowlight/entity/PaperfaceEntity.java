package com.hollowlight.entity;

import com.hollowlight.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Reader. It has a face made of pages and it will not move while you are looking at it.
 *
 * <p>Mechanically this is the mod's one "look at it" enemy: a player who keeps their eyes on it
 * is safe, a player who turns around to run is not. It is deliberately slow to kill and slow to
 * kill you; the horror is the trade you have to make between your eyes and your feet.</p>
 */
public class PaperfaceEntity extends HostileEntity {

    private int frozenTicks;

    public PaperfaceEntity(EntityType<? extends PaperfaceEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 30.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.5));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this,
                PlayerEntity.class, false));
    }

    public boolean isFrozen() {
        return this.frozenTicks > 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }
        PlayerEntity watched = this.getWorld().getClosestPlayer(this, 30.0);
        if (watched != null && isWatchedBy(watched)) {
            this.frozenTicks = 12;
            this.getNavigation().stop();
            this.setVelocity(0.0, this.getVelocity().y, 0.0);
            if (this.age % 60 == 0) {
                this.playSound(ModSounds.PAPERFACE_WATCH, 0.7F, 0.9F);
            }
        } else if (this.frozenTicks > 0) {
            this.frozenTicks--;
        }
    }

    private boolean isWatchedBy(PlayerEntity player) {
        Vec3d toEntity = this.getPos().add(0.0, this.getHeight() * 0.7, 0.0).subtract(player.getEyePos()).normalize();
        return player.getRotationVec(1.0F).dotProduct(toEntity) > 0.92;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PAPERFACE_WATCH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PAPERFACE_ATTACK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PAPERFACE_ATTACK;
    }
}
