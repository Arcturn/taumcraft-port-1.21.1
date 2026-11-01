package thaumcraft.port.essentia;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;

/** Flux cleanup is the original BASICS prerequisite that supports the complete alchemy chain. */
public final class CondenserContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<CondenserBlock> CONDENSER=PortContent.BLOCKS.register("condenser",()->new CondenserBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<CondenserLatticeBlock> LATTICE=PortContent.BLOCKS.register("condenser_lattice",()->new CondenserLatticeBlock(false,BlockBehaviour.Properties.of().strength(.5f,5).sound(SoundType.METAL).lightLevel(s->4).noOcclusion()));
    public static final DeferredBlock<CondenserLatticeBlock> DIRTY_LATTICE=PortContent.BLOCKS.register("condenser_lattice_dirty",()->new CondenserLatticeBlock(true,BlockBehaviour.Properties.of().strength(.5f,5).sound(SoundType.METAL).noOcclusion()));
    public static final Supplier<BlockEntityType<CondenserEntity>> ENTITY=ENTITIES.register("condenser",()->BlockEntityType.Builder.of(CondenserEntity::new,CONDENSER.get()).build(null));
    static {PortContent.ITEMS.registerSimpleBlockItem(CONDENSER);PortContent.ITEMS.registerSimpleBlockItem(LATTICE);PortContent.ITEMS.registerSimpleBlockItem(DIRTY_LATTICE);}
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(CondenserContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){event.registerBlockEntity(EssentiaContent.CAPABILITY,ENTITY.get(),(be,side)->be);}
    private CondenserContent(){}
}
