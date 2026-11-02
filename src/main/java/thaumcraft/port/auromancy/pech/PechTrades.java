package thaumcraft.port.auromancy.pech;

import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.CrystalItem;

/** ConfigEntities' actual BETA26 pools, including duplicate weighting and cross-pool additions. */
public final class PechTrades {
    public record Entry(int tier,ItemStack stack){}
    public static int value(ItemStack item,Level level){if(item.isEmpty())return 0;if(item.is(Items.ENDER_PEARL))return 15;return Math.min(32,RecipeAspects.get(item,level).getOrDefault(Aspect.DESIRE,0)/2);}
    public static boolean valued(ItemStack item,Level level){return !item.isEmpty()&&(item.is(Items.ENDER_PEARL)||RecipeAspects.get(item,level).getOrDefault(Aspect.DESIRE,0)>1);}
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static void add(List<Entry> pool,int tier,String... ids){for(String id:ids)pool.add(new Entry(tier,item(id.contains(":")?id:"thaumcraft:"+id)));}
    private static ItemStack book(Level level,ResourceKey<Enchantment> enchantment){var book=new ItemStack(Items.ENCHANTED_BOOK);book.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment),1);return book;}
    public static List<Entry> entries(Level level,int profession){
        var pool=new ArrayList<Entry>();
        if(profession==0){
            add(pool,1,"cluster_iron","cluster_gold","cluster_cinnabar","cluster_quartz");
            for(String metal:List.of("copper","tin","silver","lead"))if(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM,ResourceLocation.parse("c:ingots/"+metal))).map(tag->tag.size()>0).orElse(false))add(pool,1,"cluster_"+metal);
            add(pool,2,"minecraft:blaze_rod","sapling_greatwood","minecraft:dragon_breath");
            add(pool,3,"minecraft:experience_bottle","minecraft:experience_bottle","minecraft:golden_apple");
            add(pool,4,"thaumium_pick","thaumium_axe","thaumium_hoe");
            add(pool,5,"minecraft:enchanted_golden_apple","sapling_silverwood","curio_knowledge");
            // These three lines in ConfigEntities append to forInv even while defining other professions.
            add(pool,5,"minecraft:totem_of_undying");add(pool,2,"minecraft:compass");add(pool,4,"minecraft:spectral_arrow");
        } else if(profession==1){
            for(Aspect aspect:List.of(Aspect.AIR,Aspect.EARTH,Aspect.FIRE,Aspect.WATER,Aspect.ORDER,Aspect.ENTROPY))pool.add(new Entry(1,CrystalItem.stack(aspect)));
            // Original metadata-only potion stacks have no Potion NBT in 1.12.2: retain the two uncraftable-potion entries.
            add(pool,2,"minecraft:potion","minecraft:potion");pool.add(new Entry(2,CrystalItem.stack(Aspect.FLUX)));
            add(pool,3,"minecraft:experience_bottle","minecraft:experience_bottle");pool.add(new Entry(3,CrystalItem.stack(Aspect.AURA)));add(pool,3,"minecraft:golden_apple");
            add(pool,4,"cloth_boots","cloth_chest","cloth_legs");
            add(pool,5,"minecraft:enchanted_golden_apple","pech_wand","curio_knowledge","amulet_vis_found");
        } else {
            for(int dye=0;dye<15;dye++)add(pool,1,"candle_"+DyeColor.byId(15-dye).getName());
            add(pool,2,"minecraft:ghast_tear");pool.add(new Entry(2,book(level,Enchantments.POWER)));
            add(pool,3,"minecraft:experience_bottle","minecraft:experience_bottle","minecraft:golden_apple");
            // ItemsTC.eldritchEye is never assigned in BETA26 ConfigItems; its tier-four stack is empty.
            pool.add(new Entry(4,ItemStack.EMPTY));add(pool,4,"minecraft:enchanted_golden_apple");
            add(pool,5,"ring_apprentice");pool.add(new Entry(5,book(level,Enchantments.FLAME)));pool.add(new Entry(5,book(level,Enchantments.INFINITY)));add(pool,5,"curio_knowledge");
        }
        return List.copyOf(pool);
    }
    private PechTrades(){}
}
