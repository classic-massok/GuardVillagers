package dev.sterner.guardvillagers.common.entity.util;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public class ItemUtils {
    public static boolean isArmorItem(ItemStack itemstack) {
        return itemstack.isIn(ItemTags.HEAD_ARMOR)
                || itemstack.isIn(ItemTags.CHEST_ARMOR)
                || itemstack.isIn(ItemTags.LEG_ARMOR)
                || itemstack.isIn(ItemTags.FOOT_ARMOR);
    }
}

