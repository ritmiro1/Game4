package com.hollowlight.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import java.util.EnumSet;

/**
 * Walk toward the nearest well-lit block within range. Used by the hollowed, who are drawn to
 * lamplight the way the living are drawn to warmth, and who will stand in it for hours.
 */
public class LightSeekingGoal extends Goal {

    private final MobEntity mob;
    private final double speed;
    private BlockPos target;

    public LightSeekingGoal(MobEntity mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        if (this.mob.getWorld().getRandom().nextInt(20) != 0) {
            return false;
        }
        if (this.mob.getWorld().getLightLevel(LightType.BLOCK, this.mob.getBlockPos()) >= 10) {
            return false;
        }
        this.target = findLight();
        return this.target != null;
    }

    @Override
    public boolean shouldContinue() {
        return this.target != null && !this.mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        if (this.target != null) {
            this.mob.getNavigation().startMovingTo(this.target.getX() + 0.5, this.target.getY(),
                    this.target.getZ() + 0.5, this.speed);
        }
    }

    @Override
    public void stop() {
        this.target = null;
    }

    private BlockPos findLight() {
        BlockPos origin = this.mob.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(origin.add(-12, -4, -12), origin.add(12, 4, 12))) {
            if (this.mob.getWorld().getLightLevel(LightType.BLOCK, pos) >= 11
                    && this.mob.getWorld().isAir(pos) && this.mob.getWorld().isAir(pos.up())) {
                return pos.toImmutable();
            }
        }
        return null;
    }
}
