package thaumcraft.port.client.artifice;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.artifice.tool.GrappleEntity;

/** Source 64x32 grappler mesh, animated glow and four-sided textured rope. */
public final class GrappleRenderer extends EntityRenderer<GrappleEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/entity/grappler.png"),ROPE=ResourceLocation.parse("thaumcraft:textures/misc/rope.png");
    private final ModelPart model;
    public GrappleRenderer(EntityRendererProvider.Context context){super(context);model=mesh().bakeRoot();shadowRadius=0;}
    private static LayerDefinition mesh(){var mesh=new MeshDefinition();var root=mesh.getRoot();root.addOrReplaceChild("core",CubeListBuilder.create().texOffs(0,0).addBox(-1.5f,-1.5f,-1.5f,3,3,3),PartPose.ZERO);
        root.addOrReplaceChild("prong1",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.ZERO);
        root.addOrReplaceChild("prong2",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.rotation(0,Mth.HALF_PI,0));
        root.addOrReplaceChild("prong3",CubeListBuilder.create().texOffs(0,10).addBox(-.5f,-.5f,-2.5f,1,1,5),PartPose.rotation(Mth.HALF_PI,Mth.HALF_PI,0));return LayerDefinition.create(mesh,64,32);}
    @Override public ResourceLocation getTextureLocation(GrappleEntity entity){return TEXTURE;}
    @Override public void render(GrappleEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(partial,entity.yRotO,entity.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,entity.xRotO,entity.getXRot())));
        model.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,OverlayTexture.NO_OVERLAY);pose.popPose();
        glow(entity,partial,pose,buffers);
        if(entity.getOwner() instanceof LivingEntity owner)rope(entity,owner,partial,pose,buffers,light);
        super.render(entity,yaw,partial,pose,buffers,light);
    }
    private void glow(GrappleEntity entity,float partial,PoseStack pose,MultiBufferSource buffers){
        pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());float scale=1+Mth.sin(entity.tickCount/5f)*.2f+.2f;pose.scale(scale,scale,scale);
        float u=(1+entity.tickCount%6)/32f,v=7/32f;var out=buffers.getBuffer(GrappleGlow.TYPE);
        Vec3 normal=new Vec3(0,0,1);
        vertex(out,pose,new Vec3(-.5,-.5,0),u,v+1/32f,normal,15728880,0x35ffffff);
        vertex(out,pose,new Vec3(.5,-.5,0),u+1/32f,v+1/32f,normal,15728880,0x35ffffff);
        vertex(out,pose,new Vec3(.5,.5,0),u+1/32f,v,normal,15728880,0x35ffffff);
        vertex(out,pose,new Vec3(-.5,.5,0),u,v,normal,15728880,0x35ffffff);pose.popPose();
    }
    private void rope(GrappleEntity grapple,LivingEntity owner,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        Vec3 start=grapple.getPosition(partial),end=owner.getPosition(partial);var mc=Minecraft.getInstance();
        int side=(grapple.hand()==InteractionHand.MAIN_HAND?1:-1)*(owner.getMainArm()==HumanoidArm.RIGHT?1:-1);
        if(owner==mc.player&&mc.options.getCameraType().isFirstPerson()){
            double angle=Math.toRadians(Mth.rotLerp(partial,owner.yRotO,owner.getYRot())-.5);var look=owner.getViewVector(partial);
            end=end.add(-Math.cos(angle)*.1*side+look.x/5,owner.getEyeHeight()/2.6+look.y/5,-Math.sin(angle)*.1*side+look.z/5);
        }
        end=end.add(0,owner.getBbHeight()/2,0).subtract(start);float length=(float)end.length()*5;int steps=Math.max(2,(int)length);
        var points=new Vec3[steps+1];points[0]=Vec3.ZERO;points[steps]=end;
        for(int i=1;i<steps;i++){float dist=i*(length/steps),amp=grapple.amplitude*(1-i/(steps*1.25f));points[i]=end.scale(i/(double)steps).add(Mth.sin(dist/10)*amp,Mth.sin(dist/8)*amp,Mth.sin(dist/6)*amp);}
        var out=buffers.getBuffer(RenderType.entityCutoutNoCull(ROPE));
        for(int i=0;i<steps;i++){
            Vec3 tangent=points[i+1].subtract(points[i]).normalize();if(tangent.lengthSqr()<1.0E-10)continue;
            Vec3 axis=tangent.cross(Math.abs(tangent.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize(),other=tangent.cross(axis).normalize();
            for(int sideIndex=0;sideIndex<4;sideIndex++){double a=sideIndex*Math.PI/2,b=(sideIndex+1)*Math.PI/2;
                var na=axis.scale(Math.cos(a)).add(other.scale(Math.sin(a)));var nb=axis.scale(Math.cos(b)).add(other.scale(Math.sin(b)));
                float v1=(length-steps+i)*2,v2=v1+2,u1=sideIndex/4f,u2=(sideIndex+1)/4f;
                vertex(out,pose,points[i].add(na.scale(.025)),u1,v1,na,light,-1);vertex(out,pose,points[i+1].add(na.scale(.025)),u1,v2,na,light,-1);
                vertex(out,pose,points[i+1].add(nb.scale(.025)),u2,v2,nb,light,-1);vertex(out,pose,points[i].add(nb.scale(.025)),u2,v1,nb,light,-1);
            }
        }
    }
    private static void vertex(VertexConsumer out,PoseStack pose,Vec3 p,float u,float v,Vec3 normal,int light,int color){out.addVertex(pose.last().pose(),(float)p.x,(float)p.y,(float)p.z).setColor(color).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);}
    private static final class GrappleGlow extends RenderType {
        static final RenderType TYPE=create("thaumcraft_grapple_glow",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),true,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
        private GrappleGlow(){super("unused",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
    }
}
