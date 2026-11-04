package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.DrownedChoirEntity;
import com.hollowlight.entity.model.DrownedChoirEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link DrownedChoirEntity}. */
public class DrownedChoirRenderer
        extends MobEntityRenderer<DrownedChoirEntity, HollowlightEntityRenderState, DrownedChoirEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/drowned_choir.png");

    public DrownedChoirRenderer(EntityRendererFactory.Context context) {
        super(context, new DrownedChoirEntityModel(context.getPart(ModEntityModels.DROWNED_CHOIR)), 0.4F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(DrownedChoirEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
