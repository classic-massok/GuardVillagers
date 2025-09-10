package dev.sterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.util.ItemUtils;
import dev.sterner.guardvillagers.common.network.GuardData;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import dev.sterner.guardvillagers.common.entity.goal.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.DamageResistantComponent;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.VillagerGossips;
import net.minecraft.village.VillagerType;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Uuids;

import java.util.*;
import java.util.function.Predicate;
// TODO: Fix errors
public class GuardEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob, Angerable, InventoryChangedListener, InteractionObserver {
    protected static final TrackedData<Optional<LazyEntityReference<LivingEntity>>> OWNER_UNIQUE_ID = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.LAZY_ENTITY_REFERENCE);
    private static final EntityAttributeModifier USE_ITEM_SPEED_PENALTY = new EntityAttributeModifier(GuardVillagers.id("speed_penalty"), -0.25D, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final TrackedData<Optional<BlockPos>> GUARD_POS = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> PATROLLING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GUARD_VARIANT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RUNNING_TO_EAT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DATA_CHARGING_STATE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> KICKING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FOLLOWING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Map<EntityPose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<EntityPose, EntityDimensions>builder().put(EntityPose.STANDING, EntityDimensions.changing(0.6F, 1.95F)).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS).put(EntityPose.GLIDING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SWIMMING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.CROUCHING, EntityDimensions.changing(0.6F, 1.75F)).put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    private static final UniformIntProvider angerTime = TimeHelper.betweenSeconds(20, 39);
    public static final Map<EquipmentSlot, RegistryKey<LootTable>> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardEntityLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardEntityLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardEntityLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardEntityLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardEntityLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardEntityLootTables.GUARD_FEET);
    });
    private final VillagerGossips gossips = new VillagerGossips();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public SimpleInventory guardInventory = new SimpleInventory(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    public boolean spawnWithArmor;
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;

    @Override
    public int getSafeFallDistance() {
        return 3; // won’t plan paths that require dropping > ~3 blocks
    }

    public GuardEntity(EntityType<? extends GuardEntity> type, World world) {
        super(type, world);
        this.guardInventory.addListener(this);
        this.setPersistent();
        if (GuardVillagersConfig.guardEntitysOpenDoors)
            this.getNavigation().setCanOpenDoors(true);
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> 1;
            case FEET -> 3;
            case LEGS -> 2;
            default -> 0;
        };
    }

    /**
     * Credit - SmellyModder for Biome Specific Textures
     */
    public static int getRandomTypeForBiome(WorldAccess world, BlockPos pos) {
        RegistryKey<VillagerType> type = VillagerType.forBiome(world.getBiome(pos));
        if (type == VillagerType.SNOW) return 6;
        else if (type == VillagerType.TAIGA) return 5;
        else if (type == VillagerType.JUNGLE) return 4;
        else if (type == VillagerType.SWAMP) return 3;
        else if (type == VillagerType.SAVANNA) return 2;
        else if (type == VillagerType.DESERT) return 1;
        else return 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, GuardVillagersConfig.healthModifier)
                .add(EntityAttributes.MOVEMENT_SPEED, GuardVillagersConfig.speedModifier)
                .add(EntityAttributes.ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.FOLLOW_RANGE, GuardVillagersConfig.followRangeModifier);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        this.setPersistent();
        int type = GuardEntity.getRandomTypeForBiome(world, this.getBlockPos());
        if (entityData instanceof GuardEntity.GuardEntityData) {
            type = ((GuardEntity.GuardEntityData) entityData).variantData;
            entityData = new GuardEntity.GuardEntityData(type);
        }
        this.setGuardEntityVariant(type);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    protected void pushAway(Entity entity) {
        if (entity instanceof PathAwareEntity living) {
            boolean attackTargets = living.getTarget() instanceof VillagerEntity || living.getTarget() instanceof IronGolemEntity || living.getTarget() instanceof GuardEntity;
            if (attackTargets) this.setTarget(living);
        }
        super.pushAway(entity);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.dataTracker.get(GUARD_POS).orElse(null);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.dataTracker.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardVillagers.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.ITEM_SHIELD_BLOCK.value();
        } else {
            return GuardVillagers.GUARD_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardVillagers.GUARD_DEATH;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            Random random = getWorld().getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasAnyEnchantmentsWith(itemstack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP) && random.nextFloat() < GuardVillagersConfig.chanceToDropEquipment)
                this.dropStack(world, itemstack);
        }
    }

    @Override
    public void readCustomData(ReadView rv) {
        super.readCustomData(rv);

        rv.read("Owner", Uuids.STRICT_CODEC).ifPresent(this::setOwnerId);

        this.setGuardEntityVariant(rv.getInt("Type", 0));
        this.kickTicks = rv.getInt("KickTicks", 0);
        this.setFollowing(rv.getBoolean("Following", false));
        this.interacting = rv.getBoolean("Interacting", false);
        this.setPatrolling(rv.getBoolean("Patrolling", false));
        this.shieldCoolDown = rv.getInt("KickCooldown", 0);
        this.kickCoolDown = rv.getInt("ShieldCooldown", 0);
        this.lastGossipDecayTime = rv.getLong("LastGossipDecay", 0);
        this.lastGossipTime = rv.getLong("LastGossipTime", 0);
        this.spawnWithArmor = rv.getBoolean("SpawnWithArmor", false);
        rv.getOptionalInt("PatrolPosX").ifPresent(x -> {
            int y = rv.getInt("PatrolPosY", 0);
            int z = rv.getInt("PatrolPosZ", 0);
            this.dataTracker.set(GUARD_POS, Optional.of(new BlockPos(x, y, z)));
        });

        rv.read("Gossips", VillagerGossips.CODEC).ifPresent(decoded -> {
            this.gossips.clear();
            this.gossips.add(decoded);
        });

        rv.getOptionalListReadView("Inventory").ifPresent((net.minecraft.storage.ReadView.ListReadView list) -> {
            for (net.minecraft.storage.ReadView entry : list) {
                int slot = Byte.toUnsignedInt(entry.getByte("Slot", (byte) -1));
                if (slot >= this.guardInventory.size()) continue;

                // Use MAP_CODEC here; annotate lambda to avoid generic T
                entry.read(net.minecraft.item.ItemStack.MAP_CODEC).ifPresent((net.minecraft.item.ItemStack stack) -> {
                    if (!stack.isEmpty()) {
                        this.guardInventory.setStack(slot, stack);
                    }
                });
            }
        });

        // Armor items: typed list of ItemStacks
        rv.getOptionalTypedListView("ArmorItems", ItemStack.CODEC).ifPresent(armorList -> {
            for (ItemStack stack : armorList) {
                if (stack == null || stack.isEmpty()) continue;

                EquipmentSlot slot;
                EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
                slot = (eq != null) ? eq.slot() : getPreferredEquipmentSlot(stack);

                int index = GuardEntity.slotToInventoryIndex(slot);
                this.guardInventory.setStack(index, stack);
            }
        });

        // Hand items: typed list of ItemStacks; clamp to 2
        rv.getOptionalTypedListView("HandItems", ItemStack.CODEC).ifPresent(handList -> {
            int i = 0;
            for (ItemStack stack : handList) {
                if (i >= 2) break;
                if (stack == null || stack.isEmpty()) { i++; continue; }
                int handSlot = (i == 0) ? 5 : 4; // keep your existing mapping
                this.guardInventory.setStack(handSlot, stack);
                i++;
            }
        });

        // Anger data (method renamed in 1.21.6)
        if (!this.getWorld().isClient) {
            Angerable angerable = this;
            angerable.readAngerFromData(this.getWorld(), rv);
        }
    }

    @Nullable
    private static UUID readUuidFlexible(NbtCompound nbt, String key) {
        if (!nbt.contains(key)) return null;

        // 1) String form: guard with type check, then use getString (always returns String)
        if (nbt.contains(key)) {
            String s = nbt.getString(key).orElse("");
            if (!s.isEmpty()) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 2) 4-int array form (common in older saves)
        if (nbt.contains(key)) {
            int[] arr = nbt.getIntArray(key).orElse(new int[0]);
            if (arr.length == 4) {
                return Uuids.toUuid(arr);
            }
        }

        // 3) 2-long array form
        if (nbt.contains(key)) {
            long[] arr = nbt.getLongArray(key).orElse(new long[0]);
            if (arr.length == 2) {
                return new UUID(arr[0], arr[1]);
            }
        }

        return null;
    }

    static final int consumeEventId = 2003;

    @Override
    protected void consumeItem() {
        if (this.isUsingItem()) {
            Hand hand = this.getActiveHand();
            if (!this.activeItemStack.equals(this.getStackInHand(hand))) {
                this.stopUsingItem();
            } else {
                if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
                    this.getWorld().syncWorldEvent(consumeEventId, this.getBlockPos(), Item.getRawId(this.activeItemStack.getItem()));
                    ItemStack itemStack = this.activeItemStack.finishUsing(this.getWorld(), this);
                    if (itemStack != this.activeItemStack) {
                        this.setStackInHand(hand, itemStack);
                    }
                    if (!(this.activeItemStack.getUseAction() == UseAction.EAT)) this.activeItemStack.decrement(1);
                    this.stopUsingItem();
                }

            }
        }
    }

    @Override
    public void writeCustomData(WriteView wv) {
        super.writeCustomData(wv);

        // Primitives
        wv.putInt("Type", this.getGuardEntityVariant());
        wv.putInt("KickTicks", this.kickTicks);
        wv.putInt("ShieldCooldown", this.shieldCoolDown);
        wv.putInt("KickCooldown", this.kickCoolDown);
        wv.putBoolean("Following", this.isFollowing());
        wv.putBoolean("Interacting", this.interacting);
        wv.putBoolean("Patrolling", this.isPatrolling());
        wv.putBoolean("SpawnWithArmor", this.spawnWithArmor);
        wv.putLong("LastGossipTime", this.lastGossipTime);
        wv.putLong("LastGossipDecay", this.lastGossipDecayTime);

        // Owner (write as a UUID string; matches read side with Uuids.STRING_CODEC)
        this.dataTracker.get(OWNER_UNIQUE_ID)
                .map(LazyEntityReference::getUuid)
                .ifPresent(uuid -> wv.put("Owner", Uuids.STRING_CODEC, uuid));

        // Inventory: list of { Slot, <stack fields…> } using a codec (no NBT classes)
        // Only write non-empty stacks, same as before.
        final int invSize = this.guardInventory.size();
        List<StackWithSlot> invOut = new ArrayList<>();
        for (int slot = 0; slot < invSize; ++slot) {
            ItemStack stack = this.guardInventory.getStack(slot);
            if (!stack.isEmpty()) {
                invOut.add(new StackWithSlot(slot, stack.copy()));
            }
        }
        if (!invOut.isEmpty()) {
            wv.put("Inventory", StackWithSlot.CODEC.listOf(), invOut);
        }

        if (this.getPatrolPos() != null) {
            BlockPos p = this.getPatrolPos();
            wv.putInt("PatrolPosX", p.getX());
            wv.putInt("PatrolPosY", p.getY());
            wv.putInt("PatrolPosZ", p.getZ());
        }

        // Gossips via codec
        wv.put("Gossips", VillagerGossips.CODEC, this.gossips);

        // Anger
        this.writeAngerToData(wv);
    }

    private void maybeDecayGossip() {
        long i = getWorld().getTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                return this.guardInventory.getStack(0);
            case CHEST:
                return this.guardInventory.getStack(1);
            case LEGS:
                return this.guardInventory.getStack(2);
            case FEET:
                return this.guardInventory.getStack(3);
            case OFFHAND:
                return this.guardInventory.getStack(4);
            case MAINHAND:
                return this.guardInventory.getStack(5);
        }
        return ItemStack.EMPTY;
    }


    public VillagerGossips getGossips() {
        return this.gossips;
    }

    public int getPlayerEntityReputation(PlayerEntity player) {
        return this.gossips.getReputationFor(player.getUuid(), (gossipType) -> true);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            boolean heroOfTheVillage = uuid != null && getWorld().getPlayerByUuid(uuid) != null && getWorld().getPlayerByUuid(uuid).hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
            return uuid == null || (getWorld().getPlayerByUuid(uuid) != null && (!heroOfTheVillage && GuardVillagersConfig.followHero) || !GuardVillagersConfig.followHero && getWorld().getPlayerByUuid(uuid) == null) ? null : getWorld().getPlayerByUuid(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.dataTracker.get(OWNER_UNIQUE_ID)
                .map(LazyEntityReference::getUuid)
                .orElse(null);
    }

    public void setOwnerId(@Nullable UUID id) {
        if (id == null) {
            this.dataTracker.set(OWNER_UNIQUE_ID, Optional.empty());
            return;
        }
        LivingEntity owner = (this.getWorld() != null) ? this.getWorld().getPlayerByUuid(id) : null;
        if (owner != null) {
            this.dataTracker.set(OWNER_UNIQUE_ID, Optional.of(new LazyEntityReference<>(owner)));
        } else {
            this.dataTracker.set(OWNER_UNIQUE_ID, Optional.empty()); // still persist to NBT via your readUuidFlexible
        }
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        if (this.isKicking()) {
            ((LivingEntity) target).takeKnockback(1.0F, MathHelper.sin(this.getYaw() * ((float) Math.PI / 180F)), (-MathHelper.cos(this.getYaw() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            world.sendEntityStatus(this, (byte) 4);
            this.lookAtEntity(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandStack();
        hand.damage(1, this, EquipmentSlot.MAINHAND);
        return super.tryAttack(world, target);
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 4) {
            this.kickTicks = 10;
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if ((getWorld().getDifficulty() == Difficulty.NORMAL || getWorld().getDifficulty() == Difficulty.HARD) && damageSource.getAttacker() instanceof ZombieEntity) {
            ZombieVillagerEntity zombieguard = this.convertTo(
                    EntityType.ZOMBIE_VILLAGER,
                    EntityConversionContext.create(this, /*keepEquipment*/ true, /*preserveCanPickUpLoot*/ false),
                    zv -> {} // no-op finalizer
            );
            if (getWorld().getDifficulty() != Difficulty.HARD && this.random.nextBoolean() || zombieguard == null) {
                return;
            }
            zombieguard.initialize((ServerWorldAccess) getWorld(), getWorld().getLocalDifficulty(zombieguard.getBlockPos()), SpawnReason.CONVERSION, new ZombieEntity.ZombieData(false, true));
            if (!this.isSilent()) getWorld().syncWorldEvent(null, 1026, this.getBlockPos(), 0);
            this.discard();
        }
        super.onDeath(damageSource);
    }

    @Override
    public void tickMovement() {
        if (this.kickTicks > 0)
            --this.kickTicks;
        if (this.kickCoolDown > 0)
            --this.kickCoolDown;
        if (this.shieldCoolDown > 0)
            --this.shieldCoolDown;
        if (this.getHealth() < this.getMaxHealth() && this.age % 200 == 0) {
            this.heal(GuardVillagersConfig.amountOfHealthRegenerated);
        }
        if (spawnWithArmor && this.getWorld() instanceof ServerWorld serverWorld) {
            for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
                for (ItemStack stack : this.getStacksFromLootTable(equipmentslottype, serverWorld)) {
                    this.equipStack(equipmentslottype, stack);
                }
            }
            this.spawnWithArmor = false;
        }
        if (!getWorld().isClient) this.tickAngerLogic((ServerWorld) getWorld(), true);
        this.tickHandSwing();
        super.tickMovement();
    }

    @Override
    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    @Override
    protected EntityDimensions getBaseDimensions(EntityPose pose) {
        return SIZE_BY_POSE.getOrDefault(pose, EntityDimensions.changing(0.6F, 1.95F));
    }


    @Override
    protected void takeShieldHit(ServerWorld serverWorld, LivingEntity entityIn) {
        super.takeShieldHit(serverWorld, entityIn);
        if (entityIn.getMainHandStack().getItem() instanceof AxeItem) this.disableShield(true, entityIn.getMainHandStack().getItem());
    }

    @Override
    public void setCurrentHand(Hand hand) {
        super.setCurrentHand(hand);
        ItemStack itemstack = this.getStackInHand(hand);
        if (itemstack.getItem() == Items.SHIELD) { // See above

            EntityAttributeInstance modifiableattributeinstance = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTemporaryModifier(USE_ITEM_SPEED_PENALTY);
        }
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY.id()))
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
    }

    public void disableShield(boolean increase, Item item) {
        float chance = 0.25F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            getWorld().sendEntityStatus(this, (byte) 30);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(GUARD_VARIANT, 0);
        builder.add(DATA_CHARGING_STATE, false);
        builder.add(KICKING, false);
        builder.add(OWNER_UNIQUE_ID, Optional.empty());
        builder.add(FOLLOWING, false);
        builder.add(GUARD_POS, Optional.empty());
        builder.add(PATROLLING, false);
        builder.add(RUNNING_TO_EAT, false);

        super.initDataTracker(builder);
    }

    public boolean isCharging() {
        return this.dataTracker.get(DATA_CHARGING_STATE);
    }

    public void setChargingCrossbow(boolean charging) {
        this.dataTracker.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.dataTracker.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.dataTracker.set(KICKING, kicking);
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 100.0F);
        this.setEquipmentDropChance(EquipmentSlot.OFFHAND, 100.0F);
        this.spawnWithArmor = true;
    }

    public List<ItemStack> getStacksFromLootTable(EquipmentSlot slot, ServerWorld serverWorld) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            LootTable loot = serverWorld.getServer().getReloadableRegistries().getLootTable(EQUIPMENT_SLOT_ITEMS.get(slot));

            LootWorldContext.Builder worldCtx = new LootWorldContext.Builder((ServerWorld) getWorld())
                    .add(LootContextParameters.THIS_ENTITY, this);

            Vec3d origin = this.getPos();
            DamageSource ds = this.getRecentDamageSource();
            if (ds == null) ds = serverWorld.getDamageSources().generic();

            worldCtx.add(LootContextParameters.ORIGIN, origin);
            worldCtx.add(LootContextParameters.DAMAGE_SOURCE, ds);

            LootWorldContext ctx = worldCtx.build(LootContextTypes.ENTITY);
            return loot.generateLoot(ctx);
        }
        return List.of();
    }


    public int getGuardEntityVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }

    public void setGuardEntityVariant(int typeId) {
        this.dataTracker.set(GUARD_VARIANT, typeId);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new KickGoal(this));
        this.goalSelector.add(0, new GuardEatFoodGoal(this));
        this.goalSelector.add(0, new RaiseShieldGoal(this));
        this.goalSelector.add(1, new GuardRunToEatGoal(this));
        this.goalSelector.add(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.add(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.add(2, new GuardEntityMeleeGoal(this, 0.8D, true));
        this.goalSelector.add(3, new GuardEntity.FollowHeroGoal(this));
        if (GuardVillagersConfig.guardEntitysRunFromPolarBears)
            this.goalSelector.add(3, new FleeEntityGoal<>(this, PolarBearEntity.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.5D, false));
        this.goalSelector.add(3, new IronGolemWanderAroundGoal(this, 0.5D));
        this.goalSelector.add(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.guardEntitysOpenDoors) this.goalSelector.add(3, new GuardInteractDoorGoal(this, true));
        if (GuardVillagersConfig.guardEntityFormation) this.goalSelector.add(5, new FollowShieldGuards(this));
        if (GuardVillagersConfig.clericHealing) this.goalSelector.add(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.armorerRepairGuardEntityArmor)
            this.goalSelector.add(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.add(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, MerchantEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        this.targetSelector.add(5, new GuardEntity.DefendVillageGuardEntityGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RavagerEntity.class, true));
        this.targetSelector.add(2, (new RevengeGoal(this, GuardEntity.class, IronGolemEntity.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitchEntity.class, true));
        this.targetSelector.add(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.add(3, new HeroHurtTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, RaiderEntity.class, true));
        if (GuardVillagersConfig.attackAllMobs)
            this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, true, (mob, owner) -> mob instanceof Monster && !GuardVillagersConfig.mobBlackList.contains(mob.getSavedEntityId())));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, ZombieEntity.class, true));
        this.targetSelector.add(4, new UniversalAngerGoal<>(this, false));
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }


    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandStack().getItem() instanceof CrossbowItem) {
            shootCrossbowLowered(target);
        } else if (this.getMainHandStack().getItem() instanceof BowItem) {
            ItemStack itemStack = this.getProjectileType(this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW)));
            ItemStack bow = this.getMainHandStack();
            ItemEnchantmentsComponent itemEnchantmentsComponent = EnchantmentHelper.getEnchantments(itemStack);
            PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack, pullProgress, bow);
            RegistryWrapper.Impl<Enchantment> impl = this.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

            itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.POWER));
            int powerLevel = itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.POWER));

            if (powerLevel > 0) {
                float damageMod = powerLevel * 0.5F + 0.5F;
                persistentProjectileEntity.applyDamageModifier(damageMod);
            }
            int punchLevel = itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.PUNCH));
            if (punchLevel > 0) {
                //TODO persistentProjectileEntity.getKnockback().setPunch(punchLevel);
            }
            if (itemEnchantmentsComponent.getLevel(impl.getOrThrow(Enchantments.FLAME)) > 0)
                persistentProjectileEntity.setFireTicks(100);
            double d = target.getX() - this.getX();
            double e = target.getBodyY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            persistentProjectileEntity.setVelocity(d, e + g * 0.20000000298023224D, f, 1.6F, (float) (14 - this.getWorld().getDifficulty().getId() * 4));
            this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.getWorld().spawnEntity(persistentProjectileEntity);
            bow.damage(1, this, EquipmentSlot.MAINHAND);
        }
    }

    private void shootCrossbowLowered(LivingEntity target) {
        ItemStack itemstack = this.getMainHandStack();
        if (!(itemstack.getItem() instanceof CrossbowItem cb)) return;

        float speed = 3.15F;
        float divergence = dynamicDivergenceByDistance(this.distanceTo(target));

        // Aim at center mass and scale by distance
        double dist = this.distanceTo(target);
        double yAdj = target.getHeight() * 0.25D + (0.1D * dist); // 1% of distance up

        Vec3d aim = new Vec3d(target.getX(), target.getY() + yAdj, target.getZ());

        float oldYaw = this.getYaw(), oldPitch = this.getPitch(), oldHead = this.headYaw;

        // compute yaw/pitch to "aim" at that point
        Vec3d from = this.getPos().add(0.0, this.getEyeHeight(this.getPose()), 0.0);
        Vec3d v = aim.subtract(from);
        double dx = v.x, dy = v.y, dz = v.z;
        float newYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float newPitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))));

        this.setYaw(newYaw);
        this.headYaw = newYaw;
        this.setPitch(newPitch);

        // pass NULL target so shootAll uses our rotation vector verbatim
        cb.shootAll(this.getWorld(), this, Hand.MAIN_HAND, itemstack, speed, divergence, null);

        // restore rotation
        this.setYaw(oldYaw);
        this.headYaw = oldHead;
        this.setPitch(oldPitch);
    }

    private float dynamicDivergenceByDistance(float horiz) {
        final float MIN = 0.35F;
        final float MAX = 3.0F;
        final float START = 4.0F, END = 28.0F;
        float t = MathHelper.clamp((horiz - START) / (END - START), 0f, 1f);
        t *= t; // ease-in
        return MathHelper.lerp(t, MIN, MAX);
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
        super.equipStack(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getStack(1).isEmpty())
                    this.guardInventory.setStack(1, stack.copy());
                break;
            case FEET:
                if (this.guardInventory.getStack(3).isEmpty())
                    this.guardInventory.setStack(3, stack.copy());
                break;
            case HEAD:
                if (this.guardInventory.getStack(0).isEmpty())
                    this.guardInventory.setStack(0, stack.copy());
                break;
            case LEGS:
                if (this.guardInventory.getStack(2).isEmpty())
                    this.guardInventory.setStack(2, stack.copy());
                break;
            case MAINHAND:
                this.guardInventory.setStack(5, stack.copy());
                break;
            case OFFHAND:
                this.guardInventory.setStack(4, stack.copy());
                break;
        }
    }

    public int getGuardVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }


    @Override
    public ItemStack getProjectileType(ItemStack shootable) {
        if (shootable.getItem() instanceof RangedWeaponItem) {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) shootable.getItem()).getHeldProjectiles();
            ItemStack itemstack = RangedWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.dataTracker.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.dataTracker.set(FOLLOWING, following);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !GuardVillagersConfig.mobBlackList.contains(target.getSavedEntityId()) && !target.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof VillagerEntity) && !(target instanceof IronGolemEntity) && !(target instanceof GuardEntity) && super.canTarget(target);
    }

    @Override
    public void tickRiding() {
        super.tickRiding();
        if (this.getVehicle() instanceof PathAwareEntity creatureentity) {
            this.bodyYaw = creatureentity.bodyYaw;
        }
    }

    @Override
    public void postShoot() {
        this.despawnCounter = 0;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof VillagerEntity || entity instanceof IronGolemEntity)
            return;
        super.setTarget(entity);
    }


    public void gossip(VillagerEntity villager, long gameTime) {
        if ((gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) && (gameTime < villager.gossipStartTime || gameTime >= villager.gossipStartTime + 1200L)) {
            this.gossips.shareGossipFrom(villager.getGossip(), this.random, 10);
            this.lastGossipTime = gameTime;
            villager.gossipStartTime = gameTime;
        }
    }

    @Override
    public void setCharging(boolean charging) {

    }

    @Override
    public void knockback(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.knockback(this);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        boolean configValues = player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv && GuardVillagersConfig.setGuardPatrolHotv || this.getPlayerEntityReputation(player) >= GuardVillagersConfig.reputationRequirement || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !GuardVillagersConfig.giveGuardStuffHotv && !GuardVillagersConfig.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUuid());
        boolean inventoryRequirements = !player.shouldCancelInteraction();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.canActVoluntarily() && configValues) {
                if (player instanceof ServerPlayerEntity) {
                    this.openGui((ServerPlayerEntity) player);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.CONSUME;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void onInteractionWith(EntityInteraction interaction, Entity entity) {

    }

    @Override
    public void onInventoryChanged(Inventory sender) {

    }


    @Override
    public void damageArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.size(); ++i) {
                ItemStack itemstack = this.guardInventory.getStack(i);

                if (ItemUtils.isArmorItem(itemstack)) {
                    DamageResistantComponent resist = itemstack.get(DataComponentTypes.DAMAGE_RESISTANT);
                    if (resist != null && resist.resists(damageSource)) {
                        return;
                    }

                    int j = i;
                    var list = Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmorSlot).toList();

                    itemstack.damage((int) damage, this, list.get(j));

                }

            }
        }
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            WitchEntity witchentity = EntityType.WITCH.create(world, SpawnReason.MOB_SUMMONED);
            if (witchentity == null) return;
            witchentity.copyPositionAndRotation(this);
            witchentity.initialize(world, world.getLocalDifficulty(witchentity.getBlockPos()), SpawnReason.CONVERSION, null);
            witchentity.setAiDisabled(this.isAiDisabled());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistent();
            world.spawnNewEntityAndPassengers(witchentity);
            this.discard();
        } else {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Override
    public UUID getAngryAt() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setAngryAt(UUID arg0) {
        this.persistentAngerTarget = arg0;
    }

    @Override
    public int getAngerTime() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTime(int arg0) {
        this.remainingPersistentAngerTime = arg0;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(angerTime.get(random));
    }

    public void openGui(ServerPlayerEntity player) {
        this.setOwnerId(player.getUuid());
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
        this.interacting = true;
        if (!this.getWorld().isClient()) {
            player.openHandledScreen(new GuardScreenHandlerFactory());
        }
    }

    public void setGuardVariant(int i) {
        this.dataTracker.set(GUARD_VARIANT, i);
    }

    private class GuardScreenHandlerFactory implements ExtendedScreenHandlerFactory {
        private GuardEntity guard() {
            return GuardEntity.this;
        }

        @Override
        public Text getDisplayName() {
            return this.guard().getDisplayName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
            var guardInv = this.guard().guardInventory;
            return new GuardVillagerScreenHandler(syncId, inv, guardInv, this.guard());
        }

        @Override
        public Object getScreenOpeningData(ServerPlayerEntity player) {
            return new GuardData(guard().getId());
        }
    }

    public boolean isEating() {
        return GuardEatFoodGoal.isConsumable(this.getActiveItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.dataTracker.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.dataTracker.set(PATROLLING, patrolling);
    }

    @Override
    public boolean canUseRangedWeapon(RangedWeaponItem item) {
        return item instanceof BowItem || item instanceof CrossbowItem || super.canUseRangedWeapon(item);
    }

    public static class GuardEntityData implements EntityData {
        public final int variantData;

        public GuardEntityData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardEntityGoal extends TrackTargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardEntityGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setControls(EnumSet.of(Goal.Control.TARGET, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            Box box = this.guard.getBoundingBox().expand(10.0D, 8.0D, 10.0D);
            List<VillagerEntity> list = guard.getWorld().getNonSpectatingEntities(VillagerEntity.class, box);
            List<PlayerEntity> list1 = guard.getWorld().getNonSpectatingEntities(PlayerEntity.class, box);
            for (VillagerEntity villager : list) {
                for (PlayerEntity player : list1) {
                    int i = villager.getReputation(player);
                    if (i <= GuardVillagersConfig.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((PlayerEntity) this.villageAggressorTarget).isCreative();
        }

        @Override
        public void start() {
            this.guard.setTarget(this.villageAggressorTarget);
            super.start();
        }
    }

    public static class FollowHeroGoal extends Goal {
        public final GuardEntity guard;

        public FollowHeroGoal(GuardEntity mob) {
            this.guard = mob;
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null && guard.getOwner().distanceTo(guard) > 3.0D) {
                guard.getNavigation().startMovingTo(guard.getOwner(), 0.7D);
                guard.getLookControl().lookAt(guard.getOwner());
            } else {
                guard.getNavigation().stop();
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public boolean canStart() {
            return guard.isFollowing() && guard.getOwner() != null;
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
        }
    }

    public static class GuardEntityMeleeGoal extends MeleeAttackGoal {
        public final GuardEntity guard;

        public GuardEntityMeleeGoal(GuardEntity guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canStart() {
            return !(this.guard.getMainHandStack().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    float[] strafeForwardOptions = {-1.0F, -0.5F};
                    float[] strafeSidewaysOptions = {-1.0F, -0.5F, 0.5F, 1.0F};
                    float forward = strafeForwardOptions[guard.getRandom().nextInt(strafeForwardOptions.length)];
                    float sideways = strafeSidewaysOptions[guard.getRandom().nextInt(strafeSidewaysOptions.length)];
                    guard.getMoveControl().strafeTo(forward, sideways);
                    guard.lookAtEntity(target, 30.0F, 30.0F);
                }
                if (this.path != null && target.distanceTo(guard) <= 2.0D) guard.getNavigation().stop();
                super.tick();
            }
        }

        @Override
        protected void attack(LivingEntity target) {
            if (guard.isInAttackRange(target) && this.getCooldown() <= 0) {
                if (!(this.guard.getWorld() instanceof ServerWorld serverWorld)) {
                    return;
                }

                this.resetCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swingHand(Hand.MAIN_HAND);
                this.guard.tryAttack(serverWorld, target);
            }
        }
    }
}
