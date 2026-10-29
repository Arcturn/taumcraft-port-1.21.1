package thaumcraft.port.data;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.RenderShape;
import org.joml.Vector4f;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import thaumcraft.port.client.CrystalModel;
import thaumcraft.port.machine.*;
import thaumcraft.port.world.CrystalClusterBlock;
import thaumcraft.port.world.WorldContent;

/** CPU checks using Minecraft's actual model transforms, not a guessed rotation convention. */
public final class PresentationFixtures {
    public static int validate(ResourceManager resources)throws Exception{
        int count=0;
        for(String name:List.of("tube","tube_filter","tube_restrict","tube_oneway","tube_valve","tube_buffer")){
            JsonObject json;try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:blockstates/"+name+".json")).openAsReader()){json=JsonParser.parseReader(in).getAsJsonObject();}
            var checked=EnumSet.noneOf(Direction.class);
            for(var entry:json.getAsJsonArray("multipart")){
                var part=entry.getAsJsonObject();if(!part.has("when"))continue;var when=part.getAsJsonObject("when");
                for(Direction direction:Direction.values())if(when.has(direction.getName())){
                    var apply=part.getAsJsonObject("apply");int x=apply.has("x")?apply.get("x").getAsInt():0,y=apply.has("y")?apply.get("y").getAsInt():0;
                    var transform=BlockModelRotation.by(x,y).getRotation().blockCenterToCorner();var end=new Vector4f(.5f,0,.5f,1);transform.transformPosition(end);
                    var expected=new Vector4f(.5f+.5f*direction.getStepX(),.5f+.5f*direction.getStepY(),.5f+.5f*direction.getStepZ(),1);
                    require(end.distance(expected)<.00001,"Tube "+name+" points to wrong side: "+direction);checked.add(direction);count++;
                }
            }
            require(checked.size()==6,"Every tube exposes six independently checked model directions");count++;
        }
        String obj;try(var in=resources.getResourceOrThrow(ResourceLocation.parse("thaumcraft:models/obj/crystal.obj")).openAsReader()){obj=new java.io.BufferedReader(in).lines().collect(java.util.stream.Collectors.joining("\n"));}
        var groups=obj.lines().filter(l->l.startsWith("g ")).map(l->l.substring(2).trim()).toList();
        require(groups.equals(CrystalModel.PARTS)&&obj.lines().filter(l->l.startsWith("f ")).count()==96,"Original OBJ has eight twelve-face shards");count++;
        for(Direction face:Direction.values()){
            for(long seed:new long[]{0,1,123456789,Long.MIN_VALUE}){
                var grown=CrystalModel.selection(seed,face,3);
                require(new HashSet<>(grown).size()==4&&grown.stream().allMatch(i->i>=0&&i<8),"Four different original shards at full growth");count++;
                for(int size=0;size<3;size++){require(CrystalModel.selection(seed,face,size).equals(grown.subList(0,size+1)),"Growing a crystal preserves existing shards");count++;}
            }
        }
        for(var block:MachineContent.NITOR.values()){
            require(block.get().defaultBlockState().getRenderShape()==RenderShape.INVISIBLE&&block.get().newBlockEntity(BlockPos.ZERO,block.get().defaultBlockState()) instanceof NitorEntity,"Nitor uses a ticking light effect, not crossed block planes");count++;
        }
        return count+validateCrystalLighting();
    }
    private static int validateCrystalLighting(){
        int count=0;
        for(var crystal:WorldContent.CRYSTALS.values())for(int size=0;size<4;size++){
            var state=crystal.get().defaultBlockState().setValue(CrystalClusterBlock.SIZE,size);
            require(state.getLightEmission()==1,"Crystal self-light must not turn its surroundings into a torch");count++;
        }
        var sourceConsumer=new QuadBakingVertexConsumer();
        sourceConsumer.setTintIndex(0);sourceConsumer.setDirection(Direction.UP);
        sourceConsumer.setShade(true);sourceConsumer.setHasAmbientOcclusion(true);
        int[] bakedLight={0,16,224,240|(240<<16)};
        for(int vertex=0;vertex<4;vertex++)sourceConsumer.addVertex(vertex%2,.25f,vertex/2)
                .setColor(255,255,255,255).setUv(vertex%2,vertex/2).setUv2(bakedLight[vertex]&0xffff,bakedLight[vertex]>>>16).setNormal(0,1,0);
        var source=sourceConsumer.bakeQuad();int[] original=source.getVertices().clone();
        var crystal=CrystalModel.lightWorldQuad(source);int[] lit=crystal.getVertices();
        require(Arrays.equals(original,source.getVertices()),"Lighting must not mutate shared item/source quads");count++;
        require(!crystal.isShade()&&!crystal.hasAmbientOcclusion(),"Crystal faces retain TC6's unshaded, flat lighting");count++;
        require(crystal.getTintIndex()==0&&crystal.getDirection()==source.getDirection()&&crystal.getSprite()==source.getSprite(),"Crystal tint, face and texture survive lighting adaptation");count++;
        for(int vertex=0;vertex<4;vertex++){
            int offset=vertex*IQuadTransformer.STRIDE+IQuadTransformer.UV2;
            require(lit[offset]==((bakedLight[vertex]&0xffff0000)|Math.max(bakedLight[vertex]&0xffff,180)),"Crystal light floor preserves brighter block/sky light");count++;
        }
        int[] geometry=lit.clone();for(int vertex=0;vertex<4;vertex++){int offset=vertex*IQuadTransformer.STRIDE+IQuadTransformer.UV2;geometry[offset]=original[offset];}
        require(Arrays.equals(geometry,original),"Crystal geometry, UV, colour and normals remain unchanged");count++;
        // Exercise Minecraft/NeoForge's actual vertex consumer, including the merge with world light.
        for(int worldLight:new int[]{0,16,176,192,240,240<<16,240|(240<<16)}){
            var output=new QuadBakingVertexConsumer();
            output.putBulkData(new PoseStack().last(),crystal,1,1,1,1,worldLight,0,true);
            int[] rendered=output.bakeQuad().getVertices();
            for(int vertex=0;vertex<4;vertex++){
                int expected=Math.max(Math.max(bakedLight[vertex]&0xffff,180),worldLight&0xffff)
                        |(Math.max(bakedLight[vertex]>>>16,worldLight>>>16)<<16);
                require(rendered[vertex*IQuadTransformer.STRIDE+IQuadTransformer.UV2]==expected,"Actual vertex pipeline preserves crystal glow in darkness and brighter daylight");count++;
            }
        }
        return count;
    }
    private static void require(boolean result,String message){if(!result)throw new IllegalStateException(message);}
    private PresentationFixtures(){}
}
