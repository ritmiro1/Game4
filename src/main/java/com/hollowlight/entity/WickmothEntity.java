package com.hollowlight.entity;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * A lamp-eater. It is small, it does not hurt anybody, and it is the most dangerous thing in the
 * labyrinth, because it eats the only thing keeping the player sane.
 *
 * <p>Design note: the wickmoth never attacks. It flies at the nearest light and drinks it. The
 * player's counter-play is to carry a lantern in hand (which it cannot reach) or to use light
 * that is not a block.</p>
 */
public class WickmothEntity extends PathAwareEntity {

    private int cooldown;

    public WickmothEntity(EntityType<? extends WickmothEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.experiencePoints = 2;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.4);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new WanderAroundGoal(this, 0.6));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(3, new LookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        if (!this.getWorld().isClient()) {
            BlockPos light = findLight();
            if (light != null) {
                Vec3d target = new Vec3d(light.getX() + 0.5, light.getY() + 0.5, light.getZ() + 0.5);
                Vec3d direction = target.subtract(this.getPos()).normalize().multiply(0.11);
                this.setVelocity(this.getVelocity().multiply(0.7).add(direction));
                if (this.getPos().squaredDistanceTo(target) < 1.2 && --cooldown <= 0) {
                    cooldown = 20;
                    this.getWorld().removeBlock(light, false);
                    this.playSound(ModSounds.LANTERN_EMPTY, 0.5F, 1.4F);
                    this.playSound(ModSounds.MOTH_FLUTTER, 0.4F, 1.2F);
                }
            } else {
                this.setVelocity(this.getVelocity().multiply(0.92).add(0.0, Math.sin(this.age * 0.1) * 0.004, 0.0));
            }
            if (this.age % 100 == 0 && this.getWorld().getRandom().nextFloat() < 0.4F) {
                this.playSound(ModSounds.MOTH_FLUTTER, 0.3F, 1.3F);
            }
            if (this.age > 20 * 60 * 8) {
                this.discard();
            }
        }
        this.move(MovementType.SELF, this.getVelocity());
    }

    private BlockPos findLight() {
        List<BlockPos> lights = new ArrayList<>();
        BlockPos origin = this.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(origin.add(-12, -6, -12), origin.add(12, 6, 12))) {
            var state = this.getWorld().getBlockState(pos);
            if (state.isOf(ModBlocks.HOLLOW_LANTERN) || state.isOf(ModBlocks.LAMP_GLASS)
                    || state.isOf(ModBlocks.MEMORY_LAMP)) {
                lights.add(pos.toImmutable());
            }
        }
        if (lights.isEmpty()) {
            return null;
        }
        lights.sort((a, b) -> Double.compare(a.getSquaredDistance(origin), b.getSquaredDistance(origin)));
        return lights.get(0);
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
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.MOTH_FLUTTER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.MOTH_FLUTTER;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }
}
