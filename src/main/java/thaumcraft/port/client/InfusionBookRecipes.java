package thaumcraft.port.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.research.ResearchCatalog;

/** Use synchronized recipe costs/results while retaining TC6's illustrative dynamic central inputs. */
public final class InfusionBookRecipes {
    private static final JsonObject COVERAGE=ResearchCatalog.read("/thaumcraft-port/infusion-recipe-coverage.json");
    public static JsonObject live(String key,JsonObject original){
        var mc=Minecraft.getInstance();if(mc.level==null||!COVERAGE.has(key))return original;
        var ids=COVERAGE.getAsJsonArray(key);if(ids.size()!=1||ids.get(0).getAsString().startsWith("@"))return original;
        var holder=mc.level.getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:"+ids.get(0).getAsString())).orElse(null);
        if(holder==null||!(holder.value() instanceof InfusionRecipe recipe))return original;
        ItemStack central=original.has("input")?BookAssets.stack(original.getAsJsonObject("input").get("legacy").getAsString()):ItemStack.EMPTY;
        if(central.isEmpty())return original;
        if(original.has("runic_before"))InfusionAugments.setRunic(central,original.get("runic_before").getAsInt());
        var r=original.deepCopy();r.addProperty("live_infusion",true);var ingredients=new JsonArray();var stacks=new ArrayList<ItemStack>();
        for(var ingredient:recipe.components(central)){
            var options=new JsonArray();for(var stack:ingredient.getItems())options.add(legacy(stack));
            if(options.isEmpty())return original;
            ingredients.add(options.size()==1?options.get(0):options);stacks.add(ingredient.getItems()[0].copy());
        }
        r.add("ingredients",ingredients);var aspects=new JsonObject();recipe.aspects(null,central,stacks).forEach((aspect,amount)->aspects.addProperty(aspect.id(),amount));r.add("aspects",aspects);
        r.addProperty("instability",recipe.instability(null,central,stacks));r.add("input",legacy(central));
        var result=recipe.assemble(new InfusionInput(central,stacks),mc.level.registryAccess());if(!result.isEmpty())r.add("result",legacy(result));
        return r;
    }
    private static JsonObject legacy(ItemStack stack){var entry=new JsonObject();entry.addProperty("legacy",source(stack));return entry;}
    private static String source(ItemStack stack){
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),base=id+";"+stack.getCount()+";0";
        if(stack.getItem() instanceof CrystalItem)return base+";{Aspects:[{key:\""+CrystalItem.aspect(stack).id()+"\",amount:1}]}";
        var potion=stack.get(DataComponents.POTION_CONTENTS);
        if(potion!=null&&potion.potion().isPresent())return base+";{Potion:\""+potion.potion().get().unwrapKey().orElseThrow().location()+"\"}";
        var data=stack.get(DataComponents.CUSTOM_DATA);return data==null||data.isEmpty()?base:base+";"+data.copyTag();
    }
    private InfusionBookRecipes(){}
}
