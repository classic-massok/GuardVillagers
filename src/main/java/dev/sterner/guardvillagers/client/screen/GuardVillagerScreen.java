package dev.sterner.guardvillagers.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GuardVillagerScreen extends HandledScreen<GuardVillagerScreenHandler> {
    private static final Identifier GUARD_GUI_TEXTURES = GuardVillagers.id("textures/gui/inventory.png");
    /*
    private static final Identifier GUARD_GUI_TEXTURES = GuardVillagers.id("textures/gui/inventory.png");
    private static final Identifier GUARD_FOLLOWING_ICON = GuardVillagers.id( "textures/gui/following_icons.png");
    private static final Identifier GUARD_NOT_FOLLOWING_ICON = GuardVillagers.id("textures/gui/not_following_icons.png");
    private static final Identifier PATROL_ICON = GuardVillagers.id( "textures/gui/patrollingui.png");
    private static final Identifier NOT_PATROLLING_ICON = GuardVillagers.id("textures/gui/notpatrollingui.png");


     */
    private static final ButtonTextures GUARD_FOLLOWING_ICONS = new ButtonTextures(GuardVillagers.id( "following/following"),GuardVillagers.id( "following/following_highlighted"));
    private static final ButtonTextures GUARD_NOT_FOLLOWING_ICONS = new ButtonTextures(GuardVillagers.id( "following/not_following"),GuardVillagers.id("following/not_following_highlighted"));
    private static final ButtonTextures GUARD_PATROLLING_ICONS = new ButtonTextures(GuardVillagers.id( "patrolling/patrolling1"), GuardVillagers.id("patrolling/patrolling2"));
    private static final ButtonTextures GUARD_NOT_PATROLLING_ICONS = new ButtonTextures(GuardVillagers.id("patrolling/notpatrolling1"), GuardVillagers.id( "patrolling/notpatrolling2"));


    private final PlayerEntity player;
    private final GuardEntity guardEntity;
    private float mousePosX;
    private float mousePosY;
    private boolean buttonPressed;

    public GuardVillagerScreen(GuardVillagerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, handler.guardEntity.getDisplayName());
        this.titleX = 80;
        this.playerInventoryTitleX = 100;
        this.player = inventory.player;
        guardEntity = handler.guardEntity;
    }

    @Override
    protected void init() {
        super.init();
        if (!GuardVillagersConfig.followHero || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 100, this.height / 2 - 40, 20, 18, GUARD_FOLLOWING_ICONS, GUARD_NOT_FOLLOWING_ICONS, true,
                    (button) -> {
                        ClientPlayNetworking.send(new GuardFollowPacket(guardEntity.getId()));
                    })
            );
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 120, this.height / 2 - 40, 20, 18, GUARD_PATROLLING_ICONS, GUARD_NOT_PATROLLING_ICONS, false,
                    (button) -> {
                        buttonPressed = !buttonPressed;
                        ClientPlayNetworking.send(new GuardPatrolPacket(guardEntity.getId(), buttonPressed));
                    })
            );
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        // No explicit RenderSystem.setShader(...) needed in 1.21.x
        // The default color is already white, so setShaderColor is unnecessary too.

        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;

        // draw the full GUI texture (no manual shader calls needed in 1.21.x)
        ctx.drawTexture(
                net.minecraft.client.render.RenderLayer::getGuiTextured, // RenderLayer supplier
                GUARD_GUI_TEXTURES,
                i, j,                  // x, y on screen
                0f, 0f,             // u, v in texture
                this.backgroundWidth,  // region width
                this.backgroundHeight, // region height
                256, 256               // full texture size (set to your PNG size if not 256x256)
        );

        InventoryScreen.drawEntity(ctx, i + 51, j + 75, (i + 51), (j + 75 - 50), 30, 0.0625f, this.mousePosX, this.mousePosY, this.guardEntity);
    }

    /*
    CONTAINER(Identifier.ofVanilla("hud/heart/container"), Identifier.ofVanilla("hud/heart/container_blinking"), Identifier.ofVanilla("hud/heart/container"), Identifier.ofVanilla("hud/heart/container_blinking"), Identifier.ofVanilla("hud/heart/container_hardcore"), Identifier.ofVanilla("hud/heart/container_hardcore_blinking"), Identifier.ofVanilla("hud/heart/container_hardcore"), Identifier.ofVanilla("hud/heart/container_hardcore_blinking")),
        NORMAL(Identifier.ofVanilla("hud/heart/full"), Identifier.ofVanilla("hud/heart/full_blinking"), Identifier.ofVanilla("hud/heart/half"), Identifier.ofVanilla("hud/heart/half_blinking"), Identifier.ofVanilla("hud/heart/hardcore_full"), Identifier.ofVanilla("hud/heart/hardcore_full_blinking"), Identifier.ofVanilla("hud/heart/hardcore_half"), Identifier.ofVanilla("hud/heart/hardcore_half_blinking")),
        POISONED(Identifier.ofVanilla("hud/heart/poisoned_full"), Identifier.ofVanilla("hud/heart/poisoned_full_blinking"), Identifier.ofVanilla("hud/heart/poisoned_half"), Identifier.ofVanilla("hud/heart/poisoned_half_blinking"), Identifier.ofVanilla("hud/heart/poisoned_hardcore_full"), Identifier.ofVanilla("hud/heart/poisoned_hardcore_full_blinking"), Identifier.ofVanilla("hud/heart/poisoned_hardcore_half"), Identifier.ofVanilla("hud/heart/poisoned_hardcore_half_blinking")),
        WITHERED(Identifier.ofVanilla("hud/heart/withered_full"), Identifier.ofVanilla("hud/heart/withered_full_blinking"), Identifier.ofVanilla("hud/heart/withered_half"), Identifier.ofVanilla("hud/heart/withered_half_blinking"), Identifier.ofVanilla("hud/heart/withered_hardcore_full"), Identifier.ofVanilla("hud/heart/withered_hardcore_full_blinking"), Identifier.ofVanilla("hud/heart/withered_hardcore_half"), Identifier.ofVanilla("hud/heart/withered_hardcore_half_blinking")),
        ABSORBING(Identifier.ofVanilla("hud/heart/absorbing_full"), Identifier.ofVanilla("hud/heart/absorbing_full_blinking"), Identifier.ofVanilla("hud/heart/absorbing_half"), Identifier.ofVanilla("hud/heart/absorbing_half_blinking"), Identifier.ofVanilla("hud/heart/absorbing_hardcore_full"), Identifier.ofVanilla("hud/heart/absorbing_hardcore_full_blinking"), Identifier.ofVanilla("hud/heart/absorbing_hardcore_half"), Identifier.ofVanilla("hud/heart/absorbing_hardcore_half_blinking")),
        FROZEN(Identifier.ofVanilla("hud/heart/frozen_full"), Identifier.ofVanilla("hud/heart/frozen_full_blinking"), Identifier.ofVanilla("hud/heart/frozen_half"), Identifier.ofVanilla("hud/heart/frozen_half_blinking"), Identifier.ofVanilla("hud/heart/frozen_hardcore_full"), Identifier.ofVanilla("hud/heart/frozen_hardcore_full_blinking"), Identifier.ofVanilla("hud/heart/frozen_hardcore_half"), Identifier.ofVanilla("hud/heart/frozen_hardcore_half_blinking"));

     */

    private static final Identifier ARMOR_EMPTY_TEXTURE = Identifier.ofVanilla("hud/armor_empty");
    private static final Identifier ARMOR_HALF_TEXTURE = Identifier.ofVanilla("hud/armor_half");
    private static final Identifier ARMOR_FULL_TEXTURE = Identifier.ofVanilla("hud/armor_full");

    private void drawHeart(DrawContext context, HeartType type, int x, int y, boolean half) {
        RenderSystem.enableBlend();
        context.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, type.getTexture(half), x, y, 9, 9);
        RenderSystem.disableBlend();
    }

    @Override
    protected void drawForeground(DrawContext ctx, int x, int y) {
        super.drawForeground(ctx, x, y);
        int health = MathHelper.ceil(guardEntity.getHealth());
        int armor = guardEntity.getArmor();

        boolean statusU = guardEntity.hasStatusEffect(StatusEffects.POISON);
        boolean statusW = guardEntity.hasStatusEffect(StatusEffects.WITHER);
        var heart = statusU ? HeartType.POISONED : statusW ? HeartType.WITHERED : guardEntity.isFrozen() ? HeartType.FROZEN : HeartType.NORMAL;
        //Health
        for (int i = 0; i < 10; i++) {
            this.drawHeart(ctx, HeartType.CONTAINER, (i * 8) + 80, 20, false);
            //ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16, 0, 9, 9);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                this.drawHeart(ctx, HeartType.NORMAL, (i * 8) + 80, 20, false);
                this.drawHeart(ctx, HeartType.NORMAL, ((i + 1) * 8) + 80, 20, true);
                //ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
                //ctx.drawTexture(ICONS, ((i + 1) * 8) + 80, 20, 16 + 9 * (5 + statusU), 0, 9, 9);
            } else {
                //ctx.drawTexture(ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9);
                this.drawHeart(ctx, HeartType.NORMAL, (i * 8) + 80, 20, false);
            }
        }
        //Armor
        for (int i = 0; i < 10; i++) {
            ctx.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ARMOR_EMPTY_TEXTURE, (i * 8) + 80, 30, 9, 9);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                ctx.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ARMOR_FULL_TEXTURE, (i * 8) + 80, 30, 9, 9);
                ctx.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ARMOR_HALF_TEXTURE, ((i + 1) * 8) + 80, 30, 9, 9);
            } else {
                ctx.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, ARMOR_FULL_TEXTURE, (i * 8) + 80, 30, 9, 9);
            }
        }

    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ctx, mouseX, mouseY, partialTicks);
        this.mousePosX = (float) mouseX;
        this.mousePosY = (float) mouseY;
        super.render(ctx, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }


    class GuardGuiButton extends TexturedButtonWidget {
        private ButtonTextures texture;
        private ButtonTextures newTexture;
        private boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn, ButtonTextures resourceLocationIn, ButtonTextures newTexture, boolean isFollowButton, ButtonWidget.PressAction  onPressIn) {
            super(xIn, yIn, widthIn, heightIn, resourceLocationIn, onPressIn);
            this.texture = resourceLocationIn;
            this.newTexture = newTexture;
            this.isFollowButton = isFollowButton;
        }

        public boolean requirementsForTexture() {
            boolean following = GuardVillagerScreen.this.guardEntity.isFollowing();
            boolean patrol = GuardVillagerScreen.this.guardEntity.isPatrolling();
            return this.isFollowButton ? following : patrol;
        }

        @Override
        public void renderWidget(DrawContext graphics, int mouseX, int mouseY, float partialTicks) {
            ButtonTextures icon = this.requirementsForTexture() ? this.texture : this.newTexture;
            Identifier resourcelocation = icon.get(this.isFocused(), this.isSelected());
            graphics.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, resourcelocation, this.getX(), this.getY(), this.width, this.height);
        }
    }

    @Environment(value= EnvType.CLIENT)
    static enum HeartType {
        CONTAINER(Identifier.ofVanilla("hud/heart/container"), Identifier.ofVanilla("hud/heart/container")),
        NORMAL(Identifier.ofVanilla("hud/heart/full"), Identifier.ofVanilla("hud/heart/half")),
        POISONED(Identifier.ofVanilla("hud/heart/poisoned_full"), Identifier.ofVanilla("hud/heart/poisoned_half")),
        WITHERED(Identifier.ofVanilla("hud/heart/withered_full"), Identifier.ofVanilla("hud/heart/withered_half")),
        FROZEN(Identifier.ofVanilla("hud/heart/frozen_full"), Identifier.ofVanilla("hud/heart/frozen_half"));

        private final Identifier fullTexture;
        private final Identifier halfTexture;

        private HeartType(Identifier fullTexture, Identifier halfTexture) {
            this.fullTexture = fullTexture;
            this.halfTexture = halfTexture;
        }

        public Identifier getTexture(boolean half) {
            if (half) {
                return this.halfTexture;
            }
            return this.fullTexture;
        }
    }

}
