package dev.sterner.guardvillagers.client.render.state;

import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class GuardBipedRenderState extends BipedEntityRenderState {
    public int kickTicks;

    // add this so we can choose the texture without the entity
    public int guardVariant;

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