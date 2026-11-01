package thaumcraft.port.crafting.content;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

/** Real registry identities used by the original arcane and crucible recipe outputs. */
public final class CraftingContent {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "thaumcraft");

    public static final DeferredBlock<PoweredRailBlock> ACTIVATOR_RAIL = PortContent.BLOCKS.register(
            "activator_rail", () -> new PoweredRailBlock(BlockBehaviour.Properties.of()
                    .noCollission().strength(.7f).sound(SoundType.METAL), true));
    public static final DeferredBlock<HungryChestBlock> HUNGRY_CHEST = PortContent.BLOCKS.register(
            "hungry_chest", () -> new HungryChestBlock(BlockBehaviour.Properties.of()
                    .strength(2.5f).sound(SoundType.WOOD).noOcclusion()));
    public static final Supplier<BlockEntityType<HungryChestEntity>> HUNGRY_CHEST_ENTITY = ENTITIES.register(
            "hungry_chest", () -> BlockEntityType.Builder.of(HungryChestEntity::new, HUNGRY_CHEST.get()).build(null));
    public static final DeferredBlock<ArcaneLampBlock> LAMP_ARCANE = PortContent.BLOCKS.register(
            "lamp_arcane", () -> new ArcaneLampBlock(BlockBehaviour.Properties.of().strength(1)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(state -> state.getValue(ArcaneLampBlock.LIT) ? 15 : 0)));
    public static final DeferredBlock<SlabBlock> SLAB_GREATWOOD = PortContent.BLOCKS.register(
            "slab_greatwood", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));
    public static final DeferredBlock<SlabBlock> SLAB_SILVERWOOD = PortContent.BLOCKS.register(
            "slab_silverwood", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB)));

    public static final DeferredBlock<CraftingDeviceBlock> ARCANE_WORKBENCH_CHARGER = device(
            "arcane_workbench_charger", SoundType.WOOD, 1.25f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> WAND_WORKBENCH = device(
            "wand_workbench", SoundType.STONE, 2.5f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> LEVITATOR = device(
            "levitator", SoundType.WOOD, 2.5f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> REDSTONE_RELAY = device(
            "redstone_relay", SoundType.STONE, 0, Block.box(0, 0, 0, 16, 2, 16));
    public static final DeferredBlock<CraftingDeviceBlock> ARCANE_EAR = device(
            "arcane_ear", SoundType.WOOD, 1, Block.box(2, 0, 2, 14, 6, 14));
    public static final DeferredBlock<CraftingDeviceBlock> DIOPTRA = device(
            "dioptra", SoundType.STONE, 2.5f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> PATTERN_CRAFTER = device(
            "pattern_crafter", SoundType.METAL, 2.5f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> VIS_BATTERY = device(
            "vis_battery", SoundType.GLASS, .5f, Shapes.block());
    public static final DeferredBlock<CraftingDeviceBlock> VIS_GENERATOR = device(
            "vis_generator", SoundType.METAL, 2.5f, Block.box(4, 0, 4, 12, 14, 12));
    public static final DeferredBlock<CraftingDeviceBlock> TABLE_STONE = device(
            "table_stone", SoundType.STONE, 2, Shapes.or(Block.box(0, 12, 0, 16, 16, 16),
                    Block.box(1, 0, 1, 5, 12, 5), Block.box(11, 0, 1, 15, 12, 5),
                    Block.box(1, 0, 11, 5, 12, 15), Block.box(11, 0, 11, 15, 12, 15),
                    Block.box(3, 3, 3, 13, 5, 13)));

    static {
        PortContent.ITEMS.registerSimpleBlockItem("activator_rail", ACTIVATOR_RAIL);
        PortContent.ITEMS.registerSimpleBlockItem("hungry_chest", HUNGRY_CHEST);
        PortContent.ITEMS.registerSimpleBlockItem("lamp_arcane", LAMP_ARCANE);
        PortContent.ITEMS.registerSimpleBlockItem("slab_greatwood", SLAB_GREATWOOD);
        PortContent.ITEMS.registerSimpleBlockItem("slab_silverwood", SLAB_SILVERWOOD);
        for (String id : List.of("module_vision", "module_aggression", "mirrored_glass", "mind_biothaumic",
                "turret_placer_basic", "turret_placer_advanced", "grapple_gun_tip", "grapple_gun_spool",
                "seal_blank", "seal_pickup", "seal_pickup_advanced", "seal_fill", "seal_fill_advanced",
                "seal_empty", "seal_empty_advanced", "seal_guard", "seal_guard_advanced", "seal_lumber",
                "seal_breaker", "seal_use", "seal_provider", "seal_stock", "seal_breaker_advanced")) {
            PortContent.ITEMS.registerSimpleItem(id);
        }
        PortContent.ITEMS.registerSimpleItem("focus_1", new Item.Properties().stacksTo(1));
        PortContent.ITEMS.registerSimpleItem("sanity_checker", new Item.Properties().stacksTo(1));
        PortContent.ITEMS.registerSimpleItem("grapple_gun", new Item.Properties().stacksTo(1));
        PortContent.ITEMS.registerSimpleItem("focus_pouch", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    private static DeferredBlock<CraftingDeviceBlock> device(String id, SoundType sound, float hardness, VoxelShape shape) {
        var block = PortContent.BLOCKS.register(id, () -> new CraftingDeviceBlock(
                BlockBehaviour.Properties.of().strength(hardness).sound(sound).noOcclusion(), shape));
        PortContent.ITEMS.registerSimpleBlockItem(id, block);
        return block;
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(CraftingContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HUNGRY_CHEST_ENTITY.get(), (chest, side) -> chest.itemHandler());
    }

    private CraftingContent() {}
}
