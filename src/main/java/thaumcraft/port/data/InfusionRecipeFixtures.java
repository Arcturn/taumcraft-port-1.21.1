package thaumcraft.port.data;

import com.google.gson.JsonElement;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.crafting.IngredientMatching;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.research.*;

/** All 47 Infusion book references must resolve to real recipes or a functional altar blueprint. */
public final class InfusionRecipeFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception {
        var coverage=ResearchCatalog.read("/thaumcraft-port/infusion-recipe-coverage.json");
        var references=new HashSet<String>();
        ResearchCatalog.all().stream().filter(e->e.category().equals("INFUSION")).forEach(e->e.stages().forEach(s->references.addAll(ResearchCatalog.strings(s,"recipes"))));
        require(references.equals(coverage.keySet()),"Every Infusion recipe reference needs a functional route");int checks=1;
        for(var requirement:ResearchRequirementAudit.collect()){
            var entry=ResearchCatalog.get(requirement.research());
            if(entry!=null&&(entry.category().equals("INFUSION")||entry.key().equals("UNLOCKINFUSION"))){
                require(requirement.supported(),"Unregistered Infusion stage requirement: "+requirement.source());checks++;
            }
        }
        var paths=new LinkedHashSet<String>();
        for(var entry:coverage.entrySet()){
            require(!BookRecipeCatalog.recipes(entry.getKey()).isEmpty(),"Missing Infusion book recipe: "+entry.getKey());checks++;
            for(var path:entry.getValue().getAsJsonArray()){
                if(path.getAsString().startsWith("@")){
                    String material=path.getAsString().endsWith("ancient")?"ancient":path.getAsString().endsWith("eldritch")?"eldritch":"arcane";
                    require(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("thaumcraft:infusion_matrix"))&&BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("thaumcraft:pillar_"+material))&&BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("thaumcraft:pedestal_"+material)),"Missing functional altar material: "+material);checks++;
                }else paths.add(path.getAsString());
            }
        }
        paths.addAll(List.of("enchanted_fabric","cloth_chest","cloth_legs","cloth_boots","recharge_pedestal","slab_arcane_stone","slab_arcane_brick","slab_ancient","slab_eldritch","triple_meat_treat"));
        for(String path:paths){
            Recipe<?> recipe=AlchemyRecipeFixtures.recipe(resources,ops,path);
            if(recipe instanceof InfusionRecipe infusion){
                ItemStack central=sampleCentral(infusion);List<ItemStack> components=sampleComponents(infusion,central);
                require(!central.isEmpty(),"Unobtainable central input: "+path);
                require(infusion.matches(new InfusionInput(central,components),null),"Infusion input rejected: "+path);
                var reversed=new ArrayList<>(components);Collections.reverse(reversed);
                require(infusion.matches(new InfusionInput(central,reversed),null),"Pedestal matching depends on order: "+path);
                reversed.removeLast();require(!infusion.matches(new InfusionInput(central,reversed),null),"Missing pedestal accepted: "+path);
                reversed.addAll(List.of(new ItemStack(Items.DIRT),new ItemStack(Items.DIRT)));require(!infusion.matches(new InfusionInput(central,reversed),null),"Extra pedestal accepted: "+path);
                require(!new ResearchState().knows(infusion.research()),"Infusion research bypass: "+path);
                require(!infusion.aspects(null,central,components).isEmpty(),"Free infusion: "+path);
                ItemStack result=infusion.assemble(new InfusionInput(central,components),registries);
                require(!result.isEmpty()&&!(result.getItem() instanceof thaumcraft.port.content.ResearchDisplayItem),"Display-only infusion result: "+path);
                if(infusion.dynamic()){
                    central.set(DataComponents.CUSTOM_NAME,Component.literal("preserved central item"));
                    if(central.isDamageableItem())central.setDamageValue(11);
                    InfusionAugments.setWarp(central,2);InfusionAugments.setRunic(central,3);
                    result=infusion.assemble(new InfusionInput(central,sampleComponents(infusion,central)),registries);
                    require(result.getHoverName().equals(central.getHoverName())&&result.getDamageValue()==central.getDamageValue()&&InfusionAugments.getWarp(result)==2,"Augment discards central components: "+path);
                    require(InfusionAugments.getRunic(result)==(infusion.operation().equals("runic")?4:3),"Augment discards runic shielding: "+path);
                }
                checks+=9;
            }else{
                require(!recipe.getResultItem(registries).isEmpty(),"Empty Infusion support result: "+path);
                if(recipe instanceof ArcaneRecipe arcane)require(arcane.matches(CraftingInput.of(arcane.pattern().width(),arcane.pattern().height(),grid(arcane.pattern().ingredients())),null),"Uncraftable arcane support recipe: "+path);
                if(recipe instanceof ShapedRecipe shaped)require(shaped.matches(CraftingInput.of(shaped.getWidth(),shaped.getHeight(),grid(shaped.getIngredients())),null),"Uncraftable support recipe: "+path);
                checks++;
            }
        }
        var runic=(InfusionRecipe)AlchemyRecipeFixtures.recipe(resources,ops,"runic_armor");
        var armor=new ItemStack(Items.IRON_HELMET);
        for(int level=0;level<5;level++){
            InfusionAugments.setRunic(armor,level);int vis=20+(20<<level);
            require(runic.aspects(null,armor,List.of()).equals(Map.of(Aspect.PROTECT,vis,Aspect.CRYSTAL,vis/2,Aspect.ENERGY,vis/2))&&runic.instability(null,armor,List.of())==5+level/2&&runic.components(armor).size()==level+2,"Original runic scaling at level "+level);checks++;
        }
        var refining=(InfusionRecipe)AlchemyRecipeFixtures.recipe(resources,ops,"ie_refining");
        var pick=new ItemStack(Items.IRON_PICKAXE);InfusionAugments.setEnchantment(pick,"refining",1);InfusionAugments.setEnchantment(pick,"sounding",1);
        require(refining.aspects(null,pick,List.of()).equals(Map.of(Aspect.ORDER,186,Aspect.EXCHANGE,139)),"Infusion enchantment level plus other-enchantment surcharge");checks++;
        var axe=(InfusionRecipe)AlchemyRecipeFixtures.recipe(resources,ops,"elemental_axe");
        require(axe.components().getFirst().test(CrystalItem.stack(Aspect.WATER))&&!axe.components().getFirst().test(CrystalItem.stack(Aspect.FIRE)),"Elemental tools require the original crystal aspect");checks++;
        var boots=(InfusionRecipe)AlchemyRecipeFixtures.recipe(resources,ops,"traveller_boots");
        for(var fish:List.of(Items.COD,Items.SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH)){require(boots.components().getLast().test(new ItemStack(fish)),"Traveller boots accept every original fish metadata variant");checks++;}
        var matrix=(ArcaneRecipe)AlchemyRecipeFixtures.recipe(resources,ops,"infusion_matrix");
        require(matrix.research().equals("INFUSION@2")&&matrix.vis()==150&&matrix.crystals().size()==6,"Matrix stage gate and original arcane costs");checks++;
        return checks;
    }
    public static ItemStack sampleCentral(InfusionRecipe recipe){
        if(!recipe.input().isEmpty())return first(recipe.input());
        if(recipe.operation().equals("runic"))return new ItemStack(Items.IRON_HELMET);
        return new ItemStack(Set.of("arcing","essence").contains(recipe.parameter())?Items.IRON_SWORD:Items.IRON_PICKAXE);
    }
    public static List<ItemStack> sampleComponents(InfusionRecipe recipe,ItemStack central){return recipe.components(central).stream().map(InfusionRecipeFixtures::first).toList();}
    private static ItemStack first(Ingredient ingredient){var items=IngredientMatching.getItems(ingredient);require(items.length>0,"Empty ingredient: "+ingredient);return items[0].copy();}
    private static List<ItemStack> grid(List<Ingredient> ingredients){return ingredients.stream().map(i->i.isEmpty()?ItemStack.EMPTY:first(i)).toList();}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    private InfusionRecipeFixtures(){}
}
