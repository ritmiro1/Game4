package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.WickmothEntity;
import com.hollowlight.entity.model.WickmothEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link WickmothEntity}. */
public class WickmothRenderer
        extends MobEntityRenderer<WickmothEntity, HollowlightEntityRenderState, WickmothEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/wickmoth.png");

    public WickmothRenderer(EntityRendererFactory.Context context) {
        super(context, new WickmothEntityModel(context.getPart(ModEntityModels.WICKMOTH)), 0.1F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(WickmothEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
