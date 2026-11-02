package thaumcraft.port.golem.seal;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** BETA26 filter rules adapted to components and sided NeoForge item handlers. */
public final class SealInventory {
    public record Filter(boolean ignoreDamage, boolean ignoreComponents, boolean useTags, boolean useMod) { }
    public record Match(ItemStack stack, int limit) { }
    public static final Filter STRICT = new Filter(false, false, false, false);
    public static Filter filters(ISealConfigToggles.SealToggle[] toggles) {
        return new Filter(!toggles[0].value, !toggles[1].value, toggles[2].value, toggles[3].value);
    }
    public static IItemHandler handler(Level level, BlockPos pos, Direction face) {
        return level.hasChunkAt(pos) ? level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face) : null;
    }
    public static boolean matches(ItemStack template, ItemStack actual, Filter filter) {
        if (template.isEmpty() || actual.isEmpty()) return template.isEmpty() && actual.isEmpty();
        if (filter.useMod) return BuiltInRegistries.ITEM.getKey(template.getItem()).getNamespace()
                .equals(BuiltInRegistries.ITEM.getKey(actual.getItem()).getNamespace());
        if (filter.useTags && template.getTags().anyMatch(tag -> {
            String namespace = tag.location().getNamespace();
            boolean legacyVanillaMaterial = namespace.equals("minecraft") && switch (tag.location().getPath()) {
                case "planks", "logs", "logs_that_burn", "leaves", "saplings", "wool", "sand", "wooden_slabs" -> true;
                default -> false;
            };
            return (namespace.equals("c") || namespace.equals("forge") || legacyVanillaMaterial) && actual.is(tag);
        })) return true;
        if (!template.is(actual.getItem())) return false;
        if (!filter.ignoreDamage && template.getDamageValue() != actual.getDamageValue()) return false;
        if (filter.ignoreComponents) return true;
        if (!filter.ignoreDamage) return ItemStack.isSameItemSameComponents(template, actual);
        var first = template.copy(); var second = actual.copy();
        first.remove(DataComponents.DAMAGE); second.remove(DataComponents.DAMAGE);
        return ItemStack.isSameItemSameComponents(first, second);
    }
    public static boolean matchesFilters(List<ItemStack> filters, boolean blacklist, ItemStack stack, Filter filter) {
        if (stack.isEmpty()) return false;
        boolean matched = filters.stream().anyMatch(s -> !s.isEmpty() && matches(s, stack, filter));
        return blacklist != matched;
    }
    public static Match firstMatch(List<ItemStack> filters, List<Integer> sizes, boolean blacklist,
                                   Iterable<ItemStack> stacks, Filter filter) {
        for (var stack : stacks) {
            if (stack.isEmpty()) continue;
            if (blacklist) {
                if (matchesFilters(filters, true, stack, filter)) return new Match(stack, 0);
            } else for (int i = 0; i < filters.size(); i++)
                if (!filters.get(i).isEmpty() && matches(filters.get(i), stack, filter))
                    return new Match(stack, i < sizes.size() ? sizes.get(i) : 0);
        }
        return new Match(ItemStack.EMPTY, 0);
    }
    public static ItemStack firstExtractable(List<ItemStack> filters, boolean blacklist,
                                             IItemHandler inventory, Filter filter, boolean leaveOne) {
        if (inventory == null) return ItemStack.EMPTY;
        for (int i = 0; i < inventory.getSlots(); i++) {
            var stack = inventory.getStackInSlot(i);
            if (stack.isEmpty() || !matchesFilters(filters, blacklist, stack, filter)
                    || leaveOne && count(inventory, stack, filter) < 2) continue;
            var extractable = inventory.extractItem(i, stack.getCount(), true);
            if (!extractable.isEmpty()) return extractable.copy();
        }
        return ItemStack.EMPTY;
    }
    public static int count(IItemHandler inventory, ItemStack stack, Filter filter) {
        if (inventory == null || stack.isEmpty()) return 0;
        long count = 0;
        for (int i = 0; i < inventory.getSlots(); i++)
            if (matches(stack, inventory.getStackInSlot(i), filter)) count += inventory.getStackInSlot(i).getCount();
        return (int)Math.min(Integer.MAX_VALUE, count);
    }
    public static int countInWorld(Level level, BlockPos pos, ItemStack stack, double range, Filter filter) {
        long count = 0;
        for (var item : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(range)))
            if (item.isAlive() && matches(stack, item.getItem(), filter)) count += item.getItem().getCount();
        return (int)Math.min(Integer.MAX_VALUE, count);
    }
    public static int availableRoom(IItemHandler inventory, ItemStack stack) {
        if (inventory == null || stack.isEmpty()) return 0;
        return stack.getCount() - insert(inventory, stack.copy(), true).getCount();
    }
    public static ItemStack insert(IItemHandler inventory, ItemStack stack, boolean simulate) {
        return inventory == null || stack.isEmpty() ? stack : ItemHandlerHelper.insertItemStacked(inventory, stack, simulate);
    }
    /** Return the extracted item itself, never a copy of a permissive filter template. */
    public static ItemStack extract(IItemHandler inventory, ItemStack requested, Filter filter, boolean simulate) {
        if (inventory == null || requested.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = ItemStack.EMPTY;
        for (int i = 0; i < inventory.getSlots() && result.getCount() < requested.getCount(); i++) {
            var stored = inventory.getStackInSlot(i);
            if (!matches(requested, stored, filter) || !result.isEmpty() && !ItemStack.isSameItemSameComponents(result, stored)) continue;
            var taken = inventory.extractItem(i, requested.getCount() - result.getCount(), simulate);
            if (taken.isEmpty()) continue;
            if (result.isEmpty()) result = taken.copy();
            else result.grow(taken.getCount());
        }
        return result;
    }
    public static ItemStack copyLimited(ItemStack stack, int amount) {
        return stack.isEmpty() || amount <= 0 ? ItemStack.EMPTY : stack.copyWithCount(Math.min(stack.getCount(), amount));
    }
    /** A failed/cancelled entity spawn does not transfer ownership of the input stack. */
    public static boolean eject(Level level, Vec3 pos, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (level.isClientSide || !level.hasChunkAt(BlockPos.containing(pos))) return false;
        var entity = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(.2, .5, .2));
        return level.addFreshEntity(entity);
    }
    public static Vec3 output(SealPos pos) { return Vec3.atCenterOf(pos.pos).add(Vec3.atLowerCornerOf(pos.face.getNormal())); }
    private SealInventory() { }
}
