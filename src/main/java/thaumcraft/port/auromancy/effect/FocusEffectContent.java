package thaumcraft.port.auromancy.effect;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Spell-created blocks have no obtainable block items. */
public final class FocusEffectContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("thaumcraft");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredBlock<HoleBlock> HOLE=BLOCKS.register("hole",()->new HoleBlock(BlockBehaviour.Properties.of().strength(-1,6_000_000).sound(SoundType.GLASS).noCollission().noOcclusion().forceSolidOn().noLootTable().lightLevel(s->10).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<SapBlock> SAP=BLOCKS.register("effect_sap",()->new SapBlock(BlockBehaviour.Properties.of().noCollission().replaceable().air().noLootTable().strength(1.5f,999).lightLevel(s->7).randomTicks()));
    public static final Supplier<BlockEntityType<HoleBlockEntity>> HOLE_ENTITY=ENTITIES.register("hole",()->BlockEntityType.Builder.of(HoleBlockEntity::new,HOLE.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);FocusWorldEffects.register();bus.addListener(FocusEffectVisuals::registerPayloads);}
    private FocusEffectContent(){}
}
