package thaumcraft.port.alchemy.spa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import thaumcraft.port.alchemy.liquid.AlchemyLiquids;

public final class SpaMenu extends AbstractContainerMenu {
    private final Container machine;
    private final Player owner;
    public final BlockPos position;
    public final ContainerData data;

    public SpaMenu(int id, Inventory inventory, BlockPos position) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(4), position);
    }
    public SpaMenu(int id, Inventory inventory, SpaEntity machine) {
        this(id, inventory, machine, machine.data, machine.getBlockPos());
    }
    private SpaMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos position) {
        super(SpaContent.MENU.get(), id);
        this.machine = machine;
        this.data = data;
        this.position = position.immutable();
        this.owner = inventory.player;
        checkContainerSize(machine, 1);
        checkContainerDataCount(data, 4);
        addSlot(new Slot(machine, 0, 65, 31) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(AlchemyLiquids.BATH_SALTS.get()); }
        });
        for (int y = 0; y < 3; y++) for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, 9 + y * 9 + x, 8 + x * 18, 84 + y * 18));
        for (int x = 0; x < 9; x++) addSlot(new Slot(inventory, x, 8 + x * 18, 142));
        addDataSlots(data);
    }

    public boolean mixing() { return data.get(0) != 0; }
    public FluidStack fluid() {
        int amount = Math.clamp(data.get(1), 0, SpaEntity.CAPACITY);
        var fluid = BuiltInRegistries.FLUID.byId((data.get(2) & 0xffff) | (data.get(3) & 0xffff) << 16);
        return amount == 0 || fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive() && machine.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 1 || player.containerMenu != this || !stillValid(player) || !(machine instanceof SpaEntity spa)) return false;
        spa.toggleMix();
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        if (!(index == 0 ? moveItemStackTo(stack, 1, 37, true)
                : stack.is(AlchemyLiquids.BATH_SALTS.get()) && moveItemStackTo(stack, 0, 1, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return copy;
    }
}
