package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.ArcaneRecipe;
import thaumcraft.port.essentia.*;
import thaumcraft.port.research.LegacyRequirement;

public final class EssentiaFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception{
        int checks=0;
        var jar=new JarEntity(BlockPos.ZERO,EssentiaContent.JAR.get().defaultBlockState());
        jar.tank.filter(Aspect.FLUX);jar.fill(Aspect.FLUX,137,false);
        var item=new ItemStack(EssentiaContent.JAR.get());jar.saveToItem(item,registries);
        var json=ItemStack.CODEC.encodeStart(ops,item).getOrThrow();var decoded=ItemStack.CODEC.parse(ops,json).getOrThrow();
        var copy=new JarEntity(BlockPos.ZERO,jar.getBlockState());copy.loadCustomOnly(decoded.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),registries);
        require(copy.tank.amount()==137&&copy.tank.aspect()==Aspect.FLUX&&copy.tank.filter()==Aspect.FLUX,"Filled labelled jar survives item serialization and placement data");checks++;
        ArcaneRecipe upgrade=recipe(resources,ops,"jar_void");var result=upgrade.assemble(CraftingInput.of(1,1,List.of(decoded)),registries);
        var voidJar=new JarEntity(BlockPos.ZERO,EssentiaContent.VOID_JAR.get().defaultBlockState());voidJar.loadCustomOnly(result.get(DataComponents.BLOCK_ENTITY_DATA).copyTag(),registries);
        require(voidJar.voidJar&&voidJar.tank.amount()==137&&voidJar.tank.filter()==Aspect.FLUX,"Void upgrade preserves all contents and label");checks++;
        require(jar.input(Direction.UP)&&!jar.input(Direction.NORTH)&&!jar.output(Direction.DOWN),"Jar only connects through top");checks++;
        var alembic=new JarEntity(BlockPos.ZERO,EssentiaContent.ALEMBIC.get().defaultBlockState());
        require(alembic.fill(Aspect.AIR,200,false)==128&&!alembic.input(Direction.UP)&&alembic.output(Direction.NORTH)&&!alembic.output(Direction.DOWN),"Alembic 128 capacity and output ports");checks++;
        var tube=new TubeEntity(BlockPos.ZERO,EssentiaContent.TUBES.get("tube").get().defaultBlockState());tube.suction=63;tube.suctionType=Aspect.AIR;
        require(EssentiaTransport.pull(tube,Direction.WEST,alembic)==1&&alembic.tank.amount()==127&&tube.tank.amount()==1,"Tube draws exactly one from alembic");checks++;
        require(EssentiaTransport.pull(tube,Direction.WEST,alembic)==0&&alembic.tank.amount()==127,"Full tube never drains source");checks++;
        var labelled=new JarEntity(BlockPos.ZERO,jar.getBlockState());labelled.tank.filter(Aspect.AIR);
        require(EssentiaTransport.pull(labelled,Direction.UP,tube)==1&&tube.tank.amount()==0&&labelled.tank.amount()==1,"Labelled jar wins over tube suction");checks++;
        tube.insert(Aspect.AIR,1,Direction.NORTH,false);jar.tank.restore(Aspect.FLUX,249,Aspect.FLUX);
        require(EssentiaTransport.pull(jar,Direction.UP,tube)==0&&tube.tank.amount()==1&&jar.tank.amount()==249,"Mismatch loses no essentia");checks++;
        labelled.tank.restore(Aspect.AIR,250,Aspect.AIR);
        require(EssentiaTransport.pull(labelled,Direction.UP,tube)==0&&tube.tank.amount()==1,"Full ordinary jar stops suction");checks++;
        tube.open=5;tube.venting=23;tube.allowFlow=false;tube.tank.filter(Aspect.AIR);
        var restoredTube=new TubeEntity(BlockPos.ZERO,tube.getBlockState());restoredTube.loadCustomOnly(tube.saveCustomOnly(registries),registries);
        require(restoredTube.open==5&&restoredTube.venting==23&&restoredTube.tank.aspect()==Aspect.AIR&&restoredTube.suction==63&&restoredTube.tank.filter()==Aspect.AIR&&!restoredTube.allowFlow,"Tube content, filter, ports, suction and vent save");checks++;
        var valve=new TubeEntity(BlockPos.ZERO,EssentiaContent.TUBES.get("tube_valve").get().defaultBlockState());valve.allowFlow=false;
        require(valve.insert(Aspect.AIR,1,Direction.UP,false)==0&&!valve.output(Direction.UP),"Closed valve prevents movement");checks++;
        var oneWay=new TubeEntity(BlockPos.ZERO,EssentiaContent.TUBES.get("tube_oneway").get().defaultBlockState());
        require(oneWay.input(Direction.NORTH)&&!oneWay.input(Direction.SOUTH)&&oneWay.output(Direction.SOUTH)&&!oneWay.output(Direction.NORTH),"Directional tube ports");checks++;
        var centrifuge=new CentrifugeEntity(BlockPos.ZERO,EssentiaContent.CENTRIFUGE.get().defaultBlockState());
        require(centrifuge.insert(Aspect.AIR,1,Direction.DOWN,false)==0&&centrifuge.insert(Aspect.MOTION,1,Direction.UP,false)==0&&centrifuge.insert(Aspect.MOTION,0,Direction.DOWN,false)==0,"Centrifuge rejects primal, wrong port and zero input");checks++;
        require(centrifuge.insert(Aspect.MOTION,1,Direction.DOWN,true)==1&&centrifuge.incoming==null,"Centrifuge simulation");checks++;
        centrifuge.insert(Aspect.MOTION,1,Direction.DOWN,false);centrifuge.process=17;
        var restoredCentrifuge=new CentrifugeEntity(BlockPos.ZERO,centrifuge.getBlockState());restoredCentrifuge.loadCustomOnly(centrifuge.saveCustomOnly(registries),registries);
        require(restoredCentrifuge.incoming==Aspect.MOTION&&restoredCentrifuge.process==17,"Centrifuge resumes processing after save");checks++;
        for(Aspect a:Aspect.values()){
            var phial=EssentiaItem.phial(a);var phialCopy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,phial).getOrThrow()).getOrThrow();
            require(EssentiaItem.aspect(phialCopy)==a,"Phial component round trip: "+a);checks++;
        }
        var requirement=LegacyRequirement.parse("thaumcraft:phial;1;1;{Aspects:[{amount:10,key:'vitium'}]}");
        require(requirement.matches().test(EssentiaItem.phial(Aspect.FLUX))&&!requirement.matches().test(EssentiaItem.phial(Aspect.FIRE))&&!requirement.matches().test(EssentiaItem.phial(null)),"Research accepts only requested filled phial");checks++;
        require(LegacyRequirement.parse("thaumcraft:phial;1;0").matches().test(EssentiaItem.phial(null))&&!LegacyRequirement.parse("thaumcraft:phial;1;0").matches().test(EssentiaItem.phial(Aspect.AIR)),"Research requiring an empty phial rejects a filled one");checks++;
        require(thaumcraft.port.aspect.AspectTags.forItem(EssentiaItem.phial(Aspect.FIRE)).equals(Map.of(Aspect.FIRE,10)),"Filled phial exposes its ten original contained aspects");checks++;
        var smelter=new SmelterEntity(BlockPos.ZERO,EssentiaContent.SMELTERS.get("smelter_basic").get().defaultBlockState());
        smelter.setItem(0,new ItemStack(Items.STONE,23));smelter.setItem(1,new ItemStack(Items.COAL,3));smelter.burn=1000;smelter.fuelTime=1600;smelter.cook=19;smelter.smeltTime=50;smelter.boost=true;smelter.store.restore(Map.of(Aspect.AIR,97,Aspect.FLUX,11));
        var restoredSmelter=new SmelterEntity(BlockPos.ZERO,smelter.getBlockState());restoredSmelter.loadCustomOnly(smelter.saveCustomOnly(registries),registries);
        require(restoredSmelter.burn==1000&&restoredSmelter.fuelTime==1600&&restoredSmelter.cook==19&&restoredSmelter.smeltTime==50&&restoredSmelter.boost&&restoredSmelter.store.total()==108&&restoredSmelter.getItem(0).getCount()==23&&restoredSmelter.getItem(1).getCount()==3,"Smelter preserves inventory, timers, boost and every aspect");checks++;
        // Datagen does not perform the in-game fuel data-map reload. Validate the actual codec instead.
        boolean foundFuel=false;
        for(var resource:resources.getResourceStack(ResourceLocation.parse("neoforge:data_maps/item/furnace_fuels.json")))try(var in=resource.openAsReader()){
            var fuel=JsonParser.parseReader(in).getAsJsonObject().getAsJsonObject("values").get("thaumcraft:alumentum");
            if(fuel!=null){require(net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel.CODEC.parse(ops,fuel).getOrThrow().burnTime()==4800,"Original alumentum burn time via NeoForge fuel data-map codec");foundFuel=true;}
        }
        require(foundFuel,"Alumentum fuel data-map found in resource stack");checks++;
        var labelRecipe=new thaumcraft.port.crafting.LabelRecipe(CraftingBookCategory.MISC);
        var labelInput=CraftingInput.of(1,2,List.of(new ItemStack(EssentiaContent.LABEL.get()),EssentiaItem.phial(Aspect.FIRE)));
        require(labelRecipe.matches(labelInput,null)&&EssentiaItem.aspect(labelRecipe.assemble(labelInput,registries))==Aspect.FIRE&&EssentiaItem.aspect(labelRecipe.getRemainingItems(labelInput).get(1))==Aspect.FIRE,"Aspect label copies, retaining the original filled phial");checks++;
        var clearInput=CraftingInput.of(1,1,List.of(labelRecipe.assemble(labelInput,registries)));
        require(labelRecipe.matches(clearInput,null)&&EssentiaItem.aspect(labelRecipe.assemble(clearInput,registries))==null,"Clear a label without creating essentia");checks++;
        ArcaneRecipe filter=recipe(resources,ops,"tube_filter");var input=CraftingInput.of(1,2,List.of(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:filter"))),new ItemStack(EssentiaContent.TUBES.get("tube").get())));
        require(filter.shapeless()&&filter.matches(input,null),"Original shapeless arcane tube upgrade in reversed vertical layout");checks++;
        require(!filter.matches(CraftingInput.of(1,1,List.of(input.getItem(0))),null),"Shapeless recipe rejects missing ingredient");checks++;
        var pearlItem=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:primordial_pearl"));
        var advanced=recipe(resources,ops,"metal_alchemical_advanced");
        for(int damage=0;damage<8;damage++){
            var pearl=new ItemStack(pearlItem);pearl.setDamageValue(damage);
            var remainder=advanced.getRemainingItems(CraftingInput.of(1,1,List.of(pearl))).get(0);
            require(damage==7?remainder.isEmpty():remainder.is(pearlItem)&&remainder.getCount()==1&&remainder.getDamageValue()==damage+1,"Original eight-use pearl remainder, damage "+damage);checks++;
        }
        return checks;
    }
    private static ArcaneRecipe recipe(ResourceManager resources,RegistryOps<JsonElement> ops,String name)throws Exception{try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:recipe/"+name+".json")).openAsReader()){return (ArcaneRecipe)Recipe.CODEC.parse(ops,JsonParser.parseReader(in)).getOrThrow();}}
    private static void require(boolean valid,String message){if(!valid)throw new IllegalStateException(message);}
    private EssentiaFixtures(){}
}
