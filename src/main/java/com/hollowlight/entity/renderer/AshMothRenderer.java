package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.AshMothEntity;
import com.hollowlight.entity.model.AshMothEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link AshMothEntity}. */
public class AshMothRenderer extends MobEntityRenderer<AshMothEntity, AshMothEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/ash_moth.png");

    public AshMothRenderer(EntityRendererFactory.Context context) {
        super(context, new AshMothEntityModel(context.getPart(ModEntityModels.ASH_MOTH)), 0.1F);
    }

    @Override
    public Identifier getTexture(AshMothEntity entity) {
        return TEXTURE;
    }
}
