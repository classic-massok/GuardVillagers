package dev.sterner.guardvillagers.client.model;

import dev.sterner.guardvillagers.client.render.state.GuardBipedRenderState;
import dev.sterner.guardvillagers.client.render.state.GuardPlayerRenderState;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.MathHelper;

public class GuardVillagerModel extends BipedEntityModel<GuardBipedRenderState> {
    public ModelPart Nose = this.head.getChild("nose");
    public ModelPart quiver = this.body.getChild("quiver");
    public ModelPart ArmLShoulderPad = this.rightArm.getChild("shoulderPad_left");
    public ModelPart ArmRShoulderPad = this.leftArm.getChild("shoulderPad_right");

    public GuardVillagerModel(ModelPart part) {
        super(part);
        this.setRotateAngle(quiver, 0.0F, 0.0F, 0.2617993877991494F);
        this.setRotateAngle(ArmLShoulderPad, 0.0F, 0.0F, -0.3490658503988659F);
        this.setRotateAngle(ArmRShoulderPad, 0.0F, 0.0F, 0.3490658503988659F);
    }

    public static TexturedModelData createBodyLayer() {
        ModelData meshdefinition = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        ModelPartData partdefinition = meshdefinition.getRoot();
        ModelPartData torso = partdefinition.addChild("body", ModelPartBuilder.create().uv(52, 50)
                .cuboid(-4.0F, 0.0F, -2.0F, 8, 12, 4, new Dilation(0.25F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        ModelPartData head = partdefinition.addChild("head", ModelPartBuilder.create().uv(49, 99)
                .cuboid(-4.0F, -10.0F, -4.0F, 8, 10, 8, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 1.0F, 0.0F));
        ModelPartData rightArm = partdefinition.addChild("right_arm", ModelPartBuilder.create().uv(32, 75)
                        .mirrored().cuboid(-3.0F, -2.0F, -2.0F, 4, 12, 4, new Dilation(0.0F)),
                ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        ModelPartData leftArm = partdefinition.addChild("left_arm", ModelPartBuilder.create().uv(33, 48)
                .cuboid(-1.0F, -2.0F, -2.0F, 4, 12, 4, new Dilation(0.0F)), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
        torso.addChild("quiver", ModelPartBuilder.create().uv(100, 0).cuboid(-2.5F, -2.0F, 0.0F, 5, 10, 5,
                new Dilation(0.0F)), ModelTransform.pivot(0.5F, 3.0F, 2.3F));
        head.addChild("nose",
                ModelPartBuilder.create().uv(54, 0).cuboid(-1.0F, 0.0F, -2.0F, 2, 4, 2, new Dilation(0.0F)),
                ModelTransform.pivot(0.0F, -3.0F, -4.0F));
        partdefinition.addChild("right_leg", ModelPartBuilder.create().uv(16, 48).mirrored().cuboid(-2.0F,
                0.0F, -2.0F, 4, 12, 4, new Dilation(0.0F)), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));
        partdefinition.addChild("left_leg", ModelPartBuilder.create().uv(16, 28).cuboid(-2.0F, 0.0F, -2.0F,
                4, 12, 4, new Dilation(0.0F)), ModelTransform.pivot(1.9F, 12.0F, 0.0F));
        leftArm.addChild("shoulderPad_right",
                ModelPartBuilder.create().uv(72, 33).mirrored().cuboid(0.0F, 0.0F, -3.0F, 5, 3, 6, new Dilation(0.0F)),
                ModelTransform.pivot(-0.5F, -3.5F, 0.0F));
        rightArm.addChild("shoulderPad_left",
                ModelPartBuilder.create().uv(72, 33).cuboid(-5.0F, 0.0F, -3.0F, 5, 3, 6, new Dilation(0.0F)),
                ModelTransform.pivot(0.5F, -3.5F, 0.0F));
        partdefinition.addChild("hat", ModelPartBuilder.create().uv(0, 0).cuboid(-4.5F, -11.0F, -4.5F, 9,
                11, 9, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
        return TexturedModelData.of(meshdefinition, 128, 128);
    }

    public void setRotateAngle(ModelPart ModelRenderer, float x, float y, float z) {
        ModelRenderer.pitch = x;
        ModelRenderer.yaw = y;
        ModelRenderer.roll = z;
    }

    @Override
    public void setAngles(GuardBipedRenderState state) {
        super.setAngles(state);

        int kickTicks = (state instanceof GuardBipedRenderState g) ? g.kickTicks : 0;

        if (kickTicks > 0) {
            float f1 = 1.0F - (float) MathHelper.abs(10 - 2 * kickTicks) / 10.0F;
            this.rightLeg.pitch = MathHelper.lerp(f1, this.rightLeg.pitch, -1.40F);
        }

        Arm mainArm = state.mainArm;
        float ageInTicks = state.age;

        if (mainArm == Arm.RIGHT) {
            this.eatingAnimationRightHand(Hand.MAIN_HAND, state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.OFF_HAND, state, ageInTicks);
        } else {
            this.eatingAnimationRightHand(Hand.OFF_HAND, state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.MAIN_HAND, state, ageInTicks);
        }
    }

    public void eatingAnimationRightHand(Hand hand, GuardBipedRenderState state, float ageInTicks) {
        ItemStack itemstack = state.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
                || itemstack.getUseAction() == UseAction.DRINK;
        if ((state.isEating && drinkingoreating)
                || (state.itemUseTimeLeft > 0 && drinkingoreating && state.activeHand == hand)) {
            this.rightArm.yaw = -0.5F;
            this.rightArm.pitch = -1.3F;
            this.rightArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }

    public void eatingAnimationLeftHand(Hand hand, GuardBipedRenderState state, float ageInTicks) {
        ItemStack itemstack = state.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
                || itemstack.getUseAction() == UseAction.DRINK;
        if ((state.isEating && drinkingoreating)
                || (state.itemUseTimeLeft > 0 && drinkingoreating && state.activeHand == hand)) {
            this.leftArm.yaw = 0.5F;
            this.leftArm.pitch = -1.3F;
            this.leftArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            this.head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            this.head.yaw = 0.0F;
            this.hat.copyTransform(head);
        }
    }


}