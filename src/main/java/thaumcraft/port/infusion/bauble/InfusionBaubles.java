package thaumcraft.port.infusion.bauble;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import thaumcraft.port.content.PortContent;

/** Functional TC6 infusion baubles and enchanted decorations. */
public final class InfusionBaubles {
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"thaumcraft");
    public static final DeferredItem<BaubleItem> CLOUD_RING=PortContent.ITEMS.registerItem("cloud_ring",p->new BaubleItem(p.rarity(Rarity.RARE)));
    public static final DeferredItem<BaubleItem> CURIOSITY_BAND=PortContent.ITEMS.registerItem("curiosity_band",p->new BaubleItem(p.rarity(Rarity.RARE)));
    public static final DeferredItem<ApprenticeRingItem> RING_APPRENTICE=PortContent.ITEMS.registerItem("ring_apprentice",p->new ApprenticeRingItem(p.rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<VerdantCharmItem> VERDANT_CHARM=PortContent.ITEMS.registerItem("verdant_charm",p->new VerdantCharmItem(p.rarity(Rarity.RARE)));
    public static final DeferredBlock<PavingStoneBlock> PAVING_STONE_TRAVEL=paving("paving_stone_travel",false),PAVING_STONE_BARRIER=paving("paving_stone_barrier",true);
    public static final DeferredBlock<BarrierBlock> BARRIER=PortContent.BLOCKS.register("barrier",()->new BarrierBlock(BlockBehaviour.Properties.of().noOcclusion().noLootTable().replaceable().dynamicShape()));
    public static final Map<DyeColor,DeferredBlock<ThaumicBannerBlock>> BANNERS=new LinkedHashMap<>();
    static {
        for(var color:DyeColor.values()){
            var name="banner_"+color.getName();
            var block=PortContent.BLOCKS.register(name,()->new ThaumicBannerBlock(color,BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion().noCollission()));
            BANNERS.put(color,block);PortContent.ITEMS.registerItem(name,p->new ThaumicBannerItem(block.get(),p));
        }
    }
    public static final DeferredBlock<ThaumicBannerBlock> CRIMSON_BANNER=PortContent.BLOCKS.register("banner_crimson_cult",()->new ThaumicBannerBlock(DyeColor.RED,true,BlockBehaviour.Properties.of().strength(1).sound(SoundType.WOOD).noOcclusion().noCollission()));
    static {PortContent.ITEMS.registerItem("banner_crimson_cult",p->new ThaumicBannerItem(CRIMSON_BANNER.get(),p));}
    public static final Supplier<BlockEntityType<ThaumicBannerEntity>> BANNER_ENTITY=ENTITIES.register("banner",()->BlockEntityType.Builder.of(ThaumicBannerEntity::new,java.util.stream.Stream.concat(BANNERS.values().stream().map(Supplier::get),java.util.stream.Stream.of(CRIMSON_BANNER.get())).toArray(Block[]::new)).build(null));
    public static final Supplier<BlockEntityType<BarrierStoneEntity>> BARRIER_STONE_ENTITY=ENTITIES.register("barrier_stone",()->BlockEntityType.Builder.of(BarrierStoneEntity::new,PAVING_STONE_BARRIER.get()).build(null));
    private static DeferredBlock<PavingStoneBlock> paving(String name,boolean barrier){var block=PortContent.BLOCKS.register(name,()->new PavingStoneBlock(barrier,BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.STONE).noOcclusion()));PortContent.ITEMS.registerSimpleBlockItem(name,block);return block;}
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(CloudJumpPayload::register);}
    private InfusionBaubles(){}
}
