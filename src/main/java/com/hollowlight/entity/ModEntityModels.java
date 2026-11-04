package com.hollowlight.entity;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import com.hollowlight.entity.model.AshMothEntityModel;
import com.hollowlight.entity.model.DrownedChoirEntityModel;
import com.hollowlight.entity.model.EchoEntityModel;
import com.hollowlight.entity.model.HollowedEntityModel;
import com.hollowlight.entity.model.LampkeeperEntityModel;
import com.hollowlight.entity.model.PaperfaceEntityModel;
import com.hollowlight.entity.model.StitcherEntityModel;
import com.hollowlight.entity.model.WeepingOneEntityModel;
import com.hollowlight.entity.model.WickmothEntityModel;

/** Model layer locations for the nine residents. */
public final class ModEntityModels {

    public static final EntityModelLayer HOLLOWED = register("hollowed");
    public static final EntityModelLayer WICKMOTH = register("wickmoth");
    public static final EntityModelLayer PAPERFACE = register("paperface");
    public static final EntityModelLayer STITCHER = register("stitcher");
    public static final EntityModelLayer DROWNED_CHOIR = register("drowned_choir");
    public static final EntityModelLayer WEEPING_ONE = register("weeping_one");
    public static final EntityModelLayer ECHO = register("echo");
    public static final EntityModelLayer ASH_MOTH = register("ash_moth");
    public static final EntityModelLayer LAMPKEEPER = register("lampkeeper");

    private ModEntityModels() {
    }

    private static EntityModelLayer register(String name) {
        return new EntityModelLayer(Identifier.of("hollowlight", name), "main");
    }

    public static void registerLayers() {
        EntityModelLayerRegistry.registerModelLayer(HOLLOWED, HollowedEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WICKMOTH, WickmothEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(PAPERFACE, PaperfaceEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(STITCHER, StitcherEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(DROWNED_CHOIR, DrownedChoirEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(WEEPING_ONE, WeepingOneEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ECHO, EchoEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ASH_MOTH, AshMothEntityModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(LAMPKEEPER, LampkeeperEntityModel::getTexturedModelData);
    }
}
