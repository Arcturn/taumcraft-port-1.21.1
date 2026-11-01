package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.crafting.*;
import thaumcraft.port.essentia.*;
import thaumcraft.port.research.*;

/** Loads actual server recipes for every Alchemy page, then exercises real matching and outputs. */
public final class AlchemyRecipeFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception {
        var coverage=ResearchCatalog.read("/thaumcraft-port/alchemy-recipe-coverage.json");
        var references=new HashSet<String>();
        ResearchCatalog.all().stream().filter(e->e.category().equals("ALCHEMY")).forEach(e->e.stages().forEach(s->references.addAll(ResearchCatalog.strings(s,"recipes"))));
        require(references.equals(coverage.keySet()),"Every Alchemy recipe reference needs an explicit functional route");
        int checks=1;
        for(var requirement:ResearchRequirementAudit.collect()) {
            var research=ResearchCatalog.get(requirement.research());
            if(research!=null&&research.category().equals("ALCHEMY")) {
                require(requirement.supported(),"Unregistered Alchemy stage requirement: "+requirement.source());checks++;
            }
        }
        for(var entry:coverage.entrySet()) {
            require(!BookRecipeCatalog.recipes(entry.getKey()).isEmpty(),"Missing Alchemy book recipe "+entry.getKey());
            for(var path:entry.getValue().getAsJsonArray()) {
                String id=path.getAsString();
                if(id.equals("@salis_mundus_ritual")) {
                    require(EssentiaContent.THAUMATORIUM.get().newBlockEntity(net.minecraft.core.BlockPos.ZERO,EssentiaContent.THAUMATORIUM.get().defaultBlockState()) instanceof ThaumatoriumEntity,"Thaumatorium ritual must form a real machine");checks++;continue;
                }
                var recipe=recipe(resources,ops,id);
                if(recipe instanceof LabelRecipe label) {
                    var input=CraftingInput.of(2,1,List.of(new ItemStack(EssentiaContent.LABEL.get()),EssentiaItem.phial(Aspect.ALCHEMY)));
                    require(label.matches(input,null)&&EssentiaItem.aspect(label.assemble(input,registries))==Aspect.ALCHEMY,"Aspect label route");checks++;continue;
                }
                require(!recipe.getResultItem(registries).isEmpty(),"Actual Alchemy recipe has empty output: "+id);
                require(!(recipe.getResultItem(registries).getItem() instanceof thaumcraft.port.content.ResearchDisplayItem),"Display-only Alchemy output: "+id);
                if(recipe instanceof CrucibleRecipe crucible) {
                    require(!crucible.aspects().isEmpty(),"Alchemy cannot create free items: "+id);
                    var inputs=IngredientMatching.getItems(crucible.catalyst());
                    boolean optional=id.matches("crucible_metal_purification_(tin|silver|lead)");
                    require(optional||inputs.length>0,"Unobtainable catalyst: "+id);
                    if(inputs.length>0)require(crucible.matches(new SingleRecipeInput(inputs[0]),null),"Crucible catalyst rejected "+id);
                    require(!new ResearchState().knows(crucible.research()),"Research gate bypass: "+id);
                } else if(recipe instanceof ArcaneRecipe arcane) {
                    var grid=grid(arcane.pattern().ingredients());
                    require(arcane.matches(CraftingInput.of(arcane.pattern().width(),arcane.pattern().height(),grid),null),"Arcane grid cannot craft "+id);
                } else if(recipe instanceof ShapedRecipe shaped) {
                    require(shaped.matches(CraftingInput.of(shaped.getWidth(),shaped.getHeight(),grid(shaped.getIngredients())),null),"Crafting grid cannot craft "+id);
                } else if(recipe instanceof ShapelessRecipe shapeless) {
                    var grid=grid(shapeless.getIngredients());require(shapeless.matches(CraftingInput.of(grid.size(),1,grid),null),"Shapeless inputs rejected "+id);
                }
                checks++;
            }
        }
        var bottle=(CrucibleRecipe)recipe(resources,ops,"bottle_taint");
        require(bottle.catalyst().test(EssentiaItem.phial(Aspect.FLUX))&&!bottle.catalyst().test(EssentiaItem.phial(null))&&!bottle.catalyst().test(EssentiaItem.phial(Aspect.WATER)),"Taint bottle must consume specifically a full Vitium phial");checks++;
        for(Aspect a:Aspect.values()) {
            var crystal=(CrucibleRecipe)recipe(resources,ops,"vis_crystal_"+a.id());
            require(crystal.aspects().equals(Map.of(a.id(),2))&&CrystalItem.aspect(crystal.result())==a,"Crystal recipe loses aspect or conversion cost: "+a);checks++;
        }
        var morphic=(ArcaneRecipe)recipe(resources,ops,"morphic_resonator");
        require(morphic.pattern().ingredients().get(4).test(new ItemStack(thaumcraft.port.alchemy.material.AlchemyMaterials.RARE_EARTH.get()))&&!morphic.pattern().ingredients().get(4).test(new ItemStack(Items.QUARTZ)),"Morphic resonator requires Rare Earths");checks++;
        return checks;
    }
    private static List<ItemStack> grid(List<Ingredient> ingredients) {return ingredients.stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList();}
    public static Recipe<?> recipe(ResourceManager resources,RegistryOps<JsonElement> ops,String name)throws Exception {
        try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:recipe/"+name+".json")).openAsReader()){return Recipe.CODEC.parse(ops,JsonParser.parseReader(in)).getOrThrow();}
    }
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    private AlchemyRecipeFixtures(){}
}
