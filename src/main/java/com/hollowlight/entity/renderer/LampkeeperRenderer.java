package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.LampkeeperEntity;
import com.hollowlight.entity.model.LampkeeperEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link LampkeeperEntity}. */
public class LampkeeperRenderer
        extends MobEntityRenderer<LampkeeperEntity, HollowlightEntityRenderState, LampkeeperEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/lampkeeper.png");

    public LampkeeperRenderer(EntityRendererFactory.Context context) {
        super(context, new LampkeeperEntityModel(context.getPart(ModEntityModels.LAMPKEEPER)), 0.9F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(LampkeeperEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.cameraDistance = entity.distanceTo(this.dispatcher.camera.getFocusedEntity());
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
