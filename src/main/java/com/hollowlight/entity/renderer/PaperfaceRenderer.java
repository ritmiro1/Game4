package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.PaperfaceEntity;
import com.hollowlight.entity.model.PaperfaceEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link PaperfaceEntity}. */
public class PaperfaceRenderer
        extends MobEntityRenderer<PaperfaceEntity, HollowlightEntityRenderState, PaperfaceEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/paperface.png");

    public PaperfaceRenderer(EntityRendererFactory.Context context) {
        super(context, new PaperfaceEntityModel(context.getPart(ModEntityModels.PAPERFACE)), 0.5F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(PaperfaceEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
