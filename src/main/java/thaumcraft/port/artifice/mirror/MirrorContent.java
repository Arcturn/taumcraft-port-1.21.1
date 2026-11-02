package thaumcraft.port.artifice.mirror;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

public final class MirrorContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"thaumcraft");
    public static final Supplier<BlockEntityType<MirrorEntity>> ENTITY=ENTITIES.register("mirror",()->BlockEntityType.Builder.of(MirrorEntity::new,InfusionCraftingContent.MIRROR.get(),InfusionCraftingContent.MIRROR_ESSENTIA.get()).build(null));
    public static final Supplier<MenuType<HandMirrorMenu>> MENU=MENUS.register("hand_mirror",()->IMenuTypeExtension.create((id,inv,buf)->new HandMirrorMenu(id,inv,buf.readEnum(net.minecraft.world.InteractionHand.class))));
    public static void register(IEventBus bus){ENTITIES.register(bus);MENUS.register(bus);bus.addListener(MirrorContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(EssentiaContent.SOURCE,ENTITY.get(),(be,side)->be.essentia()?be:null);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->be.essentia()?null:be.items);
    }
    private MirrorContent(){}
}
