package dev.sterner.guardvillagers.common.entity;

import dev.sterner.guardvillagers.GuardVillagers;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;

public class GuardEntityLootTables {
    public static final LootContextType SLOT = LootContextTypes.register("slot", (builder) -> {
        builder.allow(LootContextParameters.THIS_ENTITY);
    });

    public static final Identifier GUARD_MAIN_HAND = GuardVillagers.id("entities/guard_main_hand");
    public static final Identifier GUARD_OFF_HAND = GuardVillagers.id("entities/guard_off_hand");
    public static final Identifier GUARD_HELMET = GuardVillagers.id("entities/guard_helmet");
    public static final Identifier GUARD_CHEST = GuardVillagers.id("entities/guard_chestplate");
    public static final Identifier GUARD_LEGGINGS = GuardVillagers.id("entities/guard_legs");
    public static final Identifier GUARD_FEET = GuardVillagers.id( "entities/guard_feet");
}
