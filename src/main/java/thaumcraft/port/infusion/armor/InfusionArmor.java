package thaumcraft.port.infusion.armor;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;

/** Original SPECIAL cloth and FORTRESS armor materials and infusion equipment. */
public final class InfusionArmor {
    public static final Holder<ArmorMaterial> CLOTH = PortContent.ARMOR_MATERIALS.register("cloth", () ->
        new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,1,ArmorItem.Type.LEGGINGS,2,ArmorItem.Type.CHESTPLATE,3,ArmorItem.Type.HELMET,1,ArmorItem.Type.BODY,3),25,
        SoundEvents.ARMOR_EQUIP_LEATHER,()->Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:fabric"))),
        List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:robes"),"",true),new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:robes"),"_overlay",false)),1,0));
    public static final Holder<ArmorMaterial> FORTRESS = PortContent.ARMOR_MATERIALS.register("fortress", () ->
        new ArmorMaterial(Map.of(ArmorItem.Type.BOOTS,3,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.CHESTPLATE,7,ArmorItem.Type.HELMET,3,ArmorItem.Type.BODY,7),25,
        SoundEvents.ARMOR_EQUIP_IRON,()->Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ingot_thaumium"))),
        List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:fortress"))),3,0));
    public static final DeferredItem<RobeArmorItem> CLOTH_CHEST=robe("cloth_chest",ArmorItem.Type.CHESTPLATE), CLOTH_LEGS=robe("cloth_legs",ArmorItem.Type.LEGGINGS), CLOTH_BOOTS=robe("cloth_boots",ArmorItem.Type.BOOTS);
    public static final DeferredItem<FortressArmorItem> FORTRESS_HELM=fortress("fortress_helm",ArmorItem.Type.HELMET), FORTRESS_CHEST=fortress("fortress_chest",ArmorItem.Type.CHESTPLATE), FORTRESS_LEGS=fortress("fortress_legs",ArmorItem.Type.LEGGINGS);
    private static DeferredItem<RobeArmorItem> robe(String id,ArmorItem.Type type){return PortContent.ITEMS.registerItem(id,p->new RobeArmorItem(type,p.durability(type.getDurability(25)).rarity(Rarity.UNCOMMON)));}
    private static DeferredItem<FortressArmorItem> fortress(String id,ArmorItem.Type type){return PortContent.ITEMS.registerItem(id,p->new FortressArmorItem(type,p.durability(type.getDurability(40)).rarity(Rarity.RARE)));}
    public static void register(IEventBus bus){InfusionEquipmentEvents.register();InfusionEnchantments.register(bus);InfusionFxPayload.register(bus);}
    private InfusionArmor(){}
}
