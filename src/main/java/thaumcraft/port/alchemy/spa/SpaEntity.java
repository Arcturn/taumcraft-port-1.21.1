package thaumcraft.port.alchemy.spa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;

/** TC6's five-bucket spa: one source every forty ticks, in a supported connected 5x5 bath. */
public final class SpaEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int CAPACITY = 5000;
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private boolean mix = true;
    private int counter;
    public final FluidTank tank = new FluidTank(CAPACITY) {
        @Override protected void onContentsChanged() { changed(); }
    };
    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            int fluid = BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());
            return switch (index) {
                case 0 -> mix ? 1 : 0;
                case 1 -> tank.getFluidAmount();
                case 2 -> fluid & 0xffff;
                case 3 -> fluid >>> 16;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 4; }
    };

    public SpaEntity(BlockPos pos, BlockState state) { super(SpaContent.ENTITY.get(), pos, state); }
    public boolean mixing() { return mix; }
    public void toggleMix() { mix = !mix; changed(); }

    public void tick() {
        if (level == null || level.isClientSide || counter++ % 40 != 0 || level.hasNeighborSignal(worldPosition)) return;
        if (tank.getFluidAmount() < 1000) return;
        if (mix && (!tank.getFluid().is(Fluids.WATER) || !canPlaceItem(0, items.getFirst()))) return;
        var output = mix ? AlchemyLiquids.PURIFYING_BATH.get() : tank.getFluid().getFluid();
        FluidStack placedFluid = mix ? new FluidStack(output, 1000) : tank.getFluid().copyWithAmount(1000);
        if (!output.getFluidType().canBePlacedInLevel(level, worldPosition.above(), placedFluid)) return;
        if (!mix && output.isSame(Fluids.WATER) && level.dimensionType().ultraWarm()) return;
        BlockState target = output.getFluidType().getBlockForFluidState(level, worldPosition.above(), output.defaultFluidState());
        if (target.isAir()) return;
        BlockPos above = worldPosition.above();
        if (level.getBlockState(above).equals(target)) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
                BlockPos candidate = above.offset(x, 0, z);
                if (validLocation(candidate, true, target) && place(candidate, target)) return;
            }
        } else if (validLocation(above, false, target)) place(above, target);
    }

    private boolean validLocation(BlockPos pos, boolean adjacent, BlockState target) {
        if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos) || !level.hasChunkAt(pos.below())) return false;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos).equals(target)) return false;
        if (!adjacent) return true;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).equals(target)) return true;
        }
        return false;
    }

    private boolean place(BlockPos pos, BlockState target) {
        if (!level.setBlockAndUpdate(pos, target)) return false;
        if (mix) items.getFirst().shrink(1);
        tank.drain(1000, FluidAction.EXECUTE);
        changed();
        return true;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        var stack = ContainerHelper.removeItem(items, slot, count);
        if (!stack.isEmpty()) changed();
        return stack;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(getMaxStackSize(stack)); changed(); }
    @Override public void clearContent() { items.clear(); changed(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && !stack.isEmpty() && stack.is(AlchemyLiquids.BATH_SALTS.get()); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.UP ? new int[0] : new int[]{0}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.UP && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.UP && slot == 0; }
    @Override public Component getDisplayName() { return Component.translatable("tile.spa.name"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new SpaMenu(id, inventory, this); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putBoolean("Mix", mix);
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        mix = !tag.contains("Mix") || tag.getBoolean("Mix");
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        if (tank.getFluidAmount() > CAPACITY) tank.getFluid().setAmount(CAPACITY);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
