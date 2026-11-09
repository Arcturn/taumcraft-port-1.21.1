package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import thaumcraft.port.world.TaintSeed;

/** Original seed mesh, with child-only pulsation and the source's emergence transform. */
public final class TaintSeedModel extends HierarchicalModel<TaintSeed> {
    private final ModelPart root;
    private final ModelPart[] segments=new ModelPart[7],growth=new ModelPart[7];
    private float height,emergence;
    public TaintSeedModel(ModelPart root){
        this.root=root;var part=root.getChild("seed");
        for(int i=0;i<7;i++){growth[i]=part.getChild("growth"+i);part=growth[i].getChild("segment"+i);segments[i]=part;}
    }
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var parent=mesh.getRoot().addOrReplaceChild("seed",CubeListBuilder.create().texOffs(0,0).addBox(-4,-4,-4,8,8,8),PartPose.offset(0,12,0));
        for(int i=0;i<7;i++){
            var scaled=parent.addOrReplaceChild("growth"+i,CubeListBuilder.create(),PartPose.ZERO);
            int size=i<4?8:4,uv=i<4?16:i==4?48:56;
            parent=scaled.addOrReplaceChild("segment"+i,CubeListBuilder.create().texOffs(0,uv).addBox(-size/2f,-size/2f,-size/2f,size,size,size),PartPose.offset(0,i<=4?-8:-4,0));
        }
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public ModelPart root(){return root;}
    @Override public void setupAnim(TaintSeed seed,float swing,float amount,float age,float yaw,float pitch){
        height=seed.getBbHeight();emergence=Math.max(0,(height*10-seed.tickCount)/(height*10))*height;
        float hurt=seed.hurtTime/200f,fi=(.1f+(hurt>0?.0125f:-.0125f))*3;
        for(int i=0;i<7;i++){
            segments[i].xRot=.1f/fi*(float)Math.sin(age*.06f-i/2f)/5+hurt+seed.attackAnim;
            segments[i].zRot=.1f/fi*(float)Math.sin(age*.05f-i/2f)/5;
            float q=(float)Math.PI*i/7,scale=1.6f-(float)Math.sin(q),pulse=(float)Math.sin(age/12-q)*.33f;
            growth[i].xScale=growth[i].zScale=scale+pulse*pulse;growth[i].yScale=scale*.9f;
        }
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer vertices,int light,int overlay,int color){
        pose.pushPose();pose.translate(0,(height==3?.6:1.2)+emergence,0);pose.scale(height/2,height/2,height/2);root.render(pose,vertices,light,overlay,color);pose.popPose();
    }
}
