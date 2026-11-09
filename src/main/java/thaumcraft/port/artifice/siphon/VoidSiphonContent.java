package thaumcraft.port.artifice.siphon;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

public final class VoidSiphonContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<BlockEntityType<VoidSiphonEntity>> ENTITY=ENTITIES.register("void_siphon",()->BlockEntityType.Builder.of(VoidSiphonEntity::new,InfusionCraftingContent.VOID_SIPHON.get()).build(null));
    public static final Supplier<MenuType<VoidSiphonMenu>> MENU=MENUS.register("void_siphon",()->new MenuType<>(VoidSiphonMenu::new,FeatureFlags.VANILLA_SET));
    public static BiConsumer<VoidSiphonEntity,Integer> stream=(entity,counter)->{};
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(VoidSiphonContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        // An unsided lookup has the same output-only contract as a hopper on any face.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->new SidedInvWrapper(be,side==null?Direction.DOWN:side));
    }
    private VoidSiphonContent(){}
}
