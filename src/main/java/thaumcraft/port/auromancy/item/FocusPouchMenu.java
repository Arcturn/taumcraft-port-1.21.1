package thaumcraft.port.auromancy.item;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** The opened hand and backing stack are fixed for the life of the menu, including off-hand pouches. */
public final class FocusPouchMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final ItemStack pouch;
    private final int lockedSlot;
    private final SimpleContainer contents;
    public FocusPouchMenu(int id, Inventory inventory, InteractionHand hand) {
        super(CasterContent.POUCH_MENU.get(), id); this.hand = hand; pouch = inventory.player.getItemInHand(hand);
        lockedSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : 40;
        contents = new SimpleContainer(FocusPouchItem.SIZE);
        var saved = FocusPouchItem.getInventory(pouch); for (int i = 0; i < saved.size(); i++) contents.setItem(i, saved.get(i));
        contents.addListener(container -> { if (!inventory.player.level().isClientSide && pouch.getItem() instanceof FocusPouchItem) {
            var items = net.minecraft.core.NonNullList.withSize(FocusPouchItem.SIZE, ItemStack.EMPTY);
            for (int i = 0; i < items.size(); i++) items.set(i, contents.getItem(i));
            FocusPouchItem.setInventory(pouch, items); inventory.setChanged();
        }});
        for (int row = 0; row < 3; row++) for (int col = 0; col < 6; col++) addSlot(new Slot(contents, col + row * 6, 37 + col * 18, 51 + row * 18) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FocusItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) inventorySlot(inventory, col + row * 9 + 9, 8 + col * 18, 151 + row * 18);
        for (int col = 0; col < 9; col++) inventorySlot(inventory, col, 8 + col * 18, 209);
    }
    private void inventorySlot(Inventory inventory, int index, int x, int y) {
        addSlot(new Slot(inventory, index, x, y) {
            @Override public boolean mayPickup(Player player) { return index != lockedSlot; }
            @Override public boolean mayPlace(ItemStack stack) { return index != lockedSlot; }
        });
    }
    public int lockedSlot() { return lockedSlot; }
    @Override public boolean stillValid(Player player) { return !player.isRemoved() && player.getItemInHand(hand) == pouch && pouch.getItem() instanceof FocusPouchItem; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || (type == ClickType.SWAP && button == lockedSlot)) return;
        super.clicked(slot, button, type, player);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        var slot = slots.get(index); if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        if (index < FocusPouchItem.SIZE) { if (!moveItemStackTo(stack, FocusPouchItem.SIZE, slots.size(), true)) return ItemStack.EMPTY; }
        else if (!(stack.getItem() instanceof FocusItem) || !moveItemStackTo(stack, 0, FocusPouchItem.SIZE, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
