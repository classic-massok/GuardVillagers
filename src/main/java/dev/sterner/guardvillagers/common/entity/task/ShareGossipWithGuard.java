package dev.sterner.guardvillagers.common.entity.task;

import com.google.common.collect.ImmutableMap;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.EntityLookTarget;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;

public class ShareGossipWithGuard extends MultiTickTask<VillagerEntity> {
    public ShareGossipWithGuard() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleState.VALUE_PRESENT));
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
//        return LookTargetUtil.canSee(villagerEntity.getBrain(), MemoryModuleType.INTERACTION_TARGET, GuardVillagers.GUARD_VILLAGER);
        return Objects.requireNonNull(villagerEntity.getBrain()
                        .getOptionalMemory(MemoryModuleType.INTERACTION_TARGET))
                .map(target -> target.getType() == GuardVillagers.GUARD_VILLAGER && villagerEntity.canSee(target))
                .orElse(false);
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        return this.shouldRun(serverWorld, villagerEntity);
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
//        LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, guard, 0.5F, 2);
        villagerEntity.getLookControl().lookAt(guard, 30.0F, 30.0F);
        guard.getLookControl().lookAt(villagerEntity, 30.0F, 30.0F);

        villagerEntity.getNavigation().startMovingTo(guard, 0.5F);
        guard.getNavigation().startMovingTo(villagerEntity, 0.5F);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.squaredDistanceTo(guard) < 5.0D) {
//            LookTargetUtil.lookAtAndWalkTowardsEachOther(villagerEntity, guard, 0.5F, 2);
            villagerEntity.getLookControl().lookAt(guard, 30.0F, 30.0F);
            guard.getLookControl().lookAt(villagerEntity, 30.0F, 30.0F);

            villagerEntity.getNavigation().startMovingTo(guard, 0.5F);
            guard.getNavigation().startMovingTo(villagerEntity, 0.5F);
            guard.gossip(villagerEntity, time);
        }
    }

    @Override
    protected void finishRunning(ServerWorld pLevel, VillagerEntity villagerEntity, long time) {
        villagerEntity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
    }
}