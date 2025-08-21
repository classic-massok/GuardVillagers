package dev.sterner.guardvillagers.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class UiRenderUtil {
    private UiRenderUtil() {}

    /**
     * Draws a living entity in a GUI, rotating it based on the mouse like vanilla inventories.
     *
     * @param ctx     Draw context
     * @param mouseX  current mouse X
     * @param mouseY  current mouse Y
     * @param entity  the entity to render
     */
    public static void drawEntityFollowMouse(DrawContext ctx, int x, int y, int mouseX, int mouseY, LivingEntity entity) {
        // Define the on-screen rect the entity should occupy
        int x1 = x + 15;         // top-left X of the preview area
        int x2 = x1 + 70;             // bottom-right X (width ~70)
        int y2 = y + 95;            // bottom-right Y (height ~100)

        int size = 27;                // scale; tweak 35–60 as you like

        // Mouse deltas relative to the rect center (controls turn/tilt)
        int cx = (x1 + x2) / 2;
        int cy = (y + y2) / 2;

        // 'f' is a base rotation bias; 0 is fine for most screens
        float base = 0.0F;

        InventoryScreen.drawEntity(ctx, x1, y, x2, y2, size, base, mouseX, mouseY, entity);
    }
}

