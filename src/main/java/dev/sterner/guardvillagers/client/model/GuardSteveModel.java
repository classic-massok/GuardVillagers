package dev.sterner.guardvillagers.client.model;


import dev.sterner.guardvillagers.client.render.state.GuardPlayerRenderState;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.MathHelper;

public class GuardSteveModel extends PlayerEntityModel {
    public GuardSteveModel(ModelPart root) {
        super(root, false);
    }

    @Override
    public void setAngles(PlayerEntityRenderState state) {
        super.setAngles(state);

        int kickTicks = (state instanceof GuardPlayerRenderState g) ? g.kickTicks : 0;

        if (kickTicks > 0) {
            float f1 = 1.0F - (float) MathHelper.abs(10 - 2 * kickTicks) / 10.0F;
            this.rightLeg.pitch = MathHelper.lerp(f1, this.rightLeg.pitch, -1.40F);
        }

        Arm mainArm = state.mainArm;
        float ageInTicks = state.age;

        if (mainArm == Arm.RIGHT) {
            this.eatingAnimationRightHand(Hand.MAIN_HAND, (GuardPlayerRenderState) state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.OFF_HAND, (GuardPlayerRenderState) state, ageInTicks);
        } else {
            this.eatingAnimationRightHand(Hand.OFF_HAND, (GuardPlayerRenderState) state, ageInTicks);
            this.eatingAnimationLeftHand(Hand.MAIN_HAND, (GuardPlayerRenderState) state, ageInTicks);
        }
    }

    public static TexturedModelData createMesh() {
        ModelData meshdefinition = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    public void eatingAnimationRightHand(Hand hand, GuardPlayerRenderState state, float ageInTicks) {
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

    public void eatingAnimationLeftHand(Hand hand, GuardPlayerRenderState state, float ageInTicks) {
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