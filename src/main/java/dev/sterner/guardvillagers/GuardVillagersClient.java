package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.client.model.GuardArmorModel;
import dev.sterner.guardvillagers.client.model.GuardSteveModel;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.client.renderer.GuardRenderer;
import dev.sterner.guardvillagers.client.screen.GuardVillagerScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.entity.model.EntityModelLayer;

import static dev.sterner.guardvillagers.GuardVillagers.*;

public class GuardVillagersClient implements ClientModInitializer {

    public static final EntityModelLayer GUARD = new EntityModelLayer(id("guard"), "main");
    public static final EntityModelLayer GUARD_STEVE = new EntityModelLayer(id("guard_steve"), "main");

    public static final EntityModelLayer GUARD_ARMOR_OUTER_HEAD  = new EntityModelLayer(id("guard_armor_outer_head"),  "main");
    public static final EntityModelLayer GUARD_ARMOR_OUTER_CHEST = new EntityModelLayer(id("guard_armor_outer_chest"), "main");
    public static final EntityModelLayer GUARD_ARMOR_OUTER_FEET  = new EntityModelLayer(id("guard_armor_outer_feet"),  "main");
    public static final EntityModelLayer GUARD_ARMOR_INNER_LEGS  = new EntityModelLayer(id("guard_armor_inner_legs"),  "main");

    @Override
    public void onInitializeClient() {
        HandledScreens.register(GUARD_SCREEN_HANDLER, GuardVillagerScreen::new);

        EntityModelLayerRegistry.registerModelLayer(GUARD, GuardVillagerModel::createBodyLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);

        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER_HEAD,  GuardArmorModel::createOuterArmorLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER_CHEST, GuardArmorModel::createOuterArmorLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_OUTER_FEET,  GuardArmorModel::createOuterArmorLayer);
        EntityModelLayerRegistry.registerModelLayer(GUARD_ARMOR_INNER_LEGS,  GuardArmorModel::createInnerArmorLayer);

        EntityRendererRegistry.register(GUARD_VILLAGER, GuardRenderer::new);
    }
}
