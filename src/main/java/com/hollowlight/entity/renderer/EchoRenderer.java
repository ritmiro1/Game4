package com.hollowlight.entity.renderer;

import com.hollowlight.entity.ModEntityModels;
import com.hollowlight.entity.EchoEntity;
import com.hollowlight.entity.model.EchoEntityModel;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** Renderer for {@link EchoEntity}. */
public class EchoRenderer extends MobEntityRenderer<EchoEntity, EchoEntityModel> {

    private static final Identifier TEXTURE = Identifier.of("hollowlight", "textures/entity/echo.png");

    public EchoRenderer(EntityRendererFactory.Context context) {
        super(context, new EchoEntityModel(context.getPart(ModEntityModels.ECHO)), 0.4F);
    }

    @Override
    public Identifier getTexture(EchoEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(EchoEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Echoes are recollections, not bodies: they are drawn at reduced opacity by the alpha
        // already baked into their texture, and they never cast a shadow.
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
