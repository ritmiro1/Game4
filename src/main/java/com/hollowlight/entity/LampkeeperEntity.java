package com.hollowlight.entity;

import com.hollowlight.registry.ModBlocks;
import com.hollowlight.registry.ModItems;
import com.hollowlight.registry.ModParticles;
import com.hollowlight.registry.ModSounds;
import com.hollowlight.sanity.SanityManager;
import com.hollowlight.story.StoryFlags;
import com.hollowlight.story.StoryManager;
import com.hollowlight.world.ending.EndingSequence;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Maravell Vane — the Lamplighter. What is left of her.
 *
 * <p>She is not a monster wearing a human shape; she is a woman made of everything she traded
 * away, still walking her round, still saying one name into the corridors. She fights you
 * because you are a light she cannot account for.</p>
 *
 * <p>Three phases, each tied to a piece of the story rather than to a damage number: at two
 * thirds she remembers there is something she is looking for; at one third she stops asking
 * and starts taking; at the end she says the name correctly for the first time in eleven years,
 * and the fight is over.</p>
 */
public class LampkeeperEntity extends HostileEntity {

    private final ServerBossBar bossBar = new ServerBossBar(
            Text.translatable("entity.hollowlight.lampkeeper.name").formatted(Formatting.GOLD),
            BossBar.Color.RED, BossBar.Style.NOTCHED_10);

    private int phase = 1;
    private int actionCooldown = 60;
    private int phaseCooldown;

    public LampkeeperEntity(EntityType<? extends LampkeeperEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 500;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 420.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.16)
                .add(EntityAttributes.ATTACK_DAMAGE, 11.0)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(EntityAttributes.ARMOR, 6.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.add(2, new net.minecraft.entity.ai.goal.WanderAroundFarGoal(this, 0.5));
        this.goalSelector.add(3, new net.minecraft.entity.ai.goal.LookAtEntityGoal(this, PlayerEntity.class, 24.0F));
        this.targetSelector.add(1, new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(this,
                PlayerEntity.class, false));
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    public ServerBossBar getBossBar() {
        return this.bossBar;
    }

    public void setBossBar(ServerBossBar bossBar) {
        // The bar is created with the entity and never replaced.
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient()) {
            spawnAuraParticles();
            return;
        }
        ServerWorld world = (ServerWorld) this.getWorld();
        this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(this) < 48.0 * 48.0) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    this.bossBar.addPlayer(serverPlayer);
                    StoryManager.grant(serverPlayer, StoryFlags.LAMPKEEPER_MET);
                }
            }
        }

        float ratio = this.getHealth() / this.getMaxHealth();
        if (ratio < 0.66F && phase == 1 && phaseCooldown <= 0) {
            enterPhase(2, world);
        } else if (ratio < 0.33F && phase == 2 && phaseCooldown <= 0) {
            enterPhase(3, world);
        }
        if (phaseCooldown > 0) {
            phaseCooldown--;
        }

        PlayerEntity target = null;
        double bestDistance = 40.0 * 40.0;
        for (PlayerEntity candidate : this.getWorld().getPlayers()) {
            double distance = candidate.squaredDistanceTo(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                target = candidate;
            }
        }
        if (target == null) {
            return;
        }
        if (target instanceof ServerPlayerEntity serverPlayer && this.age % 100 == 0) {
            SanityManager.drain(serverPlayer, 0.9F * phase, SanityManager.Cause.ENTITY);
        }
        if (--actionCooldown > 0) {
            return;
        }
        actionCooldown = Math.max(30, 90 - phase * 22);
        switch (phase) {
            case 1 -> snuff(target, world);
            case 2 -> memoryWave(target, world);
            case 3 -> {
                if (this.random.nextBoolean()) {
                    memoryWave(target, world);
                } else {
                    blink(target, world);
                }
            }
            default -> {
            }
        }
    }

    private void enterPhase(int newPhase, ServerWorld world) {
        phase = newPhase;
        phaseCooldown = 200;
        actionCooldown = 40;
        world.playSound(null, this.getBlockPos(), ModSounds.LAMPKEEPER_PHASE, SoundCategory.HOSTILE, 1.0F, newPhase == 2 ? 1.0F : 0.8F);
        world.playSound(null, this.getBlockPos(), ModSounds.LAMPKEEPER_ROAR, SoundCategory.HOSTILE, 0.9F, 0.7F);
        String key = newPhase == 2 ? "hollowlight.lampkeeper.phase2" : "hollowlight.lampkeeper.phase3";
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(this) < 40.0 * 40.0) {
                player.sendMessage(Text.translatable(key).formatted(Formatting.GOLD, Formatting.ITALIC), false);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    SanityManager.drain(serverPlayer, 10.0F, SanityManager.Cause.EVENT);
                }
            }
        }
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 2.0, this.getZ(),
                120, 2.0, 2.0, 2.0, 0.05);
    }

    /** She puts out the player's lights: the arena dims from the walls inward. */
    private void snuff(PlayerEntity target, ServerWorld world) {
        int removed = 0;
        for (BlockPos pos : BlockPos.iterate(target.getBlockPos().add(-8, -3, -8), target.getBlockPos().add(8, 3, 8))) {
            var state = world.getBlockState(pos);
            if (state.isOf(ModBlocks.HOLLOW_LANTERN) || state.isOf(ModBlocks.LAMP_GLASS)) {
                world.removeBlock(pos, false);
                world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.2, 0.2, 0.2, 0.01);
                if (++removed >= 4) {
                    break;
                }
            }
        }
        if (removed > 0) {
            world.playSound(null, target.getBlockPos(), ModSounds.LANTERN_EMPTY, SoundCategory.HOSTILE, 0.8F, 0.7F);
        } else {
            this.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    /** A ring of remembered rooms expands outward, knocking the player back and hurting the mind. */
    private void memoryWave(PlayerEntity target, ServerWorld world) {
        world.playSound(null, this.getBlockPos(), ModSounds.STINGER_DEEP, SoundCategory.HOSTILE, 1.0F, 0.6F);
        Vec3d origin = this.getPos();
        for (PlayerEntity player : world.getPlayers()) {
            double distance = player.getPos().distanceTo(origin);
            if (distance > 14.0) {
                continue;
            }
            Vec3d push = player.getPos().subtract(origin).normalize().multiply(1.1);
            player.setVelocity(push.x, 0.55, push.z);
            player.velocityModified = true;
            player.damage(world, world.getDamageSources().indirectMagic(this, this), 6.0F);
            if (player instanceof ServerPlayerEntity serverPlayer) {
                SanityManager.drain(serverPlayer, 6.0F, SanityManager.Cause.ENTITY);
            }
        }
        for (int radius = 1; radius <= 14; radius++) {
            double angleCount = Math.max(8, radius * 6);
            for (int i = 0; i < angleCount; i++) {
                double angle = (Math.PI * 2.0 / angleCount) * i;
                world.spawnParticles(ModParticles.MEMORY_GLINT,
                        origin.getX() + Math.cos(angle) * radius, origin.getY() + 1.0,
                        origin.getZ() + Math.sin(angle) * radius, 1, 0.0, 0.02, 0.0, 0.005);
            }
        }
    }

    private void blink(PlayerEntity target, ServerWorld world) {
        Vec3d behind = target.getPos().subtract(target.getRotationVec(1.0F).multiply(3.0));
        BlockPos candidate = BlockPos.ofFloored(behind);
        if (world.isAir(candidate) && world.isAir(candidate.up()) && world.isAir(candidate.up(2))) {
            world.playSound(null, this.getBlockPos(), ModSounds.STITCHER_ATTACK, SoundCategory.HOSTILE, 0.8F, 0.5F);
            this.teleport(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5, false);
            world.playSound(null, candidate, ModSounds.LAMPKEEPER_ROAR, SoundCategory.HOSTILE, 0.7F, 0.9F);
        }
    }

    private void spawnAuraParticles() {
        for (int i = 0; i < 4; i++) {
            this.getWorld().addParticle(ModParticles.PALE_MOTE,
                    this.getX() + (this.random.nextDouble() - 0.5) * 1.6,
                    this.getY() + this.random.nextDouble() * 3.4,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 1.6,
                    0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean tryAttack(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(world, target);
        if (hit) {
            this.getWorld().playSound(null, this.getBlockPos(), ModSounds.LAMPKEEPER_ROAR, SoundCategory.HOSTILE, 0.8F, 0.8F);
            if (target instanceof ServerPlayerEntity player) {
                SanityManager.drain(player, 5.0F, SanityManager.Cause.ENTITY);
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.DARKNESS, 100, 0, true, false, false));
            }
        }
        return hit;
    }

    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        this.bossBar.clearPlayers();
        world.playSound(null, this.getBlockPos(), ModSounds.LAMPKEEPER_DEATH, SoundCategory.HOSTILE, 1.2F, 0.7F);
        world.spawnParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.0, this.getZ(),
                160, 2.5, 2.5, 2.5, 0.03);
        // She leaves the thing she could not give away, and the light she carried.
        world.spawnEntity(new ItemEntity(world, this.getX(), this.getY() + 1.0, this.getZ(),
                new ItemStack(ModItems.HEART_OF_THE_HOLLOW)));
        for (int i = 0; i < 4; i++) {
            world.spawnEntity(new ItemEntity(world, this.getX(), this.getY() + 1.0, this.getZ(),
                    new ItemStack(ModItems.MEMORY_SHARD)));
        }
        for (PlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(this) < 40.0 * 40.0) {
                player.sendMessage(Text.translatable("hollowlight.lampkeeper.death")
                        .formatted(Formatting.GOLD, Formatting.ITALIC), false);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    EndingSequence.offerChoice(serverPlayer);
                }
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LAMPKEEPER_ROAR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LAMPKEEPER_ROAR;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LAMPKEEPER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    public int phase() {
        return phase;
    }
}
