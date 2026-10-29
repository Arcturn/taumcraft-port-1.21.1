package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;

/** Registered objects and vanilla furnace fields, without a server or simulated Minecraft API. */
public final class JarBellowsFixtures {
    public static int validate(ResourceManager resources,RegistryAccess registries,RegistryOps<JsonElement> ops)throws Exception{
        int checks=0;
        var jar=new JarEntity(BlockPos.ZERO,EssentiaContent.JAR.get().defaultBlockState());
        jar.tank.restore(Aspect.FIRE,80,Aspect.FIRE);jar.labelFacing=Direction.EAST;
        require(jar.takeRemote(Aspect.FIRE,10,true)==10&&jar.tank.amount()==80,"Remote extraction simulation leaves contents untouched");checks++;
        require(jar.addRemote(Aspect.FIRE,10,true)==10&&jar.tank.amount()==80,"Remote insertion simulation leaves contents untouched");checks++;
        require(jar.takeRemote(Aspect.WATER,10,false)==0&&jar.addRemote(Aspect.WATER,10,false)==0,"Remote access respects aspect and label");checks++;
        require(jar.takeRemote(Aspect.FIRE,10,false)==10&&jar.addRemote(Aspect.FIRE,5,false)==5&&jar.tank.amount()==75,"Remote access commits exact amounts");checks++;
        require(jar.attachBrace()&&!jar.attachBrace()&&jar.blocked(),"One brace per jar");checks++;
        require(jar.takeRemote(Aspect.FIRE,10,false)==0&&jar.addRemote(Aspect.FIRE,10,false)==0&&jar.takeRemote(Aspect.FIRE,10,true)==0,"Brace blocks remote access, including simulation");checks++;
        require(jar.insert(Aspect.FIRE,1,Direction.UP,false)==1&&jar.extract(Aspect.FIRE,1,Direction.UP,false)==1&&jar.tank.amount()==75,"Brace keeps pipe access");checks++;
        var copy=new JarEntity(BlockPos.ZERO,jar.getBlockState());copy.loadCustomOnly(jar.saveCustomOnly(registries),registries);
        require(copy.braced&&copy.labelFacing==Direction.EAST&&copy.tank.amount()==75&&copy.tank.filter()==Aspect.FIRE,"World save keeps brace, label direction and contents");checks++;
        var portable=new ItemStack(EssentiaContent.JAR.get());jar.savePortable(portable,registries);var tag=portable.get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        require(!tag.contains("Braced")&&!tag.contains("LabelFacing")&&JarItem.contents(portable).amount()==75&&JarItem.contents(portable).filter()==Aspect.FIRE,"Portable jar keeps contents and filter without a duplicated brace");checks++;
        for(int invalid:new int[]{Integer.MIN_VALUE,-1,0,1,6,Integer.MAX_VALUE}){var bad=jar.saveCustomOnly(registries);bad.putInt("LabelFacing",invalid);copy.loadCustomOnly(bad,registries);require(copy.labelFacing==Direction.NORTH,"Malformed label direction defaults safely");checks++;}
        for(Direction d:Direction.Plane.HORIZONTAL){jar.labelFacing=d;copy.loadCustomOnly(jar.saveCustomOnly(registries),registries);require(copy.labelFacing==d,"All horizontal label directions persist");checks++;}
        var old=new CompoundTag();old.putString("Aspect","ignis");old.putInt("Amount",12);copy.loadCustomOnly(old,registries);
        require(!copy.braced&&copy.labelFacing==Direction.NORTH&&copy.tank.amount()==12,"Pre-alpha8 jar NBT stays compatible");checks++;
        var alembic=new JarEntity(BlockPos.ZERO,EssentiaContent.ALEMBIC.get().defaultBlockState());alembic.tank.restore(Aspect.FIRE,128,Aspect.FIRE);alembic.labelFacing=Direction.WEST;
        require(!alembic.attachBrace()&&alembic.blocked()&&alembic.takeRemote(Aspect.FIRE,10,false)==0,"Alembic never becomes a braced or remote jar");checks++;
        require(!alembic.output(Direction.WEST)&&!alembic.output(Direction.DOWN)&&alembic.output(Direction.UP)&&alembic.output(Direction.EAST),"Alembic label occupies exactly its side port");checks++;
        alembic.tank.filter(null);require(alembic.output(Direction.WEST),"Removing alembic label restores its output port");checks++;
        var stack=new ItemStack(EssentiaContent.JAR.get(),4);var filled=JarItem.filledCopy(stack,alembic);
        require(filled.getCount()==1&&JarItem.contents(filled).amount()==128&&stack.getCount()==4&&JarItem.contents(stack).amount()==0&&alembic.tank.amount()==128,"Preparing a fill copies one container without mutating inputs");checks++;
        jar.tank.restore(Aspect.FIRE,240,Aspect.FIRE);jar.savePortable(portable,registries);filled=JarItem.filledCopy(portable,alembic);
        require(JarItem.contents(filled).amount()==250&&JarItem.contents(filled).filter()==Aspect.FIRE,"Portable fill is limited by free capacity");checks++;
        require(JarItem.filledCopy(filled,alembic).isEmpty(),"Full held jar does not consume alembic essentia");checks++;
        var wrong=new CompoundTag();wrong.putString("Filter","aqua");stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(wrong));
        require(JarItem.filledCopy(stack,alembic).isEmpty(),"Empty labeled jar refuses a different aspect");checks++;
        var voidItem=new ItemStack(EssentiaContent.VOID_JAR.get());var almost=new CompoundTag();almost.putString("Aspect","ignis");almost.putInt("Amount",249);voidItem.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(almost));
        require(JarItem.contents(JarItem.filledCopy(voidItem,alembic)).amount()==250,"Portable void jar fills only its free space");checks++;
        require(JarItem.filledCopy(ItemStack.EMPTY,alembic).isEmpty()&&JarItem.filledCopy(portable,jar).isEmpty(),"Portable fill rejects empty stack and non-alembic source");checks++;
        var furnace=new FurnaceBlockEntity(BlockPos.ZERO,Blocks.FURNACE.defaultBlockState());furnace.litTime=40;furnace.cookingTotalTime=200;
        require(!FurnaceBellows.advance(furnace)&&furnace.cookingProgress==0,"Bellows cannot start an idle furnace recipe");checks++;
        furnace.cookingProgress=12;require(FurnaceBellows.advance(furnace)&&furnace.cookingProgress==13&&furnace.litTime==40&&furnace.getItem(2).isEmpty(),"Bellows advance progress without burning fuel or creating output");checks++;
        furnace.cookingProgress=199;require(!FurnaceBellows.advance(furnace)&&furnace.cookingProgress==199,"Vanilla performs recipe completion");checks++;
        furnace.cookingTotalTime=30;furnace.cookingProgress=28;require(FurnaceBellows.advance(furnace)&&furnace.cookingProgress==29&&!FurnaceBellows.advance(furnace),"Custom recipe time replaces legacy hardcoded 199");checks++;
        furnace.litTime=0;furnace.cookingProgress=10;require(!FurnaceBellows.advance(furnace)&&furnace.cookingProgress==10,"Extinguished furnace is not advanced");checks++;
        try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:recipe/jar_brace.json")).openAsReader()){
            var recipe=(ShapedRecipe)Recipe.CODEC.parse(ops,JsonParser.parseReader(in)).getOrThrow();var stacks=new ArrayList<ItemStack>();for(var ingredient:recipe.getIngredients())stacks.add(ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copy());var input=CraftingInput.of(3,3,stacks);var result=recipe.assemble(input,registries);
            require(recipe.matches(input,null)&&result.is(EssentiaContent.BRACE.get())&&result.getCount()==2,"Original shaped recipe produces two braces");checks++;
        }
        return checks;
    }
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private JarBellowsFixtures(){}
}
