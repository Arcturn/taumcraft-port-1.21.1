package thaumcraft.port.infusion.armor.client;
import java.util.*;
import java.io.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.port.infusion.armor.*;

/** TC6 ore-vein sonar and animated hemispherical runic shields using original assets. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class InfusionEquipmentFx {
    private record Sonar(Vec3 point,int color,long start){}
    private record Slash(Vec3 from,Vec3 to,float angle,long start){}
    private static final List<Slash> SLASHES=new ArrayList<>();
    private record Shield(int entity,int attacker,long start,int duration){}
    private static final List<Sonar> SONAR=new ArrayList<>();private static final List<Shield> SHIELDS=new ArrayList<>();
    private static Object lastLevel;private static List<float[]> hemisphere;
    public static void receive(InfusionFxPayload packet){
        var game=Minecraft.getInstance();if(game.level==null)return;clearOnChange(game.level);
        if(packet.kind()==0){scan(packet.pos(),Math.clamp(packet.source(),1,4));return;}
        var source=game.level.getEntity(packet.source());if(source==null)return;
        if(packet.kind()==1)SHIELDS.add(new Shield(source.getId(),packet.target(),game.level.getGameTime(),8+game.level.random.nextInt(4)));
        else if(packet.kind()==2){var target=game.level.getEntity(packet.target());if(target!=null)SLASHES.add(new Slash(source.getBoundingBox().getCenter(),target.getBoundingBox().getCenter(),(float)game.level.random.nextGaussian()*20,game.level.getGameTime()));}
    }
    private static void clearOnChange(Object level){if(lastLevel!=level){SONAR.clear();SHIELDS.clear();SLASHES.clear();lastLevel=level;}}
    private static void scan(BlockPos origin,int rank){
        var level=Minecraft.getInstance().level;int range=4+rank*4;var ores=new LinkedHashSet<BlockPos>();
        for(var pos:BlockPos.betweenClosed(origin.offset(-range,-range,-range),origin.offset(range,range,range)))if(level.hasChunkAt(pos)&&InfusionEnchantments.ore(level.getBlockState(pos)))ores.add(pos.immutable());
        while(!ores.isEmpty()){
            var first=ores.iterator().next();var state=level.getBlockState(first);var queue=new ArrayDeque<BlockPos>();queue.add(first);ores.remove(first);Vec3 sum=Vec3.ZERO;int count=0;
            while(!queue.isEmpty()){var pos=queue.removeFirst();sum=sum.add(Vec3.atCenterOf(pos));count++;for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){var next=pos.offset(x,y,z);if(ores.contains(next)&&level.getBlockState(next).equals(state)){ores.remove(next);queue.add(next);}}}
            Vec3 center=sum.scale(1d/count);SONAR.add(new Sonar(center,oreColor(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()),level.getGameTime()+(long)(Math.sqrt(origin.distToCenterSqr(center))*3)));
        }
    }
    private static int oreColor(String path){for(var e:COLORS.entrySet())if(path.contains(e.getKey()))return e.getValue();return 0xffffff;}
    private static final Map<String,Integer> COLORS=Map.ofEntries(Map.entry("quartz",0xE5DED5),Map.entry("iron",14200723),Map.entry("lapis",1328572),Map.entry("gold",16576075),Map.entry("diamond",6155509),Map.entry("emerald",1564002),Map.entry("redstone",0xff0000),Map.entry("coal",0x101010),Map.entry("silver",14342653),Map.entry("tin",0xEFEFFB),Map.entry("copper",16620629),Map.entry("amber",16626469),Map.entry("cinnabar",10159368));
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var game=Minecraft.getInstance();if(game.level==null){clearOnChange(null);return;}clearOnChange(game.level);
        long now=game.level.getGameTime();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);var buffers=game.renderBuffers().bufferSource();var camera=event.getCamera().getPosition();
        SLASHES.removeIf(p->now-p.start>8);SONAR.removeIf(p->now-p.start>44);SHIELDS.removeIf(p->now-p.start>p.duration||game.level.getEntity(p.entity)==null);
        for(var sonar:SONAR){float age=now-sonar.start+partial;if(age<0)continue;float t=age/44,alpha=t<1f/3?t*3:t<2f/3?1-(t-1f/3)*.6f:(1-t)*2.4f;
            var pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());pose.translate(sonar.point.x-camera.x,sonar.point.y-camera.y,sonar.point.z-camera.z);pose.mulPose(event.getCamera().rotation());
            int frame=240+(int)age%15;float u0=(frame%16)/16f,v0=(frame/16)/16f;var consumer=buffers.getBuffer(sonar.color==0x101010?InfusionFxTypes.SOUNDING_DARK:InfusionFxTypes.SOUNDING);
            quad(consumer,pose,.9f,u0,v0,u0+1/16f,v0+1/16f,sonar.color,alpha);
        }
        buffers.endBatch(InfusionFxTypes.SOUNDING);buffers.endBatch(InfusionFxTypes.SOUNDING_DARK);
        for(var slash:SLASHES){float age=now-slash.start+partial,t=age/8;Vec3 anchor=slash.from.lerp(slash.to,t).subtract(camera),delta=slash.to.subtract(slash.from);float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90,pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));
            var pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());pose.translate(anchor.x,anchor.y,anchor.z);pose.mulPose(Axis.YP.rotationDegrees(-yaw+90));pose.mulPose(Axis.ZP.rotationDegrees(pitch+90));pose.mulPose(Axis.YP.rotationDegrees(slash.angle));
            float u=(22+Math.round(t*8))/32f;quad(buffers.getBuffer(InfusionFxTypes.SLASH),pose,.5f*(.5f+t),u,10/32f,u+1/32f,11/32f,0xffffff,(age<=1.6f?age/1.6f:(8-age)/8)*.5f);
        }buffers.endBatch(InfusionFxTypes.SLASH);
        for(var shield:SHIELDS){var entity=game.level.getEntity(shield.entity);var target=game.level.getEntity(shield.attacker);float fade=(now-shield.start+partial)/shield.duration;var texture=InfusionFxTypes.SHIELD[Math.clamp((int)(14*fade),0,14)];
            var pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());Vec3 p=entity.getPosition(partial).add(0,entity.getBbHeight()/2,0).subtract(camera);pose.translate(p.x,p.y,p.z);
            if(target!=null){var delta=entity.getBoundingBox().getCenter().subtract(target.getBoundingBox().getCenter());float yaw=(float)Math.toDegrees(Math.atan2(delta.z,delta.x))-90;float pitch=(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z)));pose.mulPose(Axis.YP.rotationDegrees(180-yaw));pose.mulPose(Axis.XP.rotationDegrees(-pitch));}
            else pose.mulPose(Axis.XP.rotationDegrees(shield.attacker==-2?-270:-90));float scale=.2f*entity.getBbHeight();pose.scale(scale,scale,scale);var consumer=buffers.getBuffer(texture);drawHemisphere(consumer,pose,Math.min(1,(1-fade)*3));if(shield.attacker==-1){pose.mulPose(Axis.XP.rotationDegrees(180));drawHemisphere(consumer,pose,Math.min(1,(1-fade)*3));}buffers.endBatch(texture);
        }
    }
    private static void quad(VertexConsumer out,PoseStack pose,float size,float u0,float v0,float u1,float v1,int color,float alpha){vertex(out,pose,-size,-size,0,u0,v1,color,alpha);vertex(out,pose,size,-size,0,u1,v1,color,alpha);vertex(out,pose,size,size,0,u1,v0,color,alpha);vertex(out,pose,-size,size,0,u0,v0,color,alpha);}
    private static void vertex(VertexConsumer out,PoseStack pose,float x,float y,float z,float u,float v,int color,float alpha){out.addVertex(pose.last().pose(),x,y,z).setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose.last(),0,0,1);}
    private static void drawHemisphere(VertexConsumer out,PoseStack pose,float alpha){if(hemisphere==null)hemisphere=readHemisphere();for(var v:hemisphere)vertex(out,pose,v[0],v[1],v[2],v[3],v[4],0xffffff,alpha);}
    private static List<float[]> readHemisphere(){var vertices=new ArrayList<float[]>();var uv=new ArrayList<float[]>();var result=new ArrayList<float[]>();try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.parse("thaumcraft:models/obj/hemis.obj"))){String line;while((line=reader.readLine())!=null){var words=line.trim().split("\\s+");if(words[0].equals("v"))vertices.add(new float[]{Float.parseFloat(words[1]),Float.parseFloat(words[2]),Float.parseFloat(words[3])});else if(words[0].equals("vt"))uv.add(new float[]{Float.parseFloat(words[1]),1-Float.parseFloat(words[2])});else if(words[0].equals("f"))for(int i=2;i<words.length-1;i++)for(String w:new String[]{words[1],words[i],words[i+1]}){var ids=w.split("/");var v=vertices.get(Integer.parseInt(ids[0])-1);var t=uv.get(Integer.parseInt(ids[1])-1);result.add(new float[]{v[0],v[1],v[2],t[0],t[1]});}}}catch(IOException error){com.mojang.logging.LogUtils.getLogger().warn("Cannot load original runic shield geometry",error);}return result;}
}
