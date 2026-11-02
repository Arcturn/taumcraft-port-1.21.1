package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.infusion.structure.*;

/** Uses the vanilla item entity's original ground transforms, fixed bob phase and one-item count. */
public final class PedestalRenderer<T extends DisplayPedestalEntity> implements BlockEntityRenderer<T> {
    public PedestalRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(T pedestal,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var game=Minecraft.getInstance();
        if(!pedestal.isEmpty() && pedestal.getLevel()!=null) {
            float ticks=(game.getCameraEntity()==null?pedestal.getLevel().getGameTime():game.getCameraEntity().tickCount)+partial;
            pose.pushPose();pose.translate(.5,.75,.5);
            float scale=pedestal instanceof RechargePedestalEntity?1.5f:1.25f;pose.scale(scale,scale,scale);pose.mulPose(Axis.YP.rotationDegrees(ticks%360));
            var item=pedestal.getItem(0).copyWithCount(1);
            var model=game.getItemRenderer().getModel(item,pedestal.getLevel(),null,0);
            // ItemEntityRenderer's exact age=0, bobOffs=0 transform, without creating
            // a randomized entity (bobOffs is final in 1.21.1).
            pose.translate(0,.1f+.25f*model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y(),0);
            game.getItemRenderer().renderStatic(item,ItemDisplayContext.GROUND,light,overlay,pose,buffers,pedestal.getLevel(),0);
            pose.popPose();
        }
        var shield=StructureClient.SHIELDS.get(pedestal);
        if(shield!=null && pedestal.getLevel()!=null) {
            float progress=(pedestal.getLevel().getGameTime()-shield.start()+partial)/shield.duration();
            if(progress>=1)StructureClient.SHIELDS.remove(pedestal);
            else if(progress>=0)renderShield(pose,buffers,progress);
        }
    }
    @Override public AABB getRenderBoundingBox(T pedestal) { return pedestal instanceof RechargePedestalEntity ? new AABB(pedestal.getBlockPos()).inflate(2) : new AABB(pedestal.getBlockPos()).expandTowards(0,1,0); }
    private static void renderShield(PoseStack pose,MultiBufferSource buffers,float progress) {
        int frame=Math.min(14,(int)(14*progress));float alpha=Math.min(1,(1-progress)*3);
        pose.pushPose();pose.translate(.5,1,.5);pose.mulPose(Axis.YP.rotationDegrees(180));pose.mulPose(Axis.XP.rotationDegrees(-90));pose.scale(.4f,.4f,.4f);
        var out=buffers.getBuffer(ShieldTypes.FRAMES[frame]);
        for(float[] vertex:Hemisphere.VERTICES)out.addVertex(pose.last().pose(),vertex[0],vertex[1],vertex[2]).setColor(.65f,.1f,.5f,alpha).setUv(vertex[3],vertex[4]).setLight(220);
        pose.popPose();
    }
    /** The actual BETA26 160-triangle hemisphere, rather than a substitute sprite or cube. */
    private static final class Hemisphere {
        static final List<float[]> VERTICES=load();
        static List<float[]> load() {
            var positions=new ArrayList<float[]>();var texture=new ArrayList<float[]>();var vertices=new ArrayList<float[]>();
            try(var stream=PedestalRenderer.class.getResourceAsStream("/assets/thaumcraft/models/infusion/hemis.obj")) {
                if(stream==null)throw new IOException("Missing original shield mesh");
                try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
                    String line;while((line=reader.readLine())!=null) {
                        String[] fields=line.trim().split("\\s+");
                        if(fields[0].equals("v"))positions.add(new float[]{Float.parseFloat(fields[1]),Float.parseFloat(fields[2]),Float.parseFloat(fields[3])});
                        else if(fields[0].equals("vt"))texture.add(new float[]{Float.parseFloat(fields[1]),1-Float.parseFloat(fields[2])});
                        else if(fields[0].equals("f"))for(int i=1;i<=4;i++) {
                            String[] index=fields[Math.min(i,fields.length-1)].split("/");var xyz=positions.get(Integer.parseInt(index[0])-1);var uv=texture.get(Integer.parseInt(index[1])-1);
                            vertices.add(new float[]{xyz[0],xyz[1],xyz[2],uv[0],uv[1]});
                        }
                    }
                }
            } catch(IOException exception) { throw new IllegalStateException("Could not load TC6 pedestal shield",exception); }
            return List.copyOf(vertices);
        }
    }
    private static final class ShieldTypes extends RenderType {
        static final RenderType[] FRAMES=new RenderType[15];
        static { for(int i=0;i<15;i++)FRAMES[i]=create("thaumcraft_pedestal_shield_"+i,DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,8192,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(SalisMundusParticle::legacyShader))
                .setTextureState(new TextureStateShard(ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/models/hemis"+(i+1)+".png"),true,false))
                // FXShieldRunes uses SRC_ALPHA, ONE and the particle pipeline's
                // 1/255 alpha cutoff, with explicit packed light 220.
                .setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setWriteMaskState(COLOR_WRITE).createCompositeState(false)); }
        ShieldTypes(String name,VertexFormat format,VertexFormat.Mode mode,int size,boolean crumbling,boolean sorted,Runnable begin,Runnable end){super(name,format,mode,size,crumbling,sorted,begin,end);}
    }
}
