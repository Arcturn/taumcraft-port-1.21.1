package thaumcraft.port.artifice.construct.client;

import java.io.*;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Original WavefrontObject face normals, flipped V and .0005 inward UV inset. */
final class ConstructObjModel {
    private record Corner(Vec3 point,float u,float v){}
    private record Face(List<Corner> corners,Vec3 normal){}
    private final Map<String,List<Face>> groups=new HashMap<>();
    ConstructObjModel(ResourceLocation resource){
        List<Vec3> vertices=new ArrayList<>();List<float[]> uv=new ArrayList<>();String group="default";
        try(var stream=Minecraft.getInstance().getResourceManager().openAsReader(resource)){
            String line;while((line=stream.readLine())!=null){var tokens=line.trim().split("\\s+");if(tokens.length<2)continue;
                switch(tokens[0]){
                    case "v"->vertices.add(new Vec3(Float.parseFloat(tokens[1]),Float.parseFloat(tokens[2]),Float.parseFloat(tokens[3])));
                    case "vt"->uv.add(new float[]{Float.parseFloat(tokens[1]),1-Float.parseFloat(tokens[2])});
                    case "g","o"->group=tokens[1];
                    case "f"->{
                        List<Corner> corners=new ArrayList<>();float u=0,v=0;
                        for(int i=1;i<tokens.length;i++){var index=tokens[i].split("/");var tex=uv.get(Integer.parseInt(index[1])-1);corners.add(new Corner(vertices.get(Integer.parseInt(index[0])-1),tex[0],tex[1]));u+=tex[0];v+=tex[1];}
                        u/=corners.size();v/=corners.size();List<Corner> inset=new ArrayList<>();for(var corner:corners)inset.add(new Corner(corner.point,corner.u+(corner.u>u?-.0005f:.0005f),corner.v+(corner.v>v?-.0005f:.0005f)));
                        Vec3 normal=corners.get(1).point.subtract(corners.getFirst().point).cross(corners.get(2).point.subtract(corners.getFirst().point)).normalize();groups.computeIfAbsent(group,k->new ArrayList<>()).add(new Face(inset,normal));
                    }
                    default->{}
                }
            }
        }catch(IOException|RuntimeException exception){throw new IllegalStateException("Cannot load source construct model "+resource,exception);}
    }
    void render(String group,PoseStack pose,VertexConsumer out,int light,int color){
        for(var face:groups.getOrDefault(group,List.of()))for(int triangle=1;triangle<face.corners.size()-1;triangle++){
            for(int index:new int[]{0,triangle,triangle+1,triangle+1}){
                var corner=face.corners.get(index);var p=corner.point;var n=face.normal;
                out.addVertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(corner.u,corner.v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);
            }
        }
    }
}
