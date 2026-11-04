package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.WeepingOneEntity;
import com.hollowlight.entity.model.WeepingOneEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link WeepingOneEntity}. */
public class WeepingOneRenderer
        extends MobEntityRenderer<WeepingOneEntity, HollowlightEntityRenderState, WeepingOneEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/weeping_one.png");

    public WeepingOneRenderer(EntityRendererFactory.Context context) {
        super(context, new WeepingOneEntityModel(context.getPart(ModEntityModels.WEEPING_ONE)), 0.3F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(WeepingOneEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
