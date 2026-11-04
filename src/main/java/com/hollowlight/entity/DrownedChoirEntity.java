package com.hollowlight.entity;

import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * One of the nine, drowned and patient. It stays under the waterline and sings — a list of
 * names, in the order it was given them — until somebody answers.
 */
public class DrownedChoirEntity extends HostileEntity {

    public DrownedChoirEntity(EntityType<? extends DrownedChoirEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 32.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.12)
                .add(EntityAttributes.ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.4));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this,
                PlayerEntity.class, false));
    }

    @Override
    protected boolean isImmobile() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.keepAir();
        if (this.getWorld().isClient()) {
            return;
        }
        FluidState fluid = this.getWorld().getFluidState(this.getBlockPos());
        boolean submerged = fluid.isIn(FluidTags.WATER);
        PlayerEntity target = this.getWorld().getClosestPlayer(this, submerged ? 20.0 : 9.0);
        if (submerged) {
            // Swim toward the nearest player in the water, slowly, singing.
            if (target != null && this.age % 40 == 0) {
                Vec3d direction = new Vec3d(target.getX() - this.getX(), 0.05, target.getZ() - this.getZ())
                        .normalize().multiply(0.09);
                this.setVelocity(this.getVelocity().multiply(0.8).add(direction));
                this.move(MovementType.SELF, this.getVelocity());
            }
            if (this.age % 140 == 0) {
                this.playSound(ModSounds.CHOIR_SING, 0.9F, 0.8F + this.random.nextFloat() * 0.2F);
                for (PlayerEntity player : this.getWorld().getPlayers()) {
                    if (player.squaredDistanceTo(this) < 400.0 && player instanceof ServerPlayerEntity serverPlayer) {
                        SanityManager.drain(serverPlayer, 0.5F, SanityManager.Cause.ENTITY);
                    }
                }
            }
        }
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CHOIR_SING;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CHOIR_SING;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CHOIR_SING;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    /** The choir never breathes, so it never runs out of air. */
    private void keepAir() {
        if (this.isSubmergedInWater()) {
            this.setAir(this.getMaxAir());
        }
    }
}
