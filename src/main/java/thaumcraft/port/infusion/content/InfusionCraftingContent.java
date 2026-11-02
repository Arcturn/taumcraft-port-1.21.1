package thaumcraft.port.infusion.content;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.crafting.content.ArcaneLampBlock;
import thaumcraft.port.crafting.content.CraftingDeviceBlock;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.auromancy.item.FocusItem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.infusion.brain.BrainJarBlock;
import thaumcraft.port.infusion.brain.BrainJarItem;
import thaumcraft.port.artifice.construct.ConstructPlacerItem;
import thaumcraft.port.artifice.mirror.MirrorBlockItem;
import thaumcraft.port.artifice.mirror.HandMirrorItem;

/** Registry identities for the remaining BETA26 infusion outputs and their void-metal prerequisites. */
public final class InfusionCraftingContent {
    public static final Holder<ArmorMaterial> VOID = PortContent.ARMOR_MATERIALS.register("void", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,3,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.CHESTPLATE,8,
                    ArmorItem.Type.HELMET,3,ArmorItem.Type.BODY,8),10,SoundEvents.ARMOR_EQUIP_IRON,
                    InfusionCraftingContent::voidIngot,List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:void"))),1,0));
    public static final Holder<ArmorMaterial> VOID_ROBE = PortContent.ARMOR_MATERIALS.register("void_robe", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,4,ArmorItem.Type.LEGGINGS,7,ArmorItem.Type.CHESTPLATE,9,
                    ArmorItem.Type.HELMET,4,ArmorItem.Type.BODY,9),10,SoundEvents.ARMOR_EQUIP_LEATHER,
                    InfusionCraftingContent::voidIngot,List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:void_robe"),"",true),
                    new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:void_robe"),"_overlay",false)),2,0));
    public static final DeferredItem<VoidArmorItem> VOID_HELM = armor("void_helm",ArmorItem.Type.HELMET,false);
    public static final DeferredItem<VoidArmorItem> VOID_CHEST = armor("void_chest",ArmorItem.Type.CHESTPLATE,false);
    public static final DeferredItem<VoidArmorItem> VOID_LEGS = armor("void_legs",ArmorItem.Type.LEGGINGS,false);
    public static final DeferredItem<VoidArmorItem> VOID_ROBE_HELM = armor("void_robe_helm",ArmorItem.Type.HELMET,true);
    public static final DeferredItem<VoidArmorItem> VOID_ROBE_CHEST = armor("void_robe_chest",ArmorItem.Type.CHESTPLATE,true);
    public static final DeferredItem<VoidArmorItem> VOID_ROBE_LEGS = armor("void_robe_legs",ArmorItem.Type.LEGGINGS,true);
    public static final DeferredItem<VoidTools.Pickaxe> VOID_PICK = PortContent.ITEMS.registerItem("void_pick", p ->
            new VoidTools.Pickaxe(p.attributes(DiggerItem.createAttributes(VoidTools.VOID,1,-2.8f))));
    public static final DeferredItem<VoidTools.Shovel> VOID_SHOVEL = PortContent.ITEMS.registerItem("void_shovel", p ->
            new VoidTools.Shovel(p.attributes(DiggerItem.createAttributes(VoidTools.VOID,1.5f,-3))));
    public static final DeferredItem<VoidTools.Crusher> PRIMAL_CRUSHER = PortContent.ITEMS.registerItem("primal_crusher", p ->
            new VoidTools.Crusher(p.attributes(DiggerItem.createAttributes(VoidTools.PRIMAL,3.5f,-2.8f))
                    .component(DataComponents.CUSTOM_DATA,InfusionAugments.defaults(Map.of("destructive",1,"refining",1)))));
    public static final DeferredItem<VisAmuletItem> AMULET_VIS = PortContent.ITEMS.registerItem("amulet_vis", p ->
            new VisAmuletItem(p.rarity(Rarity.RARE)));
    public static final DeferredItem<VisAmuletItem> AMULET_VIS_FOUND = PortContent.ITEMS.registerItem("amulet_vis_found", p ->
            new VisAmuletItem(p.rarity(Rarity.UNCOMMON),true));
    public static final DeferredItem<VoidseerCharmItem> VOIDSEER_CHARM = PortContent.ITEMS.registerItem("voidseer_charm", p ->
            new VoidseerCharmItem(p.rarity(Rarity.RARE)));
    public static final DeferredBlock<BrainJarBlock> JAR_BRAIN = PortContent.BLOCKS.register("jar_brain",()->
            new BrainJarBlock(BlockBehaviour.Properties.of().sound(PortSounds.JAR).strength(.3f)));
    public static final DeferredBlock<CraftingDeviceBlock> VOID_SIPHON = device("void_siphon",SoundType.METAL,2.5f,
            Block.box(3,0,3,13,16,13));
    public static final DeferredBlock<InfusionMirrorBlock> MIRROR = mirror("mirror");
    public static final DeferredBlock<InfusionMirrorBlock> MIRROR_ESSENTIA = mirror("mirror_essentia");
    public static final DeferredBlock<ArcaneLampBlock> LAMP_GROWTH = lamp("lamp_growth");
    public static final DeferredBlock<ArcaneLampBlock> LAMP_FERTILITY = lamp("lamp_fertility");
    static {
        PortContent.ITEMS.registerItem("jar_brain",properties -> new BrainJarItem(JAR_BRAIN.get(),properties));
        for(String id:List.of("seal_harvest","seal_butcher"))PortContent.ITEMS.registerItem(id,p->new thaumcraft.port.golem.seal.SealPlacerItem(p,id.substring(5)));
        PortContent.ITEMS.registerItem("turret_placer_bore",properties -> new ConstructPlacerItem(properties,ConstructPlacerItem.Kind.BORE));
        PortContent.ITEMS.registerItem("focus_2",properties->new FocusItem(properties.stacksTo(1).rarity(Rarity.RARE),25));
        PortContent.ITEMS.registerItem("focus_3",properties->new FocusItem(properties.stacksTo(1).rarity(Rarity.RARE),50));
        PortContent.ITEMS.registerItem("hand_mirror",properties -> new HandMirrorItem(properties.stacksTo(1).rarity(Rarity.UNCOMMON)));
        PortContent.ITEMS.registerSimpleItem("causality_collapser",new Item.Properties().stacksTo(16));
    }
    static Ingredient voidIngot(){return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ingot_void")));}
    private static DeferredItem<VoidArmorItem> armor(String name,ArmorItem.Type type,boolean robe){
        return PortContent.ITEMS.registerItem(name,p -> new VoidArmorItem(type,robe,p.durability(type.getDurability(robe?18:10))
                .rarity(robe?Rarity.EPIC:Rarity.UNCOMMON)));
    }
    private static DeferredBlock<CraftingDeviceBlock> device(String name,SoundType sound,float hardness,VoxelShape shape){
        var block=PortContent.BLOCKS.register(name,()->new CraftingDeviceBlock(BlockBehaviour.Properties.of()
                .sound(sound).strength(hardness).noOcclusion(),shape));
        PortContent.ITEMS.registerSimpleBlockItem(name,block);return block;
    }
    private static DeferredBlock<InfusionMirrorBlock> mirror(String name){
        var block=PortContent.BLOCKS.register(name,()->new InfusionMirrorBlock(BlockBehaviour.Properties.of()
                .sound(PortSounds.JAR).strength(.1f).noOcclusion()));
        PortContent.ITEMS.registerItem(name,properties -> new MirrorBlockItem(block.get(),properties));return block;
    }
    private static DeferredBlock<ArcaneLampBlock> lamp(String name){
        var block=PortContent.BLOCKS.register(name,()->new ArcaneLampBlock(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL).strength(1).noOcclusion().lightLevel(state->state.getValue(ArcaneLampBlock.LIT)?15:0),
                name.equals("lamp_growth")?ArcaneLampBlock.Kind.GROWTH:ArcaneLampBlock.Kind.FERTILITY));
        PortContent.ITEMS.registerSimpleBlockItem(name,block);return block;
    }
    public static void register(IEventBus bus) { }
    private InfusionCraftingContent() { }
}
