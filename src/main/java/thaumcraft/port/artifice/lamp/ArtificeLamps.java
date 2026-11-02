package thaumcraft.port.artifice.lamp;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

public final class ArtificeLamps {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    /** Lamplighter and arcane lamps use the same original glimmer identity. */
    public static final DeferredBlock<net.minecraft.world.level.block.AirBlock> GLIMMER = thaumcraft.port.infusion.armor.InfusionEnchantments.GLIMMER;
    public static final Supplier<BlockEntityType<LampEntity>> ENTITY = ENTITIES.register("artifice_lamp", () -> BlockEntityType.Builder.of(
            LampEntity::new, CraftingContent.LAMP_ARCANE.get(), InfusionCraftingContent.LAMP_GROWTH.get(), InfusionCraftingContent.LAMP_FERTILITY.get()).build(null));
    public static void register(IEventBus bus) { ENTITIES.register(bus); bus.addListener(ArtificeLamps::capabilities); }
    private static void capabilities(RegisterCapabilitiesEvent event) { event.registerBlockEntity(EssentiaContent.CAPABILITY,ENTITY.get(),(lamp,side) -> lamp); }
    private ArtificeLamps() {}
}
