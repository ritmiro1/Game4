package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.WickmothEntity;
import com.hollowlight.entity.model.WickmothEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link WickmothEntity}. */
public class WickmothRenderer extends MobEntityRenderer<WickmothEntity, WickmothEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/wickmoth.png");

    public WickmothRenderer(EntityRendererFactory.Context context) {
        super(context, new WickmothEntityModel(context.getPart(ModEntityModels.WICKMOTH)), 0.1F);
    }

    @Override
    public Identifier getTexture(WickmothEntity entity) {
        return TEXTURE;
    }
}
