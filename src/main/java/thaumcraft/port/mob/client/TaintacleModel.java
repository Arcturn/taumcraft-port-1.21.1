package thaumcraft.port.mob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import thaumcraft.port.mob.Taintacle;

/** TC6 tapering hierarchy and its emerge, idle and attack motion. */
public final class TaintacleModel extends HierarchicalModel<Taintacle>{
    private final ModelPart root;
    private final ModelPart[] segments;
    private float scale=1,offset;
    public TaintacleModel(ModelPart root,int length){this.root=root;segments=new ModelPart[length-1];var parent=root.getChild("base");for(int i=0;i<segments.length;i++){segments[i]=parent.getChild("segment"+i);segments[i].xScale=segments[i].yScale=segments[i].zScale=.88f;parent=segments[i];}for(String name:new String[]{"orb","tip"}){var p=parent.getChild(name);p.xScale=p.yScale=p.zScale=.88f;}}
    @Override public ModelPart root(){return root;}
    public static LayerDefinition layer(int length){
        var mesh=new MeshDefinition();var parent=mesh.getRoot().addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.offset(0,12,0));
        // The original renderer scales before translating each child; compensate its pivot here.
        for(int i=0;i<length-1;i++)parent=parent.addOrReplaceChild("segment"+i,CubeListBuilder.create().texOffs(0,16).addBox(-4,-4,-4,8,8,8),PartPose.offset(0,-8*.88f,0));
        parent.addOrReplaceChild("orb",CubeListBuilder.create().texOffs(0,56).addBox(-2,-2,-2,4,4,4),PartPose.offset(0,-8*.88f,0));
        parent.addOrReplaceChild("tip",CubeListBuilder.create().texOffs(0,32).addBox(-6,-6,-6,12,12,12),PartPose.offset(0,-8*.88f,0));
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(Taintacle entity,float walk,float amount,float age,float yaw,float pitch){
        float flail=entity.flailIntensity,mod=.0625f*.2f,frequency=flail>1?3:1-mod,intensity=flail+(entity.hurtTime>0?mod:-mod);
        for(int i=0;i<segments.length;i++){segments[i].xRot=.15f*intensity*Mth.sin(age*.1f*frequency-i/2f);segments[i].zRot=.1f/intensity*Mth.sin(age*.15f-i/2f);}
        float height=entity.getBbHeight(),duration=height*10;scale=height/3;offset=(height==3?.6f:1.2f)+Math.max(0,(duration-entity.tickCount)/duration)*height;
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer out,int light,int overlay,int color){pose.pushPose();pose.translate(0,offset,0);pose.scale(scale,scale,scale);super.renderToBuffer(pose,out,light,overlay,color);pose.popPose();}
}
