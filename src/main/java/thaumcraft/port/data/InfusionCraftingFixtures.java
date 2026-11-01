package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Recipe;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.ResearchDisplayItem;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.research.ResearchCatalog;

/** Independent BETA26 runtime/source expectations for all 56 actual infusion recipes, across every book category. */
public final class InfusionCraftingFixtures {
    public static JsonObject coverage() { return ResearchCatalog.read("/thaumcraft-port/infusion-crafting-coverage.json"); }
    public static List<JsonObject> entries() { return coverage().getAsJsonArray("infusion").asList().stream().map(JsonElement::getAsJsonObject).toList(); }
    public static ItemStack central(JsonObject expected,RegistryOps<JsonElement> ops) { return sample(expected.getAsJsonObject("input"),ops); }
    public static List<ItemStack> components(JsonObject expected,RegistryOps<JsonElement> ops) {
        return expected.getAsJsonArray("components").asList().stream().map(e->sample(e.getAsJsonObject(),ops)).toList();
    }
    private static ItemStack sample(JsonObject ingredient,RegistryOps<JsonElement> ops) { return stack(ingredient.getAsJsonArray("samples").get(0).getAsJsonObject(),ops); }
    private static ItemStack stack(JsonObject expected,RegistryOps<JsonElement> ops) {
        var result=CraftingRecipeFixtures.stack(expected,ops);
        // A modern item may supply defaults that the original NBT-less stack never had.
        // In particular, constructing crystal_essence otherwise silently creates an Air crystal.
        if(expected.has("removed_components"))for(var id:expected.getAsJsonArray("removed_components")) {
            var component=BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(id.getAsString()));
            require(component!=null,"Unknown removed legacy component: "+id);result.remove(component);
        }
        return result;
    }
    public static Map<Aspect,Integer> aspects(JsonObject expected) {
        var result=new LinkedHashMap<Aspect,Integer>();CraftingRecipeFixtures.amounts(expected.getAsJsonObject("aspects")).forEach((id,n)->result.put(Aspect.byId(id).orElseThrow(),n));return result;
    }
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception {
        var fixtures=entries();require(fixtures.size()==56,"Original infusion coverage must contain 56 actual recipes");
        Set<String> originals=new HashSet<>(),ids=new HashSet<>();int checks=1,added=0;
        for(var expected:fixtures) {
            String id=expected.get("recipe").getAsString();require(originals.add(expected.get("original").getAsString())&&ids.add(id),"Duplicate original infusion mapping: "+id);
            var location=ResourceLocation.parse(id);Recipe<?> loaded;
            try(var reader=resources.getResourceOrThrow(ResourceLocation.fromNamespaceAndPath(location.getNamespace(),"recipe/"+location.getPath()+".json")).openAsReader()) { loaded=Recipe.CODEC.parse(ops,JsonParser.parseReader(reader)).getOrThrow(); }
            require(loaded instanceof InfusionRecipe,"Original infusion recipe missing or wrong type: "+id);var recipe=(InfusionRecipe)loaded;
            var input=central(expected,ops);var components=components(expected,ops);
            require(!input.isEmpty()&&components.stream().noneMatch(ItemStack::isEmpty),"Missing original infusion input evidence: "+id);
            require(recipe.research().equals(expected.get("research").getAsString()),"Original infusion research gate changed: "+id);
            require(recipe.operation().equals(expected.get("operation").getAsString())&&recipe.parameter().equals(expected.get("parameter").getAsString()),"Original infusion output operation changed: "+id);
            require(recipe.instability(null,input,components)==expected.get("instability").getAsInt(),"Original infusion instability changed: "+id);
            require(recipe.aspects(null,input,components).equals(aspects(expected)),"Original infusion essentia cost changed: "+id);
            require(recipe.matches(new InfusionInput(input,components),null),"Original infusion input rejected: "+id);
            verifyOutput(expected,recipe.assemble(new InfusionInput(input,components),registries),input,ops);
            var reversed=new ArrayList<>(components);Collections.reverse(reversed);
            require(recipe.matches(new InfusionInput(input,reversed),null),"Infusion pedestals depend on order: "+id);
            require(!recipe.matches(new InfusionInput(ItemStack.EMPTY,components),null),"Empty central pedestal accepted: "+id);
            require(!recipe.matches(new InfusionInput(new ItemStack(Items.BARRIER),components),null),"Arbitrary central pedestal accepted: "+id);
            var missing=new ArrayList<>(components);missing.removeLast();require(!recipe.matches(new InfusionInput(input,missing),null),"Missing infusion pedestal accepted: "+id);
            var extra=new ArrayList<>(components);extra.add(new ItemStack(Items.BARRIER));require(!recipe.matches(new InfusionInput(input,extra),null),"Extra infusion pedestal accepted: "+id);
            checks+=14;
            checks+=variants(expected.getAsJsonObject("input"),-1,id,recipe,input,components,ops);
            for(int slot=0;slot<components.size();slot++)checks+=variants(expected.getAsJsonArray("components").get(slot).getAsJsonObject(),slot,id,recipe,input,components,ops);
            if(recipe.dynamic())checks+=dynamic(expected,recipe,registries,ops);
            if(expected.get("added_in_alpha930").getAsBoolean())added++;
        }
        require(added==26,"Original infusion inventory must cover the 26 previously missing recipes");return checks+1;
    }
    private static int variants(JsonObject expected,int slot,String id,InfusionRecipe recipe,ItemStack central,List<ItemStack> components,RegistryOps<JsonElement> ops) {
        int checks=0;
        for(var value:expected.getAsJsonArray("samples")) {
            var sample=stack(value.getAsJsonObject(),ops);
            require(match(recipe,central,components,slot,sample),"Original infusion ingredient alternative rejected: "+id+" slot "+slot+" / "+sample);checks++;
            var named=sample.copy();named.set(DataComponents.CUSTOM_NAME,Component.literal("Unrelated item name"));
            if(expected.has("wildcard_damage")&&named.isDamageableItem())named.setDamageValue(Math.min(7,named.getMaxDamage()-1));
            require(match(recipe,central,components,slot,named),"Infusion lost legacy wildcard/unrelated component behavior: "+id+" slot "+slot);checks++;
            if(expected.has("exact_damage")&&sample.isDamageableItem()) {
                var damaged=sample.copy();damaged.setDamageValue(1);
                require(!match(recipe,central,components,slot,damaged),"Original undamaged infusion ingredient accepted damage: "+id+" slot "+slot);checks++;
            }
        }
        if(slot>=0){require(!match(recipe,central,components,slot,new ItemStack(Items.BARRIER)),"Wrong infusion pedestal accepted: "+id+" slot "+slot);checks++;}
        if(expected.has("rejected_legacy_variants"))for(var value:expected.getAsJsonArray("rejected_legacy_variants")) {
            require(!match(recipe,central,components,slot,stack(value.getAsJsonObject(),ops)),"Wrong legacy infusion metadata accepted: "+id+" slot "+slot+" / "+value);checks++;
        }
        return checks;
    }
    private static boolean match(InfusionRecipe recipe,ItemStack central,List<ItemStack> components,int slot,ItemStack sample) {
        var variant=new ArrayList<>(components);if(slot<0)central=sample;else variant.set(slot,sample);return recipe.matches(new InfusionInput(central,variant),null);
    }
    private static int dynamic(JsonObject expected,InfusionRecipe recipe,RegistryAccess registries,RegistryOps<JsonElement> ops) {
        String id=expected.get("recipe").getAsString();var input=central(expected,ops);var components=components(expected,ops);
        input.set(DataComponents.CUSTOM_NAME,Component.literal("Original augmented item"));if(input.isDamageableItem())input.setDamageValue(11);
        InfusionAugments.setWarp(input,2);
        var result=recipe.assemble(new InfusionInput(input,components),registries);verifyOutput(expected,result,input,ops);
        require(result.getHoverName().equals(input.getHoverName())&&result.getDamageValue()==input.getDamageValue()&&InfusionAugments.getWarp(result)==2,"Dynamic infusion discarded central item components: "+id);
        int checks=2;
        if(recipe.operation().equals("runic")) {
            // Values evaluated from InfusionRunicAugmentRecipe, independent of the port's implementation.
            int[] protection={40,60,100,180,340},instability={5,5,6,6,7};
            var salis=components.getFirst();var amber=components.getLast();
            for(int level=0;level<protection.length;level++) {
                InfusionAugments.setRunic(input,level);var required=new ArrayList<ItemStack>();required.add(salis.copy());for(int n=0;n<=level;n++)required.add(amber.copy());
                require(recipe.matches(new InfusionInput(input,required),null),"Runic level rejected original amber count: "+level);
                require(recipe.aspects(null,input,required).equals(Map.of(Aspect.PROTECT,protection[level],Aspect.CRYSTAL,protection[level]/2,Aspect.ENERGY,protection[level]/2))&&recipe.instability(null,input,required)==instability[level],"Original runic scaling changed at level "+level);
                require(InfusionAugments.getRunic(recipe.assemble(new InfusionInput(input,required),registries))==level+1,"Runic augmentation did not add exactly one level");
                required.removeLast();require(!recipe.matches(new InfusionInput(input,required),null),"Runic augmentation accepted missing amber at level "+level);checks+=4;
            }
        } else if(recipe.operation().equals("enchantment")) {
            String enchantment=expected.get("parameter").getAsString();int maximum=expected.get("max_level").getAsInt();
            for(int level=0;level<maximum;level++) {
                var tool=central(expected,ops);if(level>0)InfusionAugments.setEnchantment(tool,enchantment,level);
                var costs=new LinkedHashMap<Aspect,Integer>();aspects(expected).forEach((aspect,cost)->costs.put(aspect,cost*(InfusionAugments.getEnchantment(tool,enchantment)+1)));
                require(recipe.matches(new InfusionInput(tool,components),null)&&recipe.aspects(null,tool,components).equals(costs),"Original enchantment level cost changed: "+id+" level "+level);
                require(InfusionAugments.getEnchantment(recipe.assemble(new InfusionInput(tool,components),registries),enchantment)==level+1,"Enchantment did not add exactly one level: "+id);checks+=2;
            }
            var capped=central(expected,ops);InfusionAugments.setEnchantment(capped,enchantment,maximum);
            require(!recipe.matches(new InfusionInput(capped,components),null),"Infusion enchantment exceeded original maximum: "+id);checks++;
            var crowded=central(expected,ops);InfusionAugments.setEnchantment(crowded,enchantment.equals("sounding")?"refining":"sounding",1);
            var surcharge=new LinkedHashMap<Aspect,Integer>();aspects(expected).forEach((aspect,cost)->surcharge.put(aspect,(int)(cost*1.33f)));
            require(recipe.aspects(null,crowded,components).equals(surcharge),"Original other-enchantment surcharge changed: "+id);checks++;
        }
        return checks;
    }
    public static void verifyOutput(JsonObject expected,ItemStack actual,ItemStack input,RegistryOps<JsonElement> ops) {
        String id=expected.get("recipe").getAsString();require(!actual.isEmpty()&&!(actual.getItem() instanceof ResearchDisplayItem),"Missing/display-only infusion output: "+id);
        String operation=expected.get("operation").getAsString();
        if(operation.equals("item")){
            var result=CraftingRecipeFixtures.stack(expected.getAsJsonObject("result"),ops);
            // TC6 looks enchantments up by enum ID. The order of infench entries has no effect;
            // modern item defaults built from maps need not serialize in the source insertion order.
            var normalizedActual=canonicalEnchantments(actual);var normalizedExpected=canonicalEnchantments(result);
            require(actual.getCount()==result.getCount()&&ItemStack.isSameItemSameComponents(normalizedActual,normalizedExpected),"Original infusion output identity/count/components changed: "+id+" expected "+normalizedExpected.getComponents()+" actual "+normalizedActual.getComponents());return;
        }
        require(actual.is(input.getItem())&&actual.getCount()==1,"Dynamic infusion changed central item identity/count: "+id);
        String parameter=expected.get("parameter").getAsString();
        boolean correct=switch(operation) {
            case "runic"->InfusionAugments.getRunic(actual)==InfusionAugments.getRunic(input)+1;
            case "enchantment"->InfusionAugments.getEnchantment(actual,parameter)==InfusionAugments.getEnchantment(input,parameter)+1;
            case "goggles"->InfusionAugments.goggles(actual);
            case "mask"->InfusionAugments.mask(actual)==Integer.parseInt(parameter);
            case "verdant"->InfusionAugments.type(actual)==Integer.parseInt(parameter);
            default->false;
        };require(correct,"Original infusion output augmentation changed: "+id);
    }
    private static ItemStack canonicalEnchantments(ItemStack source) {
        var copy=source.copy();var data=copy.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(data.contains("infench",Tag.TAG_LIST)) {
            var values=new ArrayList<Tag>();data.getList("infench",Tag.TAG_COMPOUND).forEach(value->values.add(value.copy()));
            values.sort(Comparator.comparingInt((Tag value)->((CompoundTag)value).getInt("id")).thenComparing(Tag::toString));
            var ordered=new ListTag();values.forEach(ordered::add);data.put("infench",ordered);copy.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        }
        return copy;
    }
    private static void require(boolean valid,String message) { if(!valid)throw new IllegalStateException(message); }
    private InfusionCraftingFixtures() {}
}
