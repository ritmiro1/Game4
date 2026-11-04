package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.PaperfaceEntity;
import com.hollowlight.entity.model.PaperfaceEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link PaperfaceEntity}. */
public class PaperfaceRenderer extends MobEntityRenderer<PaperfaceEntity, PaperfaceEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/paperface.png");

    public PaperfaceRenderer(EntityRendererFactory.Context context) {
        super(context, new PaperfaceEntityModel(context.getPart(ModEntityModels.PAPERFACE)), 0.5F);
    }

    @Override
    public Identifier getTexture(PaperfaceEntity entity) {
        return TEXTURE;
    }
}
