package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Custom atmospheric particles: dust in lamplight, pale motes, whisper smoke. */
public final class ModParticles {

    public static final SimpleParticleType LAMP_DUST = FabricParticleTypes.simple(true);
    public static final SimpleParticleType PALE_MOTE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType WHISPER_SMOKE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType MEMORY_GLINT = FabricParticleTypes.simple(true);

    private ModParticles() {
    }

    public static void bootstrap() {
        Registry.register(Registries.PARTICLE_TYPE, Hollowlight.id("lamp_dust"), LAMP_DUST);
        Registry.register(Registries.PARTICLE_TYPE, Hollowlight.id("pale_mote"), PALE_MOTE);
        Registry.register(Registries.PARTICLE_TYPE, Hollowlight.id("whisper_smoke"), WHISPER_SMOKE);
        Registry.register(Registries.PARTICLE_TYPE, Hollowlight.id("memory_glint"), MEMORY_GLINT);
    }
}
