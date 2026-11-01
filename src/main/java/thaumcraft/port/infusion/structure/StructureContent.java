package thaumcraft.port.infusion.structure;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;

public final class StructureContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "thaumcraft");
    public static final Supplier<DataComponentType<Integer>> CHARGE = PortContent.COMPONENTS.register("tc_charge", () -> DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredBlock<PedestalBlock> PEDESTAL_ARCANE = pedestal("pedestal_arcane", false);
    public static final DeferredBlock<PedestalBlock> PEDESTAL_ANCIENT = pedestal("pedestal_ancient", false);
    public static final DeferredBlock<PedestalBlock> PEDESTAL_ELDRITCH = pedestal("pedestal_eldritch", true);
    public static final DeferredBlock<PillarBlock> PILLAR_ARCANE = pillar("pillar_arcane");
    public static final DeferredBlock<PillarBlock> PILLAR_ANCIENT = pillar("pillar_ancient");
    public static final DeferredBlock<PillarBlock> PILLAR_ELDRITCH = pillar("pillar_eldritch");
    public static final DeferredBlock<Block> STONE_ANCIENT = PortContent.BLOCKS.registerSimpleBlock("stone_ancient", stone().requiresCorrectToolForDrops().strength(2, 10));
    public static final DeferredBlock<Block> STONE_ELDRITCH_TILE = PortContent.BLOCKS.registerSimpleBlock("stone_eldritch_tile", stone().requiresCorrectToolForDrops().strength(15, 1000));
    public static final DeferredBlock<SlabBlock> SLAB_ARCANE_STONE = slab("slab_arcane_stone");
    public static final DeferredBlock<SlabBlock> SLAB_ARCANE_BRICK = slab("slab_arcane_brick");
    public static final DeferredBlock<SlabBlock> SLAB_ANCIENT = slab("slab_ancient");
    public static final DeferredBlock<SlabBlock> SLAB_ELDRITCH = slab("slab_eldritch");
    public static final DeferredBlock<StabilizerBlock> STABILIZER = PortContent.BLOCKS.register("stabilizer", () -> new StabilizerBlock(stone().strength(2, 20).noOcclusion().lightLevel(s -> 4)));
    public static final DeferredBlock<InlayBlock> INLAY = PortContent.BLOCKS.register("inlay", () -> new InlayBlock(BlockBehaviour.Properties.of().requiresCorrectToolForDrops().strength(.5f).sound(SoundType.METAL).noCollission().noOcclusion().lightLevel(s -> 1)));
    public static final DeferredBlock<Block> MATRIX_COST = PortContent.BLOCKS.registerSimpleBlock("matrix_cost", stone().requiresCorrectToolForDrops().strength(2, 10));
    public static final DeferredBlock<Block> MATRIX_SPEED = PortContent.BLOCKS.registerSimpleBlock("matrix_speed", stone().requiresCorrectToolForDrops().strength(2, 10));
    public static final DeferredBlock<RechargePedestalBlock> RECHARGE_PEDESTAL = PortContent.BLOCKS.register("recharge_pedestal", () -> new RechargePedestalBlock(stone().strength(2, 20).noOcclusion()));
    public static final Supplier<BlockEntityType<PedestalEntity>> PEDESTAL_ENTITY = ENTITIES.register("pedestal", () -> BlockEntityType.Builder.of(PedestalEntity::new, PEDESTAL_ARCANE.get(), PEDESTAL_ANCIENT.get(), PEDESTAL_ELDRITCH.get()).build(null));
    public static final Supplier<BlockEntityType<RechargePedestalEntity>> RECHARGE_ENTITY = ENTITIES.register("recharge_pedestal", () -> BlockEntityType.Builder.of(RechargePedestalEntity::new, RECHARGE_PEDESTAL.get()).build(null));
    public static final Supplier<BlockEntityType<StabilizerEntity>> STABILIZER_ENTITY = ENTITIES.register("stabilizer", () -> BlockEntityType.Builder.of(StabilizerEntity::new, STABILIZER.get()).build(null));
    static {
        for (var block : new DeferredBlock<?>[]{PEDESTAL_ARCANE, PEDESTAL_ANCIENT, PEDESTAL_ELDRITCH, PILLAR_ARCANE, PILLAR_ANCIENT, PILLAR_ELDRITCH,
                STONE_ANCIENT, STONE_ELDRITCH_TILE, SLAB_ARCANE_STONE, SLAB_ARCANE_BRICK, SLAB_ANCIENT, SLAB_ELDRITCH, STABILIZER, INLAY, MATRIX_COST, MATRIX_SPEED, RECHARGE_PEDESTAL})
            PortContent.ITEMS.registerSimpleBlockItem(block);
    }
    private static BlockBehaviour.Properties stone() { return BlockBehaviour.Properties.of().sound(SoundType.STONE); }
    private static DeferredBlock<PedestalBlock> pedestal(String id, boolean eldritch) { return PortContent.BLOCKS.register(id, () -> new PedestalBlock(eldritch, stone().strength(2, 20).noOcclusion())); }
    private static DeferredBlock<PillarBlock> pillar(String id) { return PortContent.BLOCKS.register(id, () -> new PillarBlock(stone().strength(2.5f, 2).noOcclusion().pushReaction(PushReaction.BLOCK))); }
    private static DeferredBlock<SlabBlock> slab(String id) { return PortContent.BLOCKS.register(id, () -> new SlabBlock(stone().requiresCorrectToolForDrops().strength(2, 10))); }
    public static void register(IEventBus bus) { ENTITIES.register(bus); bus.addListener(StructureContent::capabilities); }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PEDESTAL_ENTITY.get(), (be, side) -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RECHARGE_ENTITY.get(), (be, side) -> side == null ? new InvWrapper(be) : new SidedInvWrapper(be, side));
    }
    private StructureContent() {}
}
