package thaumcraft.port.artifice.furnace;

import java.util.*;
import java.util.function.DoubleSupplier;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import thaumcraft.port.infusion.recipe.InfusionFood;

/** ConfigRecipes' ordered smelting-bonus table. Every matching entry rolls independently. */
public final class InfernalFurnaceBonuses {
    private static final List<String> ORES=List.of("gold","iron","cinnabar","copper","tin","silver","lead","quartz");
    private static final List<String> CLUSTERS=List.of("iron","gold","cinnabar","copper","tin","silver","lead","quartz");
    private static final List<String> RARE=List.of("diamond","redstone","lapis","emerald","gold","iron","cinnabar","copper","tin","silver","lead","quartz");
    private static final float[] RARE_CHANCES={.025f,.01f,.01f,.025f,.02f,.01f,.025f,.01f,.01f,.02f,.01f,.01f};
    public static List<ItemStack> roll(ItemStack input,int bellows,DoubleSupplier random){
        var result=new ArrayList<ItemStack>();
        for(int pass=0;pass<=Math.min(4,Math.max(0,bellows));pass++){
            for(String metal:ORES)if(ore(input,metal))add(result,nugget(metal),.33f,random);
            for(String metal:CLUSTERS)if(input.is(item("cluster_"+metal)))add(result,nugget(metal),.33f,random);
            ItemStack meat=InfusionFood.smeltingBonus(input);if(!meat.isEmpty())add(result,meat.getItem(),.33f,random);
            for(int n=0;n<RARE.size();n++)if(ore(input,RARE.get(n)))add(result,item("rareearth"),RARE_CHANCES[n],random);
            if(CLUSTERS.stream().anyMatch(metal->input.is(item("cluster_"+metal))))add(result,item("rareearth"),.02f,random);
        }
        return result;
    }
    private static void add(List<ItemStack> result,Item item,float chance,DoubleSupplier random){if(random.getAsDouble()<=chance)result.add(new ItemStack(item));}
    private static Item nugget(String metal){return switch(metal){
        case "iron"->Items.IRON_NUGGET;case "gold"->Items.GOLD_NUGGET;
        case "cinnabar"->item("nugget_quicksilver");case "quartz"->item("nugget_quartz");
        default->InfernalFurnaceContent.NUGGETS.get(metal).get();
    };}
    private static boolean ore(ItemStack stack,String metal){
        if(stack.is(TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","ores/"+metal))))return true;
        // Vanilla's old ore-dictionary entries remain valid even with an overridden common tag.
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("minecraft")&&(id.getPath().equals(metal+"_ore")
                ||id.getPath().equals("deepslate_"+metal+"_ore")||metal.equals("quartz")&&id.getPath().equals("nether_quartz_ore"));
    }
    private static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("thaumcraft",id));}
    private InfernalFurnaceBonuses(){}
}
