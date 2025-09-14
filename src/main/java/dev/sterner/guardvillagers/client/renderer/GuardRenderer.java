package dev.sterner.guardvillagers.client.renderer;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.GuardArmorModel;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.client.render.state.GuardBipedRenderState;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.item.consume.UseAction; // moved in 1.21.4
import org.jetbrains.annotations.Nullable;

/**
 * Renderer updated for 1.21.4 render-state API with minimal changes.
 */
public class GuardRenderer extends BipedEntityRenderer<
        GuardEntity,
        GuardBipedRenderState,
        BipedEntityModel<GuardBipedRenderState>> {

    private final BipedEntityModel<GuardBipedRenderState> normal;

    public GuardRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardVillagerModel(context.getPart(GuardVillagersClient.GUARD)), 0.5F);
        this.normal = this.getModel();

        BipedEntityModel<GuardBipedRenderState> steve =
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER));
        this.model = GuardVillagersConfig.useSteveModel ? steve : normal;

        // --- NEW: build independent models per slot ---
        BipedEntityModel<GuardBipedRenderState> headOuter =
                new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER_HEAD));
        BipedEntityModel<GuardBipedRenderState> chestOuter =
                new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER_CHEST));
        BipedEntityModel<GuardBipedRenderState> feetOuter =
                new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER_FEET));
        BipedEntityModel<GuardBipedRenderState> legsInner =
                new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_INNER_LEGS));

        // Permanently prune visibility so each instance only renders its slot
        pruneToSlot(headOuter,  EquipmentSlot.HEAD);
        pruneToSlot(chestOuter, EquipmentSlot.CHEST);
        pruneToSlot(legsInner,  EquipmentSlot.LEGS);  // leggings -> inner
        pruneToSlot(feetOuter,  EquipmentSlot.FEET);

        // Adult vs Baby model sets: if you don't have baby shapes, reuse adult safely.
        EquipmentModelData<BipedEntityModel<GuardBipedRenderState>> adult =
                new EquipmentModelData<>(headOuter, chestOuter, legsInner, feetOuter);

        this.addFeature(new ArmorFeatureRenderer<>(
                this,
                adult,          // state.baby == false -> uses this set
                adult,          // state.baby == true  -> same set; fine if guards aren’t “baby”
                context.getEquipmentRenderer()
        ));
    }

    private static void pruneToSlot(BipedEntityModel<?> m, EquipmentSlot slot) {
        // Hide everything first
        m.head.visible = m.hat.visible = false;
        m.body.visible = m.rightArm.visible = m.leftArm.visible = false;
        m.rightLeg.visible = m.leftLeg.visible = false;

        // Enable only what this slot needs
        switch (slot) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case CHEST -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS, FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            default -> {}
        }
    }

    /* --------------------
     * New render-state hooks (1.21+)
     * -------------------- */

    @Override
    public GuardBipedRenderState createRenderState() {
        return new GuardBipedRenderState();
    }

    @Override
    public void updateRenderState(GuardEntity entity, GuardBipedRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.guardVariant = entity.getGuardVariant();

        // Mirror vanilla flags that used to live on the model:
        state.sneaking = entity.isSneaking();
        state.mainArm = entity.getMainArm();

        // Populate hand stacks so the arm-pose logic can use them
        ItemStack main = entity.getMainHandStack();
        ItemStack off  = entity.getOffHandStack();

        // Compute arm poses (used to be assigned on the model; now they go on the state)
        BipedEntityModel.ArmPose mainPose = getArmPose(entity, main, off, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose offPose  = getArmPose(entity, main, off, Hand.OFF_HAND);

        if (state.mainArm == Arm.RIGHT) {
            state.rightArmPose = mainPose;
            state.leftArmPose  = offPose;
        } else {
            state.rightArmPose = offPose;
            state.leftArmPose  = mainPose;
        }
    }

    private BipedEntityModel.ArmPose getArmPose(GuardEntity entityIn, ItemStack itemStackMain, ItemStack itemStackOff, Hand handIn) {
        BipedEntityModel.ArmPose pose = BipedEntityModel.ArmPose.EMPTY;
        ItemStack itemstack = handIn == Hand.MAIN_HAND ? itemStackMain : itemStackOff;

        if (!itemstack.isEmpty()) {
            pose = BipedEntityModel.ArmPose.ITEM;
            if (entityIn.getItemUseTimeLeft() > 0) {
                UseAction useaction = itemstack.getUseAction();
                switch (useaction) {
                    case BLOCK:
                        pose = BipedEntityModel.ArmPose.BLOCK;
                        break;
                    case BOW:
                        pose = BipedEntityModel.ArmPose.BOW_AND_ARROW;
                        break;
                    case SPEAR:
                        pose = BipedEntityModel.ArmPose.THROW_SPEAR;
                        break;
                    case CROSSBOW:
                        if (handIn == entityIn.getActiveHand()) {
                            pose = BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                        }
                        break;
                    default:
                        pose = BipedEntityModel.ArmPose.EMPTY;
                        break;
                }
            } else {
                boolean flag1 = itemStackMain.getItem() instanceof CrossbowItem;
                boolean flag2 = itemStackOff.getItem() instanceof CrossbowItem;
                if (flag1 && entityIn.isAttacking()) {
                    pose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }
                if (flag2 && itemStackMain.getItem().getUseAction(itemStackMain) == UseAction.NONE
                        && entityIn.isAttacking()) {
                    pose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                }
            }
        }
        return pose;
    }

    @Override
    protected void scale(GuardBipedRenderState state, MatrixStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Nullable
    @Override
    public Identifier getTexture(GuardBipedRenderState state) {
        return !GuardVillagersConfig.useSteveModel
                ? GuardVillagers.id("textures/entity/guard/guard_" + state.guardVariant + ".png")
                : GuardVillagers.id("textures/entity/guard/guard_steve_" + state.guardVariant + ".png");
    }
}
