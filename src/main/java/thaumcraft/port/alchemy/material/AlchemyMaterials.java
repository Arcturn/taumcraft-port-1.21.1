package thaumcraft.port.alchemy.material;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;

/** The original metallurgy items and the ordinary ore-harvest source of Rare Earths. */
public final class AlchemyMaterials {
    public static final DeferredItem<Item> COMPLEX_MECHANISM = PortContent.ITEMS.registerSimpleItem("mechanism_complex");
    public static final DeferredItem<Item> RARE_EARTH = PortContent.ITEMS.registerSimpleItem("rareearth");
    public static final DeferredBlock<Block> FLESH = PortContent.BLOCKS.register("flesh_block", () ->
            new Block(BlockBehaviour.Properties.of().strength(.25f,2).sound(thaumcraft.port.content.PortSounds.GORE)));
    public static final Tier THAUMIUM = new Tier() {
        public int getUses() { return 500; }
        public float getSpeed() { return 7; }
        public float getAttackDamageBonus() { return 2.5f; }
        public int getEnchantmentValue() { return 22; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        public Ingredient getRepairIngredient() { return ingots(); }
    };
    public static final Holder<ArmorMaterial> ARMOR = PortContent.ARMOR_MATERIALS.register("thaumium", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,2,ArmorItem.Type.LEGGINGS,5,
                    ArmorItem.Type.CHESTPLATE,6,ArmorItem.Type.HELMET,2,ArmorItem.Type.BODY,6),
                    25, SoundEvents.ARMOR_EQUIP_IRON, AlchemyMaterials::ingots,
                    List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:thaumium"))),1,0));
    public static final java.util.Map<String,DeferredBlock<TallowCandleBlock>> CANDLES = new java.util.LinkedHashMap<>();
    static {
        for (DyeColor color : DyeColor.values()) {
            String id="candle_"+color.getName();
            var block=PortContent.BLOCKS.register(id,()->new TallowCandleBlock(BlockBehaviour.Properties.of().mapColor(color)));
            CANDLES.put(id,block);PortContent.ITEMS.registerSimpleBlockItem(block);
        }
        PortContent.ITEMS.registerSimpleBlockItem(FLESH);
        for (String metal : List.of("tin", "silver", "lead", "quartz")) PortContent.ITEMS.registerSimpleItem("cluster_"+metal);
        PortContent.ITEMS.registerItem("thaumium_sword", p -> new SwordItem(THAUMIUM,p.attributes(SwordItem.createAttributes(THAUMIUM,3,-2.4f))));
        PortContent.ITEMS.registerItem("thaumium_pick", p -> new PickaxeItem(THAUMIUM,p.attributes(DiggerItem.createAttributes(THAUMIUM,1,-2.8f))));
        PortContent.ITEMS.registerItem("thaumium_shovel", p -> new ShovelItem(THAUMIUM,p.attributes(DiggerItem.createAttributes(THAUMIUM,1.5f,-3))));
        // TC6's axe constructor sets its damage to 8, irrespective of material bonus.
        PortContent.ITEMS.registerItem("thaumium_axe", p -> new AxeItem(THAUMIUM,p.attributes(DiggerItem.createAttributes(THAUMIUM,5.5f,-3))));
        PortContent.ITEMS.registerItem("thaumium_hoe", p -> new HoeItem(THAUMIUM,p.attributes(DiggerItem.createAttributes(THAUMIUM,-2.5f,-.5f))));
        armor("thaumium_helm",ArmorItem.Type.HELMET);
        armor("thaumium_chest",ArmorItem.Type.CHESTPLATE);
        armor("thaumium_legs",ArmorItem.Type.LEGGINGS);
        armor("thaumium_boots",ArmorItem.Type.BOOTS);
    }
    private static Ingredient ingots() { return Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ingots/thaumium"))); }
    private static void armor(String id, ArmorItem.Type type) {
        PortContent.ITEMS.registerItem(id,p -> new ArmorItem(ARMOR,type,p.durability(type.getDurability(25)).rarity(Rarity.UNCOMMON)));
    }
    public static void register(IEventBus bus) { ClusterCookingRecipe.register(); NeoForge.EVENT_BUS.addListener(AlchemyMaterials::drops); }
    public static float rareEarthChance(Block block) {
        String id=BuiltInRegistries.BLOCK.getKey(block).toString();
        return switch(id) {
            case "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "thaumcraft:ore_amber", "thaumcraft:deepslate_ore_amber", "thaumcraft:ore_quartz", "thaumcraft:deepslate_ore_quartz" -> .05f;
            case "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore" -> .075f;
            case "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore", "minecraft:nether_quartz_ore" -> .01f;
            case "minecraft:coal_ore", "minecraft:deepslate_coal_ore" -> .001f;
            default -> 0;
        };
    }
    private static void drops(BlockDropsEvent event) {
        float chance=rareEarthChance(event.getState().getBlock());
        if(chance<=0 || EnchantmentHelper.getItemEnchantmentLevel(event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),event.getTool())>0) return;
        if(event.getLevel().random.nextFloat()<chance) {
            var p=event.getPos();
            event.getDrops().add(new ItemEntity(event.getLevel(),p.getX()+.5,p.getY()+.5,p.getZ()+.5,new ItemStack(RARE_EARTH.get())));
        }
    }
    private AlchemyMaterials() {}
}
