package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.EchoEntity;
import com.hollowlight.entity.model.EchoEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link EchoEntity}. */
public class EchoRenderer
        extends MobEntityRenderer<EchoEntity, HollowlightEntityRenderState, EchoEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/echo.png");

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new EchoEntityModel(context.getPart(ModEntityModels.ECHO)), 0.4F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(EchoEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
