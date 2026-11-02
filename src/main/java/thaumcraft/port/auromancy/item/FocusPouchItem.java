package thaumcraft.port.auromancy.item;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Eighteen focus-only slots, using vanilla container components for save and inventory synchronization. */
public final class FocusPouchItem extends Item implements ICurioItem {
    public static final int SIZE = 18;
    public FocusPouchItem(Properties properties) { super(properties.stacksTo(1).rarity(Rarity.UNCOMMON)); }
    public static NonNullList<ItemStack> getInventory(ItemStack pouch) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items); return items;
    }
    public static void setInventory(ItemStack pouch, java.util.List<ItemStack> items) {
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var pouch = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) server.openMenu(new SimpleMenuProvider(
                (id, inv, user) -> new FocusPouchMenu(id, inv, hand), pouch.getHoverName()), buffer -> buffer.writeEnum(hand));
        return InteractionResultHolder.sidedSuccess(pouch, level.isClientSide);
    }
    @Override public boolean canEquipFromUse(SlotContext context, ItemStack stack) { return false; }
}
