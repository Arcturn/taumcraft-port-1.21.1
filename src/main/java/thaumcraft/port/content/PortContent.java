package thaumcraft.port.content;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.ThaumcraftPort;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.SalisMundusRecipe;
import thaumcraft.port.integration.CurioItem;

public final class PortContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ThaumcraftPort.MOD_ID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ThaumcraftPort.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ThaumcraftPort.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ThaumcraftPort.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThaumcraftPort.MOD_ID);

    public static final Supplier<DataComponentType<String>> CRYSTAL_ASPECT = COMPONENTS.register("crystal_aspect",
            () -> DataComponentType.<String>builder().persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final DeferredItem<CrystalItem> CRYSTAL_ESSENCE = ITEMS.registerItem("crystal_essence", CrystalItem::new);
    public static final Supplier<DataComponentType<Integer>> CELESTIAL_KIND=COMPONENTS.register("celestial_kind",()->DataComponentType.<Integer>builder().persistent(Codec.intRange(0,12)).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DeferredItem<Item> SCRIBING_TOOLS=ITEMS.registerSimpleItem("scribing_tools",new Item.Properties().durability(100));
    // Display-only carrier for the original research art in vanilla advancement icons.
    public static final DeferredItem<Item> RESEARCH_ICON=ITEMS.registerSimpleItem("research_icon");
    public static final java.util.Map<String,DeferredItem<ResearchDisplayItem>> RESEARCH_DISPLAY_ICONS=ResearchDisplayItem.register(ITEMS);
    public static final DeferredItem<CelestialNotesItem> CELESTIAL_NOTES=ITEMS.registerItem("celestial_notes",CelestialNotesItem::new);
    public static final DeferredItem<ThaumometerItem> THAUMOMETER = ITEMS.registerItem("thaumometer", ThaumometerItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON = ITEMS.registerItem("thaumonomicon", ThaumonomiconItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SalisMundusItem> SALIS_MUNDUS = ITEMS.registerItem("salis_mundus", SalisMundusItem::new);
    public static final DeferredItem<CurioItem> CHARM_UNDYING = ITEMS.registerItem("charm_undying", CurioItem::new,
            new Item.Properties().rarity(Rarity.RARE));

    public static final Supplier<RecipeSerializer<SalisMundusRecipe>> SALIS_RECIPE =
            SERIALIZERS.register("salis_mundus", () -> new SimpleCraftingRecipeSerializer<>(SalisMundusRecipe::new));

    static {
        for (String id : List.of("amber", "quicksilver", "ingot_brass", "ingot_thaumium", "ingot_void",
                "nugget_brass", "nugget_thaumium", "nugget_void", "nugget_quicksilver", "nugget_quartz")) {
            ITEMS.registerSimpleItem(id);
        }
        for (String id : List.of("amulet_mundane", "ring_mundane", "girdle_mundane", "amulet_fancy", "ring_fancy", "girdle_fancy")) {
            ITEMS.registerItem(id, CurioItem::new);
        }
        for (String id : List.of("metal_brass", "metal_thaumium", "metal_void")) {
            addBlock(id, SoundType.METAL, 5f, 10f);
        }
        for (String ore : List.of("amber", "cinnabar", "quartz")) {
            float hardness = ore.equals("amber") ? 1.5f : ore.equals("cinnabar") ? 2f : 3f;
            addBlock("ore_" + ore, SoundType.STONE, hardness, 5f);
            addBlock("deepslate_ore_" + ore, SoundType.DEEPSLATE, hardness * 1.5f, 5f);
        }
        for (String id : List.of("amber_block", "amber_brick")) {
            addBlock(id, SoundType.STONE, 3f, 5f);
        }
        TABS.register("thaumcraft", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.thaumcraft"))
                .icon(() -> new ItemStack(THAUMONOMICON.get()))
                .displayItems((parameters, output) -> {
                    ITEMS.getEntries().forEach(entry -> {
                        if (entry.get() != CRYSTAL_ESSENCE.get() && entry.get()!=RESEARCH_ICON.get() && !(entry.get() instanceof ResearchDisplayItem)) output.accept(entry.get());
                    });
                    for (Aspect aspect : Aspect.values()) output.accept(CrystalItem.stack(aspect));
                    for(int kind=1;kind<13;kind++)output.accept(CelestialNotesItem.stack(kind));
                    for (Aspect aspect : Aspect.values()) output.accept(thaumcraft.port.essentia.EssentiaItem.phial(aspect));
                }).build());
    }

    private static void addBlock(String id, SoundType sound, float hardness, float resistance) {
        var properties = BlockBehaviour.Properties.of().strength(hardness, resistance).sound(sound).requiresCorrectToolForDrops();
        DeferredBlock<? extends Block> block = id.equals("ore_amber") || id.equals("ore_quartz")
                || id.equals("deepslate_ore_amber") || id.equals("deepslate_ore_quartz")
                ? BLOCKS.register(id, () -> new DropExperienceBlock(UniformInt.of(1,4), properties))
                : BLOCKS.registerSimpleBlock(id, properties);
        ITEMS.registerSimpleBlockItem(id, block);
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SERIALIZERS.register(bus);
        TABS.register(bus);
    }

    private PortContent() {}
}
