package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.*;
import thaumcraft.port.essentia.*;

public final class EssentiaExtensionFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception{
        int checks=0;
        var state=EssentiaContent.TUBES.get("tube_buffer").get().defaultBlockState();
        var buffer=new BufferEntity(BlockPos.ZERO,state);buffer.contents.insert(Aspect.AIR,4,false);buffer.contents.insert(Aspect.FIRE,6,false);buffer.chokes[Direction.EAST.ordinal()]=2;buffer.chokes[Direction.NORTH.ordinal()]=1;buffer.open=61;buffer.bellows=3;
        var copy=new BufferEntity(BlockPos.ZERO,state);copy.loadCustomOnly(buffer.saveCustomOnly(registries),registries);
        require(copy.contents.values().equals(buffer.contents.values())&&copy.open==61&&copy.bellows==3&&copy.suction(Direction.EAST)==0&&copy.suction(Direction.NORTH)==1&&copy.suction(Direction.SOUTH)==96,"Mixed buffer save retains aspects, ports and per-face chokes");checks++;
        copy.contents.clear();copy.insert(Aspect.AIR,1,Direction.SOUTH,false);copy.insert(Aspect.FIRE,1,Direction.SOUTH,false);copy.bellows=0;
        var jar=new JarEntity(BlockPos.ZERO,EssentiaContent.JAR.get().defaultBlockState());jar.tank.filter(Aspect.FIRE);
        require(EssentiaTransport.pull(jar,Direction.UP,copy)==1&&jar.tank.aspect()==Aspect.FIRE&&copy.contents.amount(Aspect.AIR)==1&&copy.contents.amount(Aspect.FIRE)==0,"Typed demand pulls requested aspect from multi-aspect source, not random advertised aspect");checks++;
        require(EssentiaTransport.pull(jar,Direction.UP,copy)==0&&copy.contents.total()==1,"Missing requested aspect never drains another");checks++;
        require(copy.insert(Aspect.WATER,2,Direction.SOUTH,false)==0&&copy.insert(Aspect.WATER,1,Direction.UP,false)==0,"Buffer keeps TC6 single-unit insertion and disabled port rule");checks++;
        CompoundTag bad=copy.saveCustomOnly(registries);bad.putByteArray("Chokes",new byte[]{-10,100});bad.putInt("Bellows",Integer.MAX_VALUE);copy.loadCustomOnly(bad,registries);
        require(copy.chokes[0]==0&&copy.chokes[1]==2&&copy.chokes[2]==0&&copy.bellows==6,"Malformed short choke array is safely bounded");checks++;
        var thaumatorium=new ThaumatoriumEntity(BlockPos.ZERO,EssentiaContent.THAUMATORIUM.get().defaultBlockState());var uuid=UUID.fromString("11111111-2222-3333-4444-555555555555");
        thaumatorium.setItem(0,new ItemStack(Items.IRON_INGOT,13));thaumatorium.selected.put(ResourceLocation.parse("thaumcraft:thaumium"),uuid);thaumatorium.contents.insert(Aspect.FIRE,7,false);thaumatorium.contents.insert(Aspect.ORDER,4,false);thaumatorium.capacity=3;thaumatorium.demand=Aspect.FIRE;thaumatorium.heated=true;
        var restored=new ThaumatoriumEntity(BlockPos.ZERO,thaumatorium.getBlockState());restored.loadCustomOnly(thaumatorium.saveCustomOnly(registries),registries);
        require(restored.getItem(0).getCount()==13&&restored.selected.equals(thaumatorium.selected)&&restored.contents.values().equals(thaumatorium.contents.values())&&restored.capacity==3&&restored.demand==Aspect.FIRE&&restored.heated,"Thaumatorium persists catalyst, recipe owner, accumulated essentia and status");checks++;
        require(!restored.input(Direction.NORTH)&&!restored.input(Direction.UP)&&!restored.input(Direction.DOWN)&&restored.input(Direction.EAST)&&!restored.output(Direction.EAST),"Lower machine ports exclude front, bottom and internal top");checks++;
        var top=new ThaumatoriumEntity(BlockPos.ZERO,EssentiaContent.THAUMATORIUM_TOP.get().defaultBlockState());require(top.input(Direction.UP)&&!top.output(Direction.UP)&&top.main()==null&&top.suction(Direction.UP)==0,"Detached top cannot consume or expose a stale master");checks++;
        var recipe=new CrucibleRecipe("",Ingredient.of(Items.IRON_INGOT),Map.of("ignis",8,"ordo",4),new ItemStack(Items.GOLD_INGOT));
        require(ThaumatoriumEntity.missing(recipe,restored.contents)==Aspect.FIRE,"Machine requests the remaining required aspect");checks++;
        restored.contents.insert(Aspect.FIRE,1,false);require(ThaumatoriumEntity.missing(recipe,restored.contents)==null,"Completion requires every recipe amount");checks++;
        for(String name:List.of("tube_buffer","bellows","smelter_aux","smelter_vent","brain_box","mind_clockwork"))try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:recipe/"+name+".json")).openAsReader()){
            var arcane=(ArcaneRecipe)Recipe.CODEC.parse(ops,JsonParser.parseReader(in)).getOrThrow();var stacks=new ArrayList<ItemStack>();for(var ingredient:arcane.pattern().ingredients())stacks.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copy());var input=CraftingInput.of(arcane.pattern().width(),arcane.pattern().height(),stacks);
            require(arcane.matches(input,null)&&arcane.assemble(input,registries).is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+name))),"Extension recipe matches and assembles: "+name);checks++;
            if(name.equals("tube_buffer")){for(int i=0;i<stacks.size();i++)if(stacks.get(i).is(EssentiaContent.PHIAL.get())){stacks.set(i,EssentiaItem.phial(Aspect.FIRE));break;}require(!arcane.matches(CraftingInput.of(3,3,stacks),null),"Filled phial cannot be silently consumed as empty glass in buffer recipe");checks++;}
        }
        var tube=new TubeEntity(BlockPos.ZERO,EssentiaContent.TUBES.get("tube").get().defaultBlockState());tube.suction=191;tube.suctionType=Aspect.AIR;
        var tubeCopy=new TubeEntity(BlockPos.ZERO,tube.getBlockState());tubeCopy.loadCustomOnly(tube.saveCustomOnly(registries),registries);require(tubeCopy.suction==191,"Six-bellows buffer pressure survives neighboring tube save");checks++;
        return checks;
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private EssentiaExtensionFixtures(){}
}
