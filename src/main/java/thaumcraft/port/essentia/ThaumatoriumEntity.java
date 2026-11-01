package thaumcraft.port.essentia;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.CrucibleRecipe;
import thaumcraft.port.machine.*;
import thaumcraft.port.research.*;

/** TC6 automatic crucible: one catalyst slot, selected recipes and accumulated essentia. */
public final class ThaumatoriumEntity extends EssentiaEntity implements WorldlyContainer, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    public final MixedEssentia contents = new MixedEssentia(18500);
    public final LinkedHashMap<ResourceLocation, UUID> selected = new LinkedHashMap<>();
    public Aspect demand;
    public int capacity = 1;
    public boolean heated;
    private CrucibleRecipe active;
    private ResourceLocation activeId;
    private int counter;

    public ThaumatoriumEntity(BlockPos pos, BlockState state) {
        super(EssentiaContent.THAUMATORIUM_ENTITY.get(), pos, state);
    }

    public boolean top() { return ((ThaumatoriumBlock) getBlockState().getBlock()).top; }
    public ThaumatoriumEntity main() {
        if (!top()) return this;
        return level != null && level.hasChunkAt(worldPosition.below())
                && level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumEntity be && !be.top() ? be : null;
    }
    public Direction front() { return getBlockState().getValue(ThaumatoriumBlock.FACING); }
    public boolean formed() {
        return level != null && level.hasChunkAt(worldPosition.above())
                && level.getBlockState(worldPosition.above()).is(EssentiaContent.THAUMATORIUM_TOP.get())
                && level.getBlockState(worldPosition.below()).is(MachineContent.CRUCIBLE.get());
    }
    public boolean powered() {
        return level != null && (level.hasNeighborSignal(worldPosition) || level.hasNeighborSignal(worldPosition.above())
                || level.hasNeighborSignal(worldPosition.below()));
    }
    public boolean connects(Direction side) { return side != null && side != front(); }
    public boolean input(Direction side) { return connects(side); }
    public boolean output(Direction side) { return false; }
    public Aspect aspect(Direction side) { return null; }
    public int amount(Direction side) { return 0; }
    public Aspect suctionType(Direction side) { var be = main(); return be == null ? null : be.demand; }
    public int suction(Direction side) {
        var be = main();
        return (side == null || input(side)) && be != null && be.demand != null && be.heated
                && be.formed() && !be.powered() && !be.isEmpty() ? 128 : 0;
    }
    public int minimumSuction() { return 0; }
    public int extract(Aspect aspect, int amount, Direction side, boolean simulate) { return 0; }
    public int insert(Aspect aspect, int amount, Direction side, boolean simulate) {
        var be = main();
        if (!input(side) || be == null || be.active == null || aspect == null || suction(side) == 0) return 0;
        int accepted = be.contents.insert(aspect,
                Math.min(amount, be.active.aspects().getOrDefault(aspect.id(), 0) - be.contents.amount(aspect)), simulate);
        if (accepted > 0 && !simulate) be.sync();
        return accepted;
    }
    public RecipeHolder<CrucibleRecipe> recipe(ResourceLocation id) {
        if (level == null) return null;
        var holder = level.getRecipeManager().byKey(id).orElse(null);
        return holder != null && holder.value() instanceof CrucibleRecipe recipe ? new RecipeHolder<>(id, recipe) : null;
    }
    public boolean permitted(UUID player, CrucibleRecipe recipe) {
        return level instanceof ServerLevel server && ResearchStorage.get(server.getServer()).player(player).knows(recipe.research());
    }
    public List<RecipeHolder<CrucibleRecipe>> choices(ServerPlayer player) {
        // TC6 generateRecipeHashlist keeps the selected order when no catalyst is present.
        if (items.getFirst().isEmpty()) return selected.keySet().stream().map(this::recipe).filter(Objects::nonNull).toList();
        return level.getRecipeManager().getAllRecipesFor(MachineContent.CRUCIBLE_RECIPE.get()).stream()
                .filter(holder -> selected.containsKey(holder.id())
                        || holder.value().catalyst().test(items.getFirst()) && permitted(player.getUUID(), holder.value()))
                .sorted(Comparator.<RecipeHolder<CrucibleRecipe>, String>comparing(holder ->
                        holder.value().getResultItem(level.registryAccess()).getHoverName().getString())
                        .thenComparing(holder -> holder.id().toString())).toList();
    }
    public void choose(ServerPlayer player, ResourceLocation id) {
        if (!stillValid(player)) return;
        if (selected.remove(id) != null) {
            resetActive();
            sync();
            return;
        }
        var recipe = recipe(id);
        if (recipe == null || !recipe.value().catalyst().test(items.getFirst())
                || !permitted(player.getUUID(), recipe.value()) || selected.size() >= capacity) return;
        selected.put(id, player.getUUID());
        sync();
    }
    private void resetActive() { demand = null; active = null; activeId = null; }
    private void updateEnvironment() {
        heated = CrucibleEntity.isHeatSource(level.getBlockState(worldPosition.below(2)));
        int upgrades = 0;
        for (int y = 0; y < 2; y++) for (Direction side : Direction.values()) {
            if (side == Direction.DOWN || side == front()) continue;
            var pos = worldPosition.above(y).relative(side);
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (state.is(EssentiaContent.BRAIN_BOX.get()) && state.getValue(BrainBoxBlock.FACING) == side.getOpposite()) upgrades++;
        }
        capacity = 1 + upgrades * 2;
        while (selected.size() > capacity) {
            var last = new ArrayList<>(selected.keySet()).get(selected.size() - 1);
            selected.remove(last);
        }
        if (activeId != null && !selected.containsKey(activeId)) resetActive();
    }
    public static Aspect missing(CrucibleRecipe recipe, MixedEssentia contents) {
        return recipe.aspects().keySet().stream().sorted().map(id -> Aspect.byId(id).orElseThrow())
                .filter(aspect -> contents.amount(aspect) < recipe.aspects().get(aspect.id())).findFirst().orElse(null);
    }
    @Override public void tick() {
        if (top() || !(level instanceof ServerLevel server)) return;
        Aspect oldDemand = demand;
        boolean wasHeated = heated;
        int oldCapacity = capacity;
        // Original machine-local cadence: environment at 0,40,... and processing at 5,10,...
        if (counter % 40 == 0) updateEnvironment();
        counter = (counter + 1) % 40;
        if (!formed() || !heated || powered() || items.getFirst().isEmpty()) demand = null;
        else if (counter % 5 == 0) process(server);
        if (oldDemand != demand || wasHeated != heated || oldCapacity != capacity) sync();
    }
    private void process(ServerLevel server) {
        if (activeId == null || !selected.containsKey(activeId) || active == null || !active.catalyst().test(items.getFirst())) {
            resetActive();
            for (var id : selected.keySet()) {
                var recipe = recipe(id);
                if (recipe != null && recipe.value().catalyst().test(items.getFirst())) {
                    activeId = id;
                    active = recipe.value();
                    break;
                }
            }
        }
        if (active == null) return;
        demand = missing(active, contents);
        if (demand == null) {
            ItemStack result = active.assemble(new SingleRecipeInput(items.getFirst()), server.registryAccess());
            UUID owner = selected.get(activeId);
            items.getFirst().shrink(1);
            contents.clear();
            var player = server.getServer().getPlayerList().getPlayer(owner);
            // The original fires the Forge crafting event with the remaining catalyst slot.
            // Our ItemCraftedEvent listener records research progress once through that same event.
            if (player != null) EventHooks.firePlayerCraftingEvent(player, result, new SimpleContainer(items.getFirst()));
            ejectResult(server, result);
            server.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, .25f,
                    2.6f + (server.random.nextFloat() - server.random.nextFloat()) * .8f);
            resetActive();
            sync();
        } else {
            for (int y = 0; y < 2; y++) {
                if (!(level.getBlockEntity(worldPosition.above(y)) instanceof ThaumatoriumEntity sink)) continue;
                for (Direction side : Direction.values()) {
                    if (side == front() || side == Direction.DOWN || y == 0 && side == Direction.UP) continue;
                    if (EssentiaTransport.pull(sink, side, sink.neighbour(side)) > 0) return;
                }
            }
        }
    }
    private void ejectResult(ServerLevel server, ItemStack stack) {
        Direction side = front();
        BlockPos outputPos = worldPosition.relative(side);
        ItemStack remainder = stack;
        if (server.hasChunkAt(outputPos)) {
            var handler = server.getCapability(Capabilities.ItemHandler.BLOCK, outputPos, side.getOpposite());
            if (handler != null) remainder = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        }
        if (remainder.isEmpty()) return;
        // A blocked outlet drops inside the machine, as InventoryUtils.ejectStackAt did in TC6.
        BlockPos origin = server.getBlockState(outputPos).isSolidRender(server, outputPos)
                ? worldPosition.relative(side.getOpposite()) : worldPosition;
        var dropped = new ItemEntity(server, origin.getX() + .5 + side.getStepX(), origin.getY(),
                origin.getZ() + .5 + side.getStepZ(), remainder);
        dropped.setDeltaMovement(side.getStepX() * .3, 0, side.getStepZ() * .3);
        server.addFreshEntity(dropped);
    }
    public int getContainerSize() { return 1; }
    public boolean isEmpty() { return getItem(0).isEmpty(); }
    public ItemStack getItem(int slot) {
        var be = main(); return be == null ? ItemStack.EMPTY : be.items.get(slot);
    }
    public ItemStack removeItem(int slot, int amount) {
        var be = main();
        if (be == null) return ItemStack.EMPTY;
        var stack = ContainerHelper.removeItem(be.items, slot, amount);
        if (!stack.isEmpty()) be.sync();
        return stack;
    }
    public ItemStack removeItemNoUpdate(int slot) {
        var be = main(); return be == null ? ItemStack.EMPTY : ContainerHelper.takeItem(be.items, slot);
    }
    public void setItem(int slot, ItemStack stack) {
        var be = main();
        if (be == null) return;
        be.items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        // Keep a matching in-progress recipe and its essentia when automation tops up the stack.
        if (be.active != null && !be.active.catalyst().test(stack)) be.resetActive();
        be.sync();
    }
    public boolean stillValid(Player player) { return main() != null && Container.stillValidBlockEntity(this, player); }
    public void clearContent() {
        var be = main();
        if (be != null) { be.items.clear(); be.resetActive(); be.sync(); }
    }
    public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }
    public Component getDisplayName() { return EssentiaContent.THAUMATORIUM.get().getName(); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        var be = main(); return be == null ? null : new ThaumatoriumMenu(id, inventory, be);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (top()) return;
        ContainerHelper.saveAllItems(tag, items, registries);
        var aspects = new CompoundTag();
        contents.values().forEach((aspect, amount) -> aspects.putInt(aspect.id(), amount));
        tag.put("Contents", aspects);
        var list = new ListTag();
        selected.forEach((id, uuid) -> {
            var value = new CompoundTag();
            value.putString("Recipe", id.toString()); value.putUUID("Player", uuid); list.add(value);
        });
        tag.put("Recipes", list); tag.putInt("Capacity", capacity); tag.putBoolean("Heated", heated);
        if (demand != null) tag.putString("Demand", demand.id());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        var values = new EnumMap<Aspect, Integer>(Aspect.class);
        var aspects = tag.getCompound("Contents");
        for (Aspect aspect : Aspect.values()) values.put(aspect, aspects.getInt(aspect.id()));
        contents.restore(values);
        selected.clear();
        var list = tag.getList("Recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(19, list.size()); i++) {
            var value = list.getCompound(i);
            var id = ResourceLocation.tryParse(value.getString("Recipe"));
            if (id != null && value.hasUUID("Player")) selected.put(id, value.getUUID("Player"));
        }
        capacity = Math.clamp(tag.getInt("Capacity"), 1, 19);
        heated = tag.getBoolean("Heated"); demand = readAspect(tag, "Demand"); active = null; activeId = null; counter = 0;
    }
}
