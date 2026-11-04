package com.hollowlight.client;

import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;

/**
 * Four small custom particles. All of them are drawn on the translucent sheet, drift slowly and
 * never use additive blending, so they read as dust rather than as sparkles.
 */
public final class HollowlightParticles {

    private HollowlightParticles() {
    }

    public static ParticleFactory<SimpleParticleType> dustProvider() {
        return (type, world, x, y, z, vx, vy, vz) -> new Mote(world, x, y, z, 0.05F, 60, 0.62F, 0.55F, 0.42F, 0.004);
    }

    public static ParticleFactory<SimpleParticleType> moteProvider() {
        return (type, world, x, y, z, vx, vy, vz) -> new Mote(world, x, y, z, 0.07F, 80, 0.78F, 0.86F, 0.72F, 0.006);
    }

    public static ParticleFactory<SimpleParticleType> smokeProvider() {
        return (type, world, x, y, z, vx, vy, vz) -> new Mote(world, x, y, z, 0.16F, 100, 0.28F, 0.26F, 0.31F, 0.002);
    }

    public static ParticleFactory<SimpleParticleType> glintProvider() {
        return (type, world, x, y, z, vx, vy, vz) -> new Mote(world, x, y, z, 0.03F, 30, 0.98F, 0.88F, 0.62F, 0.0);
    }

    /** A soft drifting sprite. Colours are baked in so the mod needs no extra sprite sheets. */
    private static final class Mote extends SpriteBillboardParticle {

        private final double drift;

        Mote(ClientWorld world, double x, double y, double z, float scale, int maxAge,
             float red, float green, float blue, double drift) {
            super(world, x, y, z);
            this.scale = scale;
            this.maxAge = maxAge + world.getRandom().nextInt(maxAge / 2);
            this.drift = drift;
            this.setColor(red, green, blue);
            this.velocityX = (world.getRandom().nextDouble() - 0.5) * drift * 20.0;
            this.velocityY = world.getRandom().nextDouble() * drift * 10.0;
            this.velocityZ = (world.getRandom().nextDouble() - 0.5) * drift * 20.0;
            this.gravityStrength = 0.0F;
            this.velocityMultiplier = 0.99F;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.age > this.maxAge * 0.7F) {
                this.markDead();
            }
            this.velocityY += this.drift * 0.05;
        }

        @Override
        public ParticleTextureSheet getType() {
            return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
        }

        @Override
        public float getSize(float tickDelta) {
            return this.scale * (1.0F - this.age / (float) this.maxAge * 0.6F);
        }
    }
}
