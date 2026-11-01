package thaumcraft.port.infusion.tools;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.infusion.recipe.InfusionAugments;

/** BETA26 ELEMENTALTOOLS and BOOTSTRAVELLER, including the recipe's inherent infusions. */
public final class InfusionTools {
    public static final Tier ELEMENTAL = new Tier() {
        public int getUses() { return 1500; }
        public float getSpeed() { return 9; }
        public float getAttackDamageBonus() { return 3; }
        public int getEnchantmentValue() { return 18; }
        public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        public Ingredient getRepairIngredient() {
            return Ingredient.of(TagKey.create(Registries.ITEM, ResourceLocation.parse("c:ingots/thaumium")));
        }
    };
    public static final DeferredItem<ElementalAxeItem> AXE = PortContent.ITEMS.registerItem("elemental_axe", p ->
            new ElementalAxeItem(p.attributes(DiggerItem.createAttributes(ELEMENTAL,5,-3))
                    .rarity(Rarity.RARE).component(DataComponents.CUSTOM_DATA,InfusionAugments.defaults(Map.of("burrowing",1,"collector",1)))));
    public static final DeferredItem<ElementalPickItem> PICK = PortContent.ITEMS.registerItem("elemental_pick", p ->
            new ElementalPickItem(p.attributes(DiggerItem.createAttributes(ELEMENTAL,1,-2.8f))
                    .rarity(Rarity.RARE).component(DataComponents.CUSTOM_DATA,InfusionAugments.defaults(Map.of("refining",1,"sounding",2)))));
    public static final DeferredItem<ElementalShovelItem> SHOVEL = PortContent.ITEMS.registerItem("elemental_shovel", p ->
            new ElementalShovelItem(p.attributes(DiggerItem.createAttributes(ELEMENTAL,1.5f,-3))
                    .rarity(Rarity.RARE).component(DataComponents.CUSTOM_DATA,InfusionAugments.defaults(Map.of("destructive",1)))));
    public static final DeferredItem<ElementalHoeItem> HOE = PortContent.ITEMS.registerItem("elemental_hoe", p ->
            new ElementalHoeItem(p.attributes(DiggerItem.createAttributes(ELEMENTAL,-3,0)).rarity(Rarity.RARE)));
    public static final DeferredItem<ElementalSwordItem> SWORD = PortContent.ITEMS.registerItem("elemental_sword", p ->
            new ElementalSwordItem(p.attributes(SwordItem.createAttributes(ELEMENTAL,3,-2.4f))
                    .rarity(Rarity.RARE).component(DataComponents.CUSTOM_DATA,InfusionAugments.defaults(Map.of("arcing",2)))));
    public static final Holder<ArmorMaterial> TRAVELLER_MATERIAL = PortContent.ARMOR_MATERIALS.register("traveller", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,1,ArmorItem.Type.LEGGINGS,2,ArmorItem.Type.CHESTPLATE,3,
                    ArmorItem.Type.HELMET,1,ArmorItem.Type.BODY,3),25,SoundEvents.ARMOR_EQUIP_LEATHER,
                    () -> Ingredient.of(Items.LEATHER),List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:traveller"))),1,0));
    public static final DeferredItem<TravellerBootsItem> BOOTS = PortContent.ITEMS.registerItem("traveller_boots", p ->
            new TravellerBootsItem(p.durability(350).rarity(Rarity.RARE)));

    public static void register(IEventBus bus) {
        bus.addListener(ElementalToolPayload::register);
        NeoForge.EVENT_BUS.register(TravellerBootsItem.class);
    }
    private InfusionTools() {}
}
