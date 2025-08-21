package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.village.VillagerProfession;

import java.util.List;
public class ArmorerRepairGuardArmorGoal extends Goal {
    private final GuardEntity guard;
    private VillagerEntity villager;

    public ArmorerRepairGuardArmorGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canStart() {
        List<VillagerEntity> list = this.guard.getWorld().getNonSpectatingEntities(VillagerEntity.class, this.guard.getBoundingBox().expand(10.0D, 3.0D, 10.0D));
        if (!list.isEmpty()) {
            for (VillagerEntity mob : list) {
                if (mob != null) {
                    boolean isArmorerOrWeaponSmith = mob.getVillagerData().profession().matchesKey(VillagerProfession.ARMORER) || mob.getVillagerData().profession().matchesKey(VillagerProfession.WEAPONSMITH);
                    if (isArmorerOrWeaponSmith && guard.getTarget() == null) {
                        if (mob.getVillagerData().profession().matchesKey(VillagerProfession.ARMORER)) {
                            for (int i = 0; i < guard.guardInventory.size() - 2; ++i) {
                                ItemStack itemstack = guard.guardInventory.getStack(i);
                                if (isArmorItem(itemstack) && itemstack.getDamage() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                        if (mob.getVillagerData().profession().matchesKey(VillagerProfession.WEAPONSMITH)) {
                            for (int i = 4; i < 6; ++i) {
                                ItemStack itemstack = guard.guardInventory.getStack(i);
                                if (itemstack.isDamaged() && itemstack.getDamage() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        guard.getLookControl().lookAt(villager, 30.0F, 30.0F);
        if (guard.distanceTo(villager) >= 2.0D) {
            guard.getNavigation().startMovingTo(villager, 0.5D);
            villager.getNavigation().startMovingTo(guard, 0.5D);
        } else {
            RegistryEntry<VillagerProfession> profession = villager.getVillagerData().profession();
            if (profession.matchesKey(VillagerProfession.ARMORER)) {
                for (int i = 0; i < guard.guardInventory.size() - 2; ++i) {
                    ItemStack itemstack = guard.guardInventory.getStack(i);
                    if (isArmorItem(itemstack) && itemstack.getDamage() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamage(itemstack.getDamage() - guard.getRandom().nextInt(5));
                    }
                }
            }
            if (profession.matchesKey(VillagerProfession.WEAPONSMITH)) {
                for (int i = 4; i < 6; ++i) {
                    ItemStack itemstack = guard.guardInventory.getStack(i);
                    if (itemstack.isDamaged() && itemstack.getDamage() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamage(itemstack.getDamage() - guard.getRandom().nextInt(5));
                    }
                }
            }
        }
    }

    private boolean isArmorItem(ItemStack itemstack) {
        return itemstack.isIn(ItemTags.HEAD_ARMOR) || itemstack.isIn(ItemTags.CHEST_ARMOR) || itemstack.isIn(ItemTags.LEG_ARMOR) || itemstack.isIn(ItemTags.FOOT_ARMOR);
    }
}