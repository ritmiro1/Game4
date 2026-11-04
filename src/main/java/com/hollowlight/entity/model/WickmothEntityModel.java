package com.hollowlight.entity.model;

import com.hollowlight.entity.renderer.HollowlightEntityRenderState;
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
 * Model for {@link WickmothEntity}.
 *
 * <p>Generated together with its texture (texture size 32x32) from a single geometry spec, so the UV
 * layout of the sheet and the cuboids here are guaranteed to agree.</p>
 */
public class WickmothEntityModel extends EntityModel<HollowlightEntityRenderState> {

    private final ModelPart body;
    private final ModelPart wing_left;
    private final ModelPart wing_right;
    private final ModelPart antenna;

    public WickmothEntityModel(ModelPart root) {
        this.body = root.getChild("body");
        this.wing_left = root.getChild("body").getChild("wing_left");
        this.wing_right = root.getChild("body").getChild("wing_right");
        this.antenna = root.getChild("body").getChild("antenna");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 3.0F),
                ModelTransform.pivot(0.0F, 20.0F, 0.0F));
        ModelPartData wing_left = body.addChild("wing_left",
                ModelPartBuilder.create().uv(11, 0).cuboid(0.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F),
                ModelTransform.pivot(1.0F, -2.0F, 0.0F));
        ModelPartData wing_right = body.addChild("wing_right",
                ModelPartBuilder.create().uv(0, 6).cuboid(-4.0F, 0.0F, -2.0F, 4.0F, 1.0F, 4.0F),
                ModelTransform.pivot(-1.0F, -2.0F, 0.0F));
        ModelPartData antenna = body.addChild("antenna",
                ModelPartBuilder.create().uv(17, 6).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 1.0F),
                ModelTransform.pivot(0.0F, -2.0F, 0.0F));
        return TexturedModelData.of(modelData, 32, 32);
    }

    @Override
    public void setAngles(HollowlightEntityRenderState state) {
        float limbAngle = state.limbFrequency;
        float limbDistance = state.limbAmplitudeMultiplier;
        float animationProgress = state.age;
        float headYaw = state.yawDegrees;
        float headPitch = state.pitch;
        this.body.pitch = MathHelper.cos(animationProgress * 0.2F) * 0.15F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.body.render(matrices, vertices, light, overlay, color);
    }
}
