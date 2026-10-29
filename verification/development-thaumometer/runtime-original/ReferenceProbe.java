package tcprobe;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid="tc_reference_probe",name="TC Reference Probe",version="1",clientSideOnly=true)
public class ReferenceProbe {
    int ticks=0, joined=0, commandIndex=0;
    boolean started=false,ready=false,failed=false;
    static final Path ROOT=Paths.get(System.getProperty("tcprobe.root"));
    String[] setup={"/gamerule doDaylightCycle false","/time set 6000","/difficulty peaceful","/gamerule doMobSpawning false","/tp @p 0.5 4 0.5 0 0","/setblock 0 5 3 minecraft:stone","/setblock -1 5 3 minecraft:diamond_block","/setblock 1 5 3 minecraft:gold_block","/replaceitem entity @p slot.hotbar.0 thaumcraft:thaumometer 1","/replaceitem entity @p slot.hotbar.1 minecraft:diamond 1","/replaceitem entity @p slot.hotbar.2 minecraft:iron_ingot 1"};
    @Mod.EventHandler public void init(FMLInitializationEvent event){MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);System.out.println("TCPROBE_INIT");}
    static Object field(Object obj,String name)throws Exception {Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();while(c!=null){try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(obj instanceof Class?null:obj);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
    static void set(Object obj,String name,Object value)throws Exception {Class<?> c=obj.getClass();while(c!=null){try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(obj,value);return;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
    static Object call(Object obj,String name,Object...args)throws Exception {Class<?> c=obj instanceof Class?(Class<?>)obj:obj.getClass();while(c!=null){for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(obj instanceof Class?null:obj,args);}c=c.getSuperclass();}throw new NoSuchMethodException(name);}
    static Object minecraft()throws Exception{return call(Class.forName("net.minecraft.client.Minecraft"),"func_71410_x");}
    static void log(String text){System.out.println("TCPROBE "+text);try{Files.write(ROOT.resolve("probe-events.log"),(text+"\n").getBytes("UTF-8"),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){e.printStackTrace();}}
    static Object player(Object mc)throws Exception{return field(mc,"field_71439_g");}
    static void chat(Object p,String command)throws Exception{call(p,"func_71165_d",command);log("COMMAND "+command);}
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||failed)return;
        try{
            ticks++;Object mc=minecraft();Object p=player(mc);
            if(!started&&ticks>80){
                started=true;Class<?> gt=Class.forName("net.minecraft.world.GameType"),wt=Class.forName("net.minecraft.world.WorldType"),ws=Class.forName("net.minecraft.world.WorldSettings");
                Object creative=Enum.valueOf((Class)gt,"CREATIVE"),flat=field(wt,"field_77138_c");
                Object settings=ws.getConstructor(long.class,gt,boolean.class,boolean.class,wt).newInstance(4369743L,creative,false,false,flat);
                call(settings,"func_77166_b");log("CREATE_DISPOSABLE_WORLD");call(mc,"func_71371_a","ThaumometerSoundReference","Thaumometer Sound Reference",settings);
            }
            if(p==null)return;
            joined++;
            if(joined>=40&&joined%6==0&&commandIndex<setup.length)chat(p,setup[commandIndex++]);
            if(!ready&&commandIndex==setup.length&&joined>130){ready=true;set(field(p,"field_71071_by"),"field_70461_c",0);log("READY ticks="+ticks);Files.write(ROOT.resolve("ready"),Arrays.asList("ready"));snapshot(mc,"fresh");}
            Path command=ROOT.resolve("command.txt");
            if(ready&&Files.isRegularFile(command)){String s=new String(Files.readAllBytes(command),"UTF-8").trim();Files.delete(command);log("ACTION "+s);String[] parts=s.split(" ",2);
                if(parts[0].equals("chat"))chat(p,parts[1]);
                if(parts[0].equals("slot"))set(field(p,"field_71071_by"),"field_70461_c",Integer.valueOf(parts[1]));
                if(parts[0].equals("inventory")){Class<?> ep=Class.forName("net.minecraft.entity.player.EntityPlayer");Object gui=Class.forName("net.minecraft.client.gui.inventory.GuiInventory").getConstructor(ep).newInstance(p);call(mc,"func_147108_a",gui);}
                if(parts[0].equals("close"))call(mc,"func_147108_a",new Object[]{null});
                if(parts[0].equals("snapshot"))snapshot(mc,parts.length>1?parts[1]:"manual");
                if(parts[0].equals("quit")){call(mc,"func_71400_g");}
            }
        }catch(Throwable e){failed=true;e.printStackTrace();log("FAIL "+e);}
    }
    static void snapshot(Object mc,String label)throws Exception{
        Object p=player(mc),inv=field(p,"field_71071_by");List<?> items=(List<?>)field(inv,"field_70462_a");
        Object knowledge=call(Class.forName("thaumcraft.api.capabilities.ThaumcraftCapabilities"),"getKnowledge",p);
        log("SNAPSHOT "+label+" research="+call(knowledge,"getResearchList")+" item="+call(p,"func_184614_ca"));
        try{log("KEYS "+label+" leftShift="+call(Class.forName("org.lwjgl.input.Keyboard"),"isKeyDown",42)+" rightShift="+call(Class.forName("org.lwjgl.input.Keyboard"),"isKeyDown",54));}catch(Exception e){log("KEY_PROBE_ERROR "+e);}
        try{Object target=field(Class.forName("thaumcraft.client.lib.events.RenderEventHandler"),"thaumTarget");if(target!=null){Object ea=call(Class.forName("thaumcraft.api.aspects.AspectHelper"),"getEntityAspects",target);StringBuilder et=new StringBuilder();if(ea!=null)for(Object a:(Object[])call(ea,"getAspects"))et.append(call(a,"getTag")).append('=').append(call(ea,"getAmount",a)).append(',');log("ENTITY_ASPECTS "+target.getClass().getName()+" "+et);}}catch(Exception e){log("ENTITY_PROBE_ERROR "+e);}
        for(int i=0;i<3;i++){Object aspects=call(Class.forName("thaumcraft.common.lib.crafting.ThaumcraftCraftingManager"),"getObjectTags",items.get(i));StringBuilder out=new StringBuilder();if(aspects!=null){Object[] array=(Object[])call(aspects,"getAspects");for(Object a:array)out.append(call(a,"getTag")).append('=').append(call(aspects,"getAmount",a)).append(',');}log("ASPECTS slot="+i+" "+out);}
    }
    @SubscribeEvent public void sound(Event event){
        if(!event.getClass().getName().equals("net.minecraftforge.client.event.sound.PlaySoundEvent"))return;
        try{Object sound=call(event,"getSound");if(sound!=null){Object id=call(sound,"func_147650_b");if(id.toString().startsWith("thaumcraft:"))log("SOUND tick="+ticks+" id="+id+" rawVolume="+field(sound,"field_147662_b")+" rawPitch="+field(sound,"field_147663_c"));}}catch(Throwable e){log("SOUND_PROBE_ERROR "+e);}
    }
}
