package thaumcraft.port.artifice.device;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.content.CraftingContent;

/** The six BETA26 devices share only their transport; each retains its original state machine. */
public final class ArtificeDevices {
    public static java.util.function.Consumer<ArtificeDeviceEntity> clientTick = entity -> {};
    public static java.util.function.Consumer<ArtificeDeviceEntity> clientAnimate = entity -> {};
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<ArtificeDeviceBlock> ARCANE_EAR_TOGGLE = PortContent.BLOCKS.register("arcane_ear_toggle",
            () -> new ArtificeDeviceBlock(ArtificeDeviceBlock.Kind.ARCANE_EAR_TOGGLE,BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion()));
    public static final Supplier<BlockEntityType<ArtificeDeviceEntity>> ENTITY = ENTITIES.register("artifice_device",
            () -> BlockEntityType.Builder.of(ArtificeDeviceEntity::new,CraftingContent.LEVITATOR.get(),CraftingContent.REDSTONE_RELAY.get(),
                    CraftingContent.ARCANE_EAR.get(),ARCANE_EAR_TOGGLE.get(),CraftingContent.DIOPTRA.get(),CraftingContent.PATTERN_CRAFTER.get(),CraftingContent.VIS_GENERATOR.get()).build(null));
    static { PortContent.ITEMS.registerSimpleBlockItem("arcane_ear_toggle",ARCANE_EAR_TOGGLE); }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);bus.addListener(ArtificeDevices::capabilities);
        NeoForge.EVENT_BUS.addListener(ArtificeDevices::note);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,ENTITY.get(),(be,side)->
                be.kind()==ArtificeDeviceBlock.Kind.VIS_GENERATOR&&side==be.facing()?be:null);
    }
    private static void note(NoteBlockEvent.Play event) {
        if(event.getLevel() instanceof ServerLevel level&&!event.isCanceled())
            ArtificeDeviceEntity.hearNote(level,event.getPos(),event.getInstrument(),event.getVanillaNoteId());
    }
    private ArtificeDevices() {}
}
