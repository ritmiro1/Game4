package com.hollowlight.entity;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

/**
 * An echo: a person-shaped recollection that walks a corridor, notices you, and is not there.
 *
 * <p>It cannot be hit, does not collide, has no drops and always fades. It exists so that the
 * player learns, early, not to trust what the labyrinth shows them.</p>
 */
public class EchoEntity extends PathAwareEntity {

    private boolean noticed;
    private boolean spoke;

    public EchoEntity(EntityType<? extends EchoEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.setNoGravity(false);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.12);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new WanderAroundGoal(this, 0.3));
        this.goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            return;
        }
        if (this.age > 240) {
            this.discard();
            return;
        }
        if (this.age % 30 == 0 && !this.spoke) {
            this.playSound(ModSounds.ECHO_VOICE, 0.5F, 1.05F);
            this.spoke = true;
        }
        if (!this.noticed) {
            PlayerEntity player = this.getWorld().getClosestPlayer(this, 16.0);
            if (player != null) {
                this.noticed = true;
                this.lookAt(net.minecraft.entity.EntityAnchorArgumentType.EntityAnchor.EYES, player.getEyePos());
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    SanityManager.drain(serverPlayer, 0.8F, SanityManager.Cause.EVENT);
                }
                this.playSound(ModSounds.ECHO_VOICE, 0.6F, 0.9F);
            }
        }
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ECHO_VOICE;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }
}
