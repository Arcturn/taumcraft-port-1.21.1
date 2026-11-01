package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.ResearchDisplayItem;
import thaumcraft.port.crafting.*;
import thaumcraft.port.essentia.EssentiaItem;
import thaumcraft.port.research.ResearchCatalog;

/** Independent expectations extracted from the unmodified TC6 BETA26 runtime and conditional ore source. */
public final class CraftingRecipeFixtures {
    public static JsonObject coverage() { return ResearchCatalog.read("/thaumcraft-port/crafting-recipe-coverage.json"); }
    public static List<JsonObject> entries(String kind) {
        return coverage().getAsJsonArray(kind).asList().stream().map(JsonElement::getAsJsonObject).toList();
    }
    public static ItemStack stack(JsonObject expected,RegistryOps<JsonElement> ops) {
        return ItemStack.STRICT_CODEC.parse(ops,expected).getOrThrow();
    }
    public static List<ItemStack> grid(JsonObject expected,RegistryOps<JsonElement> ops) {
        return expected.getAsJsonArray("ingredients").asList().stream().map(e->sample(e.getAsJsonObject(),ops)).toList();
    }
    public static ItemStack sample(JsonObject ingredient,RegistryOps<JsonElement> ops) {
        var samples=ingredient.getAsJsonArray("samples");
        return samples.isEmpty()?ItemStack.EMPTY:stack(samples.get(0).getAsJsonObject(),ops);
    }
    public static List<ItemStack> conditionalSamples(JsonObject catalyst) {
        var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse(catalyst.get("tag").getAsString()));
        var result=new ArrayList<ItemStack>();
        // Ingredient.getItems() substitutes a display-only BARRIER when a tag is empty.
        for(var holder:BuiltInRegistries.ITEM.getTagOrEmpty(tag))result.add(new ItemStack(holder));
        return result;
    }
    public static Map<String,Integer> amounts(JsonObject object) {
        var result=new LinkedHashMap<String,Integer>();object.entrySet().forEach(e->result.put(e.getKey(),e.getValue().getAsInt()));return result;
    }
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception {
        var expected=coverage();var arcane=expected.getAsJsonArray("arcane");var crucible=expected.getAsJsonArray("crucible");
        require(arcane.size()==89&&crucible.size()==78,"Original crafting coverage must contain 89 arcane and 78 crucible recipes");
        var ids=new HashSet<String>();int checks=1;
        for(var element:arcane) {
            var entry=element.getAsJsonObject();String id=entry.get("recipe").getAsString();require(ids.add(id),"Duplicate original recipe mapping: "+id);
            var loaded=read(resources,ops,id);require(loaded instanceof ArcaneRecipe,"Wrong recipe type for "+id);var recipe=(ArcaneRecipe)loaded;
            require(recipe.research().equals(entry.get("research").getAsString()),"Original research gate changed: "+id);
            require(recipe.vis()==entry.get("vis").getAsInt()&&recipe.crystals().equals(amounts(entry.getAsJsonObject("crystals"))),"Original vis/crystal cost changed: "+id);
            require(recipe.shapeless()==entry.get("shapeless").getAsBoolean(),"Original shaped/shapeless behavior changed: "+id);
            require(recipe.sourceOrder()==entry.get("source_order").getAsInt(),"Original registry precedence changed: "+id);
            verifyOutput(entry,recipe.getResultItem(registries),ops);
            checks+=7+verifyArcane(entry,recipe,registries,ops);
        }
        int optional=0;
        for(var element:crucible) {
            var entry=element.getAsJsonObject();String id=entry.get("recipe").getAsString();require(ids.add(id),"Duplicate original recipe mapping: "+id);
            var loaded=read(resources,ops,id);require(loaded instanceof CrucibleRecipe,"Wrong recipe type for "+id);var recipe=(CrucibleRecipe)loaded;
            require(recipe.research().equals(entry.get("research").getAsString()),"Original research gate changed: "+id);
            require(recipe.aspects().equals(amounts(entry.getAsJsonObject("aspects"))),"Original crucible aspect cost changed: "+id);
            verifyOutput(entry,recipe.getResultItem(registries),ops);
            var catalyst=entry.getAsJsonObject("catalyst");var samples=catalyst.getAsJsonArray("samples");
            if(entry.get("conditional").getAsBoolean())optional++;
            require(!samples.isEmpty()||entry.get("conditional").getAsBoolean(),"Missing original catalyst evidence: "+id);
            require(!recipe.matches(new SingleRecipeInput(new ItemStack(Items.BARRIER)),null),"Arbitrary catalyst accepted: "+id);
            require(!recipe.matches(new SingleRecipeInput(ItemStack.EMPTY),null),"Empty catalyst accepted: "+id);
            for(var sample:samples) {
                var input=stack(sample.getAsJsonObject(),ops);
                require(recipe.matches(new SingleRecipeInput(input),null),"Original catalyst variant rejected: "+id+" / "+input);
                verifyOutput(entry,recipe.assemble(new SingleRecipeInput(input),registries),ops);checks+=2;
            }
            if(catalyst.has("rejected_legacy_variants"))for(var wrong:catalyst.getAsJsonArray("rejected_legacy_variants")) {
                require(!recipe.matches(new SingleRecipeInput(stack(wrong.getAsJsonObject(),ops)),null),"Wrong legacy catalyst metadata accepted: "+id+" / "+wrong);checks++;
            }
            if(samples.isEmpty()) {
                // Compatibility tags can acquire members from another mod or a data pack.
                for(var input:conditionalSamples(catalyst)) {
                    require(recipe.matches(new SingleRecipeInput(input),null),"Conditional ore tag member rejected: "+id);checks++;
                }
            }
            if(catalyst.has("components_required"))checks+=verifyComponents(entry,recipe,ops);
            checks+=7;
        }
        require(optional==4&&ids.size()==167,"Conditional/source recipe count changed");
        return checks+1;
    }
    private static int verifyArcane(JsonObject expected,ArcaneRecipe recipe,RegistryAccess registries,RegistryOps<JsonElement> ops) {
        String id=expected.get("recipe").getAsString();int width=expected.get("width").getAsInt(),height=expected.get("height").getAsInt();
        var ingredients=expected.getAsJsonArray("ingredients");var grid=grid(expected,ops);int checks=0;
        var input=CraftingInput.of(width,height,grid);
        require(recipe.matches(input,null),"Original arcane grid rejected: "+id);
        verifyOutput(expected,recipe.assemble(input,registries),ops);checks+=2;
        if(!recipe.shapeless()) {
            require(recipe.pattern().width()==width&&recipe.pattern().height()==height,"Original trimmed layout dimensions changed: "+id);checks++;
        }
        // Every expanded legacy ore/metadata alternative is tested in its actual recipe cell.
        for(int cell=0;cell<ingredients.size();cell++) {
            var ingredient=ingredients.get(cell).getAsJsonObject();var samples=ingredient.getAsJsonArray("samples");
            for(var alternative:samples) {
                var variant=new ArrayList<>(grid);variant.set(cell,stack(alternative.getAsJsonObject(),ops));
                require(recipe.matches(CraftingInput.of(width,height,variant),null),"Original ingredient alternative rejected: "+id+" cell "+cell+" "+alternative);checks++;
            }
            if(ingredient.has("rejected_legacy_variants"))for(var rejected:ingredient.getAsJsonArray("rejected_legacy_variants")) {
                var variant=new ArrayList<>(grid);variant.set(cell,stack(rejected.getAsJsonObject(),ops));
                require(!recipe.matches(CraftingInput.of(width,height,variant),null),"Wrong legacy ingredient metadata accepted: "+id+" cell "+cell+" / "+rejected);checks++;
            }
            if(!grid.get(cell).isEmpty()) {
                var named=grid.get(cell).copy();named.set(DataComponents.CUSTOM_NAME,Component.literal("unrelated metadata"));
                if(ingredient.has("wildcard_damage")&&named.isDamageableItem())named.setDamageValue(Math.min(7,named.getMaxDamage()-1));
                var variant=new ArrayList<>(grid);variant.set(cell,named);
                require(recipe.matches(CraftingInput.of(width,height,variant),null),"Original wildcard/unrelated metadata rejected: "+id+" cell "+cell);checks++;
            }
            var wrong=new ArrayList<>(grid);wrong.set(cell,new ItemStack(Items.BARRIER));
            require(!recipe.matches(CraftingInput.of(width,height,wrong),null),"Wrong/extra ingredient accepted: "+id+" cell "+cell);checks++;
            if(!grid.get(cell).isEmpty()) {
                var missing=new ArrayList<>(grid);missing.set(cell,ItemStack.EMPTY);
                require(!recipe.matches(CraftingInput.of(width,height,missing),null),"Missing ingredient accepted: "+id+" cell "+cell);checks++;
            }
        }
        for(int top=0;top<=3-height;top++)for(int left=0;left<=3-width;left++)for(boolean mirrored:List.of(false,true)) {
            var placed=new ArrayList<>(Collections.nCopies(9,ItemStack.EMPTY));
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)placed.set(left+x+(top+y)*3,grid.get((mirrored?width-1-x:x)+y*width));
            require(recipe.matches(CraftingInput.ofPositioned(3,3,placed).input(),null),"Original mirrored/offset crafting rejected: "+id+" offset "+left+","+top);checks++;
            for(int cell=0;cell<9;cell++)if(placed.get(cell).isEmpty()) {
                var extra=new ArrayList<>(placed);extra.set(cell,new ItemStack(Items.BARRIER));
                require(!recipe.matches(CraftingInput.ofPositioned(3,3,extra).input(),null),"Extra grid item accepted: "+id);checks++;break;
            }
        }
        if(recipe.shapeless()) {
            var reversed=new ArrayList<>(grid);Collections.reverse(reversed);
            require(recipe.matches(CraftingInput.of(width,height,reversed),null),"Shapeless ingredients depend on order: "+id);checks++;
        }
        if(id.equals("thaumcraft:tube_buffer")) {
            var filled=new ArrayList<>(grid);
            for(int i=0;i<filled.size();i++)if(BuiltInRegistries.ITEM.getKey(filled.get(i).getItem()).toString().equals("thaumcraft:phial"))filled.set(i,EssentiaItem.phial(Aspect.FIRE));
            require(!recipe.matches(CraftingInput.of(width,height,filled),null),"Tube buffer accepted a filled phial");checks++;
        }
        return checks;
    }
    private static int verifyComponents(JsonObject expected,CrucibleRecipe recipe,RegistryOps<JsonElement> ops) {
        String id=expected.get("recipe").getAsString();var exact=sample(expected.getAsJsonObject("catalyst"),ops);var named=exact.copy();named.set(DataComponents.CUSTOM_NAME,Component.literal("unrelated metadata"));
        require(recipe.matches(new SingleRecipeInput(named),null),"Original component ingredient rejected unrelated metadata: "+id);
        ItemStack wrong=exact.getItem() instanceof CrystalItem?CrystalItem.stack(Aspect.FIRE):EssentiaItem.phial(Aspect.FIRE);
        require(!recipe.matches(new SingleRecipeInput(wrong),null),"Required aspect ignored: "+id);
        require(!recipe.matches(new SingleRecipeInput(new ItemStack(exact.getItem())),null),"Missing required aspect accepted: "+id);
        return 3;
    }
    public static void verifyOutput(JsonObject expected,ItemStack actual,RegistryOps<JsonElement> ops) {
        var result=stack(expected.getAsJsonObject("result"),ops);String id=expected.get("recipe").getAsString();
        require(!actual.isEmpty()&&!(actual.getItem() instanceof ResearchDisplayItem),"Display-only/unregistered recipe output: "+id);
        require(actual.getCount()==result.getCount()&&ItemStack.isSameItemSameComponents(actual,result),"Original output identity/count/components changed: "+id+" expected "+result+" actual "+actual);
    }
    private static Recipe<?> read(ResourceManager resources,RegistryOps<JsonElement> ops,String id)throws Exception {
        var location=ResourceLocation.parse(id);var path=ResourceLocation.fromNamespaceAndPath(location.getNamespace(),"recipe/"+location.getPath()+".json");
        try(var reader=resources.getResourceOrThrow(path).openAsReader()) {return Recipe.CODEC.parse(ops,JsonParser.parseReader(reader)).getOrThrow();}
    }
    private static void require(boolean value,String message) {if(!value)throw new IllegalStateException(message);}
    private CraftingRecipeFixtures() {}
}
