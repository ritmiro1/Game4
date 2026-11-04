package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.AshMothEntity;
import com.hollowlight.entity.model.AshMothEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

/** Renderer for {@link AshMothEntity}. */
public class AshMothRenderer
        extends MobEntityRenderer<AshMothEntity, HollowlightEntityRenderState, AshMothEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/ash_moth.png");

    public AshMothRenderer(EntityRendererFactory.Context context) {
        super(context, new AshMothEntityModel(context.getPart(ModEntityModels.ASH_MOTH)), 0.1F);
    }

    @Override
    public HollowlightEntityRenderState createRenderState() {
        return new HollowlightEntityRenderState();
    }

    @Override
    public void updateRenderState(AshMothEntity entity, HollowlightEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
    }

    @Override
    public Identifier getTexture(HollowlightEntityRenderState state) {
        return TEXTURE;
    }
}
