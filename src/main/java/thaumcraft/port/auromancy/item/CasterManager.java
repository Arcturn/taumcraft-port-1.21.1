package thaumcraft.port.auromancy.item;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.content.CasterItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.content.RevealingEquipment;
import top.theillusivec4.curios.api.CuriosApi;

/** Inventory transactions and the original per-caster (not per-hand) cooldown and architect controls. */
public final class CasterManager {
    // Entity equality uses its numeric id. Client and integrated-server copies
    // have the same id, so sharing a map lets prediction block the server cast.
    // As in TC6, keep logical sides separate. Weak keys avoid retaining departed casters.
    private static final Map<LivingEntity, Long> CLIENT_COOLDOWNS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<LivingEntity, Long> SERVER_COOLDOWNS = Collections.synchronizedMap(new WeakHashMap<>());
    private static Map<LivingEntity, Long> cooldowns(LivingEntity caster) {
        return caster.level().isClientSide ? CLIENT_COOLDOWNS : SERVER_COOLDOWNS;
    }
    private record FocusSlot(Supplier<ItemStack> read, Consumer<ItemStack> write) { }
    public static boolean isOnCooldown(LivingEntity caster) { return getCooldown(caster) > 0; }
    public static float getCooldown(LivingEntity caster) { return Math.max(0, (cooldowns(caster).getOrDefault(caster, 0L) - System.nanoTime()) / 1_000_000_000f); }
    public static void setCooldown(LivingEntity caster, int ticks) {
        var cooldowns = cooldowns(caster);
        if (ticks <= 0) cooldowns.remove(caster); else cooldowns.put(caster, System.nanoTime() + ticks * 50_000_000L);
    }
    public static float getTotalVisDiscount(Player player) { return RevealingEquipment.visDiscount(player); }
    public static boolean consumeVisFromInventory(Player player, float amount, boolean crafting, boolean simulate) {
        for (int i = player.getInventory().items.size() - 1; i >= 0; i--) {
            var stack = player.getInventory().items.get(i);
            if (stack.getItem() instanceof CasterItem caster && caster.consumeVis(stack, player, amount, crafting, simulate)) return true;
        }
        return false;
    }
    public static InteractionHand heldCasterHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof CasterItem) return InteractionHand.MAIN_HAND;
        return player.getOffhandItem().getItem() instanceof CasterItem ? InteractionHand.OFF_HAND : null;
    }
    private record Slots(List<FocusSlot> selection, List<FocusSlot> returning) { }
    private static Slots slots(Player player) {
        var worn = new ArrayList<FocusSlot>();
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
            for (var handler : inventory.getCurios().values()) for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                var pouch = handler.getStacks().getStackInSlot(i);
                if (inventory.isSlotActive(handler.getIdentifier(), i) && pouch.getItem() instanceof FocusPouchItem) addPouch(worn, pouch);
            }
        });
        var selection = new ArrayList<>(worn); var returning = new ArrayList<>(worn); var inventory = inventorySlots(player);
        // Source selection order: worn pouches, then each inventory slot (including that slot's pouch).
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.get(i).read.get();
            if (stack.getItem() instanceof FocusPouchItem) {
                var pouch = new ArrayList<FocusSlot>(); addPouch(pouch, stack); selection.addAll(pouch); returning.addAll(pouch);
            } else selection.add(inventory.get(i));
        }
        // Modern off-hand pouches participate as well, without confusing off hand with anatomical left.
        if (player.getOffhandItem().getItem() instanceof FocusPouchItem) {
            var pouch = new ArrayList<FocusSlot>(); addPouch(pouch, player.getOffhandItem()); selection.addAll(pouch); returning.addAll(pouch);
        }
        returning.addAll(inventory); return new Slots(selection, returning);
    }
    private static void addPouch(List<FocusSlot> slots, ItemStack pouch) {
        for (int index = 0; index < FocusPouchItem.SIZE; index++) {
            final int slot = index;
            slots.add(new FocusSlot(() -> FocusPouchItem.getInventory(pouch).get(slot), value -> {
                var contents = FocusPouchItem.getInventory(pouch); contents.set(slot, value); FocusPouchItem.setInventory(pouch, contents);
            }));
        }
    }
    private static List<FocusSlot> inventorySlots(Player player) {
        var slots = new ArrayList<FocusSlot>();
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            final int slot = index;
            slots.add(new FocusSlot(() -> player.getInventory().getItem(slot), value -> player.getInventory().setItem(slot, value)));
        }
        return slots;
    }
    private static String key(ItemStack stack) { return stack.getItem() instanceof FocusItem focus ? focus.getSortingHelper(stack) : null; }
    public static SortedMap<String, ItemStack> getAvailableFoci(Player player) {
        var result = new TreeMap<String, ItemStack>();
        for (var slot : slots(player).selection) { var stack = slot.read.get(); var key = key(stack); if (key != null) result.put(key, stack.copy()); }
        return result;
    }
    /** Resolve all slots and capacity before changing anything: a full inventory cannot eat either focus. */
    public static boolean changeFocus(Player player, InteractionHand hand, String selectedKey) {
        var caster = player.getItemInHand(hand); if (!(caster.getItem() instanceof CasterItem)) return false;
        var slots = slots(player);
        var available = new TreeMap<String, FocusSlot>();
        for (var slot : slots.selection) { var value = key(slot.read.get()); if (value != null) available.put(value, slot); }
        FocusSlot selected = null;
        if (!"REMOVE".equals(selectedKey)) {
            if (available.isEmpty()) return false;
            if (!available.containsKey(selectedKey)) { var higher = available.higherKey(selectedKey); selectedKey = higher == null ? available.firstKey() : higher; }
            selected = available.get(selectedKey);
        }
        var old = CasterItem.getFocusStack(caster); var replacement = selected == null ? ItemStack.EMPTY : selected.read.get().copyWithCount(1);
        FocusSlot returning = null;
        if (!old.isEmpty()) {
            for (var slot : slots.returning) if (slot == selected || slot.read.get().isEmpty()) { returning = slot; break; }
            if (returning == null) return false;
        }
        if (selected != null) selected.write.accept(ItemStack.EMPTY);
        if (returning != null) returning.write.accept(old.copy());
        CasterItem.setFocus(caster, replacement); player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), PortSounds.get("ticks"), net.minecraft.sounds.SoundSource.PLAYERS, .3f, selected == null ? .9f : 1f);
        return true;
    }
    public static boolean contains(FocusPackage pack, String key) {
        if (pack == null) return false;
        for (var node : pack.nodes) {
            if (node.getKey().equals(key)) return true;
            if (node instanceof FocusPackage child && contains(child, key)) return true;
            if (node instanceof FocusModSplit split) for (var child : split.getSplitPackages()) if (contains(child, key)) return true;
        }
        return false;
    }
    private static int area(ItemStack stack, String key, int fallback) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); return tag.contains(key) ? Math.clamp(tag.getInt(key), 0, 3) : fallback;
    }
    public static int getAreaX(ItemStack stack) { return area(stack, "areax", 3); }
    public static int getAreaY(ItemStack stack) { return area(stack, "areay", 3); }
    public static int getAreaZ(ItemStack stack) { return area(stack, "areaz", 3); }
    public static int getAreaDim(ItemStack stack) { return area(stack, "aread", 0); }
    public static void toggleMisc(Player player, InteractionHand hand, int modifier) {
        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CasterItem) || !contains(CasterItem.getFocus(stack), "thaumcraft.PLAN")) return;
        int dimension = getAreaDim(stack), x = getAreaX(stack), y = getAreaY(stack), z = getAreaZ(stack);
        if (modifier == 1) dimension = (dimension + 1) % 4;
        else if (modifier == 0) { if (dimension == 0 || dimension == 1) x = (x + 1) % 4; if (dimension == 0 || dimension == 2) z = (z + 1) % 4; if (dimension == 0 || dimension == 3) y = (y + 1) % 4; }
        else return;
        final int dx = x, dy = y, dz = z, dim = dimension;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { tag.putInt("areax", dx); tag.putInt("areay", dy); tag.putInt("areaz", dz); tag.putInt("aread", dim); });
        player.getInventory().setChanged(); player.containerMenu.broadcastChanges();
    }
    private CasterManager() { }
}
