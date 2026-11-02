package thaumcraft.port.golem;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.artifice.construct.OwnedConstruct;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.golem.ai.*;
import thaumcraft.port.golem.item.GolemBellItem;
import thaumcraft.port.golem.item.GolemPlacerItem;
import thaumcraft.port.golem.projectile.GolemDartEntity;
import thaumcraft.port.golem.task.Task;
import thaumcraft.port.research.ResearchStorage;

/** BETA26 owned modular worker. Equipment packets persist and synchronize the carried stacks. */
public class GolemEntity extends OwnedConstruct implements IGolemAPI, RangedAttackMob {
    private static final EntityDataAccessor<Long> PROPERTIES = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos home = BlockPos.ZERO;
    private boolean hasHome;
    private boolean firstRun = true;
    private int rankXp;
    private Task task;
    private long appliedProperties = Long.MIN_VALUE;
    private boolean appliedFollowing;
    public boolean redrawParts = true;
    public float wheelRotation, previousWheelRotation;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level) {
        super(type, level); xpReward = 5;
        setDropChance(EquipmentSlot.MAINHAND, 0); setDropChance(EquipmentSlot.OFFHAND, 0);
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(.6);
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10).add(Attributes.MOVEMENT_SPEED, .3)
                .add(Attributes.ATTACK_DAMAGE, 0).add(Attributes.FOLLOW_RANGE, 40).add(Attributes.STEP_HEIGHT, .6);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(PROPERTIES, 0L); builder.define(FLAGS, (byte)0);
        builder.define(COLOR, (byte)0); builder.define(CLIMBING, false);
    }
    @Override protected void registerGoals() { }
    @Override public GolemProperties getProperties() { return GolemProperties.fromLong(entityData.get(PROPERTIES)); }
    @Override public void setProperties(IGolemProperties properties) {
        entityData.set(PROPERTIES, properties.toLong()); redrawParts = true;
        if (!level().isClientSide) updateProperties();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor); if (accessor.equals(PROPERTIES)) redrawParts = true;
    }
    public boolean hasTrait(EnumGolemTrait trait) { return getProperties().hasTrait(trait); }
    @Override public byte getGolemColor() { return entityData.get(COLOR); }
    public void setGolemColor(byte color) { entityData.set(COLOR, (byte)Mth.clamp(color, 0, 16)); }
    public byte getFlags() { return entityData.get(FLAGS); }
    public void setFlags(byte flags) { entityData.set(FLAGS, flags); }
    public boolean isFollowingOwner() { return (getFlags() & 2) != 0; }
    public void setFollowingOwner(boolean following) {
        releaseTask(); setFlags((byte)(following ? getFlags() | 2 : getFlags() & ~2));
        if (following) { hasHome = false; clearRestriction(); } else setHomePosition(blockPosition());
        if (!level().isClientSide) updateProperties();
    }
    @Override public boolean isInCombat() { return (getFlags() & 8) != 0; }
    public void setInCombat(boolean combat) { setFlags((byte)(combat ? getFlags() | 8 : getFlags() & ~8)); }
    @Override public void setTarget(LivingEntity target) { super.setTarget(target); setInCombat(target != null); }
    public Task getTask() { return task; }
    public void setTask(Task next) { task = next; }
    public void releaseTask() { if (task != null) task.setReserved(false); task = null; getNavigation().stop(); }
    public BlockPos getHomePosition() { return home; }
    public boolean hasHome() { return hasHome; }
    public void setHomePosition(BlockPos pos) { home = pos.immutable(); hasHome = true; restrictTo(home, hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32); }
    public boolean isWithinHome(BlockPos pos) { return !hasHome || home.distSqr(pos) < Math.pow(hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32, 2); }
    public int getRankXp() { return rankXp; }
    public void setRankXp(int xp) { rankXp = Math.max(0, xp); }
    public int nextRankXp() { int rank = getProperties().getRank(); return (rank + 1) * (rank + 1) * 1000; }
    public void initializePlaced() { setHomePosition(blockPosition()); updateProperties(); setHealth(getMaxHealth()); }
    public float getGolemMoveSpeed() {
        return 1 + getProperties().getRank() * .025f + (hasTrait(EnumGolemTrait.LIGHT) ? .2f : 0)
                + (hasTrait(EnumGolemTrait.HEAVY) ? -.175f : 0) + (hasTrait(EnumGolemTrait.FLYER) ? -.33f : 0)
                + (hasTrait(EnumGolemTrait.WHEELED) ? .25f : 0);
    }
    private void updateProperties() {
        var properties = getProperties(); var material = properties.getMaterial();
        int health = 10 + material.healthMod;
        if (hasTrait(EnumGolemTrait.FRAGILE)) health = (int)(health * .75);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(health + properties.getRank());
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(hasTrait(EnumGolemTrait.WHEELED) ? .5 : .6);
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(hasTrait(EnumGolemTrait.SCOUT) ? 56 : 40);
        int armor = material.armor;
        if (hasTrait(EnumGolemTrait.ARMORED)) armor = (int)Math.max(armor * 1.5, armor + 1);
        if (hasTrait(EnumGolemTrait.FRAGILE)) armor = (int)(armor * .75);
        getAttribute(Attributes.ARMOR).setBaseValue(armor);
        double damage = material.damage;
        if (hasTrait(EnumGolemTrait.BRUTAL)) damage = Math.max(damage * 1.5, damage + 1);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(hasTrait(EnumGolemTrait.FIGHTER) ? damage + properties.getRank() * .25 : 0);
        if (hasHome) restrictTo(home, hasTrait(EnumGolemTrait.SCOUT) ? 48 : 32);
        // Ranking happens inside a running task's completion callback. Rebuilding the selectors
        // there would invalidate their iterator and abandon the current reservation.
        long partsMask = (1L << 40) - 1;
        if (appliedProperties == Long.MIN_VALUE || (appliedProperties & partsMask) != (properties.toLong() & partsMask) || appliedFollowing != isFollowingOwner()) {
            navigation = hasTrait(EnumGolemTrait.FLYER) ? new GolemFlyingNavigation(this, level())
                    : hasTrait(EnumGolemTrait.CLIMBER) ? new WallClimberNavigation(this, level()) : new GolemGroundNavigation(this, level());
            moveControl = hasTrait(EnumGolemTrait.FLYER) ? new GolemFlyingMoveControl(this) : new MoveControl(this);
            setNoGravity(hasTrait(EnumGolemTrait.FLYER)); createGoals();
        }
        appliedProperties = properties.toLong(); appliedFollowing = isFollowingOwner();
    }
    private void createGoals() {
        goalSelector.removeAllGoals(g -> true); targetSelector.removeAllGoals(g -> true);
        if (isFollowingOwner()) goalSelector.addGoal(4, new GolemFollowOwnerGoal(this));
        else {
            goalSelector.addGoal(3, new GolemTaskGoal(this, (byte)1));
            goalSelector.addGoal(4, new GolemTaskGoal(this, (byte)0));
            goalSelector.addGoal(5, new GolemHomeGoal(this));
        }
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8)); goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        if (hasTrait(EnumGolemTrait.FIGHTER)) {
            if (navigation instanceof GroundPathNavigation) goalSelector.addGoal(0, new FloatGoal(this));
            if (hasTrait(EnumGolemTrait.RANGED)) goalSelector.addGoal(1, new GolemArrowAttackGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
            if (isFollowingOwner()) {
                targetSelector.addGoal(1, new GolemOwnerTargetGoal(this, true));
                targetSelector.addGoal(2, new GolemOwnerTargetGoal(this, false));
            }
            targetSelector.addGoal(3, new HurtByTargetGoal(this));
        }
    }
    @Override public void aiStep() { updateSwingTime(); super.aiStep(); }
    @Override public void tick() {
        if (!level().isClientSide && (appliedProperties != entityData.get(PROPERTIES) || appliedFollowing != isFollowingOwner())) updateProperties();
        super.tick();
        if (!level().isClientSide) {
            if (firstRun) { firstRun = false; if (hasHome && !blockPosition().equals(home) && !isFollowingOwner()) goHome(); }
            if (task != null && task.isSuspended()) task = null;
            var target = getTarget();
            if (target != null && (!target.isAlive() || hasTrait(EnumGolemTrait.RANGED) && distanceToSqr(target) > 1024
                    || target instanceof Player && level().getServer() != null && !level().getServer().isPvpAllowed())) setTarget(null);
            if (tickCount % 20 == 0) thaumcraft.port.golem.seal.GolemInteractionHelper.flushReturns(this);
            if (tickCount % (hasTrait(EnumGolemTrait.REPAIR) ? 40 : 100) == 0) heal(1);
            entityData.set(CLIMBING, hasTrait(EnumGolemTrait.CLIMBER) && horizontalCollision);
        } else {
            GolemVisuals.tick.accept(this);
            if (tickCount < 20 || tickCount % 20 == 0) redrawParts = true;
            previousWheelRotation = wheelRotation;
            if (hasTrait(EnumGolemTrait.WHEELED)) {
                double dx = getX() - xo, dy = getY() - yo, dz = getZ() - zo;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                float travelDirection = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90;
                wheelRotation += (float)(distance / 1.571 * (360 - (getYRot() - travelDirection)));
                if (wheelRotation > 360) wheelRotation -= 360;
            }
        }
    }
    @Override public void handleEntityEvent(byte event) {
        if (event >= 5 && event <= 9 && level().isClientSide) GolemVisuals.event.accept(this, event); else super.handleEntityEvent(event);
    }
    @Override public boolean onClimbable() { return entityData.get(CLIMBING); }
    public boolean isBesideClimbableBlock() { return onClimbable(); }
    public void setBesideClimbableBlock(boolean climbing) { entityData.set(CLIMBING, climbing); }
    @Override public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !hasTrait(EnumGolemTrait.FLYER) && !hasTrait(EnumGolemTrait.CLIMBER) && super.causeFallDamage(distance, multiplier, source);
    }
    @Override protected MovementEmission getMovementEmission() { return hasTrait(EnumGolemTrait.HEAVY) && !hasTrait(EnumGolemTrait.FLYER) ? MovementEmission.ALL : MovementEmission.NONE; }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE) && hasTrait(EnumGolemTrait.FIREPROOF) || source.is(DamageTypes.CACTUS)) return false;
        if (source.is(DamageTypeTags.IS_EXPLOSION) && hasTrait(EnumGolemTrait.BLASTPROOF)) amount = Math.min(getMaxHealth() / 2, amount * .3f);
        if (hasHome && (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD))) goHome();
        return super.hurt(source, amount);
    }
    public boolean goHome() {
        if (!hasHome || !level().hasChunkAt(home)) return false;
        Vec3 previous = position(); BlockPos pos = home;
        while (pos.getY() < level().getMaxBuildHeight() && !level().getBlockState(pos).getCollisionShape(level(), pos).isEmpty()) pos = pos.above();
        setPos(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
        if (!level().noCollision(this)) { setPos(previous); return false; }
        getNavigation().stop(); return true;
    }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || player.getItemInHand(hand).is(Items.NAME_TAG)) return InteractionResult.PASS;
        if (!isOwner(player)) return super.mobInteract(player, hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            releaseTask(); dropCarried();
            if (getCarrying().stream().anyMatch(s -> !s.isEmpty()) || thaumcraft.port.golem.seal.GolemInteractionHelper.hasPendingReturns(this)) return InteractionResult.FAIL;
            var placer = item("golem");
            GolemPlacerItem.setProperties(placer, getProperties(), rankXp);
            if (spawnAtLocation(placer, .5f) == null) return InteractionResult.FAIL;
            playSound(PortSounds.get("zap"), 1, 1); discard(); player.swing(hand, true);
        } else if (player.getItemInHand(hand).getItem() instanceof GolemBellItem
                && player instanceof ServerPlayer server && ResearchStorage.get(server.getServer()).player(player.getUUID()).isResearchKnown("GOLEMDIRECT")) {
            setFollowingOwner(!isFollowingOwner()); playSound(PortSounds.get("scan"), 1, 1);
            player.displayClientMessage(Component.translatable(isFollowingOwner() ? "golem.follow" : "golem.stay", ""), true);
            level().broadcastEntityEvent(this, (byte)(isFollowingOwner() ? 5 : 8)); player.swing(hand, true);
        } else if (player.getItemInHand(hand).getItem() instanceof DyeItem dye) {
            // Legacy dye metadata was inverse to the modern DyeColor id.
            setGolemColor((byte)(dye.getDyeColor().getId() + 1)); player.getItemInHand(hand).shrink(1);
            playSound(PortSounds.get("zap"), 1, 1); player.swing(hand, true);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            if (hasTrait(EnumGolemTrait.DEFT)) living.lastHurtByPlayerTime = 100;
            if (target instanceof Mob && !living.isAlive()) addRankXp(8);
        }
        return hit;
    }
    @Override public void performRangedAttack(LivingEntity target, float distanceFactor) {
        var dart = new GolemDartEntity(GolemContent.DART.get(), level()); dart.setOwner(this);
        dart.setPos(getX(), getEyeY() - .1, getZ());
        dart.setBaseDamage(getAttributeValue(Attributes.ATTACK_DAMAGE) / 3 + distanceFactor + random.nextGaussian() * .25);
        dart.shoot(target.getX() - getX(), target.getEyeY() + distanceFactor * distanceFactor - dart.getY(), target.getZ() - getZ(), 1.6f, 3);
        level().addFreshEntity(dart); playSound(SoundEvents.ARROW_SHOOT, 1, 1 / (random.nextFloat() * .4f + .8f));
    }
    @Override public void addRankXp(int amount) {
        var properties = getProperties(); int rank = properties.getRank();
        if (!hasTrait(EnumGolemTrait.SMART) || level().isClientSide || rank >= 10) return;
        rankXp += amount; int required = (rank + 1) * (rank + 1) * 1000;
        if (rankXp >= required) {
            rankXp -= required; properties.setRank(rank + 1); setProperties(properties);
            level().broadcastEntityEvent(this, (byte)9); playSound(SoundEvents.PLAYER_LEVELUP, .25f, 1);
        }
    }
    private int carryingSlots() { return hasTrait(EnumGolemTrait.HAULER) ? 2 : 1; }
    private ItemStack carried(int slot) { return getItemInHand(slot == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND); }
    private void carried(int slot, ItemStack stack) { setItemInHand(slot == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, stack); }
    @Override public ItemStack holdItem(ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) return incoming == null ? ItemStack.EMPTY : incoming;
        var remainder = incoming.copy();
        for (int slot = 0; slot < carryingSlots(); slot++) {
            var held = carried(slot);
            if (held.isEmpty()) { int count = Math.min(remainder.getCount(), remainder.getMaxStackSize()); carried(slot, remainder.split(count)); }
            else if (ItemStack.isSameItemSameComponents(held, remainder)) {
                int count = Math.min(remainder.getCount(), held.getMaxStackSize() - held.getCount());
                if (count > 0) { held.grow(count); remainder.shrink(count); }
            }
            if (remainder.isEmpty()) return ItemStack.EMPTY;
        }
        return remainder;
    }
    @Override public ItemStack dropItem(ItemStack requested) {
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < carryingSlots(); slot++) {
            var held = carried(slot); if (held.isEmpty()) continue;
            if (requested == null || requested.isEmpty()) { result = held.copy(); carried(slot, ItemStack.EMPTY); }
            else if (ItemStack.isSameItemSameComponents(held, requested)) { result = held.split(requested.getCount()); if (held.isEmpty()) carried(slot, ItemStack.EMPTY); }
            if (!result.isEmpty()) break;
        }
        if (carryingSlots() > 1 && carried(0).isEmpty() && !carried(1).isEmpty()) { carried(0, carried(1)); carried(1, ItemStack.EMPTY); }
        return result;
    }
    @Override public int canCarryAmount(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        int space = 0;
        for (int slot = 0; slot < carryingSlots(); slot++) { var held = carried(slot); if (held.isEmpty()) space += stack.getMaxStackSize(); else if (ItemStack.isSameItemSameComponents(held, stack)) space += Math.max(0, held.getMaxStackSize() - held.getCount()); }
        return space;
    }
    @Override public boolean canCarry(ItemStack stack, boolean partial) { int space = canCarryAmount(stack); return space > 0 && (partial || space >= stack.getCount()); }
    @Override public boolean isCarrying(ItemStack stack) { return stack != null && !stack.isEmpty() && getCarrying().stream().anyMatch(s -> !s.isEmpty() && ItemStack.isSameItemSameComponents(s, stack)); }
    @Override public NonNullList<ItemStack> getCarrying() { var result = NonNullList.withSize(carryingSlots(), ItemStack.EMPTY); for (int i = 0; i < result.size(); i++) result.set(i, carried(i)); return result; }
    public void dropCarried() {
        thaumcraft.port.golem.seal.GolemInteractionHelper.flushReturns(this);
        for (int slot = 0; slot < 2; slot++) if (!carried(slot).isEmpty() && spawnAtLocation(carried(slot).copy(), .25f) != null) carried(slot, ItemStack.EMPTY);
        thaumcraft.port.golem.seal.GolemInteractionHelper.flushReturns(this);
    }
    @Override protected void dropInventory() { dropCarried(); }
    @Override public void remove(RemovalReason reason) { if (!level().isClientSide) releaseTask(); super.remove(reason); }
    @Override public void die(DamageSource source) { releaseTask(); super.die(source); }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        float bonus = looting(source) * .15f;
        for (var component : getProperties().generateComponents()) if (random.nextFloat() < .3f + bonus) {
            var drop = component.copy(); if (drop.getCount() > 0) drop.shrink(random.nextInt(drop.getCount())); spawnAtLocation(drop, .25f);
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putLong("props", getProperties().toLong()); tag.putLong("homepos", home.asLong());
        tag.putBoolean("hasHome", hasHome); tag.putByte("gflags", getFlags()); tag.putInt("rankXP", rankXp); tag.putByte("color", getGolemColor());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try { entityData.set(PROPERTIES, GolemProperties.fromLong(tag.getLong("props")).toLong()); } catch (IllegalArgumentException invalid) { entityData.set(PROPERTIES, 0L); }
        home = BlockPos.of(tag.getLong("homepos")); hasHome = !tag.contains("hasHome") || tag.getBoolean("hasHome");
        setFlags(tag.getByte("gflags")); rankXp = Math.max(0, tag.getInt("rankXP")); setGolemColor(tag.getByte("color")); updateProperties();
    }
    @Override public LivingEntity getGolemEntity() { return this; }
    @Override public Level getGolemWorld() { return level(); }
    @Override public void swingArm() { swing(InteractionHand.MAIN_HAND, true); }
    @Override public boolean accepts(ItemStack stack) { return true; }
    @Override public String placerId() { return "golem"; }
}
