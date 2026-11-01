package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.aspect.Aspect;

/** The original single catalyst slot, with recipe buttons synchronized separately from inventory slots. */
public final class ThaumatoriumMenu extends AbstractContainerMenu {
    public record RecipeView(ResourceLocation id, ItemStack output, Map<Aspect,Integer> aspects) {}

    private final Container machine;
    private final Player viewer;
    private CompoundTag lastSent, selectionContext;
    public String token = UUID.randomUUID().toString();
    public long revision = -1;
    public int capacity = 1;
    public boolean catalystPresent;
    public List<RecipeView> recipes = List.of();
    public List<ResourceLocation> selected = List.of();
    public Map<Aspect,Integer> contents = Map.of();

    public ThaumatoriumMenu(int id, Inventory inv) { this(id, inv, new SimpleContainer(1)); }

    public ThaumatoriumMenu(int id, Inventory inv, Container machine) {
        super(EssentiaContent.THAUMATORIUM_MENU.get(), id);
        this.machine = machine;
        viewer = inv.player;
        addSlot(new Slot(machine, 0, 55, 24));
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++)
            addSlot(new Slot(inv, 9 + y * 9 + x, 8 + x * 18, 135 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(inv, x, 8 + x * 18, 193));
    }

    /** Server snapshot. Essentia progress alone must not invalidate a recipe click in flight. */
    public CompoundTag snapshot() {
        if (!(machine instanceof ThaumatoriumEntity be) || !(viewer instanceof ServerPlayer player))
            return new CompoundTag();
        var context = new CompoundTag();
        var choices = new ListTag();
        for (var holder : be.choices(player)) {
            var choice = new CompoundTag();
            choice.putString("id", holder.id().toString());
            choice.put("output", holder.value().result().save(player.registryAccess()));
            var aspects = new CompoundTag();
            holder.value().aspects().forEach(aspects::putInt);
            choice.put("aspects", aspects);
            choices.add(choice);
        }
        context.put("recipes", choices);
        var chosen = new ListTag();
        be.selected.keySet().forEach(id -> chosen.add(StringTag.valueOf(id.toString())));
        context.put("selected", chosen);
        context.putInt("capacity", be.capacity);
        context.putBoolean("catalystPresent", !be.isEmpty());
        if (!context.equals(selectionContext)) {
            revision++;
            selectionContext = context.copy();
        }
        var data = context.copy();
        data.putLong("revision", revision);
        var amounts = new CompoundTag();
        be.contents.values().forEach((a, n) -> amounts.putInt(a.id(), n));
        data.put("contents", amounts);
        return data;
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof ServerPlayer player) || !(machine instanceof ThaumatoriumEntity)) return;
        var data = snapshot();
        if (!data.equals(lastSent)) {
            PacketDistributor.sendToPlayer(player, new ThaumatoriumSyncPayload(containerId, token, data));
            lastSent = data.copy();
        }
    }

    public void select(ServerPlayer player, ThaumatoriumSelectPayload request) {
        if (player != viewer || player.containerMenu != this || request.container() != containerId
                || !token.equals(request.token()) || !stillValid(player)
                || !(machine instanceof ThaumatoriumEntity be)) return;
        // Re-evaluate eligibility before comparing the revision, including changes by another viewer.
        snapshot();
        if (request.revision() == revision && selectionContext.getList("recipes", Tag.TAG_COMPOUND)
                .stream().map(tag -> ((CompoundTag)tag).getString("id")).anyMatch(request.recipe().toString()::equals))
            be.choose(player, request.recipe());
        broadcastChanges();
    }

    public void receive(String nonce, CompoundTag data) {
        token = nonce;
        revision = data.getLong("revision");
        capacity = Math.clamp(data.getInt("capacity"), 1, 19);
        catalystPresent = data.getBoolean("catalystPresent");
        var available = new ArrayList<RecipeView>();
        var recipeList = data.getList("recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < recipeList.size(); i++) {
            var recipe = recipeList.getCompound(i);
            var id = ResourceLocation.tryParse(recipe.getString("id"));
            var output = ItemStack.parseOptional(viewer.registryAccess(), recipe.getCompound("output"));
            if (id != null && !output.isEmpty())
                available.add(new RecipeView(id, output, readAspects(recipe.getCompound("aspects"))));
        }
        recipes = List.copyOf(available);
        var chosen = new ArrayList<ResourceLocation>();
        for (var tag : data.getList("selected", Tag.TAG_STRING)) {
            var id = ResourceLocation.tryParse(tag.getAsString());
            if (id != null && chosen.size() < 19) chosen.add(id);
        }
        selected = List.copyOf(chosen);
        contents = readAspects(data.getCompound("contents"));
    }

    private static Map<Aspect,Integer> readAspects(CompoundTag tag) {
        var aspects = new EnumMap<Aspect,Integer>(Aspect.class);
        for (Aspect aspect : Aspect.values()) {
            int amount = tag.getInt(aspect.id());
            if (amount > 0) aspects.put(aspect, amount);
        }
        return Collections.unmodifiableMap(aspects);
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (!(index == 0 ? moveItemStackTo(stack, 1, 37, false) : moveItemStackTo(stack, 0, 1, false)))
            return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
