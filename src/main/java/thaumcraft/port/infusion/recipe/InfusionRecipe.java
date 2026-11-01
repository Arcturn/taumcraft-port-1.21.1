package thaumcraft.port.infusion.recipe;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.research.ResearchStorage;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Actual matrix recipes. Dynamic TC6 augments operate on a copy of the entire central stack. */
public record InfusionRecipe(String research, Ingredient input, List<Ingredient> components,
        Map<String,Integer> baseAspects, int baseInstability, ItemStack output, String operation, String parameter)
        implements Recipe<InfusionInput> {
    private static final Codec<String> ASPECT=Codec.STRING.validate(s->Aspect.byId(s).isPresent()?DataResult.success(s):DataResult.error(()->"Unknown aspect: "+s));
    private static final Codec<String> OPERATION=Codec.STRING.validate(s->Set.of("item","runic","enchantment","goggles","mask","verdant").contains(s)?DataResult.success(s):DataResult.error(()->"Unknown infusion operation: "+s));
    public static final MapCodec<InfusionRecipe> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
        Codec.STRING.fieldOf("research").forGetter(InfusionRecipe::research),
        Ingredient.CODEC.optionalFieldOf("input",Ingredient.EMPTY).forGetter(InfusionRecipe::input),
        Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(InfusionRecipe::components),
        Codec.unboundedMap(ASPECT,Codec.intRange(1,1000000)).fieldOf("aspects").forGetter(InfusionRecipe::baseAspects),
        Codec.intRange(0,100).fieldOf("instability").forGetter(InfusionRecipe::baseInstability),
        ItemStack.CODEC.optionalFieldOf("result",ItemStack.EMPTY).forGetter(InfusionRecipe::output),
        OPERATION.optionalFieldOf("operation","item").forGetter(InfusionRecipe::operation),
        Codec.STRING.optionalFieldOf("parameter","").forGetter(InfusionRecipe::parameter)).apply(i,InfusionRecipe::new));
    public InfusionRecipe {
        components=List.copyOf(components);baseAspects=Collections.unmodifiableMap(new LinkedHashMap<>(baseAspects));output=output.copy();
        if(operation.equals("item")&&(output.isEmpty()||input.isEmpty()))throw new IllegalArgumentException("An item infusion needs a central ingredient and result");
        if(operation.equals("enchantment")&&InfusionAugments.maxLevel(parameter)==0)throw new IllegalArgumentException("Unknown infusion enchantment "+parameter);
        if((operation.equals("mask")||operation.equals("verdant"))&&!Set.of("0","1","2").contains(parameter))throw new IllegalArgumentException("Invalid augment variant "+parameter);
        if(operation.equals("runic")&&components.size()!=2)throw new IllegalArgumentException("Runic augmentation needs salis mundus and amber");
    }
    public boolean matches(ServerPlayer player,ItemStack central,List<ItemStack> pedestals){
        if(player==null)return false;
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());
        // BETA26 deliberately checks discovery rather than completion for runic augmentation.
        boolean unlocked=operation.equals("runic")?state.isResearchKnown(research):state.knows(research);
        return unlocked&&matches(new InfusionInput(central,pedestals),player.level());
    }
    @Override public boolean matches(InfusionInput in,Level level){
        if(in.central().isEmpty()||(!input.isEmpty()&&!input.test(in.central())))return false;
        if(operation.equals("runic")&&(!(in.central().getItem() instanceof ArmorItem||in.central().getItem() instanceof ICurioItem)||InfusionAugments.getRunic(in.central())>=127))return false;
        if(operation.equals("enchantment")&&(InfusionAugments.getEnchantment(in.central(),parameter)>=InfusionAugments.maxLevel(parameter)||!canEnchant(in.central(),parameter)))return false;
        return matchComponents(components(in.central()),in.components());
    }
    public List<Ingredient> components(ItemStack central){
        if(!operation.equals("runic"))return components;
        var list=new ArrayList<>(components);for(int n=0;n<InfusionAugments.getRunic(central);n++)list.add(components.get(1));return List.copyOf(list);
    }
    /** Bipartite matching handles repeated ingredients and overlapping item tags without order dependence. */
    public static boolean matchComponents(List<Ingredient> ingredients,List<ItemStack> stacks){
        if(stacks.size()!=ingredients.size()||stacks.stream().anyMatch(ItemStack::isEmpty))return false;
        int[] assigned=new int[stacks.size()];Arrays.fill(assigned,-1);
        for(int n=0;n<ingredients.size();n++)if(!assign(n,ingredients,stacks,assigned,new boolean[stacks.size()]))return false;
        return true;
    }
    private static boolean assign(int ingredient,List<Ingredient> ingredients,List<ItemStack> stacks,int[] assigned,boolean[] seen){
        for(int n=0;n<stacks.size();n++)if(!seen[n]&&ingredients.get(ingredient).test(stacks.get(n))){seen[n]=true;if(assigned[n]<0||assign(assigned[n],ingredients,stacks,assigned,seen)){assigned[n]=ingredient;return true;}}
        return false;
    }
    public static boolean canEnchant(ItemStack stack,String enchantment){
        boolean axe=stack.getItem() instanceof AxeItem,pick=stack.getItem() instanceof PickaxeItem,shovel=stack.getItem() instanceof ShovelItem;
        boolean[] weapon={false};stack.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(Attributes.ATTACK_DAMAGE))weapon[0]=true;});
        return switch(enchantment){case "collector"->axe||pick||shovel||weapon[0];case "destructive","lamplight"->axe||pick||shovel;case "burrowing"->axe||pick;case "sounding","refining"->pick;case "arcing","essence"->weapon[0];default->false;};
    }
    public ItemStack result(ServerPlayer player,ItemStack central,List<ItemStack> pedestals){
        ItemStack result=assemble(new InfusionInput(central,pedestals),player==null?null:player.registryAccess());
        if(operation.equals("enchantment")&&player!=null&&!result.isEmpty()&&player.getRandom().nextInt(10)<InfusionAugments.enchantments(central).size())InfusionAugments.setWarp(result,InfusionAugments.getWarp(central)+1);
        return result;
    }
    @Override public ItemStack assemble(InfusionInput in,HolderLookup.Provider registries){
        if(operation.equals("item"))return output.copy();
        if(in.central().isEmpty())return ItemStack.EMPTY;
        ItemStack result=in.central().copyWithCount(1);
        switch(operation){
            case "runic"->InfusionAugments.setRunic(result,InfusionAugments.getRunic(result)+1);
            case "enchantment"->{int next=InfusionAugments.getEnchantment(result,parameter)+1;if(next>InfusionAugments.maxLevel(parameter))return ItemStack.EMPTY;InfusionAugments.setEnchantment(result,parameter,next);}
            case "goggles"->InfusionAugments.setGoggles(result,true);
            case "mask"->InfusionAugments.setMask(result,Integer.parseInt(parameter));
            case "verdant"->InfusionAugments.setType(result,Integer.parseInt(parameter));
            default->throw new IllegalStateException("Unknown infusion operation "+operation);
        }
        return result;
    }
    public Map<Aspect,Integer> aspects(ServerPlayer player,ItemStack central,List<ItemStack> pedestals){
        if(operation.equals("runic")){
            int level=InfusionAugments.getRunic(central);int vis=(int)Math.min(Integer.MAX_VALUE,20.0+20.0*Math.pow(2,level));
            return Map.of(Aspect.PROTECT,vis,Aspect.CRYSTAL,vis/2,Aspect.ENERGY,vis/2);
        }
        float modifier=1;
        if(operation.equals("enchantment")){
            int level=InfusionAugments.getEnchantment(central,parameter);int other=InfusionAugments.enchantments(central).size()-(level>0?1:0);modifier=level+1+other*.33f;
        }
        var result=new LinkedHashMap<Aspect,Integer>();for(var entry:baseAspects.entrySet())result.put(Aspect.byId(entry.getKey()).orElseThrow(),(int)(entry.getValue()*modifier));return Collections.unmodifiableMap(result);
    }
    public int instability(ServerPlayer player,ItemStack central,List<ItemStack> pedestals){return baseInstability+(operation.equals("runic")?InfusionAugments.getRunic(central)/2:0);}
    public int experience(ServerPlayer player,ItemStack central,List<ItemStack> pedestals){return 0;}
    public boolean dynamic(){return !operation.equals("item");}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries){return output.copy();}
    @Override public boolean canCraftInDimensions(int x,int y){return false;}
    @Override public RecipeSerializer<?> getSerializer(){return InfusionRecipes.SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return InfusionRecipes.TYPE.get();}
    public static final class Serializer implements RecipeSerializer<InfusionRecipe>{
        public MapCodec<InfusionRecipe> codec(){return CODEC;}
        public StreamCodec<RegistryFriendlyByteBuf,InfusionRecipe> streamCodec(){return ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());}
    }
}
