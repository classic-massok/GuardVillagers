package dev.sterner.guardvillagers.client.renderer;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.GuardArmorModel;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.client.render.state.GuardBipedRenderState;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.util.math.MatrixStack;
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
        // Base model: villager-shaped guard
        super(context, new GuardVillagerModel(context.getPart(GuardVillagersClient.GUARD)), 0.5F);
        this.normal = this.getModel();

        // Optional Steve-shaped guard (player model). PlayerEntityModel still extends biped model in 1.21.x,
        // so we can keep it as our generic biped model type here.
        BipedEntityModel<GuardBipedRenderState> steve = new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER));

        if (GuardVillagersConfig.useSteveModel) {
            this.model = steve;
        } else {
            this.model = normal;
        }

        // Create concrete variables first (helps the compiler)
        BipedEntityModel<GuardBipedRenderState> innerArmor =
                !GuardVillagersConfig.useSteveModel
                        ? new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_INNER))
                        : new BipedEntityModel<>(context.getPart(GuardVillagersClient.GUARD_ARMOR_INNER));

        BipedEntityModel<GuardBipedRenderState> outerArmor =
                !GuardVillagersConfig.useSteveModel
                        ? new GuardArmorModel(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER))
                        : new BipedEntityModel<>(context.getPart(GuardVillagersClient.GUARD_ARMOR_OUTER));


        EquipmentModelData<BipedEntityModel<GuardBipedRenderState>> innerData =
                new EquipmentModelData<>(innerArmor, innerArmor, innerArmor, innerArmor);
        EquipmentModelData<BipedEntityModel<GuardBipedRenderState>> outerData =
                new EquipmentModelData<>(outerArmor, outerArmor, outerArmor, outerArmor);

        this.addFeature(new ArmorFeatureRenderer<>(this, innerData, outerData, context.getEquipmentRenderer()));
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
