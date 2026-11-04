package com.hollowlight.entity.model;

import com.hollowlight.entity.AshMothEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

/**
 * Model for {@link AshMothEntity}.
 *
 * <p>Generated together with its texture (texture size 32x32) from a single geometry spec, so the UV
 * layout of the sheet and the cuboids here are guaranteed to agree.</p>
 */
public class AshMothEntityModel extends EntityModel<AshMothEntity> {

    private final ModelPart body;
    private final ModelPart wing_left;
    private final ModelPart wing_right;

    public AshMothEntityModel(ModelPart root) {
        this.body = root.getChild("body");
        this.wing_left = root.getChild("body").getChild("wing_left");
        this.wing_right = root.getChild("body").getChild("wing_right");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -1.0F, -1.0F, 2.0F, 1.0F, 2.0F),
                ModelTransform.pivot(0.0F, 22.0F, 0.0F));
        ModelPartData wing_left = body.addChild("wing_left",
                ModelPartBuilder.create().uv(9, 0).cuboid(0.0F, 0.0F, -1.0F, 3.0F, 1.0F, 3.0F),
                ModelTransform.pivot(1.0F, -1.0F, 0.0F));
        ModelPartData wing_right = body.addChild("wing_right",
                ModelPartBuilder.create().uv(0, 5).cuboid(-3.0F, 0.0F, -1.0F, 3.0F, 1.0F, 3.0F),
                ModelTransform.pivot(-1.0F, -1.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(AshMothEntity entity, float limbAngle, float limbDistance,
                     float animationProgress, float headYaw, float headPitch) {
        this.body.pitch = MathHelper.cos(animationProgress * 0.2F) * 0.15F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.body.render(matrices, vertices, light, overlay, color);
    }
}
