package thaumcraft.port.verification;

import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.opengl.GL11;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.client.ClientScanner;
import thaumcraft.port.client.SalisMundusParticle;
import thaumcraft.port.client.ScanRuneParticle;
import thaumcraft.port.content.PortContent;

/** Disposable client probe: exercises real item use, particles, sound events and world rendering. */
public final class ThaumometerRuntimeProbe {
    private static final String[] PHASES={"unscanned_pig_billboard_and_highlights","block_scan","repeat_block_scan","miss_scan","offhand_block_scan","put_away_clears_label"};
    private final Path output=Path.of(System.getProperty("thaumcraft.probe.output","tooltip-probe")).resolve("scanner-runtime");
    private final Gson gson=new Gson();
    private final List<Object> results=new ArrayList<>();
    private final List<Map<String,Object>> sounds=new ArrayList<>();
    private volatile boolean prepared,handChanged;
    private boolean setup,used,capture,finished;
    private int ticks,phase,nextTick,useTick,pigId=-1;
    private LinkedHashMap<String,Object> current;

    public static void start(){new ThaumometerRuntimeProbe();}
    private ThaumometerRuntimeProbe(){NeoForge.EVENT_BUS.register(this);}

    @SubscribeEvent public void sound(PlaySoundEvent event){
        var sound=event.getOriginalSound();
        if(!sound.getLocation().toString().equals("thaumcraft:scan"))return;
        var value=new LinkedHashMap<String,Object>();
        value.put("phase",phase<PHASES.length?PHASES[phase]:"done");value.put("tick",ticks);
        value.put("event",sound.getLocation().toString());value.put("volume",rawSoundValue(sound,"volume"));
        value.put("pitch",rawSoundValue(sound,"pitch"));value.put("source",sound.getSource().getName());
        value.put("scope","raw request volume/pitch before PlaySoundEvent resolves the sound; original scan sound has unit multipliers");
        sounds.add(value);System.out.println("THAUMOMETER_SOUND "+gson.toJson(value));
    }

    @SubscribeEvent public void tick(ClientTickEvent.Post event){
        if(finished)return;ticks++;
        var mc=Minecraft.getInstance();
        try{
            if(ticks>1800)throw new IllegalStateException("Scanner runtime timeout");
            if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null)return;
            if(!setup){
                setup=true;mc.player.closeContainer();mc.setScreen(null);mc.mouseHandler.grabMouse();
                mc.options.guiScale().set(2);mc.resizeDisplay();
                var id=mc.player.getUUID();var server=mc.getSingleplayerServer();
                server.execute(()->{
                    var player=server.getPlayerList().getPlayer(id);var level=player.serverLevel();level.setDayTime(6000);
                    for(int x=-10;x<=10;x++)for(int z=-5;z<=12;z++)level.setBlock(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState(),3);
                    for(int x=-3;x<=3;x++)for(int y=81;y<=85;y++)level.setBlock(new BlockPos(x,y,8),Blocks.STONE.defaultBlockState(),3);
                    level.setBlock(new BlockPos(2,82,3),Blocks.STONE.defaultBlockState(),3);
                    player.teleportTo(.5,81,.5);player.setYRot(0);player.setXRot(10);
                    player.getInventory().clearContent();player.getInventory().selected=0;
                    player.getInventory().setItem(0,new ItemStack(PortContent.THAUMOMETER.get()));
                    player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();
                    var pig=EntityType.PIG.create(level);pig.moveTo(.5,81,5.5,180,0);pig.setNoAi(true);pig.setInvulnerable(true);pig.setPersistenceRequired();level.addFreshEntity(pig);pigId=pig.getId();prepared=true;
                });
                nextTick=ticks+100;return;
            }
            if(!prepared||ticks<nextTick)return;
            if(phase>=PHASES.length){finish();return;}
            aim(phase==0?0:phase==3?0:-33.69f,phase==0?10:phase==3?-90:1.9f);
            if(current==null){
                current=new LinkedHashMap<>();current.put("case",PHASES[phase]);current.put("start_tick",ticks);
                if(phase==4||phase==5){
                    handChanged=false;var id=mc.player.getUUID();var server=mc.getSingleplayerServer();
                    server.execute(()->{
                        var player=server.getPlayerList().getPlayer(id);
                        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                        player.setItemInHand(InteractionHand.OFF_HAND,phase==4?new ItemStack(PortContent.THAUMOMETER.get()):ItemStack.EMPTY);
                        player.inventoryMenu.broadcastChanges();handChanged=true;
                    });
                    nextTick=ticks+30;return;
                }
                nextTick=ticks+20;return;
            }
            if((phase==4||phase==5)&&!handChanged)return;
            if(!used){
                used=true;useTick=ticks;
                if(phase==0){
                    var pig=mc.level.getEntity(pigId);
                    if(pig==null)throw new IllegalStateException("Pig not synchronized");
                    current.put("scanned_subjects",ClientJournal.state.scanned().size());
                    current.put("aspects",AspectTags.forEntity(pig).toString());
                    current.put("client_readout_target",field(ClientScanner.class,null,"entityReadout")==pig);
                    current.put("tag_scale",field(Class.forName("thaumcraft.port.client.ScannerWorldAspects"),null,"tagScale"));
                    current.put("highlight_particles",particles().stream().filter(SalisMundusParticle.class::isInstance).count());
                    if(!ClientJournal.state.scanned().isEmpty()||field(ClientScanner.class,null,"entityReadout")!=pig||AspectTags.forEntity(pig).isEmpty())throw new IllegalStateException("Unscanned entity readout failed");
                }else if(phase==5){
                    if(field(ClientScanner.class,null,"entityReadout")!=null)throw new IllegalStateException("Stale label after removing scanner");
                    current.put("readout_cleared",true);
                }else{
                    int before=runecount(),soundBefore=sounds.size();
                    var use=Minecraft.class.getDeclaredMethod("startUseItem");use.setAccessible(true);use.invoke(mc);
                    int added=runecount()-before,audio=sounds.size()-soundBefore;
                    current.put("runes_added_immediately",added);current.put("sounds_added_immediately",audio);
                    current.put("swinging_immediately",mc.player.swinging);
                    if(mc.player.swinging)throw new IllegalStateException("Ordinary scanner use must not swing the arm (original 1.12.2 ordinary use only resets equipped progress)");
                    current.put("hit_result",mc.hitResult==null?"null":mc.hitResult.getType().name());
                    current.put("item_cooldown",mc.player.getCooldowns().isOnCooldown(PortContent.THAUMOMETER.get()));
                    current.put("sound",audio>0?sounds.get(sounds.size()-1):Map.of());
                    current.put("new_runes",runes());
                    int expected=phase==3?0:10;
                    if(added!=expected||audio!=1)throw new IllegalStateException("Expected "+expected+" runes and1 local scan sound: "+gson.toJson(current));
                    var sound=sounds.get(sounds.size()-1);
                    if(((Number)sound.get("volume")).floatValue()!=.5f||((Number)sound.get("pitch")).floatValue()!=1||!sound.get("source").equals("player"))throw new IllegalStateException("Scan audio mismatch "+sound);
                }
                nextTick=ticks+2;return;
            }
            capture=true;
        }catch(Throwable failure){fail(failure);}
    }

    private static float rawSoundValue(net.minecraft.client.resources.sounds.SoundInstance sound,String name){
        try{return ((Number)field(net.minecraft.client.resources.sounds.AbstractSoundInstance.class,sound,name)).floatValue();}
        catch(Exception failure){throw new IllegalStateException("Cannot inspect raw sound request",failure);}
    }
    private void aim(float yaw,float pitch){var player=Minecraft.getInstance().player;player.setYRot(yaw);player.yRotO=yaw;player.setXRot(pitch);player.xRotO=pitch;}
    private int runecount()throws Exception{return (int)particles().stream().filter(ScanRuneParticle.class::isInstance).count();}
    private List<Particle> particles()throws Exception{
        var engine=Minecraft.getInstance().particleEngine;Set<Particle> all=Collections.newSetFromMap(new IdentityHashMap<>());
        var map=(Map<?,?>)field(ParticleEngine.class,engine,"particles");
        for(var value:map.values())for(var p:(Collection<?>)value)all.add((Particle)p);
        for(var p:(Collection<?>)field(ParticleEngine.class,engine,"particlesToAdd"))all.add((Particle)p);
        return List.copyOf(all);
    }
    private List<Object> runes()throws Exception{
        var result=new ArrayList<Object>();
        for(var particle:particles())if(particle instanceof ScanRuneParticle){
            var entry=new LinkedHashMap<String,Object>();
            for(String n:List.of("age","lifetime","x","y","z","alpha"))entry.put(n,field(Particle.class,particle,n));
            result.add(entry);
        }return result;
    }
    private static Object field(Class<?> owner,Object value,String name)throws Exception{Field field=owner.getDeclaredField(name);field.setAccessible(true);return field.get(value);}

    @SubscribeEvent(priority=EventPriority.LOWEST)public void render(RenderGuiEvent.Post event){
        if(finished||!capture)return;
        try{
            event.getGuiGraphics().flush();int error=GL11.glGetError();current.put("gl_error",error);
            int afterTicks=ticks-useTick;
            if(phase>=1&&phase<=4){
                boolean swinging=Minecraft.getInstance().player.swinging;
                current.put("swinging_after_"+afterTicks+"_ticks",swinging);
                if(swinging)throw new IllegalStateException("Unexpected scanner arm swing at +"+afterTicks+" ticks");
            }
            if(error!=GL11.GL_NO_ERROR)throw new IllegalStateException("OpenGL error "+error);
            Files.createDirectories(output);var file=output.resolve(String.format("%02d-%s%s.png",phase+1,PHASES[phase],phase>=1&&phase<=4?String.format("-after-%02dt",afterTicks):""));
            try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(file);}
            if(phase>=1&&phase<=4&&afterTicks<6){
                current.put("screenshot_after_2_ticks",file.getFileName().toString());capture=false;nextTick=useTick+6;return;
            }
            current.put("screenshot",file.getFileName().toString());current.put("pass",true);results.add(current);
            System.out.println("THAUMOMETER_CASE "+gson.toJson(current));
            phase++;current=null;used=capture=false;nextTick=ticks+10;
        }catch(Throwable failure){fail(failure);}
    }
    private void finish()throws Exception{
        finished=true;Files.createDirectories(output);Files.writeString(output.resolve("thaumometer-runtime.json"),gson.toJson(Map.of("cases",results,"sounds",sounds,"pass",true))+"\n");
        System.out.println("THAUMOMETER_COMPLETE cases="+results.size()+" sounds="+sounds.size());Minecraft.getInstance().stop();
    }
    private void fail(Throwable failure){
        finished=true;failure.printStackTrace();
        try{Files.createDirectories(output);Files.writeString(output.resolve("failure.txt"),failure+"\n"+gson.toJson(current)+"\n");}catch(Exception ignored){}
        System.out.println("THAUMOMETER_FAILED "+failure);Minecraft.getInstance().stop();
    }
}
