package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.StitcherEntity;
import com.hollowlight.entity.model.StitcherEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link StitcherEntity}. */
public class StitcherRenderer extends MobEntityRenderer<StitcherEntity, StitcherEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/stitcher.png");

    public StitcherRenderer(EntityRendererFactory.Context context) {
        super(context, new StitcherEntityModel(context.getPart(ModEntityModels.STITCHER)), 0.7F);
    }

    @Override
    public Identifier getTexture(StitcherEntity entity) {
        return TEXTURE;
    }
}
