package thaumcraft.port.infusion.matrix;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

public final class MatrixContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<InfusionMatrixBlock> MATRIX=PortContent.BLOCKS.register("infusion_matrix",()->new InfusionMatrixBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).sound(SoundType.STONE).strength(2,12).noOcclusion()));
    public static final Supplier<BlockEntityType<InfusionMatrixEntity>> ENTITY=ENTITIES.register("infusion_matrix",()->BlockEntityType.Builder.of(InfusionMatrixEntity::new,MATRIX.get()).build(null));
    static { PortContent.ITEMS.registerSimpleBlockItem("infusion_matrix",MATRIX); }
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(MatrixContent::payloads);}
    private static void payloads(RegisterPayloadHandlersEvent event){
        event.registrar("1").playToClient(MatrixEffectPayload.TYPE,MatrixEffectPayload.STREAM_CODEC,
                (payload,context)->context.enqueueWork(()->thaumcraft.port.client.InfusionMatrixEffects.receive(payload)));
    }
    private MatrixContent(){}
}
