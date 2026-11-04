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
 * Model for {@link LampkeeperEntity}.
 *
 * <p>Generated together with its texture (texture size 128x128) from a single geometry spec, so the UV
 * layout of the sheet and the cuboids here are guaranteed to agree.</p>
 */
public class LampkeeperEntityModel extends EntityModel<HollowlightEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart arm_left;
    private final ModelPart arm_right;
    private final ModelPart arm_mid_left;
    private final ModelPart arm_mid_right;
    private final ModelPart skirt;
    private final ModelPart lamp;

    public LampkeeperEntityModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.arm_left = root.getChild("body").getChild("arm_left");
        this.arm_right = root.getChild("body").getChild("arm_right");
        this.arm_mid_left = root.getChild("body").getChild("arm_mid_left");
        this.arm_mid_right = root.getChild("body").getChild("arm_mid_right");
        this.skirt = root.getChild("skirt");
        this.lamp = root.getChild("body").getChild("lamp");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-5.0F, -22.0F, -3.0F, 10.0F, 22.0F, 6.0F),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create().uv(33, 0).cuboid(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                ModelTransform.pivot(0.0F, -22.0F, 0.0F));
        ModelPartData arm_left = body.addChild("arm_left",
                ModelPartBuilder.create().uv(74, 0).cuboid(0.0F, 0.0F, -1.5F, 2.5F, 20.0F, 3.0F),
                ModelTransform.pivot(5.0F, -20.0F, 0.0F));
        ModelPartData arm_right = body.addChild("arm_right",
                ModelPartBuilder.create().uv(85, 0).cuboid(-2.5F, 0.0F, -1.5F, 2.5F, 20.0F, 3.0F),
                ModelTransform.pivot(-5.0F, -20.0F, 0.0F));
        ModelPartData arm_mid_left = body.addChild("arm_mid_left",
                ModelPartBuilder.create().uv(96, 0).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                ModelTransform.pivot(4.0F, -14.0F, 1.0F));
        ModelPartData arm_mid_right = body.addChild("arm_mid_right",
                ModelPartBuilder.create().uv(105, 0).cuboid(-2.0F, 0.0F, -1.0F, 2.0F, 16.0F, 2.0F),
                ModelTransform.pivot(-4.0F, -14.0F, 1.0F));
        ModelPartData lamp = body.addChild("lamp",
                ModelPartBuilder.create().uv(41, 29).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                ModelTransform.pivot(0.0F, -30.0F, 0.0F));
        ModelPartData skirt = root.addChild("skirt",
                ModelPartBuilder.create().uv(0, 29).cuboid(-6.0F, -10.0F, -4.0F, 12.0F, 10.0F, 8.0F),
                ModelTransform.pivot(0.0F, 24.0F, 0.0F));
        return TexturedModelData.of(modelData, 128, 128);
    }

    @Override
    public void setAngles(HollowlightEntityRenderState state) {
        float limbAngle = state.limbFrequency;
        float limbDistance = state.limbAmplitudeMultiplier;
        float animationProgress = state.age;
        float headYaw = state.yawDegrees;
        float headPitch = state.pitch;
        this.arm_left.pitch = MathHelper.cos(limbAngle * 0.6662F + 0.0F) * 0.7F * limbDistance;
        this.arm_right.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 0.7F * limbDistance;
        this.arm_mid_left.pitch = MathHelper.cos(limbAngle * 0.6662F + 0.0F) * 0.7F * limbDistance;
        this.arm_mid_right.pitch = MathHelper.cos(limbAngle * 0.6662F + (float) Math.PI) * 0.7F * limbDistance;
        this.head.yaw = headYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;
        this.body.pitch = MathHelper.cos(animationProgress * 0.05F) * 0.05F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
        this.body.render(matrices, vertices, light, overlay, color);
        this.skirt.render(matrices, vertices, light, overlay, color);
    }
}
