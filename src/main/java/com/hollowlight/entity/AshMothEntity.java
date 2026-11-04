package com.hollowlight.entity;

import com.hollowlight.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/** A moth the colour of dust. Harmless, pointless, and the only living thing down here. */
public class AshMothEntity extends PathAwareEntity {

    public AshMothEntity(EntityType<? extends AshMothEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 3.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (this.getWorld().isClient()) {
            return;
        }
        if (this.age % 20 == 0) {
            this.setVelocity(this.getVelocity().add((this.random.nextDouble() - 0.5) * 0.06,
                    (this.random.nextDouble() - 0.5) * 0.04, (this.random.nextDouble() - 0.5) * 0.06));
            if (this.random.nextFloat() < 0.2F) {
                this.playSound(ModSounds.MOTH_FLUTTER, 0.2F, 1.6F);
            }
        }
        this.setVelocity(this.getVelocity().multiply(0.94));
        this.move(MovementType.SELF, this.getVelocity());
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.MOTH_FLUTTER;
    }

    @Override
    protected float getSoundVolume() {
        return 0.2F;
    }
}
