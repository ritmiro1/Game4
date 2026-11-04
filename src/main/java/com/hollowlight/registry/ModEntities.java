package com.hollowlight.registry;

import com.hollowlight.Hollowlight;
import com.hollowlight.entity.AshMothEntity;
import com.hollowlight.entity.DrownedChoirEntity;
import com.hollowlight.entity.EchoEntity;
import com.hollowlight.entity.HollowedEntity;
import com.hollowlight.entity.LampkeeperEntity;
import com.hollowlight.entity.PaperfaceEntity;
import com.hollowlight.entity.StitcherEntity;
import com.hollowlight.entity.WeepingOneEntity;
import com.hollowlight.entity.WickmothEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

/**
 * The nine residents of the labyrinth. Sizes are chosen so that every entity fits the corridors:
 * the widest is the stitcher at 1.2 blocks across in a 5-block-wide passage.
 */
public final class ModEntities {

    private static final List<EntityType<?>> ALL = new ArrayList<>();

    public static final EntityType<HollowedEntity> HOLLOWED = register("hollowed",
            EntityType.Builder.create(HollowedEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7F, 1.9F).maxTrackingRange(10).build());

    public static final EntityType<WickmothEntity> WICKMOTH = register("wickmoth",
            EntityType.Builder.create(WickmothEntity::new, SpawnGroup.AMBIENT)
                    .dimensions(0.6F, 0.6F).maxTrackingRange(8).build());

    public static final EntityType<PaperfaceEntity> PAPERFACE = register("paperface",
            EntityType.Builder.create(PaperfaceEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.7F, 2.2F).maxTrackingRange(12).build());

    public static final EntityType<StitcherEntity> STITCHER = register("stitcher",
            EntityType.Builder.create(StitcherEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.2F, 3.2F).maxTrackingRange(12).build());

    public static final EntityType<DrownedChoirEntity> DROWNED_CHOIR = register("drowned_choir",
            EntityType.Builder.create(DrownedChoirEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8F, 1.8F).maxTrackingRange(10).build());

    public static final EntityType<WeepingOneEntity> WEEPING_ONE = register("weeping_one",
            EntityType.Builder.create(WeepingOneEntity::new, SpawnGroup.CREATURE)
                    .dimensions(0.8F, 1.5F).maxTrackingRange(10).build());

    public static final EntityType<EchoEntity> ECHO = register("echo",
            EntityType.Builder.create(EchoEntity::new, SpawnGroup.AMBIENT)
                    .dimensions(0.6F, 1.8F).maxTrackingRange(10).build());

    public static final EntityType<AshMothEntity> ASH_MOTH = register("ash_moth",
            EntityType.Builder.create(AshMothEntity::new, SpawnGroup.AMBIENT)
                    .dimensions(0.3F, 0.3F).maxTrackingRange(6).build());

    public static final EntityType<LampkeeperEntity> LAMPKEEPER = register("lampkeeper",
            EntityType.Builder.create(LampkeeperEntity::new, SpawnGroup.MONSTER)
                    .dimensions(1.8F, 4.2F).maxTrackingRange(16).makeFireImmune().build());

    private ModEntities() {
    }

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, Hollowlight.id(name));
        EntityType<T> type = Registry.register(Registries.ENTITY_TYPE, key, builder.build(key));
        ALL.add(type);
        return type;
    }

    public static List<EntityType<?>> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static void bootstrap() {
        FabricDefaultAttributeRegistry.register(HOLLOWED, HollowedEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WICKMOTH, WickmothEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(PAPERFACE, PaperfaceEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(STITCHER, StitcherEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(DROWNED_CHOIR, DrownedChoirEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WEEPING_ONE, WeepingOneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ECHO, EchoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ASH_MOTH, AshMothEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LAMPKEEPER, LampkeeperEntity.createAttributes());
        Hollowlight.LOGGER.debug("Registered {} entities", ALL.size());
    }
}
