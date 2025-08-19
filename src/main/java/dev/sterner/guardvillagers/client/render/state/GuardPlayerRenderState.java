// dev/sterner/guardvillagers/client/render/state/GuardPlayerRenderState.java
package dev.sterner.guardvillagers.client.render.state;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class GuardPlayerRenderState extends PlayerEntityRenderState {
    public int kickTicks;

    // Add the minimal fields your old code used:
    public ItemStack mainHandStack = ItemStack.EMPTY;
    public ItemStack offHandStack = ItemStack.EMPTY;

    public boolean isEating;          // mirrors entity.isEating()
    public int itemUseTimeLeft;       // mirrors entity.getItemUseTimeLeft()
    public Hand activeHand;           // mirrors entity.getActiveHand()

    public ItemStack getStackInHand(Hand hand) {
        return hand == Hand.MAIN_HAND ? mainHandStack : offHandStack;
    }
}