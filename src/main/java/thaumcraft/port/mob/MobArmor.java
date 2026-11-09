package thaumcraft.port.mob;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;

/** Real, damageable crimson plate required by the inhabited zombie's original equipment. */
public final class MobArmor {
    public static final Holder<ArmorMaterial> PLATE=PortContent.ARMOR_MATERIALS.register("cultist_plate",()->new ArmorMaterial(
            Map.of(ArmorItem.Type.BOOTS,2,ArmorItem.Type.LEGGINGS,5,ArmorItem.Type.CHESTPLATE,6,ArmorItem.Type.HELMET,2,ArmorItem.Type.BODY,6),13,
            SoundEvents.ARMOR_EQUIP_IRON,()->Ingredient.of(Items.IRON_INGOT),List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:cultist_plate"))),0,0));
    public static final DeferredItem<PlateArmorItem> HELM=plate("crimson_plate_helm",ArmorItem.Type.HELMET),CHEST=plate("crimson_plate_chest",ArmorItem.Type.CHESTPLATE),LEGS=plate("crimson_plate_legs",ArmorItem.Type.LEGGINGS);
    private static DeferredItem<PlateArmorItem> plate(String name,ArmorItem.Type type){return PortContent.ITEMS.registerItem(name,p->new PlateArmorItem(type,p.durability(type.getDurability(18)).rarity(Rarity.UNCOMMON)));}
    public static void register(){}
    public static final class PlateArmorItem extends ArmorItem {
        public PlateArmorItem(Type type,Properties properties){super(PLATE,type,properties);}
        @Override public String getDescriptionId(){return "item.crimson_plate_"+switch(type){case HELMET->"helm";case CHESTPLATE->"chest";default->"legs";}+".name";}
        @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return ResourceLocation.parse("thaumcraft:textures/entity/armor/"+(entity instanceof InhabitedZombie?"zombie":"cultist")+"_plate_armor.png");}
    }
    private MobArmor(){}
}
