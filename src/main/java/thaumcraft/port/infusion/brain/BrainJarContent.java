package thaumcraft.port.infusion.brain;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

public final class BrainJarContent {
    /** Client installs the original spark renderer without linking client classes on a server. */
    public static java.util.function.BiConsumer<net.minecraft.world.level.Level,net.minecraft.core.BlockPos> fullSpark=(level,pos)->{};
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final Supplier<BlockEntityType<BrainJarEntity>> ENTITY=ENTITIES.register("jar_brain",()->BlockEntityType.Builder.of(BrainJarEntity::new,InfusionCraftingContent.JAR_BRAIN.get()).build(null));
    public static void register(IEventBus bus){ENTITIES.register(bus);}
    private BrainJarContent(){}
}
