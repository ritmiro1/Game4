package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.LampkeeperEntity;
import com.hollowlight.entity.model.LampkeeperEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link LampkeeperEntity}. */
public class LampkeeperRenderer extends MobEntityRenderer<LampkeeperEntity, LampkeeperEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/lampkeeper.png");

    public LampkeeperRenderer(EntityRendererFactory.Context context) {
        super(context, new LampkeeperEntityModel(context.getPart(ModEntityModels.LAMPKEEPER)), 0.9F);
    }

    @Override
    public Identifier getTexture(LampkeeperEntity entity) {
        return TEXTURE;
    }
}
