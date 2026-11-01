package thaumcraft.port.infusion.recipe;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Persistent TC6 augment keys, retained inside the modern CustomData component. */
public final class InfusionAugments {
    private static final List<String> IDS=List.of("collector","destructive","burrowing","sounding","refining","arcing","essence","visbattery","vischarge","swift","agile","infested","lamplight");
    private static final int[] MAXIMUM={1,1,1,4,4,4,5,3,1,4,1,1,1};
    private static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    private static void update(ItemStack stack,Consumer<CompoundTag> action){var tag=data(stack);action.accept(tag);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));}
    public static int getRunic(ItemStack stack){return Math.max(0,data(stack).getInt("TC.RUNIC"));}
    public static void setRunic(ItemStack stack,int amount){update(stack,t->t.putByte("TC.RUNIC",(byte)Math.clamp(amount,0,127)));}
    public static int getWarp(ItemStack stack){return Math.max(0,data(stack).getInt("TC.WARP"));}
    public static void setWarp(ItemStack stack,int amount){update(stack,t->t.putByte("TC.WARP",(byte)Math.clamp(amount,0,127)));}
    public static int mask(ItemStack stack){var tag=data(stack);return tag.contains("mask",Tag.TAG_ANY_NUMERIC)?Math.clamp(tag.getInt("mask"),-1,2):-1;}
    public static void setMask(ItemStack stack,int value){update(stack,t->t.putInt("mask",Math.clamp(value,0,2)));}
    public static boolean goggles(ItemStack stack){return data(stack).getBoolean("goggles");}
    public static void setGoggles(ItemStack stack,boolean value){update(stack,t->t.putBoolean("goggles",value));}
    public static int type(ItemStack stack){return Math.clamp(data(stack).getInt("type"),0,2);}
    public static void setType(ItemStack stack,int value){update(stack,t->t.putByte("type",(byte)Math.clamp(value,0,2)));}
    public static int maxLevel(String id){int index=IDS.indexOf(id.toLowerCase(Locale.ROOT));return index<0?0:MAXIMUM[index];}
    public static Map<String,Integer> enchantments(ItemStack stack){
        var result=new LinkedHashMap<String,Integer>();
        for(var element:data(stack).getList("infench",Tag.TAG_COMPOUND)){
            var entry=(CompoundTag)element;int id=entry.getShort("id"),level=entry.getShort("lvl");
            if(id>=0&&id<IDS.size()&&level>0)result.merge(IDS.get(id),Math.min(MAXIMUM[id],level),Math::max);
        }
        return Collections.unmodifiableMap(result);
    }
    public static int getEnchantment(ItemStack stack,String id){return enchantments(stack).getOrDefault(id.toLowerCase(Locale.ROOT),0);}
    public static void setEnchantment(ItemStack stack,String id,int level){
        int index=IDS.indexOf(id.toLowerCase(Locale.ROOT));if(index<0||level<1||level>MAXIMUM[index])return;
        update(stack,tag->set(tag,index,level));
    }
    private static void set(CompoundTag tag,int index,int level){
        var list=tag.getList("infench",Tag.TAG_COMPOUND);
        for(var element:list){var entry=(CompoundTag)element;if(entry.getShort("id")==index){if(entry.getShort("lvl")<level)entry.putShort("lvl",(short)level);return;}}
        var entry=new CompoundTag();entry.putShort("id",(short)index);entry.putShort("lvl",(short)level);list.add(entry);tag.put("infench",list);
    }
    public static CustomData defaults(Map<String,Integer> levels){
        var tag=new CompoundTag();levels.forEach((id,level)->{int index=IDS.indexOf(id.toLowerCase(Locale.ROOT));if(index>=0&&level>0&&level<=MAXIMUM[index])set(tag,index,level);});return CustomData.of(tag);
    }
    private InfusionAugments(){}
}
