package thaumcraft.port.boss;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.infusion.armor.VisDiscountGear;
import thaumcraft.port.infusion.content.*;

/** Original equipment required by the praetor and the highest-quality treasure rolls. */
public final class BossGear {
    public static final Holder<ArmorMaterial> PRAETOR=PortContent.ARMOR_MATERIALS.register("cultist_leader",()->new ArmorMaterial(
            Map.of(ArmorItem.Type.BOOTS,3,ArmorItem.Type.LEGGINGS,6,ArmorItem.Type.CHESTPLATE,7,ArmorItem.Type.HELMET,3,ArmorItem.Type.BODY,7),20,
            SoundEvents.ARMOR_EQUIP_IRON,()->Ingredient.of(Items.IRON_INGOT),List.of(new ArmorMaterial.Layer(ResourceLocation.parse("thaumcraft:cultist_leader"))),1,0));
    public static final Tier CRIMSON=new Tier(){
        public int getUses(){return 200;}public float getSpeed(){return 8;}public float getAttackDamageBonus(){return 3.5f;}public int getEnchantmentValue(){return 20;}
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops(){return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;}
        public Ingredient getRepairIngredient(){return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:ingot_void")));}
    };
    public static final DeferredItem<PraetorArmorItem> PRAETOR_HELM=praetor("crimson_praetor_helm",ArmorItem.Type.HELMET),PRAETOR_CHEST=praetor("crimson_praetor_chest",ArmorItem.Type.CHESTPLATE),PRAETOR_LEGS=praetor("crimson_praetor_legs",ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<CrimsonBootsItem> CRIMSON_BOOTS=PortContent.ITEMS.registerItem("crimson_boots",p->new CrimsonBootsItem(p.durability(ArmorItem.Type.BOOTS.getDurability(15)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BladeItem> CRIMSON_BLADE=PortContent.ITEMS.registerItem("crimson_blade",p->new BladeItem(true,p.attributes(SwordItem.createAttributes(CRIMSON,3,-2.4f)).rarity(Rarity.EPIC)));
    public static final DeferredItem<BladeItem> VOID_SWORD=PortContent.ITEMS.registerItem("void_sword",p->new BladeItem(false,p.attributes(SwordItem.createAttributes(VoidTools.VOID,3,-2.4f)).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<VoidArmorItem> VOID_BOOTS=PortContent.ITEMS.registerItem("void_boots",p->new VoidArmorItem(ArmorItem.Type.BOOTS,false,p.durability(ArmorItem.Type.BOOTS.getDurability(10)).rarity(Rarity.UNCOMMON)));
    private static DeferredItem<PraetorArmorItem> praetor(String name,ArmorItem.Type type){return PortContent.ITEMS.registerItem(name,p->new PraetorArmorItem(type,p.durability(type.getDurability(30)).rarity(Rarity.RARE)));}
    public static void register(){}
    public static final class PraetorArmorItem extends ArmorItem {
        public PraetorArmorItem(Type type,Properties properties){super(PRAETOR,type,properties);}
        @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return ResourceLocation.parse("thaumcraft:textures/entity/armor/cultist_leader_armor.png");}
    }
    public static final class CrimsonBootsItem extends ArmorItem implements WarpingGear,VisDiscountGear {
        public CrimsonBootsItem(Properties properties){super(ArmorMaterials.IRON,Type.BOOTS,properties);}
        @Override public int gearWarp(ItemStack stack,Player player){return 1;}
        @Override public int visDiscount(ItemStack stack,Player player){return 1;}
        @Override public ResourceLocation getArmorTexture(ItemStack stack,Entity entity,EquipmentSlot slot,ArmorMaterial.Layer layer,boolean inner){return ResourceLocation.parse("thaumcraft:textures/entity/armor/cultistboots.png");}
    }
    public static final class BladeItem extends SwordItem implements WarpingGear {
        private final boolean crimson;
        public BladeItem(boolean crimson,Properties properties){super(crimson?CRIMSON:VoidTools.VOID,properties);this.crimson=crimson;}
        @Override public int gearWarp(ItemStack stack,Player player){return crimson?2:1;}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
            super.inventoryTick(stack,level,entity,slot,selected);
            if(!level.isClientSide&&entity instanceof LivingEntity&&entity.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);
        }
        @Override public boolean hurtEnemy(ItemStack stack,LivingEntity victim,LivingEntity attacker){
            if(!victim.level().isClientSide&&(!(victim instanceof Player target)||!(attacker instanceof Player player)||player.canHarmPlayer(target))){
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,crimson?60:80));
                if(crimson)victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,120));
            }
            return super.hurtEnemy(stack,victim,attacker);
        }
    }
    private BossGear(){}
}
