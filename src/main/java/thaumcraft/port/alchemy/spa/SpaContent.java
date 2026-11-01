package thaumcraft.port.alchemy.spa;

import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

public final class SpaContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "thaumcraft");
    public static final DeferredBlock<SpaBlock> SPA = PortContent.BLOCKS.register("spa",
            () -> new SpaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2, 12).sound(SoundType.STONE)));
    public static final Supplier<BlockEntityType<SpaEntity>> ENTITY = ENTITIES.register("spa",
            () -> BlockEntityType.Builder.of(SpaEntity::new, SPA.get()).build(null));
    public static final Supplier<MenuType<SpaMenu>> MENU = MENUS.register("spa",
            () -> IMenuTypeExtension.create((id, inventory, buffer) -> new SpaMenu(id, inventory, buffer.readBlockPos())));
    static { PortContent.ITEMS.registerSimpleBlockItem("spa", SPA); }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(SpaContent::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(), (spa, side) -> spa.tank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(),
                (spa, side) -> side == Direction.UP ? null : new SidedInvWrapper(spa, side == null ? Direction.DOWN : side));
    }
    private SpaContent() {}
}
