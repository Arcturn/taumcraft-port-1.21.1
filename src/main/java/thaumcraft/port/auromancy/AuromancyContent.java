package thaumcraft.port.auromancy;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.auromancy.focal.*;
import thaumcraft.port.crafting.content.CraftingContent;

public final class AuromancyContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<BlockEntityType<FocalManipulatorEntity>> FOCAL_ENTITY=ENTITIES.register("focal_manipulator",()->BlockEntityType.Builder.of(FocalManipulatorEntity::new,CraftingContent.WAND_WORKBENCH.get()).build(null));
    public static final Supplier<MenuType<FocalManipulatorMenu>> FOCAL_MENU=MENUS.register("focal_manipulator",()->IMenuTypeExtension.create((id,inventory,buffer)->{
        var pos=buffer.readBlockPos();var be=inventory.player.level().getBlockEntity(pos);
        var table=be instanceof FocalManipulatorEntity focal?focal:new FocalManipulatorEntity(pos,CraftingContent.WAND_WORKBENCH.get().defaultBlockState());
        return new FocalManipulatorMenu(id,inventory,table);
    }));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);}
    private AuromancyContent(){}
}
