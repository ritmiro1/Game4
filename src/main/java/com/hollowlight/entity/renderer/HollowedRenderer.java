package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.HollowedEntity;
import com.hollowlight.entity.model.HollowedEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link HollowedEntity}. */
public class HollowedRenderer
        extends MobEntityRenderer<HollowedEntity, HollowlightEntityRenderState, HollowedEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/hollowed.png");

    public HollowedRenderer(EntityRendererFactory.Context context) {
        super(context, new HollowedEntityModel(context.getPart(ModEntityModels.HOLLOWED)), 0.4F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(HollowedEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.cameraDistance = entity.distanceTo(this.dispatcher.camera.getFocusedEntity());
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
