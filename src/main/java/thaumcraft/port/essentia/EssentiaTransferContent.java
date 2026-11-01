package thaumcraft.port.essentia;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

/** TC6's rear-connected wireless essentia input and output. */
public final class EssentiaTransferContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<EssentiaTransferBlock> INPUT=block("essentia_input",true),OUTPUT=block("essentia_output",false);
    public static final net.neoforged.neoforge.registries.DeferredItem<ResonatorItem> RESONATOR=PortContent.ITEMS.registerItem("resonator",ResonatorItem::new);
    public static final Supplier<BlockEntityType<EssentiaTransferEntity>> ENTITY=ENTITIES.register("essentia_transfer",()->BlockEntityType.Builder.of(EssentiaTransferEntity::new,INPUT.get(),OUTPUT.get()).build(null));
    private static DeferredBlock<EssentiaTransferBlock> block(String name,boolean input){
        var block=PortContent.BLOCKS.register(name,()->new EssentiaTransferBlock(input,BlockBehaviour.Properties.of().strength(1,10).sound(SoundType.METAL).noOcclusion()));
        PortContent.ITEMS.registerSimpleBlockItem(name,block);
        return block;
    }
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(EssentiaTransferContent::capabilities);bus.addListener(EssentiaTransferContent::network);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(EssentiaContent.CAPABILITY,ENTITY.get(),(be,face)->be);}
    private static void network(RegisterPayloadHandlersEvent event){event.registrar("9").playToClient(EssentiaStreamPayload.TYPE,EssentiaStreamPayload.STREAM_CODEC,(payload,context)->context.enqueueWork(()->thaumcraft.port.client.EssentiaStreams.receive(payload)));}
    private EssentiaTransferContent(){}
}
