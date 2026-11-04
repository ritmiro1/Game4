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
 * Model for {@link StitcherEntity}.
 *
 * <p>Generated together with its texture (texture size 128x128) from a single geometry spec, so the UV
 * layout of the sheet and the cuboids here are guaranteed to agree.</p>
 */
public class StitcherEntityModel extends EntityModel<HollowlightEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart arm_upper_left;
    private final ModelPart arm_upper_right;
    private final ModelPart arm_lower_left;
    private final ModelPart arm_lower_right;
    private final ModelPart leg_left;
    private final ModelPart leg_right;

    public StitcherEntityModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.arm_upper_left = root.getChild("body").getChild("arm_upper_left");
        this.arm_upper_right = root.getChild("body").getChild("arm_upper_right");
        this.arm_lower_left = root.getChild("body").getChild("arm_lower_left");
        this.arm_lower_right = root.getChild("body").getChild("arm_lower_right");
        this.leg_left = root.getChild("leg_left");
        this.leg_right = root.getChild("leg_right");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -18.0F, -2.5F, 8.0F, 18.0F, 5.0F),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create().uv(27, 0).cuboid(-3.0F, -7.0F, -3.0F, 6.0F, 7.0F, 6.0F),
                ModelTransform.pivot(0.0F, -18.0F, 0.0F));
        ModelPartData arm_upper_left = body.addChild("arm_upper_left",
                ModelPartBuilder.create().uv(52, 0).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                ModelTransform.pivot(4.0F, -16.0F, 0.0F));
        ModelPartData arm_upper_right = body.addChild("arm_upper_right",
                ModelPartBuilder.create().uv(61, 0).cuboid(-2.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                ModelTransform.pivot(-4.0F, -16.0F, 0.0F));
        ModelPartData arm_lower_left = body.addChild("arm_lower_left",
                ModelPartBuilder.create().uv(70, 0).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F),
                ModelTransform.pivot(4.0F, -9.0F, 0.0F));
        ModelPartData arm_lower_right = body.addChild("arm_lower_right",
                ModelPartBuilder.create().uv(79, 0).cuboid(-2.0F, 0.0F, -1.0F, 2.0F, 14.0F, 2.0F),
                ModelTransform.pivot(-4.0F, -9.0F, 0.0F));
        ModelPartData leg_left = root.addChild("leg_left",
                ModelPartBuilder.create().uv(88, 0).cuboid(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                ModelTransform.pivot(2.0F, 24.0F, 0.0F));
        ModelPartData leg_right = root.addChild("leg_right",
                ModelPartBuilder.create().uv(101, 0).cuboid(-1.5F, -6.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                ModelTransform.pivot(-2.0F, 24.0F, 0.0F));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(HollowlightEntityRenderState state) {
        float limbAngle = state.limbFrequency;
        float limbDistance = state.limbAmplitudeMultiplier;
        float animationProgress = state.age;
        float headYaw = state.yawDegrees;
        float headPitch = state.pitch;
        this.leg_left.pitch = MathHelper.cos(limbAngle * 0.6662F + 0.0F) * 1.1F * limbDistance;
        this.leg_right.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 1.1F * limbDistance;
        this.arm_upper_left.pitch = MathHelper.cos(limbAngle * 0.6662F + 0.0F) * 0.7F * limbDistance;
        this.arm_upper_right.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 0.7F * limbDistance;
        this.head.yaw = headYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.body.render(matrices, vertices, light, overlay, color);
        this.leg_left.render(matrices, vertices, light, overlay, color);
        this.leg_right.render(matrices, vertices, light, overlay, color);
    }
}
