package thaumcraft.port.auromancy.item;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CasterContent {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "thaumcraft");
    public static final Supplier<MenuType<FocusPouchMenu>> POUCH_MENU = MENUS.register("focus_pouch", () -> IMenuTypeExtension.create(
            (id, inventory, buffer) -> new FocusPouchMenu(id, inventory, buffer.readEnum(InteractionHand.class))));
    public static void register(IEventBus bus) { MENUS.register(bus); }
    private CasterContent() { }
}
