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
 * Model for {@link WeepingOneEntity}.
 *
 * <p>Generated together with its texture (texture size 64x64) from a single geometry spec, so the UV
 * layout of the sheet and the cuboids here are guaranteed to agree.</p>
 */
public class WeepingOneEntityModel extends EntityModel<HollowlightEntityRenderState> {

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart hair;
    private final ModelPart arm_left;
    private final ModelPart arm_right;
    private final ModelPart knee;

    public WeepingOneEntityModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("body").getChild("head");
        this.hair = root.getChild("body").getChild("head").getChild("hair");
        this.arm_left = root.getChild("body").getChild("arm_left");
        this.arm_right = root.getChild("body").getChild("arm_right");
        this.knee = root.getChild("knee");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData body = root.addChild("body",
                ModelPartBuilder.create().uv(0, 0).cuboid(-3.0F, -10.0F, -2.0F, 6.0F, 10.0F, 4.0F),
                ModelTransform.pivot(0.0F, 20.0F, 0.0F));
        ModelPartData head = body.addChild("head",
                ModelPartBuilder.create().uv(21, 0).cuboid(-3.5F, -7.0F, -3.5F, 7.0F, 7.0F, 7.0F),
                ModelTransform.pivot(0.0F, -10.0F, 0.0F));
        ModelPartData hair = head.addChild("hair",
                ModelPartBuilder.create().uv(0, 15).cuboid(-4.0F, 0.0F, -4.0F, 8.0F, 9.0F, 1.0F),
                ModelTransform.pivot(0.0F, -7.0F, 0.0F));
        ModelPartData arm_left = body.addChild("arm_left",
                ModelPartBuilder.create().uv(19, 15).cuboid(0.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                ModelTransform.pivot(3.0F, -9.0F, 0.0F));
        ModelPartData arm_right = body.addChild("arm_right",
                ModelPartBuilder.create().uv(28, 15).cuboid(-2.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                ModelTransform.pivot(-3.0F, -9.0F, 0.0F));
        ModelPartData knee = root.addChild("knee",
                ModelPartBuilder.create().uv(37, 15).cuboid(-3.0F, 0.0F, -2.0F, 6.0F, 3.0F, 4.0F),
                ModelTransform.pivot(0.0F, 20.0F, 0.0F));
        return TexturedModelData.of(modelData, 64, 64);
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
        this.head.yaw = headYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;
        this.body.pitch = 0.5F;
        this.head.pitch = 0.9F;
    }

}
